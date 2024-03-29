import kotlinx.serialization.Serializable

@Serializable
sealed interface Display : Actuator

@Serializable
sealed interface StringDisplay : Display {
    suspend fun display(value: String)
    suspend fun displayLines(lines: List<String>)
    suspend fun displayTable(linesOfElements: List<List<String>>)
}

interface BackLightDisplay : Display {
    suspend fun backLight(illuminated: Boolean)
}