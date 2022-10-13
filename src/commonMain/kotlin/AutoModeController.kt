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


    var lastOpenRate: Double? = null

    fun openRate(t: Double, dt: Double): Double {
        val rate =
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
                                val temperaturBased = (0.7 / (hotLowerBound - coldUpperBound)) * (coldUpperBound - t) // linear between coldUpperBound -> 0 and hotLowerBound -> 0.7
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
        return rate
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