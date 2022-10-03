import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


sealed interface TemperatureSensor : SamplingValuesSensor<Double> {

    val usefulPrecision: Int

    suspend fun stateMessage(): String {
        val t = currentValueOrNull(10.toDuration(DurationUnit.SECONDS))
        return t?.let{ it.toString(usefulPrecision) + "°" } ?: "?°"
    }
}

sealed class BaseTemperatureSensor: BaseSamplingValuesSensor<Double>(0.0), TemperatureSensor, Sampleable {
    override var usefulPrecision: Int = 0

    override fun sample(validityPeriod: Duration): Map<String, SampleValue> {
        return currentValueOrNull(validityPeriod)?.let { mapOf("temperature" to SampleDoubleValue(it)) } ?: emptyMap()
    }

}

@Serializable
sealed class PushButton : Sensor {
    @Transient
    val clickListeners = mutableListOf<() -> Unit>()
    @Transient
    val longClickListeners = mutableListOf<() -> Unit>()

    fun addOnClickListener(listener: () -> Unit) {
        clickListeners.add(listener)
    }
    fun removeOnClickListener(listener: () -> Unit) {
        clickListeners.remove(listener)
    }
    fun addOnLongClickListener(listener: () -> Unit) {
        longClickListeners.add(listener)
    }
    fun removeOnLongClickListener(listener: () -> Unit) {
        longClickListeners.remove(listener)
    }
    protected fun pushed() {
        clickListeners.forEach { it() }
    }
    protected fun longPushed() {
        longClickListeners.forEach { it() }
    }
}


