import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
class DS18B20TempartureSensor(override var id: String, val address: ULong) : BaseTemperatureSensor() {

    @Transient
    val oneWire by lazy {
        pi.oneWireDevice(OneWireDeviceId(0x28.toUShort(), address))
    }

    @Transient
    override val samplingPeriod = 5.toDuration(DurationUnit.SECONDS) // parametric? 750ms is the default driver?

    override var lastValue: InstantValue<Double>? = null

    override suspend fun sampleValue(): Double? {
        if(oneWire != null) {
            try {
                /*val content = "fc 00 55 00 7f ff 0c 10 ed : crc=ed YES\n" +
                    "fc 00 55 00 7f ff 0c 10 ed t=15750"
                */
                val content = oneWire!!.read()
                if (content.lines()[0].endsWith("YES")) {
                    val temp = content.lines()[1].substringAfter("t=").toInt().toDouble() / 1000.0
                    return (temp * 10.0).roundToInt().toDouble() / 10.0 // rounding to 1 decimal?
                } else {
                    return null
                }
            } catch (e: Exception) {
                println("$id: ${e.message}")
                return null
            }
        } else {
            return null
        }
    }
}
