
enum class RelayState { activated, inactive }
interface ElectricRelay: Actuator {
    // TODO decide between var vs functions!
    val state: RelayState
    fun activate()
    fun deactivate()
}