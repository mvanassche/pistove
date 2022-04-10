interface Display : Actuator {
}

interface StringDisplay : Display {
    suspend fun display(value: String)
}

interface BackLightDisplay : Display {
    suspend fun backLight(illuminated: Boolean)
}