import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.DurationUnit
import kotlin.time.toDuration


sealed interface TemperatureSensor : SamplingValuesSensor<Double> {

    suspend fun stateMessage(): String {
        return withTimeoutOrNull(1000) {
            val result = currentValue(10.toDuration(DurationUnit.SECONDS))
            "${result}°C"
        } ?: "?°C"
    }
}

sealed class BaseTemperatureSensor: BaseSamplingValuesSensor<Double>(0.0), TemperatureSensor

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


