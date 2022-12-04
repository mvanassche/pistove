import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
sealed class AutoModeController : Controller {
    abstract val valve: ElectricValveController
    abstract val fumes: TemperatureSensor
    abstract suspend fun stateMessage(): String
}



@Serializable
class SlowlyCloseWhenCooling(
    override val id: String,
    override val valve: ElectricValveController,
    override val fumes: TemperatureSensor,
    val lastUserValveRate: PersistentStateWithTimestamp<Double?>
) : AutoModeController(), Sampleable {

    @Transient
    var daFumes: StateFlow<InstantValue<Double>>? = null

    override suspend fun startControlling(): Boolean {

        val fumes = fumes.windowed(30.0.seconds)
            .aggregate { it.average() }
            .sampleInstantValue(30.0.seconds)

        // average fumes before  deriving for less noise?
        val dFumes = fumes.zipWithNext().map {
            if (it.second.time > it.first.time) {
                InstantValue(
                    (it.second.value - it.first.value) / ((it.second.time.toEpochMilliseconds() - it.first.time.toEpochMilliseconds()).toDouble() / 3600000.0), // in °/h
                    Instant.fromEpochMilliseconds((it.first.time.toEpochMilliseconds() + it.second.time.toEpochMilliseconds()) / 2)
                )
            } else {
                InstantValue(0.0, it.second.time)
            }
        }

        coroutineScope {
            launch {
                daFumes = dFumes.windowed(5.0.minutes).map {
                    InstantValue(it.map { it.value }.average(), it.last().time)
                }.stateIn(this)

                //daFumes.zipWithNext()
            }
        }

        awaitCancellation()
        return true
    }

    /*
                      t'°/h
                  ▲                     │
                  │  HEATING UP         │
               100├────────┬────────────┤
                  │        │ RECHARGING │
            F     │        │            │
            R   50│        ├────────────┤
            E     │        │            │
            E     │  C     │   STABLE   │
            Z  -30│  O     ├────────────┤  H
            I     │  L     │  FLAMELESS │  O
            N     │  D     │  COOLING   │  T
            G -100│        ├────────────┤
                  │        │  FLAME     │
                  │        │  COOLING   │
                  │        │            │    t°
                  └────────┴────────────┴─────►
                  0       100           250
     */
    val burningHotLowerBound = 250.0 // °
    val hotLowerBound = 150.0 // °
    val coldUpperBound = 50.0 // °
    val heatingUpLowerBound = 100.0 // °/h
    val flameCoolingUpperBound = -175.0 // °/h
    val flamelessCoolingUpperBound = -20.0 // °/h
    val rechargingLowerBound = 50.0 // °/h

    @Transient
    val temperatureBasedFunction = LinearFunction(Pair(250.0, 0.5), Pair(100.0, 0.0))


    var lastOpenRate: Double? = null


    @Transient
    val fuzzyOpenRate: FuzzyPredicate<Double> by lazy {
        val fumesState = fumes.map { it.value } // FIXME Warning, dropping the time component is dangerous...
        val daFumesState = daFumes!!.toState().map { it.value }

        val burningHot = FunctionOverStateFuzzyAtom(FuzzyTrueFrom(250.0, 20.0), fumesState)
        val hot = FunctionOverStateFuzzyAtom(FuzzyTrueInRange(150.0, 250.0, 20.0), fumesState)
        val warm = FunctionOverStateFuzzyAtom(FuzzyTrueInRange(50.0, 150.0, 20.0), fumesState)
        val cold = FunctionOverStateFuzzyAtom(FuzzyTrueUntil(50.0, 20.0), fumesState)

        val cooling = FunctionOverStateFuzzyAtom(FuzzyTrueUntil(-20.0, 10.0), daFumesState)
        val fastCooling = FunctionOverStateFuzzyAtom(FuzzyTrueUntil(-100.0, 20.0), daFumesState)
        val gentleCooling = FunctionOverStateFuzzyAtom(FuzzyTrueInRange(-100.0, -20.0, 10.0), daFumesState)
        val stable = FunctionOverStateFuzzyAtom(FuzzyTrueInRange(-20.0, 20.0, 10.0), daFumesState)
        val warming = FunctionOverStateFuzzyAtom(FuzzyTrueFrom(20.0, 10.0), daFumesState) // > 20°/h  +/- 10


        val recharging = FunctionOverStateFuzzyAtom(FuzzyTrueUntilDuration(30.toDuration(DurationUnit.MINUTES), 10.toDuration(DurationUnit.MINUTES)), PersistentState(1000.toDuration(DurationUnit.HOURS))) // TODO FIXME

        // FIXME Until not really connected, disable user rule
        //val userRecentlyChangedOpenRate = FunctionOverStateFuzzyAtom(TrueUntilDuration(10.toDuration(DurationUnit.MINUTES)), lastUserValveRate.timeSinceLastChange)
        val userRecentlyChangedOpenRate = AlwaysFalseCondition

        val ignition = recharging or warming
        val fullFire = burningHot
        val dyingFlames = hot and fastCooling and not(recharging)
        val embers = hot and (stable or gentleCooling) and not(recharging)
        val discharging = warm and (stable or gentleCooling) and not(recharging)
        val idle = cold and stable and not(recharging)

        val tempBasedState = fumesState.apply(LinearFunction(Pair(250.0, 0.5), Pair(100.0, 0.0))).map { min(max(it, 0.0), 0.5) }
        val dyingFlamesTempBasedState = fumesState.apply(LinearFunction(Pair(250.0, 0.9), Pair(180.0, 0.6))).map { min(max(it, 0.6), 0.9) }

        val r1 = ignition implies 1.0 // implies ValveOpenRate(1.0)
        val r2 = fullFire implies 1.0
        val r2b = dyingFlames and not(userRecentlyChangedOpenRate) implies dyingFlamesTempBasedState
        val r3 = embers and not(userRecentlyChangedOpenRate) implies tempBasedState
        val r4 = (discharging or idle) and not(userRecentlyChangedOpenRate) implies 0.0
        val r5 = userRecentlyChangedOpenRate implies lastUserValveRate.map { it ?: 1.0 }

        or(listOf(r1, r2, r2b, r3, r4, r5))
    }


    fun openRate(t: Double, dt: Double): Double {
        return fuzzyOpenRate.state.let {
            if(it.confidence >= 0.5) {
                lastOpenRate = it.value
                return it.value
            } else {
                return lastOpenRate ?: 1.0
            }
        }
    }

    override val devices: Set<Device>
        get() = setOf(fumes) + valve.devices

    override fun sample(validityPeriod: Duration): Map<String, SampleValue> {
        return daFumes?.currentValueOrNull(validityPeriod)?.let { mapOf("fumes-evolution" to SampleDoubleValue(it)) } ?: emptyMap()
    }

    override suspend fun stateMessage(): String {
        return (daFumes?.currentValueOrNull(1.0.minutes)?.let {
            it.toString(0) + "°/h" + (openRate(fumes.state.value, it) * 100.0).toString(0)
        } ?: "")
    }
}