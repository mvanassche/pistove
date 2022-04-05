interface Display : Actuator {
}

interface StringDisplay : Display {
    fun display(value: String)
}

interface BackLightDisplay : Display {
    var illuminatedBackLight: Boolean
}