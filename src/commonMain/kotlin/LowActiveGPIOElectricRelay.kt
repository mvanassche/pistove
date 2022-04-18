import kotlinx.serialization.Serializable
@Serializable
class LowActiveGPIOElectricRelay(override val id: String, val bcm: Int, /*@Required*/ val defaultState: RelayState = RelayState.inactive) : ElectricRelay {

    val output by lazy {
        pi.gpioDigitalOutput(bcm, relayStateToDigitalState(defaultState))
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


/*
fun main(vararg args: String) {
    if (args[1] == "1") {
        LowActiveGPIOElectricRelay(args[0].toInt()).activate()
    } else {
        LowActiveGPIOElectricRelay(args[0].toInt()).deactivate()
    }
}*/

