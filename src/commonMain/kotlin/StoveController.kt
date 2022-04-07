import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class StoveController(
    val valve: ElectricValveController,
    val fumes: TemperatureSensor,
    val room: TemperatureSensor,
    val openButton: PushButton,
    val closeButton: PushButton,
    val autoButton: PushButton
) : Controller {
    val autoModeController: AutoModeController
    init { // TODO ? move this into a start function (all controllers? interface?)
        autoModeController = CloseWhenCold(valve, fumes) // TODO make the choice of controller parametric?
    }

    override suspend fun startControlling() {
        // ??? what to do? should we start all here or assume they are started elsewhere?
        coroutineScope {
            launch { valve.startControlling() }
            launch { fumes.startSensing() }
            launch { room.startSensing() }
            launch { openButton.startSensing() }
            launch { closeButton.startSensing() }
            launch { autoButton.startSensing() }
            openButton.addOnClickListener { this.launch { open() } }
            closeButton.addOnClickListener { this.launch { close() } }
            autoButton.addOnClickListener { this.launch { auto() } }
            while(true) delay(100000) // TODO: find something better? wait until stopControlling is called?
        }
    }

    fun stopControlling() {

    }

    suspend fun open() {
        valve.open()
    }

    suspend fun close() {
        valve.close()
    }

    suspend fun auto() {
        autoModeController.startControlling()
    }

    suspend fun stateMessage(): String {
        return "${valve.stateMessage()} ${fumes.stateMessage()} ${room.stateMessage()}"
    }

    // TODO generalize this? be creative here maybe these business objects should be part of the state of the controller, being updated permanently??
    // TODO Also, put these validity periods in one place, please!
    suspend fun toStove(): Stove {
        return Stove(
            fumes.maybeCurrentValue(30.toDuration(DurationUnit.SECONDS)),
            Room(room.maybeCurrentValue(30.toDuration(DurationUnit.SECONDS))),
            Valve(valve.state)
        )
    }
}


sealed class AutoModeController : Controller {
    abstract val valve: ElectricValveController
    abstract val fumes: TemperatureSensor
}

class CloseWhenCold(override val valve: ElectricValveController, override val fumes: TemperatureSensor) : AutoModeController() {
    override suspend fun startControlling() {
        when(valve.state) {
            ValveState.open -> {
                if(fumes.currentValue(10.toDuration(DurationUnit.SECONDS)) > 300.0) {// TODO parametric
                    fumes.waitForCurrentValueCondition(10.toDuration(DurationUnit.SECONDS)) { it < 200.0 } // TODO parametric
                    valve.close()
                } else {
                    // TODO state change? nothing done, this is a dumb controller, please launch it again when stove is hot.
                }
            }
            else -> { } // TODO state change? nothing done, this is a dumb controller, please launch it again when stove open and hot.
        }
    }
}

