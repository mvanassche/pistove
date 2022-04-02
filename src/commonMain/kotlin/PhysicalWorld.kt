import kotlinx.serialization.Serializable

@Serializable
sealed class PhysicalObject

@Serializable
data class Stove(val fireTemperature: Double?, val inRoom: Room, val airIntake: Valve) : PhysicalObject()

@Serializable
data class Room(val temperature: Double?) : PhysicalObject()

@Serializable
data class Valve(val state: ValveState?) : PhysicalObject()

