import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

enum class ValveState { open, opening, closing, closed }

class ElectricValveController(val openRelay: ElectricRelay, val closeRelay: ElectricRelay) : State<ValveState?>, Controller {

    val pstate: PersistentState<ValveState?> = PersistentState(null)
    override var state: ValveState?
        get() = pstate.state
        set(value) { pstate.state = value }

    suspend fun open() {
        state = ValveState.opening
        closeRelay.deactivate()
        openRelay.activate()
        delay(5.seconds) // TODO parameter
        //synchronized(this) { // TODO synchronized
            if(state == ValveState.opening) {
                state = ValveState.open
                openRelay.deactivate() // optional ?
            }
        //}
    }

    suspend fun close() {
        state = ValveState.closing
        openRelay.deactivate()
        closeRelay.activate()
        delay(5.seconds) // TODO parameter
        //synchronized(this) { // TODO synchronized
            if(state == ValveState.closing) {
                state = ValveState.closed
                closeRelay.deactivate() // optional ?
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
