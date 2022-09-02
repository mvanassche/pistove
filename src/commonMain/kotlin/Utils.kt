import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.takeWhile
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
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