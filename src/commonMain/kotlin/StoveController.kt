import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import pistove.status.physical.Environment
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


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
    val rechargeButton = PushButtonGPIO("recharge-button", 19)
    val display = Display1602LCDI2C("display", 1, 0x27)
    val comm = DisplayAndBuzzerUserCommunication("user-communication", display, buzzer)
    return StoveController("stove", valve, fumes, accumulator, room, outsideTemperatureSensor, openButton, closeButton, rechargeButton, comm)
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
    val rechargeButton: PushButton,
    val userCommunication: BasicUserCommunication
) : Controller {

    val accumulatorLowTemperature = 30.0
    val accumulatorHighTemperature = 130.0

    @Transient
    val lastUserValveRate = PersistentStateWithTimestamp<Double?>(null)

    @Transient
    val lastTimeRecharged = PersistentStateWithTimestamp<Unit>(Unit)

    @Transient
    var daFumes: StateFlow<InstantValue<Double>>? = null

    @Transient
    var chargedRate: StateFlow<InstantValue<Double>>? = null

    @Transient
    var autoModeController: AutoModeController? = null

    override suspend fun startControlling(): Boolean {
        // ??? what to do? should we start all here or assume they are started elsewhere?
        coroutineScope {

            val fumesSmoothed = fumes.windowed(30.0.seconds)
                .aggregate { it.average() }
                .sampleInstantValue(30.0.seconds)

            // average fumes before  deriving for less noise?
            val dFumes = fumesSmoothed.zipWithNext().map {
                if (it.second.time > it.first.time) {
                    InstantValue(
                        (it.second.value - it.first.value) / ((it.second.time.toEpochMilliseconds() - it.first.time.toEpochMilliseconds()).toDouble() / 3600000.0), // in °/h
                        Instant.fromEpochMilliseconds((it.first.time.toEpochMilliseconds() + it.second.time.toEpochMilliseconds()) / 2)
                    )
                } else {
                    InstantValue(0.0, it.second.time)
                }
            }

            launch { valve.startControlling() }
            launch { fumes.startSensing() }
            launch { accumulator.startSensing() }
            launch { room.startSensing() }
            launch { outside.startSensing() }
            launch { openButton.startSensing() }
            launch { closeButton.startSensing() }
            launch { rechargeButton.startSensing() }
            openButton.addOnClickListener { this.launch { openSome() } }
            closeButton.addOnClickListener { this.launch { closeSome() } }
            openButton.addOnLongClickListener { this.launch { open() } }
            closeButton.addOnLongClickListener { this.launch { close() } }
            rechargeButton.addOnClickListener { this.launch { onRecharged() } }
            identifieables.filterIsInstance<WatcheableState>().forEach {
                it.addOnStateChange { launch { refreshDisplay() } }
            }
            launch {
                daFumes = dFumes.windowed(5.0.minutes).map {
                    InstantValue(it.map { it.value }.average(), it.last().time)
                }.stateIn(this)
                autoModeController = SlowlyCloseWhenCooling("auto-close", valve, fumes, daFumes!!, lastUserValveRate, lastTimeRecharged)
                autoModeController?.startControlling()
            }
            launch {
                chargedRate = accumulator.windowed(5.0.minutes).map {
                    InstantValue((max(0.0, it.map { it.value }.average() - accumulatorLowTemperature) / (accumulatorHighTemperature - accumulatorLowTemperature)))
                }.stateIn(this)
            }
            awaitCancellation()
        }
        return true
    }

    override val devices: Set<Device>
        get() = setOf(fumes, accumulator, room, outside, openButton, closeButton, rechargeButton) + valve.devices + userCommunication.devices

    override val identifieables: Set<Identifiable>
        get() = devices + valve + (autoModeController?.let { setOf(it) } ?: emptySet())

    fun stopControlling() {

    }

    suspend fun setOpenRateTo(rate: Double) {
        userCommunication.acknowledge()
        lastUserValveRate.state = rate
        if(valve.setOpenRateTo(rate)) {
            userCommunication.acknowledge()
        }
    }
    suspend fun open() {
        setOpenRateTo(1.0)
    }
    suspend fun openSome() {
        setOpenRateTo((valve.openRateOrTarget ?: 0.0) + 0.1)
    }
    suspend fun closeSome() {
        setOpenRateTo((valve.openRateOrTarget ?: 1.0) - 0.1)
    }
    suspend fun close() {
        setOpenRateTo(0.0)
    }

    suspend fun auto() {
        userCommunication.acknowledge()
        autoModeController?.let { it.enabled = !it.enabled }
    }

    suspend fun onRecharged() {
        lastTimeRecharged.state = Unit
        userCommunication.acknowledge()
    }

    suspend fun refreshDisplay() {
        (userCommunication as? StringDisplay)?.displayTable(stateDisplayString())
    }

    suspend fun accumulatorStateMessage(): String {
        return (chargedRate?.currentValueOrNull(10.0.minutes)?.let { (it * 100.0).toString(0) + "%" } ?: accumulator.stateMessage())
    }

    suspend fun stateDisplayString(): List<List<String>> {
        return listOf(
            listOf(room.stateMessage(), fumes.stateMessage(), accumulatorStateMessage()),
            listOf(/*outside.stateMessage(), */valve.stateMessage(), autoModeController?.stateMessage() ?: "")
        )
    }

    val physicalStatus: pistove.status.physical.Controller
        get() {
            return pistove.status.physical.Controller(
                autoModeController?.enabled ?: false,
                emptyList(),
                pistove.status.physical.Environment(
                    outside.lastValue,
                    pistove.status.physical.House(
                        room.lastValue,
                        pistove.status.physical.WoodStove(
                            valve.state,
                            pistove.status.physical.StoveBurningChamber(fumes.lastValue),
                            pistove.status.physical.HeatAccumulator(
                                accumulator.lastValue,
                                chargedRate?.value
                            )
                        )
                    )
                )
            )
        }


}



