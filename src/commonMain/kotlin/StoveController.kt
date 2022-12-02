import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


fun stoveController(): StoveController {
    val powerRelay = LowActiveGPIOElectricRelay("power-relay", 5)
    val openCloseRelay = LowActiveGPIOElectricRelay( "direction-relay", 6)
    val valve = ElectricValveController("air-intake-valve", powerRelay = powerRelay, openCloseRelay = openCloseRelay)
    //val fumes = TestTemperatureSensor("stove-thermometer").also { it.usefulPrecision = 0 }
    val fumes = MAX31855TemperaturSensor("stove-thermometer", 0).also { it.usefulPrecision = 0 }
    //val accumulator = EmptyTemperatureSensor("accumulator-thermometer")
    val accumulator = MAX31855TemperaturSensor("accumulator-thermometer", 1).also { it.usefulPrecision = 0 }
    val room = SHT31TemperaturSensor("room-thermometer", 1, 0x45).also { it.usefulPrecision = 1 }
    val outsideTemperatureSensor = DS18B20TempartureSensor("outside-thermometer", 0x1b9c071e64ff.toULong()).also { it.usefulPrecision = 0 }
    val buzzer = PassivePiezoBuzzerHardwarePWM("buzzer", 12)
    val openButton = PushButtonGPIO("open-button", 13)
    val closeButton = PushButtonGPIO("close-button", 26)
    val autoButton = PushButtonGPIO("auto-button", 19)
    val display = Display1602LCDI2C("display", 1, 0x27)
    val comm = DisplayAndBuzzerUserCommunication("user-communication", display, buzzer)
    return StoveController("stove", valve, fumes, accumulator, room, outsideTemperatureSensor, openButton, closeButton, autoButton, comm)
}

@Serializable
class StoveController(
    override val id: String,
    val valve: ElectricValveController,
    val fumes: TemperatureSensor,
    val accumulator: TemperatureSensor,
    val room: TemperatureSensor,
    val outside: TemperatureSensor,
    val openButton: PushButton,
    val closeButton: PushButton,
    val autoButton: PushButton,
    val userCommunication: BasicUserCommunication
) : Controller {

    @Transient
    val lastUserValveRate = PersistentStateWithTimestamp<Double?>(null)

    @Transient
    val autoModeController: AutoModeController = SlowlyCloseWhenCooling("auto-close", valve, fumes, lastUserValveRate)

    override suspend fun startControlling(): Boolean {
        // ??? what to do? should we start all here or assume they are started elsewhere?
        coroutineScope {
            launch { valve.startControlling() }
            launch { fumes.startSensing() }
            launch { accumulator.startSensing() }
            launch { room.startSensing() }
            launch { outside.startSensing() }
            launch { openButton.startSensing() }
            launch { closeButton.startSensing() }
            launch { autoButton.startSensing() }
            openButton.addOnClickListener { this.launch { openSome() } }
            closeButton.addOnClickListener { this.launch { closeSome() } }
            openButton.addOnLongClickListener { this.launch { open() } }
            closeButton.addOnLongClickListener { this.launch { close() } }
            autoButton.addOnClickListener { this.launch { auto() } }
            identifieables.filterIsInstance<WatcheableState>().forEach {
                it.addOnStateChange { launch { refreshDisplay() } }
            }
            awaitCancellation()
        }
        return true
    }

    override val devices: Set<Device>
        get() = setOf(fumes, accumulator, room, outside, openButton, closeButton, autoButton) + valve.devices + userCommunication.devices + autoModeController.devices

    override val identifieables: Set<Identifiable>
        get() = devices + autoModeController + valve

    fun stopControlling() {

    }

    suspend fun open() {
        userCommunication.acknowledge()
        lastUserValveRate.state = valve.state?.targetOrKnownOpenRate
        if(valve.open()) {
            userCommunication.acknowledge()
        }
    }
    suspend fun setOpenRateTo(rate: Double) {
        userCommunication.acknowledge()
        lastUserValveRate.state = valve.state?.targetOrKnownOpenRate
        if(valve.setOpenRateTo(rate)) {
            userCommunication.acknowledge()
        }
    }
    suspend fun openSome() {
        userCommunication.acknowledge()
        lastUserValveRate.state = valve.state?.targetOrKnownOpenRate
        if(valve.openMore()) {
            userCommunication.acknowledge()
        }
    }
    suspend fun closeSome() {
        userCommunication.acknowledge()
        lastUserValveRate.state = valve.state?.targetOrKnownOpenRate
        if(valve.closeMore()) {
            userCommunication.acknowledge()
        }
    }

    suspend fun close() {
        userCommunication.acknowledge()
        lastUserValveRate.state = valve.state?.targetOrKnownOpenRate
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

    suspend fun refreshDisplay() {
        (userCommunication as? StringDisplay)?.displayTable(stateDisplayString())
    }

    suspend fun stateDisplayString(): List<List<String>> {
        return listOf(listOf(room.stateMessage(), fumes.stateMessage(), accumulator.stateMessage()), listOf(/*outside.stateMessage(), */valve.stateMessage(), autoModeController.stateMessage())
        )
    }

}



