@file:UseSerializers(DurationSerializer::class)
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
class MAX31855TemperaturSensor(override val id: String, val bus: Int, val channel: Int) : TemperatureSensor, BaseTemperatureSensor(), TestableDevice, FaultReporting {

    @Transient
    private val logger = KotlinLogging.logger {}

    @Transient
    val max = MAX31855(bus= bus, channel = channel, name = id)

    override val lastFault: InstantValue<String>?
        get() = max.lastFault

    override var lastValue: InstantValue<Double>? = null

    //@Transient
    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)

    override suspend fun sampleValue(): Double? {
        return max.temperature.let {
            if(!it.isNaN()) {
                it.toDouble()
                    .also { logger.debug { "$id: $it°C" } }
            } else {
                null
            }
        }
    }

    override suspend fun test() {
        repeat(1000) {
            println("$this: ${sampleValue()}°C")
            delay(500)
        }
    }

}

