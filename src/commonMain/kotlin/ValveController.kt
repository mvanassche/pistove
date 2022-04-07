import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

enum class ValveState { open, opening, closing, closed }

class ElectricValveController(val powerRelay: ElectricRelay, val openCloseRelay: ElectricRelay) : State<ValveState?>, Controller {

    val timeout = 160.seconds // TODO persistent parameter.

    val pstate: PersistentState<ValveState?> = PersistentState(null)
    override var state: ValveState?
        get() = pstate.state
        set(value) { pstate.state = value }

    suspend fun open(): Boolean {
        state = ValveState.opening
        openCloseRelay.deactivate()
        powerRelay.activate()
        delay(timeout)
        //synchronized(this) { // TODO synchronized
            if(state == ValveState.opening) {
                state = ValveState.open
                powerRelay.deactivate() // optional ?
                return true
            } else {
                return false
            }
        //}
    }

    suspend fun close(): Boolean {
        state = ValveState.closing
        openCloseRelay.activate()
        powerRelay.activate()
        delay(timeout)
        //synchronized(this) { // TODO synchronized
            if(state == ValveState.closing) {
                state = ValveState.closed
                powerRelay.deactivate() // optional ?
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

    suspend fun stateMessage(): String {
        return state.toString()
    }

}
