import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputProvider
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