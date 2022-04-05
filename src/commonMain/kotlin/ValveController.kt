import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

enum class ValveState { open, opening, closing, closed }

class ElectricValveController(val openRelay: ElectricRelay, val closeRelay: ElectricRelay) : State<ValveState?>, Controller {

    val timeout = 160.seconds // TODO persistent parameter.

    val pstate: PersistentState<ValveState?> = PersistentState(null)
    override var state: ValveState?
        get() = pstate.state
        set(value) { pstate.state = value }

    suspend fun open() {
        state = ValveState.opening
        closeRelay.openCircuit()
        openRelay.closeCircuit()
        delay(timeout)
        //synchronized(this) { // TODO synchronized
            if(state == ValveState.opening) {
                state = ValveState.open
                openRelay.openCircuit() // optional ?
            }
        //}
    }

    suspend fun close() {
        state = ValveState.closing
        openRelay.openCircuit()
        closeRelay.closeCircuit()
        delay(timeout)
        //synchronized(this) { // TODO synchronized
            if(state == ValveState.closing) {
                state = ValveState.closed
                closeRelay.openCircuit() // optional ?
            }
        //}
    }


    override suspend fun startControlling() {
        // TODO start() check the state, and activate/deactivate depending on it for consistency with physical state: if closing, call close, if opening, call open
    }

    suspend fun stateMessage(): String {
        return state.toString()
    }

}
