import com.pi4j.io.w1.W1Master
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class TMPDS18B20TemperatureSensor: BaseTemperatureSensor() {
    val device = W1Master().getDevices(com.pi4j.component.temperature.TemperatureSensor::class.java).first()
    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)

    override suspend fun sampleValue(): Double {
        return device.temperature
    }
}

