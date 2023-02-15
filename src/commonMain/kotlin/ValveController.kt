@file:UseSerializers(DurationSerializer::class) // https://github.com/Kotlin/kotlinx.serialization/issues/1472
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

@Serializable
sealed interface ValveState {
    val nominalRate: Double
}

@Serializable
sealed interface KnownValveState : ValveState {
    val openRate: Double
}

@Serializable
sealed interface MovingValveState : ValveState {
    val targetOpenRate: Double
    override val nominalRate: Double
        get() = targetOpenRate
}

@Serializable
sealed interface MovingFromValveState : MovingValveState, KnownValveState

@Serializable
class OpeningValveState(val timeForFullMotion: Duration, val startOpenRate: Double, override val targetOpenRate: Double,
                        val startedAt: Instant = Clock.System.now()) : MovingFromValveState {
    override val openRate: Double
        get() = min(1.0, startOpenRate + (Clock.System.now() - startedAt).div(timeForFullMotion))
    override fun toString(): String {
        return "${(openRate * 100.0).roundToInt()}>${(targetOpenRate * 100.0).roundToInt()}%"
    }
}

@Serializable
class ClosingValveState(val timeForFullMotion: Duration, val startOpenRate: Double, override val targetOpenRate: Double,
                        val startedAt: Instant = Clock.System.now()) : MovingFromValveState {
    override val openRate: Double
        get() = max(0.0, startOpenRate - (Clock.System.now() - startedAt).div(timeForFullMotion))
    override fun toString(): String {
        return "${(openRate * 100.0).roundToInt()}>${(targetOpenRate * 100.0).roundToInt()}%"
    }
}

@Serializable
class ResetOpeningValveState(override val targetOpenRate: Double, val startedAt: Instant = Clock.System.now()) : MovingValveState {
    override fun toString(): String {
        return "?>${(targetOpenRate * 100.0).roundToInt()}%"
    }
}


@Serializable
class NotMovingValveState(override val openRate: Double) : KnownValveState {
    override val nominalRate: Double
        get() = openRate
    override fun toString(): String {
        return when(openRate) {
            0.0 -> "closed"
            1.0 -> "open"
            else -> "${(openRate * 100.0).roundToInt()}%"
        }
    }
}

interface ValveController : State<ValveState?>, Controller, Sampleable {
    val openRateOrTarget: Double? // TODO rename

    suspend fun setOpenRateTo(targetOpenRate0: Double): Boolean

    suspend fun open(): Boolean {
        return setOpenRateTo(1.0)
    }

    suspend fun openMore(): Boolean {
        return setOpenRateTo((openRateOrTarget ?: 0.0) + 0.1)
    }

    suspend fun close(): Boolean {
        return setOpenRateTo(0.0)
    }

    suspend fun closeMore(): Boolean {
        return setOpenRateTo((openRateOrTarget ?: 1.0) - 0.1)
    }

    override fun sample(validityPeriod: Duration): Map<String, SampleValue> {
        return state?.let {
            mapOf("state" to SampleStringValue(it.toString())) +
                    ((it as? KnownValveState)?.let { mapOf("open-rate" to SampleDoubleValue(it.openRate)) } ?: emptyMap())
        } ?: emptyMap()
    }
}

@Serializable
open class ElectricValveController(override val id: String, val powerRelay: ElectricRelay, val openCloseRelay: ElectricRelay)
    : ValveController, WatcheableState {

    open val timeForFullMotion = 150.seconds

    open val extraTimeForSafety = 10.seconds

    open fun isClosed() = state?.let { (it as? NotMovingValveState)?.openRate?.let { it == 0.0 } }

    open fun isMoving() = state?.let { it is MovingValveState }

    /*val pstate: PersistentState<ValveState?> = PersistentState(null)
    override var state: ValveState?
        get() = pstate.state
        set(value) { pstate.state = value }*/

    override var state: ValveState? = null
        set(value) {
            field = value
            listeners.forEach { it.invoke() }
        }

    @Transient
    override val listeners = mutableListOf<() -> Unit>()

    val openRate: Double? get() = (state as? KnownValveState)?.openRate
    override val openRateOrTarget: Double? get() = (state as? MovingValveState)?.targetOpenRate ?: openRate


    @Transient
    val mutex = Mutex()

    protected fun startOpen() {
        //println("start open")
        openCloseRelay.state = RelayState.inactive
        powerRelay.state = RelayState.activated
    }
    protected fun startClose() {
        //println("start close")
        openCloseRelay.state = RelayState.activated
        powerRelay.state = RelayState.activated
    }
    protected fun stopMoving() {
        openCloseRelay.state = RelayState.inactive // optional
        powerRelay.state = RelayState.inactive
    }



    override suspend fun setOpenRateTo(targetOpenRate0: Double): Boolean {
        // if not moving, just go for it, wait, then recheck the target?
        // if the state is reset -> just change the rate, wait, check if the target still holds, if it does stop
        // if moving, change target (and direction) wait, check if the target still holds, if it does stop

        val targetOpenRate = min(1.0, max(0.0, targetOpenRate0))

        // phase 1 wait from unknown position
        mutex.withLock {
            val state = this.state
            when(state) {
                is KnownValveState -> null
                is ResetOpeningValveState -> {
                    this.state = ResetOpeningValveState(targetOpenRate)
                    timeForFullMotion + extraTimeForSafety - (state.startedAt - Clock.System.now()) // subtract the time already passed
                }
                null -> {
                    startOpen()
                    this.state = ResetOpeningValveState(targetOpenRate)
                    timeForFullMotion + extraTimeForSafety
                }
            }
        }?.let {
            //println("wait for reset to be done $state")
            delay(it)
        }
        mutex.withLock {
            if(this.state is ResetOpeningValveState) {
                this.state = NotMovingValveState(1.0)
            }
        }

        // phase 2 wait until target
        mutex.withLock {
            val state = this.state
            when(state) {
                is KnownValveState -> {
                    if (targetOpenRate > state.openRate) {
                        startOpen()
                        this.state = OpeningValveState(timeForFullMotion, state.openRate, targetOpenRate)
                    } else {
                        startClose()
                        this.state = ClosingValveState(timeForFullMotion, state.openRate, targetOpenRate)
                    }
                    val extraTime = if (targetOpenRate == 0.0 || targetOpenRate == 1.0) extraTimeForSafety else ZERO
                    timeForFullMotion * (targetOpenRate - state.openRate).absoluteValue + extraTime
                }
                else -> null
            }
        }?.let {
            delay(it)
        }
        return mutex.withLock {
            val state = this.state
            if(state is MovingValveState && state.targetOpenRate == targetOpenRate) {
                stopMoving()
                this.state = NotMovingValveState((state as? KnownValveState)?.openRate ?: targetOpenRate)
                true
            } else {
                false
            }
        }
    }

    suspend fun delay(time: Duration) {
        //println("delay $time in state $state")
        kotlinx.coroutines.delay(time)
    }


    override suspend fun startControlling(): Boolean {
        // TODO start() check the state, and activate/deactivate depending on it for consistency with physical state: if closing, call close, if opening, call open
        return true
    }

    override val devices: Set<Device>
        get() = setOf(powerRelay, openCloseRelay)

    suspend fun stateMessage(): String {
        return state?.toString() ?: "?"
    }


}
