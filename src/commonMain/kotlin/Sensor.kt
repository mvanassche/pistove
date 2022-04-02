import kotlinx.coroutines.withTimeoutOrNull
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

abstract class BaseTemperatureSensor: BaseSamplingValuesSensor<Double>(0.0), TemperatureSensor


abstract class PushButton : Sensor {
    val listeners = mutableListOf<suspend () -> Unit>()
    fun addOnClickListener(listener: suspend () -> Unit) {
        listeners.add(listener)
    }
    protected suspend fun pushed() {
        listeners.forEach { it() }
    }
}


