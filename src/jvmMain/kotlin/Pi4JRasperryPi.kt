import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputProvider


//https://pi4j.com/documentation/create-context/

val context = Pi4J.newAutoContext()

class Pi4JRasperryPi : RaspberryPi {
    override fun gpioDigitalOutput(bcm: Int, defaultState: DigitalState): GPIODigitalOutput {
        val config = DigitalOutput.newConfigBuilder(context)
            .address(bcm)
            .initial(defaultState.toPi4J())
            .shutdown(defaultState.toPi4J())
        val pdo = context.dout<DigitalOutputProvider>().create<DigitalOutput>(config)
        return object : GPIODigitalOutput {
            override var state: DigitalState?
                get() = pdo.state().toState()
                set(value) {
                    pdo.state(value.toPi4J())
                }

            override fun <T> transact(process: GPIOProtocol.() -> T): T {
                return process(this)
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

fun DigitalState?.toPi4J(): com.pi4j.io.gpio.digital.DigitalState {
    return when(this) {
        DigitalState.low -> com.pi4j.io.gpio.digital.DigitalState.LOW
        DigitalState.high -> com.pi4j.io.gpio.digital.DigitalState.HIGH
        null -> com.pi4j.io.gpio.digital.DigitalState.UNKNOWN
    }
}

actual fun raspberryPiFromEnvironment(): RaspberryPi {
    return Pi4JRasperryPi()
}