

class LowActiveGPIOElectricRelay(val pi: RaspberryPi, gpioPin: Int, defaultState: RelayState = RelayState.inactive) : ElectricRelay {

    val output by lazy {
        pi.gpioDigitalOutput(gpioPin, relayStateToDigitalState(defaultState))
    }

    override val state: RelayState
        get() {
            return when(output.state) {
                DigitalState.high -> RelayState.inactive
                DigitalState.low -> RelayState.activated
                else -> RelayState.inactive // ???
            }
        }

    fun relayStateToDigitalState(state: RelayState): DigitalState {
        return when(state) {
            RelayState.activated -> DigitalState.low
            RelayState.inactive -> DigitalState.high
        }
    }

    override fun activate() {
        output.state = DigitalState.low
    }

    override fun deactivate() {
        output.state = DigitalState.high
    }
}



fun main(vararg args: String) {
    val pi = raspberryPiFromEnvironment()
    if (args[1] == "1") {
        LowActiveGPIOElectricRelay(pi, args[0].toInt()).activate()
    } else {
        LowActiveGPIOElectricRelay(pi, args[0].toInt()).deactivate()
    }
}