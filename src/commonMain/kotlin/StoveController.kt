import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration


fun stoveController(): StoveController {
    val powerRelay = LowActiveGPIOElectricRelay("power-relay", 5)
    val openCloseRelay = LowActiveGPIOElectricRelay( "direction-relay", 6)
    val valve = ElectricValveController("air-intake-valve", powerRelay = powerRelay, openCloseRelay = openCloseRelay)
    val fumes = MAX31855TemperaturSensor("stove-temperature", 0).also { it.usefulPrecision = 0 }
    val room = SHT31TemperaturSensor("room-temperature", 1, 0x45).also { it.usefulPrecision = 1 }
    val outsideTemperatureSensor = DS18B20TempartureSensor("outside-temperature", 0x1b9c071e64ff.toULong()).also { it.usefulPrecision = 0 }
    val buzzer = PassivePiezoBuzzerHardwarePWM("buzzer", 12)
    val openButton = PushButtonGPIO("open-button", 13)
    val closeButton = PushButtonGPIO("close-button", 26)
    val autoButton = PushButtonGPIO("auto-button", 19)
    val display = Display1602LCDI2C("display", 1, 0x27)
    return StoveController("stove", valve, fumes, room, outsideTemperatureSensor, openButton, closeButton, autoButton, DisplayAndBuzzerUserCommunication(display, buzzer))
}

@Serializable
class StoveController(
    override val id: String,
    val valve: ElectricValveController,
    val fumes: TemperatureSensor,
    val room: TemperatureSensor,
    val outside: TemperatureSensor,
    val openButton: PushButton,
    val closeButton: PushButton,
    val autoButton: PushButton,
    val userCommunication: BasicUserCommunication,
    val autoModeController: AutoModeController = CloseWhenCold("auto-close-when-cold", valve, fumes)
) : Controller {

    override suspend fun startControlling(): Boolean {
        // ??? what to do? should we start all here or assume they are started elsewhere?
        coroutineScope {
            launch { valve.startControlling() }
            launch { fumes.startSensing() }
            launch { room.startSensing() }
            launch { outside.startSensing() }
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
        get() = setOf(fumes, room, outside, openButton, closeButton, autoButton) + valve.devices + userCommunication.devices + autoModeController.devices

    override val identifieables: Set<Identifiable>
        get() = devices + autoModeController + valve

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

    suspend fun stateDisplayString(): List<List<String>> {
        return listOf(listOf(room.stateMessage(), fumes.stateMessage(), "??°"), listOf(outside.stateMessage(), valve.stateMessage(), "??%"))
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
class CloseWhenCold(override val id: String, override val valve: ElectricValveController, override val fumes: TemperatureSensor) : AutoModeController() {
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


