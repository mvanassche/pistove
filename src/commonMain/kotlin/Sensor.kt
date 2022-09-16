import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.DurationUnit
import kotlin.time.toDuration


sealed interface TemperatureSensor : SamplingValuesSensor<Double> {

    val usefulPrecision: Int

    suspend fun stateMessage(): String {
        val t = currentValueOrNull(10.toDuration(DurationUnit.SECONDS))
        return t?.let{ it.toString(usefulPrecision) + "°" } ?: "?°"
    }
}

sealed class BaseTemperatureSensor: BaseSamplingValuesSensor<Double>(0.0), TemperatureSensor {
    override var usefulPrecision: Int = 0
}

@Serializable
sealed class PushButton : Sensor {
    @Transient
    val listeners = mutableListOf<() -> Unit>()
    fun addOnClickListener(listener: () -> Unit) {
        listeners.add(listener)
    }
    fun removeOnClickListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
    protected fun pushed() {
        listeners.forEach { it() }
    }
}


