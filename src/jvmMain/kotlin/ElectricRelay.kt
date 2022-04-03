import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputProvider
import com.pi4j.io.gpio.digital.DigitalState


class GPIOElectricRelay(gpioPin: Int, shutdownState: RelayState? = RelayState.open_circuit) : ElectricRelay {
    val output: DigitalOutput
    init {
        val pi4j = Pi4J.newAutoContext()
        output = pi4j.dout<DigitalOutputProvider>().create<DigitalOutput>(gpioPin)
        output.addListener(System.out::println)
        shutdownState?.let { output.config().shutdownState(relayStateToDigitalState(shutdownState)) }
    }

    override val state: RelayState
        get() {
            return when(output.state()) {
                DigitalState.HIGH -> RelayState.closed_circuit
                DigitalState.LOW -> RelayState.open_circuit
                else -> RelayState.open_circuit // ???
            }
        }

    fun relayStateToDigitalState(state: RelayState): DigitalState {
        return when(state) {
            RelayState.closed_circuit -> DigitalState.HIGH
            RelayState.open_circuit -> DigitalState.LOW
        }
    }

    override fun closeCircuit() {
        output.high()
    }

    override fun openCircuit() {
        output.low()
    }
}



fun main(vararg args: String) {
    if (args[1] == "1") {
        GPIOElectricRelay(args[0].toInt(), null).closeCircuit()
    } else {
        GPIOElectricRelay(args[0].toInt(), null).openCircuit()
    }
}