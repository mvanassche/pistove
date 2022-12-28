import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.DurationUnit


fun ClosedRange<Double>.fuzzy(giveOrTake: Double): Function<Double, Double> {
    if(start == Double.NEGATIVE_INFINITY) {
        return FuzzyTrueUntil(endInclusive, giveOrTake)
    } else if(endInclusive == Double.POSITIVE_INFINITY) {
        return FuzzyTrueFrom(start, giveOrTake)
    } else {
        return FuzzyTrueInRange(start, endInclusive, giveOrTake)
    }
}

class FuzzyTrueFrom(val from: Double, val giveOrTake: Double) : Function<Double, Double> {
    override fun value(x: Double): Double {
        return 1.0 / (1.0 + 10.0.pow(-(1.0/giveOrTake)*(x-from)))
    }
}

class FuzzyTrueUntil(val until: Double, val giveOrTake: Double) : Function<Double, Double> {
    override fun value(x: Double): Double {
        return 1.0 / (1.0 + 10.0.pow((1.0/giveOrTake)*(x-until)))
    }
}

class FuzzyTrueInRange(val from: Double, val until: Double, val giveOrTakeFrom: Double, val giveOrTakeTo: Double) : Function<Double, Double> {
    constructor(from: Double, until: Double, giveOrTake: Double) : this(from, until, giveOrTake, giveOrTake)
    override fun value(x: Double): Double {
        return min(
            1.0 / (1.0 + 10.0.pow(-(1.0/giveOrTakeFrom)*(x-from))),
            1.0 / (1.0 + 10.0.pow((1.0/giveOrTakeTo)*(x-until)))
        )
    }
}

open class FuzzyTrueUntilDuration(val until: Duration, val giveOrTake: Duration) : Function<Duration, Double> {
    override fun value(x: Duration): Double {
        return 1.0 / (1.0 + 10.0.pow((1.0/giveOrTake.toDouble(DurationUnit.MILLISECONDS))*(x.toDouble(DurationUnit.MILLISECONDS)-until.toDouble(DurationUnit.MILLISECONDS))))
    }
}

class TrueUntilDuration(val until: Duration) : Function<Duration, Double> {
    override fun value(x: Duration): Double {
        if(x <= until) {
            return 1.0
        } else {
            return 0.0
        }
    }
}


fun <X, Y> State<X>.apply(f: Function<X, Y>): State<Y> {
    return object : State<Y> {
        override val state: Y
            get() = f.value(this@apply.state)

    }
}

data class ConfidenceValue<V>(val value: V, val confidence: Double) {
    override fun toString(): String {
        return "$value (${(confidence * 100.0).toString(0)}%)"
    }
}

interface FuzzyPredicate<V> : State<ConfidenceValue<V>>

typealias FuzzyCondition = FuzzyPredicate<Unit>

object AlwaysFalseCondition: BaseFuzzyCondition() {
    override val confidence: Double
        get() = 0.0

}

abstract class BaseFuzzyCondition : FuzzyCondition {
    override val state: ConfidenceValue<Unit>
        get() = ConfidenceValue(Unit, confidence)
    abstract val confidence: Double

    override fun toString(): String {
        return (confidence * 100.0).toString(0) + "%"
    }
}

infix fun FuzzyCondition.and(other: FuzzyCondition) = FuzzyConjunction(listOf(this, other)) {}
infix fun FuzzyCondition.or(other: FuzzyCondition) = FuzzyDisjunction(listOf(this, other)) {}
fun not(atom: FuzzyCondition) = FuzzyNegation(atom)
infix fun <V> FuzzyCondition.implies(consequence: State<V>) = FuzzyImplication(this) { consequence.state }
infix fun <V> FuzzyCondition.implies(consequence: V) = FuzzyImplication(this) { consequence }


class FuzzyConjunction<V>(val predicates: Collection<FuzzyPredicate<V>>, val and: (Collection<V>) -> V) : FuzzyPredicate<V> {
    override val state: ConfidenceValue<V>
        get() = predicates.map { it.state }.let { ConfidenceValue(and(it.map { it.value }), it.minBy { it.confidence }.confidence) }

    override fun toString(): String {
        return predicates.joinToString(" and ") { it.toString() }
    }
}

class FuzzyDisjunction<V>(val predicates: Collection<FuzzyPredicate<V>>, val or: (Collection<V>) -> V) : FuzzyPredicate<V> {
    override val state: ConfidenceValue<V>
        get() = predicates.map { it.state }.let { ConfidenceValue(or(it.map { it.value }), 1.0 - it.productOf { 1.0 - it.confidence }) }

    override fun toString(): String {
        return "(" + predicates.joinToString(" or ") { it.toString() } + ")"
    }
}

class FuzzyNegation<V>(val predicate: FuzzyPredicate<V>) : FuzzyPredicate<V> {
    override val state: ConfidenceValue<V>
        get() = predicate.state.let { ConfidenceValue(it.value, 1.0 - it.confidence) }

    override fun toString(): String {
        return "not($predicate)"
    }
}

class FuzzyImplication<Body, Head>(val condition: FuzzyPredicate<Body>, val consequence: (Body) -> Head) : FuzzyPredicate<Head> {
    override val state: ConfidenceValue<Head>
        get() = condition.state.let { ConfidenceValue(consequence(it.value), it.confidence) }

    override fun toString(): String {
        return "[$state ← $condition]"
    }
}

class FunctionOverStateFuzzyAtom<Q>(val name: String, val function: Function<Q, Double>, val inputState: State<Q>) : BaseFuzzyCondition() {
    override val confidence: Double
        get() = function.value(inputState.state)

    override fun toString(): String {
        return "$name ${super.toString()}"
    }
}


fun or(predicates: Collection<FuzzyPredicate<Double>>) = FuzzyWeightedAverageDisjunction(predicates)

class FuzzyWeightedAverageDisjunction(val predicates: Collection<FuzzyPredicate<Double>>) : FuzzyPredicate<Double> {

    override val state: ConfidenceValue<Double>
        get() {
            val wv = predicates.map { it.state.let { Pair(it.value, it.confidence) } }
            val avg = weightedAverage(wv)

            val distances = wv.map { Pair(abs(it.first - avg), it.second) }
            val largestDistance = distances.maxBy { it.first }.first
            val relativeDistances = distances.map { Pair(it.first / largestDistance, it.second) }
            val confidence = 1.0 - relativeDistances.productOf { 1.0 - (it.second * (1.0 - it.first)) }

            return ConfidenceValue(avg, confidence)
        }

    // pair(value, weight)
    fun weightedAverage(wv: Collection<Pair<Double, Double>>): Double {
        return wv.sumOf { it.first * it.second } / wv.sumOf { it.second }
    }

    override fun toString(): String {
        return "$state:\n" +
            predicates.filter { it.state.confidence > 0.4 }.joinToString("\n") { "\t$it" }
    }
}

