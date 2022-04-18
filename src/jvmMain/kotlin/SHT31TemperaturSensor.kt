import kotlinx.coroutines.runBlocking


fun main(vararg args: String) {
    runBlocking {
        SHT31TemperaturSensor("test", args[0].toInt(), args[1].toInt(16)).let {
            println("$it: ${it.sampleValue()}°C")
        }
    }
}