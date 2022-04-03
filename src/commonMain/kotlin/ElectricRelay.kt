
enum class RelayState { closed_circuit, open_circuit }
interface ElectricRelay: Actuator {
    // TODO decide between var vs functions!
    val state: RelayState
    fun closeCircuit()
    fun openCircuit()
}