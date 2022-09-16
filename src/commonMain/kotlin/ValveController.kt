import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class ValveState { open, opening, closing, closed }

@Serializable
class ElectricValveController(override val id: String, val powerRelay: ElectricRelay, val openCloseRelay: ElectricRelay) : State<ValveState?>, Controller {

    @Transient
    val timeout = 160.seconds // TODO persistent parameter.

    /*val pstate: PersistentState<ValveState?> = PersistentState(null)
    override var state: ValveState?
        get() = pstate.state
        set(value) { pstate.state = value }*/

    override var state: ValveState? = null


    suspend fun open(): Boolean {
        state = ValveState.opening
        openCloseRelay.state = RelayState.inactive
        powerRelay.state = RelayState.activated
        delay(timeout)
        //synchronized(this) { // TODO synchronized
            if(state == ValveState.opening) {
                state = ValveState.open
                powerRelay.state = RelayState.inactive // optional ?
                return true
            } else {
                return false
            }
        //}
    }

    suspend fun close(): Boolean {
        state = ValveState.closing
        openCloseRelay.state = RelayState.activated
        powerRelay.state = RelayState.activated
        delay(timeout)
        //synchronized(this) { // TODO synchronized
            if(state == ValveState.closing) {
                state = ValveState.closed
                powerRelay.state = RelayState.inactive // optional ?
                return true
            } else {
                return false
            }
        //}
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
