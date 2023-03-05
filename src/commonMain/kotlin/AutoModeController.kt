import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val logger = KotlinLogging.logger {}

@Serializable
sealed class AutoModeController : Controller {
    abstract val valve: ElectricValveController
    abstract val fumes: TemperatureSensor
    abstract suspend fun stateMessage(): String
    abstract var enabled: Boolean
}



@Serializable
class SlowlyCloseWhenCooling(
    override val id: String,
    override val valve: ElectricValveController,
    override val fumes: TemperatureSensor,
    val daFumes: StateFlow<InstantValue<Double>>,
    val lastUserValveRate: PersistentStateWithTimestamp<Double?>,
    val lastTimeRecharged: PersistentStateWithTimestamp<Unit>
) : AutoModeController(), Sampleable {

    override var enabled = true

    override suspend fun startControlling(): Boolean {

        coroutineScope {

            while(true) {
                if(enabled) {
                    launch {
                        openRate()?.let {
                            if (it != lastOpenRate) {
                                logger.info { "Change valve to $it because: $fuzzyOpenRate" }
                                lastOpenRate = it
                                valve.setOpenRateTo(it)
                            }
                        }
                    }
                }
                delay(30.toDuration(DurationUnit.SECONDS))
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
        val daFumesState = daFumes.toState().map { it.value }

        val burningHot = FunctionOverStateFuzzyAtom("burning hot", (250.0 .. Double.POSITIVE_INFINITY).fuzzy(20.0), fumesState)
        val hot = FunctionOverStateFuzzyAtom("hot", FuzzyTrueInRange(150.0, 250.0, 20.0), fumesState)
        val warm = FunctionOverStateFuzzyAtom("warm", FuzzyTrueInRange(50.0, 150.0, 20.0), fumesState)
        val cold = FunctionOverStateFuzzyAtom("cold", FuzzyTrueUntil(50.0, 20.0), fumesState)

        val cooling = FunctionOverStateFuzzyAtom("cooling", FuzzyTrueUntil(-20.0, 10.0), daFumesState)
        val fastCooling = FunctionOverStateFuzzyAtom("cooling fast", FuzzyTrueUntil(-100.0, 20.0), daFumesState)
        val gentleCooling = FunctionOverStateFuzzyAtom("gentle cooling", FuzzyTrueInRange(-100.0, -20.0, 10.0), daFumesState)
        val stable = FunctionOverStateFuzzyAtom("stable", FuzzyTrueInRange(-20.0, 20.0, 10.0), daFumesState)
        val warming = FunctionOverStateFuzzyAtom("warming", FuzzyTrueFrom(20.0, 10.0), daFumesState) // > 20°/h  +/- 10


        val recharging = FunctionOverStateFuzzyAtom("recharged", FuzzyTrueUntilDuration(30.toDuration(DurationUnit.MINUTES), 10.toDuration(DurationUnit.MINUTES)), lastTimeRecharged.timeSinceLastChange)

        val userRecentlyChangedOpenRate = FunctionOverStateFuzzyAtom("user said so", TrueUntilDuration(10.toDuration(DurationUnit.MINUTES)), lastUserValveRate.timeSinceLastChange)
        //val userRecentlyChangedOpenRate = AlwaysFalseCondition

        val ignition = recharging or warming
        val fullFire = burningHot
        val dyingFlames = hot and fastCooling and not(recharging)
        val embers = hot and (stable or gentleCooling) and not(recharging)
        val discharging = warm and (stable or gentleCooling) and not(recharging)
        val idle = cold and stable and not(recharging)

        val tempBasedState = fumesState.apply(LinearFunction(Pair(250.0, 0.5), Pair(120.0, 0.0))).map { min(max(it, 0.0), 0.5) }
        val dyingFlamesTempBasedState = fumesState.apply(LinearFunction(Pair(250.0, 0.9), Pair(180.0, 0.6))).map { min(max(it, 0.6), 0.9) }

        val ignitionSpeedBasedState = daFumesState.apply(LinearFunction(Pair(0.0, 0.3), Pair(200.0, 1.0)))
            .map { max(it, tempBasedState.state) }
            .map { min(max(it, 0.3), 1.0) }

        val r1 = ignition and not(fullFire) implies ignitionSpeedBasedState //1.0 // implies ValveOpenRate(1.0)
        val r2 = fullFire and not(userRecentlyChangedOpenRate) implies 1.0
        val r2b = dyingFlames and not(userRecentlyChangedOpenRate) implies dyingFlamesTempBasedState
        val r3 = embers and not(userRecentlyChangedOpenRate) implies tempBasedState
        val r4 = (discharging or idle) and not(userRecentlyChangedOpenRate) implies 0.0
        val r5 = not(fullFire) and userRecentlyChangedOpenRate implies lastUserValveRate.map { it ?: 1.0 }
        val r5b = fullFire and userRecentlyChangedOpenRate implies lastUserValveRate.map { it ?: 1.0 }.map { max(it, 0.4) }

        or(listOf(r1, r2, r2b, r3, r4, r5, r5b))
    }


    fun openRate(): Double? {
        return fuzzyOpenRate.state.let {
            if(it.confidence >= 0.5) {
                val rounded = (it.value * 10.0).roundToInt() / 10.0
                return rounded
            } else {
                return lastOpenRate
            }
        }
    }

    override val devices: Set<Device>
        get() = setOf(fumes) + valve.devices

    override fun sample(validityPeriod: Duration): Map<String, SampleValue> {
        return daFumes?.currentValueOrNull(validityPeriod)?.let { mapOf("fumes-evolution" to SampleDoubleValue(it)) } ?: emptyMap()
    }

    override suspend fun stateMessage(): String {
        val df = (daFumes?.currentValueOrNull(1.0.minutes)?.let { it.toString(0) + "°/h" } ?: "")
        val message: String
        if(enabled) {
            message = " ON"
        } else {
            message = openRate()?.let { (it * 100.0).toString(0) } ?: "?"
        }
        return df + message
    }
}