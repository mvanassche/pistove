import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MAX31855TemperaturSensor(channel: Int) : BaseTemperatureSensor() {

    val max = MAX31855(channel)

    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)

    override suspend fun sampleValue(): Double {
        return max.temperature.toDouble()
    }
}

