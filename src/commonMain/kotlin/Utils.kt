import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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

    fun <V2>map(t: (V) -> V2): State<V2> {
        return object : State<V2> {
            override val state: V2
                get() = t(this@State.state)

        }
    }
}

interface WatcheableState {
    val listeners: MutableList<() -> Unit>
    fun addOnStateChange(listener: () -> Unit) {
        listeners.add(listener)
    }
}

@Serializable
open class PersistentState<V>(override var state: V) : State<V>


fun <V> State<InstantValue<V>>.maybeCurrentValue(validityPeriod: Duration): V? {
    return state.maybeCurrentValue(validityPeriod)
}

fun <V> InstantValue<V>.maybeCurrentValue(validityPeriod: Duration): V? {
    if(time >= Clock.System.now() - validityPeriod) {
        return value
    } else {
        return null
    }
}

sealed interface SamplingValuesSensor<V>: Sensor, StateFlow<InstantValue<V>>, State<InstantValue<V>>, SensorWithState<V>


fun <V> StateFlow<InstantValue<V>>.currentValueOrNull(validityPeriod: Duration): V? {
    if(value.time >= Clock.System.now() - validityPeriod) {
        return value.value
    } else {
        return null
    }
}

suspend fun <V> StateFlow<InstantValue<V>>.currentValue(validityPeriod: Duration): V {
    if(value.time >= Clock.System.now() - validityPeriod) {
        return value.value
    } else {
        //println("WAITING FOR CURRENT VALUE $id ${coroutineScope {  }}")
        takeWhile { it.time < Clock.System.now() - validityPeriod }.last()
        //println("GOTTEN FOR CURRENT VALUE $id")
        return value.value
    }
}

suspend fun <V> StateFlow<InstantValue<V>>.waitForCurrentValueCondition(validityPeriod: Duration, condition: (V) -> Boolean) {
    takeWhile { it.time < Clock.System.now() - validityPeriod || !condition(it.value) }.last()
}

fun <V> StateFlow<V>.toState(): State<V> {
    return object : State<V> {
        override val state: V
            get() = this@toState.value

    }
}

//@Serializable
sealed class BaseSamplingValuesSensor<V>(
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
        get() = PrimitiveSerialDescriptor("CustomDuration", PrimitiveKind.LONG)


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


fun <T> Flow<T>.windowed(shouldBeInWindow: (T) -> Boolean): Flow<List<T>> = flow {
    val queue = ArrayDeque<T>()
    collect { element ->
        queue.addLast(element)
        while(queue.firstOrNull()?.let { !shouldBeInWindow(it) } == true) { queue.removeFirst() }
        if(queue.isNotEmpty()) {
            emit(queue.toList())
        }
    }
}

fun <T> Flow<InstantValue<T>>.windowed(timeWindow: Duration): Flow<List<InstantValue<T>>> = windowed {
    Clock.System.now() - it.time < timeWindow
}

fun <T> Flow<T>.zipWithNext(): Flow<Pair<T, T>> = flow {
    var previous: T? = null
    collect {
        if(previous == null) {
            previous = it
        } else {
            emit(Pair(previous!!, it))
            previous = it
        }
    }
}

fun <T> Flow<InstantValue<T>>.sampleInstantValue(period: Duration): Flow<InstantValue<T>> = flow {
    var lastEmittedAt: Instant? = null
    collect {
        if(lastEmittedAt == null || (it.time - lastEmittedAt) >= period) {
            emit(it)
        }
    }
}

fun <T> Flow<List<InstantValue<T>>>.aggregate(aggregation: (List<T>) -> T): Flow<InstantValue<T>> = map {
    if(it.isNotEmpty()) {
        InstantValue(aggregation(it.map { it.value }), it.last().time)
    } else {
        null
    }
}.filterNotNull()

/*fun <T> Flow<InstantValue<T>>.derive(): Flow<InstantValue<T>> = zipWithNext().map {
    if (it.second.time > it.first.time) {
        InstantValue(
            (it.second.value - it.first.value) / ((it.second.time.toEpochMilliseconds() - it.first.time.toEpochMilliseconds()).toDouble() / 3600000.0), // in °/h
            Instant.fromEpochMilliseconds((it.first.time.toEpochMilliseconds() + it.second.time.toEpochMilliseconds()) / 2)
        )
    } else {
        InstantValue(0.0, it.second.time)
    }
}*/


/*fun <V> ReceiveChannel<InstantValue<V>>.zipWithNext(): ReceiveChannel<InstantValue<V>> {
    var previous: V? = null
    return produce {

    }
    this.
    collect {
        if(previous == null) {
            previous = it
        } else {
            emit(Pair(previous!!, it))
            previous = it
        }
    }
}*/



interface Function<X, Y> {
    fun value(x: X): Y
}

/**
 * y = mx + b
 */
@Serializable
class LinearFunction(val m: Double, val b: Double) : Function<Double, Double> {
    constructor(p1: Pair<Double, Double>, p2: Pair<Double, Double>) :
            this((p2.second - p1.second) / (p2.first - p1.first), p1.second - ((p2.second - p1.second) / (p2.first - p1.first) * p1.first))

    override fun value(x: Double): Double {
        return m * x + b
    }
}