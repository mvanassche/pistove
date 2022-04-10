import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputProvider
import com.pi4j.io.i2c.I2C
import com.pi4j.io.i2c.I2CProvider
import com.pi4j.io.pwm.Pwm
import com.pi4j.io.pwm.PwmType
import com.pi4j.io.spi.Spi
import com.pi4j.io.spi.SpiProvider
import com.pi4j.plugin.pigpio.provider.pwm.PiGpioPwmProvider
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration


//https://pi4j.com/documentation/create-context/

val context by lazy { Pi4J.newAutoContext() }

class Pi4JRasperryPi : RaspberryPi {
    override fun gpioDigitalOutput(bcm: Int, defaultState: DigitalState): GPIODigitalOutput {
        val config = DigitalOutput.newConfigBuilder(context)
            .address(bcm)
            .initial(defaultState.toPi4j())
            .shutdown(defaultState.toPi4j())
        val pdo = context.dout<DigitalOutputProvider>().create<DigitalOutput>(config)
        return object : GPIODigitalOutput {
            override var state: DigitalState?
                get() = pdo.state().toState()
                set(value) {
                    pdo.state(value.toPi4j())
                }
        }
    }

    override fun gpioDigitalInput(bcm: Int, pullResistance: PullResistance?, debounce: Duration?): GPIODigitalInput {
        val config = DigitalInput.newConfigBuilder(context)
            .address(bcm)
            .let { config -> (debounce?.let { config.debounce(it.inWholeMicroseconds) } ?: config) }
            .let { config -> (pullResistance?.let { config.pull(it.toPi4j()) } ?: config) }
            .provider("pigpio-digital-input")
        val pdi = context.create(config)
        return object : GPIODigitalInput {
            override val state: DigitalState?
                get() = pdi.state().toState()

            override fun addOnChangeListener(listener: (DigitalState?) -> Unit) {
                pdi.addListener({ e -> listener(e.state().toState()) })
            }

            override fun removeOnChangeListener(listener: (DigitalState?) -> Unit) {
                TODO("Not yet implemented") // TODO TODO !!!
            }

        }
    }

    // https://github.com/Pi4J/pi4j-v2/discussions/158 this is probably overkill, but I did reach some concurrency issues in the past, so better be safe than sorry
    val i2cBusesLocks = Array(10) { it.toString() } // TODO, there is smarter to do here (max 10 I2C buses?)

    override fun i2c(bus: Int, device: Int): I2CBusDevice {
        val config = I2C.newConfigBuilder(context)
            .bus(bus)
            .device(device)
            .build()
        val i2CProvider = context.provider<I2CProvider>("pigpio-i2c")
        val i2c: I2C = i2CProvider.create(config)
        return object : I2CBusDevice {
            override fun <T> transact(process: I2CBusDevice.() -> T): T {
                synchronized(i2cBusesLocks[bus]) {
                    return process()
                }
            }

            override fun write(bytes: ByteArray) {
                synchronized(i2cBusesLocks[bus]) {
                    i2c.write(bytes)
                }
            }

            override fun read(bytes: ByteArray) {
                synchronized(i2cBusesLocks[bus]) {
                    i2c.read(bytes)
                }
            }
        }
    }

    override fun pwm(bcm: Int, hardware: Boolean): GPIOPWM {
        val config = Pwm.newConfigBuilder(context)
            .address(bcm)
            .pwmType(if(hardware) PwmType.HARDWARE else PwmType.SOFTWARE)
            .frequency(1) // Why not 1?
            .shutdown(0)
            .initial(0)
            .build()

        val pwm = context.providers().get(PiGpioPwmProvider::class.java).create(config)
        pwm.off() // TODO can't you put that in config?
        return object : GPIOPWM {
            override var frequency: Int
                get() = pwm.frequency
                set(value) { pwm.frequency = value }
            override var dutyCycle: Double
                get() = pwm.dutyCycle.toDouble()
                set(value) { pwm.dutyCycle(value.toFloat()) }

            override fun on() {
                pwm.on()
            }
            override fun off() {
                pwm.off()
            }
        }
    }

    override fun spi(channel: Int): GPIOSPI {
        val config = Spi.newConfigBuilder(context)
            .address(channel)
            .baud(500000) //Spi.DEFAULT_BAUD) // TODO is that a parameter?
            .build()
        val spiProvider = context.provider<SpiProvider>("pigpio-spi")
        val spi = spiProvider.create(config)
        spi.open()
        return object : GPIOSPI {
            override fun transfer(bytes: ByteArray) {
                spi.transfer(bytes)
            }
        }
    }
}

fun com.pi4j.io.gpio.digital.DigitalState.toState(): DigitalState? {
    return when(this) {
        com.pi4j.io.gpio.digital.DigitalState.LOW -> DigitalState.low
        com.pi4j.io.gpio.digital.DigitalState.HIGH -> DigitalState.high
        com.pi4j.io.gpio.digital.DigitalState.UNKNOWN -> null
    }
}

fun DigitalState?.toPi4j(): com.pi4j.io.gpio.digital.DigitalState {
    return when(this) {
        DigitalState.low -> com.pi4j.io.gpio.digital.DigitalState.LOW
        DigitalState.high -> com.pi4j.io.gpio.digital.DigitalState.HIGH
        null -> com.pi4j.io.gpio.digital.DigitalState.UNKNOWN
    }
}

fun PullResistance.toPi4j() =
    when(this) {
        PullResistance.pull_up -> com.pi4j.io.gpio.digital.PullResistance.PULL_UP
        PullResistance.pull_down -> com.pi4j.io.gpio.digital.PullResistance.PULL_DOWN
    }


actual fun raspberryPiFromEnvironment(): RaspberryPi {
    if(System.getProperty("os.arch") == "arm") { // TODO be more precise to make sure we are on pi.
        return Pi4JRasperryPi()
    } else {
        return DummyPi
    }
}