import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Implemented by sensors that can report a problem distinct from "no value yet" — e.g. a chip-reported
 * fault or a communication failure — so a diagnostics view can surface it even while [SensorWithState.lastValue]
 * still shows an older, valid reading.
 */
interface FaultReporting {
    val lastFault: InstantValue<String>?
}

@Serializable
sealed interface TemperatureSensor : SamplingValuesSensor<Double> {

    val usefulPrecision: Int

    suspend fun stateMessage(): String {
        val t = currentValueOrNull(10.toDuration(DurationUnit.SECONDS))
        return t?.let{ it.toString(usefulPrecision) + "°" } ?: "?°"
    }
}

/**
 * For an optional sensor (as opposed to one the controller can't function without): don't sense right away,
 * probe every [retryPeriod] until it first answers, then sense normally. Lets a sensor be unplugged, never
 * installed, or plugged in later without needing a code change or restart to match.
 */
suspend fun TemperatureSensor.startSensingWhenAvailable(retryPeriod: Duration = 30.toDuration(DurationUnit.SECONDS)) {
    val samplingSensor = this as? BaseSamplingValuesSensor<*>
    if (samplingSensor != null) {
        samplingSensor.startSensingWhenAvailable(retryPeriod)
    } else {
        startSensing()
    }
}

//@Serializable
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


@Serializable
sealed class ToggleButton : Sensor {
    @Transient
    val changeListeners = mutableListOf<(Boolean) -> Unit>()

    fun addChangeListener(listener: (Boolean) -> Unit) {
        changeListeners.add(listener)
    }
    fun removeChangeListener(listener: (Boolean) -> Unit) {
        changeListeners.remove(listener)
    }
    protected fun changed(pushed: Boolean) {
        changeListeners.forEach { it(pushed) }
    }
}

@Serializable
sealed interface RotatyButton : Sensor {

    @Transient
    val changeListeners: MutableList<(Int) -> Unit>

    val counter: Int

    fun addChangeListener(listener: (Int) -> Unit) {
        changeListeners.add(listener)
    }
    fun removeChangeListener(listener: (Int) -> Unit) {
        changeListeners.remove(listener)
    }
    fun changed(diff: Int) {
        changeListeners.forEach { it(diff) }
    }
}

