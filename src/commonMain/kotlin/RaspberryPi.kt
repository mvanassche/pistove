import kotlinx.serialization.Serializable
import kotlin.time.Duration

interface RaspberryPi {

    fun gpioDigitalOutput(bcm: Int, defaultState: DigitalState): GPIODigitalOutput

    fun gpioDigitalInput(bcm: Int, pullResistance: PullResistance?, debounce: Duration?): GPIODigitalInput

    fun i2c(bus: Int, device: Int): I2CBusDevice

    fun pwm(bcm: Int, hardware: Boolean): GPIOPWM

    fun spi(channel: Int): GPIOSPI

    suspend fun availableOneWireDevices(): Set<OneWireDevice>
    fun oneWireDevice(id: OneWireDeviceId): OneWireDevice?

    fun addBeforeShutdown(handler: (RaspberryPi) -> Unit)

}

interface GPIOProtocol

enum class DigitalState { low, high }

enum class PullResistance { pull_up, pull_down }

interface GPIODigitalOutput : GPIOProtocol {
    var state: DigitalState?
}

interface GPIODigitalInput : GPIOProtocol {
    val state: DigitalState?
    fun addOnChangeListener(listener: (DigitalState?) -> Unit)
    fun removeOnChangeListener(listener: (DigitalState?) -> Unit)
}

interface I2CBusDevice : GPIOProtocol {
    suspend fun <T> transact(process: suspend I2CBusDeviceTransaction.() -> T): T
    suspend fun writes(vararg byte: Byte) { transact { write(byte.toTypedArray().toByteArray()) } }
    suspend fun write(bytes: ByteArray) { transact { write(bytes) } }
    suspend fun read(bytes: ByteArray) { transact { read(bytes) } }
}
interface I2CBusDeviceTransaction : GPIOProtocol {
    fun write(bytes: ByteArray)
    fun read(bytes: ByteArray)
    // TODO registers
}

interface GPIOPWM : GPIOProtocol {
    var frequency: Int // Hertz
    var dutyCycle: Double // 0% - 100%
    fun on()
    fun off()
}

interface GPIOSPI : GPIOProtocol {
    fun transfer(bytes: ByteArray)
}

@Serializable
data class OneWireDeviceId(val familyCode: UShort, val address: ULong) {
    constructor(asString: String) : this(asString.substring(0, 2).toUShort(16), asString.substring(3).toULong(16))
    override fun toString(): String {
        //28-1b9c071e64ff
        return familyCode.toString(16).padStart(2, '0') + "-" + address.toString(16).padStart(12, '0')
    }
}

interface OneWireDevice : GPIOProtocol {
    val oneWireId: OneWireDeviceId
    suspend fun read(): String // not sure that's correct, could be bytes? what is the general case?
}


val pi by lazy { raspberryPiFromEnvironment() }
expect fun raspberryPiFromEnvironment(): RaspberryPi

object DummyPi : RaspberryPi {
    override fun gpioDigitalOutput(bcm: Int, defaultState: DigitalState): GPIODigitalOutput {
        return object : GPIODigitalOutput {
            override var state: DigitalState? = null
        }
    }

    override fun gpioDigitalInput(bcm: Int, pullResistance: PullResistance?, debounce: Duration?): GPIODigitalInput {
        return object : GPIODigitalInput {
            override val state: DigitalState? = null
            override fun addOnChangeListener(listener: (DigitalState?) -> Unit) {}
            override fun removeOnChangeListener(listener: (DigitalState?) -> Unit) {}
        }
    }

    override fun i2c(bus: Int, device: Int): I2CBusDevice {
        return object : I2CBusDevice {
            override suspend fun <T> transact(process: suspend I2CBusDeviceTransaction.() -> T): T {
                return process(object : I2CBusDeviceTransaction {
                    override fun write(bytes: ByteArray) {}
                    override fun read(bytes: ByteArray) {}
                })
            }
        }
    }

    override fun pwm(bcm: Int, hardware: Boolean): GPIOPWM {
        return object : GPIOPWM {
            override var frequency: Int = 0
            override var dutyCycle: Double = 0.0
            override fun on() {}
            override fun off() {}
        }
    }

    override fun spi(channel: Int): GPIOSPI {
        return object : GPIOSPI {
            override fun transfer(bytes: ByteArray) {}
        }
    }

    override suspend fun availableOneWireDevices(): Set<OneWireDevice> {
        return emptySet()
    }

    override fun oneWireDevice(id: OneWireDeviceId): OneWireDevice {
        return object : OneWireDevice {
            override val oneWireId = id
            override suspend fun read(): String {
                return ""
            }
        }
    }

    override fun addBeforeShutdown(handler: (RaspberryPi) -> Unit) {}
}