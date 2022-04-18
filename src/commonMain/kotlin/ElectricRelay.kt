import kotlinx.serialization.Serializable

enum class RelayState { activated, inactive }


@Serializable
sealed interface ElectricRelay: Actuator {
    // TODO decide between var vs functions!
    val state: RelayState
    fun activate()
    fun deactivate()
}

