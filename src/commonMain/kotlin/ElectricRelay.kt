import kotlinx.serialization.Serializable

enum class RelayState { activated, inactive }


@Serializable
sealed interface ElectricRelay: Actuator, State<RelayState> {
    override var state: RelayState
}

