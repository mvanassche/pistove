import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.takeWhile
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


interface Identifiable {
    val id: String
}

interface State<V> {
    val state: V
}

@Serializable
open class PersistentState<V>(override var state: V) : State<V>


fun <V> State<InstantValue<V>>.maybeCurrentValue(validityPeriod: Duration): V? {
    if(state.time >= Clock.System.now() - validityPeriod) {
        return state.value
    } else {
        return null
    }
}

sealed interface SamplingValuesSensor<V>: Sensor, StateFlow<InstantValue<V>>, State<InstantValue<V>>, SensorWithState<V> {

    fun currentValueOrNull(validityPeriod: Duration): V? {
        if(value.time >= Clock.System.now() - validityPeriod) {
            return value.value
        } else {
            return null
        }
    }

    suspend fun currentValue(validityPeriod: Duration): V {
        if(value.time >= Clock.System.now() - validityPeriod) {
            return value.value
        } else {
            takeWhile { it.time < Clock.System.now() - validityPeriod }.last()
            return value.value
        }
    }

    suspend fun waitForCurrentValueCondition(validityPeriod: Duration, condition: (V) -> Boolean) {
        takeWhile { it.time < Clock.System.now() - validityPeriod || !condition(it.value) }.last()
    }

}

abstract class BaseSamplingValuesSensor<V>(
    val initialValue: V,
    val flow: MutableStateFlow<InstantValue<V>> = MutableStateFlow(InstantValue(initialValue, Instant.DISTANT_PAST))
): SamplingValuesSensor<V>, StateFlow<InstantValue<V>> by flow, SensorWithState<V> {


    abstract override var lastValue: InstantValue<V>?

    abstract val samplingPeriod: Duration

    abstract suspend fun sampleValue(): V?

    override val state: InstantValue<V>
        get() = value

    override suspend fun startSensing() {
        while(true) {
            delay(samplingPeriod.inWholeMilliseconds)
            val sample = sampleValue()
            if(sample != null) {
                val timedSample = InstantValue<V>(sample)
                lastValue = timedSample
                flow.emit(timedSample)
            }
        }
    }

    /*override suspend fun lastValue(): InstantValue<V> {
        // TODO get the next one
        flow.collectLatest {  }
        return flow.replayCache.last()
    }*/
}

fun List<InstantValue<Double>>.averageValue(duration: Duration): Double {
    val minInstant = this.last().time - duration
    return this.filter { it.time > minInstant }.map { it.value }.average()
}

/*suspend fun SharedFlow<InstantValue<Double>>.movingAverage(duration: Duration): SharedFlow<InstantValue<Double>> {
    // TODO fix this, there should be a nice elegant way to do this. also, duration not taken into account!
    val result = MutableSharedFlow<InstantValue<Double>>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    coroutineScope {
        launch {
            this@movingAverage.collect { v ->
                val avg = this@movingAverage.replayCache.map { it.value }.average()
                result.emit(InstantValue(avg, v.time))
            }
        }
    }
    return result
}*/


inline fun <reified T> State<T>.toJson(): String {
    return Json.encodeToString(PersistentState(this.state))
}

object DurationSerializer : KSerializer<Duration> {
    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().toDuration(DurationUnit.NANOSECONDS)
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Duration", PrimitiveKind.LONG)


    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeNanoseconds)
    }
}


fun padStringElementsToFit(maxSize: Int, elements: List<String>): String {
    val line: String
    if(elements.sumOf { it.length } > maxSize) {
        line = elements.joinToString("").substring(0, maxSize - 1)
    } else {
        val stringsPadded = elements.toMutableList()
        val averageLength = (maxSize - elements.last().length).toDouble() / (elements.size - 1).toDouble()
        //val averageLength = maxSize.toDouble() / elements.size.toDouble()
        var spacesToAdd = maxSize - elements.sumOf { it.length }
        val averageSpacesToDistribute = spacesToAdd.toDouble() / elements.size.toDouble()
        spacesToAdd -= averageSpacesToDistribute.toInt()
        (0..elements.lastIndex - 1).forEach {
            val endIndex = stringsPadded.take(it + 1).sumOf { it.length }
            val difference = max(0, min(((averageLength * (it + 1)) - endIndex).roundToInt(), spacesToAdd))
            spacesToAdd -= difference
            stringsPadded[it] = stringsPadded[it] + " ".repeat(difference)
        }
        spacesToAdd = maxSize - stringsPadded.sumOf { it.length }
        if(spacesToAdd > 0) {
            stringsPadded[stringsPadded.lastIndex] = " ".repeat(spacesToAdd) + stringsPadded[stringsPadded.lastIndex]
        }
        line = stringsPadded.joinToString("")
    }
    return line
}


fun Double.toString(precision: Int): String {
    if(precision <= 0) { // no support for negative, sorry
        return this.roundToInt().toString()
    } else {
        val digits = (this * 10.0.pow(precision)).roundToInt().toString()
        return digits.dropLast(precision) + "." + digits.takeLast(precision)
    }
}