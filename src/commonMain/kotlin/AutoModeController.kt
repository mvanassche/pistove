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
class CloseWhenCold(override val id: String, override val valve: ElectricValveController, override val fumes: TemperatureSensor) : AutoModeController() {
    override suspend fun stateMessage() = ""

    override suspend fun startControlling(): Boolean {
        if(valve.isClosed() == false) {
            if(fumes.currentValue(10.toDuration(DurationUnit.SECONDS)) > 200.0) {// TODO parametric
                fumes.waitForCurrentValueCondition(10.toDuration(DurationUnit.SECONDS)) { it < 120.0 } // TODO parametric
                return valve.close()
            } else {
                // TODO state change? nothing done, this is a dumb controller, please launch it again when stove is hot.
            }
        }
        return false
    }

    override val devices: Set<Device>
        get() = setOf(fumes) + valve.devices

}



@Serializable
class SlowlyCloseWhenCooling(override val id: String, override val valve: ElectricValveController, override val fumes: TemperatureSensor) : AutoModeController(), Sampleable {

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
    val hotLowerBound = 250.0 // °
    val heatingUpLowerBound = 100.0 // °/h
    val coldUpperBound = 100.0 // °
    val flameCoolingUpperBound = -175.0 // °/h
    val flamelessCoolingUpperBound = -20.0 // °/h
    val rechargingLowerBound = 50.0 // °/h

    @Transient
    val temperatureBasedFunction = LinearFunction(Pair(hotLowerBound, 0.6), Pair(coldUpperBound, 0.0))


    var lastOpenRate: Double? = null


    @Transient
    val fuzzyOpenRate: FuzzyDoubleDisjunction by lazy {
        val fumesState = fumes.map { it.value } // FIXME Warning, dropping the time component is dangerous...
        val daFumesState = daFumes!!.toState().map { it.value }

        val burningHot = FunctionOverStateFuzzyAtom(TrueFrom(250.0, 20.0), fumesState)
        val hot = FunctionOverStateFuzzyAtom(TrueInRange(150.0, 250.0, 20.0), fumesState)
        val warm = FunctionOverStateFuzzyAtom(TrueInRange(50.0, 150.0, 20.0), fumesState)
        val cold = FunctionOverStateFuzzyAtom(TrueUntil(50.0, 20.0), fumesState)

        val cooling = FunctionOverStateFuzzyAtom(TrueUntil(-20.0, 10.0), daFumesState)
        val gentleCooling = FunctionOverStateFuzzyAtom(TrueInRange(-100.0, -20.0, 10.0), daFumesState)
        val stable = FunctionOverStateFuzzyAtom(TrueInRange(-20.0, 20.0, 10.0), daFumesState)
        val warming = FunctionOverStateFuzzyAtom(TrueFrom(20.0, 10.0), daFumesState)

        val recharging = FunctionOverStateFuzzyAtom(TrueUntilDuration(30.toDuration(DurationUnit.MINUTES), 10.toDuration(DurationUnit.MINUTES)), PersistentState(1000.toDuration(DurationUnit.HOURS))) // TODO FIXME

        val ignition = recharging or warming
        val fullFire = burningHot
        val embers = hot and (stable or gentleCooling) and not(recharging)
        val discharging = warm and (stable or gentleCooling) and not(recharging)
        val idle = cold and stable and not(recharging)

        val tempBasedState = fumesState.apply(temperatureBasedFunction).map { min(max(it, 0.0), 0.5) }

        val r1 = ignition implies 1.0
        val r2 = fullFire implies 1.0
        val r3 = embers implies tempBasedState
        val r4 = (discharging or idle) implies 0.0

        or(listOf(r1, r2, r3, r4))
    }


    fun openRate(t: Double, dt: Double): Double {
        /*val rate =
            if(t > hotLowerBound) {
                1.0
            } else {
                if(dt > heatingUpLowerBound) {
                    1.0
                } else {
                    if(t < coldUpperBound) {
                        0.0
                    } else {
                        when {
                            dt < flameCoolingUpperBound -> max(0.0, min(0.9, max(0.2, lastOpenRate ?: 1.0))) // max 0.2 and last rate, as in if it is cooling too fast, it means not enough air
                            dt in flameCoolingUpperBound..flamelessCoolingUpperBound -> {
                                //val coolingSpeedBased = max(0.0, min(1.0,(0.5 / (flameCoolingUpperBound - flamelessCoolingUpperBound)) * (dt - flamelessCoolingUpperBound)))
                                val temperaturBased = temperatureBasedFunction.value(t) // linear between coldUpperBound -> 0 and hotLowerBound -> 0.7
                                max(0.0, min(lastOpenRate ?: 1.0, temperaturBased))
                            }
                            dt in flamelessCoolingUpperBound..rechargingLowerBound -> 0.0
                            dt > rechargingLowerBound -> 0.5 // TODO function
                            else -> 1.0 // should not occur, I believe the when is exhaustive
                        }
                    }
                }
            }
        lastOpenRate = rate
        return rate*/
        return fuzzyOpenRate.state
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