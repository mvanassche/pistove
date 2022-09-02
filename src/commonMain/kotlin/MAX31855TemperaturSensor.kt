@file:UseSerializers(DurationSerializer::class)
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
class MAX31855TemperaturSensor(override val id: String, val channel: Int) : TemperatureSensor, BaseTemperatureSensor(), TestableDevice {

    @Transient
    val max = MAX31855(channel)

    override var lastValue: InstantValue<Double>? = null

    //@Transient
    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)

    override suspend fun sampleValue(): Double? {
        return max.temperature.let {
            if(!it.isNaN()) {
                it.toDouble()
            } else {
                null
            }
        }
    }

    override suspend fun test() {
        repeat(10) {
            println("$this: ${sampleValue()}°C")
            delay(500)
        }
    }

}

