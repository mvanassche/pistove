import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration


fun stoveController(): StoveController {
    val powerRelay = LowActiveGPIOElectricRelay("power-relay", 5)
    val openCloseRelay = LowActiveGPIOElectricRelay( "direction-relay", 6)
    val valve = ElectricValveController(powerRelay = powerRelay, openCloseRelay = openCloseRelay)
    val fumes = MAX31855TemperaturSensor("stove-temperature", 0)
    val room = SHT31TemperaturSensor("room-temperature", 1, 0x45)
    val buzzer = PassivePiezoBuzzerHardwarePWM("buzzer", 12)
    val openButton = PushButtonGPIO("open", 13)
    val closeButton = PushButtonGPIO("close", 26)
    val autoButton = PushButtonGPIO("auto", 19)
    val display = Display1602LCDI2C("display", 1, 0x27)
    val autoModeController: AutoModeController = CloseWhenCold(valve, fumes)
    return StoveController(valve, fumes, room, openButton, closeButton, autoButton, DisplayAndBuzzerUserCommunication(display, buzzer), autoModeController)
}

@Serializable
class StoveController constructor(
    val valve: ElectricValveController,
    val fumes: TemperatureSensor,
    val room: TemperatureSensor,
    val openButton: PushButton,
    val closeButton: PushButton,
    val autoButton: PushButton,
    val userCommunication: BasicUserCommunication,
    val autoModeController: AutoModeController = CloseWhenCold(valve, fumes)
) : Controller {

    override suspend fun startControlling(): Boolean {
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
            awaitCancellation()
        }
        return true
    }

    override val devices: Set<Device>
        get() = setOf(fumes, room, openButton, closeButton, autoButton) + valve.devices + userCommunication.devices + autoModeController.devices

    fun stopControlling() {

    }

    suspend fun open() {
        userCommunication.acknowledge()
        if(valve.open()) {
            userCommunication.acknowledge()
        }
    }

    suspend fun close() {
        userCommunication.acknowledge()
        if(valve.close()) {
            userCommunication.acknowledge()
        }
    }

    suspend fun auto() {
        userCommunication.acknowledge() // TODO only ackownledge when it is engaged for real!!?? return on startControlling!
        if(autoModeController.startControlling()) {
            userCommunication.acknowledge()
        } else {
            userCommunication.alert()
        }
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

@Serializable
sealed class AutoModeController : Controller {
    abstract val valve: ElectricValveController
    abstract val fumes: TemperatureSensor
}

@Serializable
class CloseWhenCold(override val valve: ElectricValveController, override val fumes: TemperatureSensor) : AutoModeController() {
    override suspend fun startControlling(): Boolean {
        when(valve.state) {
            ValveState.open -> {
                if(fumes.currentValue(10.toDuration(DurationUnit.SECONDS)) > 300.0) {// TODO parametric
                    fumes.waitForCurrentValueCondition(10.toDuration(DurationUnit.SECONDS)) { it < 200.0 } // TODO parametric
                    return valve.close()
                } else {
                    // TODO state change? nothing done, this is a dumb controller, please launch it again when stove is hot.
                }
            }
            else -> { } // TODO state change? nothing done, this is a dumb controller, please launch it again when stove open and hot.
        }
        return false
    }

    override val devices: Set<Device>
        get() = setOf(fumes) + valve.devices
}


