sealed interface Display : Actuator {
}

interface StringDisplay : Display {
    suspend fun display(value: String)
    suspend fun display(linesOfElements: List<List<String>>)
}

interface BackLightDisplay : Display {
    suspend fun backLight(illuminated: Boolean)
}