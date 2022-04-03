interface Display : Actuator {
}

interface StringDisplay : Display {
    fun display(value: String)
}