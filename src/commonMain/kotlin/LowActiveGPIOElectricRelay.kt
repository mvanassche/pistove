import kotlinx.serialization.Serializable
@Serializable
class LowActiveGPIOElectricRelay(override val id: String, val bcm: Int, /*@Required*/ val defaultState: RelayState = RelayState.inactive) : ElectricRelay {

    val output by lazy {
        pi.gpioDigitalOutput(bcm, relayStateToDigitalState(defaultState))
    }

    override var state: RelayState = defaultState
        get() {
            return field
        }
        set(value) {
            field = value
            output.state = relayStateToDigitalState(value)
        }

    fun relayStateToDigitalState(state: RelayState): DigitalState {
        return when(state) {
            RelayState.activated -> DigitalState.low
            RelayState.inactive -> DigitalState.high
        }
    }
}