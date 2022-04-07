import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputConfig
import com.pi4j.io.gpio.digital.DigitalOutputProvider
import com.pi4j.io.gpio.digital.DigitalState


class LowActiveGPIOElectricRelay(gpioPin: Int, defaultState: RelayState = RelayState.inactive) : ElectricRelay {
    val output: DigitalOutput
    init {
        val pi4j = Pi4J.newAutoContext()
        val config = DigitalOutput.newConfigBuilder(pi4j)
            .address(gpioPin)
            .initial(relayStateToDigitalState(defaultState))
            .shutdown(relayStateToDigitalState(defaultState))
        output = pi4j.dout<DigitalOutputProvider>().create(config)
        output.addListener(System.out::println)
    }

    override val state: RelayState
        get() {
            return when(output.state()) {
                DigitalState.HIGH -> RelayState.inactive
                DigitalState.LOW -> RelayState.activated
                else -> RelayState.inactive // ???
            }
        }

    fun relayStateToDigitalState(state: RelayState): DigitalState {
        return when(state) {
            RelayState.activated -> DigitalState.LOW
            RelayState.inactive -> DigitalState.HIGH
        }
    }

    override fun activate() {
        output.low()
    }

    override fun deactivate() {
        output.high()
    }
}



fun main(vararg args: String) {
    if (args[1] == "1") {
        LowActiveGPIOElectricRelay(args[0].toInt()).activate()
    } else {
        LowActiveGPIOElectricRelay(args[0].toInt()).deactivate()
    }
}