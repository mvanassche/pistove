import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.DurationUnit


class TrueFrom(val from: Double, val giveOrTake: Double) : Function<Double, Double> {
    override fun value(x: Double): Double {
        return 1.0 / (1.0 + 10.0.pow(-(1.0/giveOrTake)*(x-from)))
    }
}

class TrueUntil(val until: Double, val giveOrTake: Double) : Function<Double, Double> {
    override fun value(x: Double): Double {
        return 1.0 / (1.0 + 10.0.pow((1.0/giveOrTake)*(x-until)))
    }
}

class TrueInRange(val from: Double, val until: Double, val giveOrTake: Double) : Function<Double, Double> {
    override fun value(x: Double): Double {
        return min(
            1.0 / (1.0 + 10.0.pow(-(1.0/giveOrTake)*(x-from))),
            1.0 / (1.0 + 10.0.pow((1.0/giveOrTake)*(x-until)))
        )
    }
}

class TrueUntilDuration(val until: Duration, val giveOrTake: Duration) : Function<Duration, Double> {
    override fun value(x: Duration): Double {
        return 1.0 / (1.0 + 10.0.pow((1.0/giveOrTake.toDouble(DurationUnit.MILLISECONDS))*(x.toDouble(DurationUnit.MILLISECONDS)-until.toDouble(DurationUnit.MILLISECONDS))))
    }
}

fun <X, Y> State<X>.apply(f: Function<X, Y>): State<Y> {
    return object : State<Y> {
        override val state: Y
            get() = f.value(this@apply.state)

    }
}


interface FuzzyCondition {
    val confidence: Double
    infix fun and(other: FuzzyCondition) = FuzzyConjunction(listOf(this, other))
    infix fun or(other: FuzzyCondition) = FuzzyDisjunction(listOf(this, other))
    infix fun <V> implies(consequence: State<V>) = FuzzyImplication(this, consequence)
    infix fun <V> implies(consequence: V) = FuzzyImplication(this, consequence)
}
fun not(atom: FuzzyCondition) = FuzzyNegation(atom)

interface ValuedFuzzyCondition<V> : FuzzyCondition, State<V>

fun or(atoms: Collection<ValuedFuzzyCondition<Double>>) = FuzzyDoubleDisjunction(atoms)

open class FuzzyConjunction(open val atoms: Collection<FuzzyCondition>) : FuzzyCondition {
    override val confidence: Double
        get() = atoms.minOf { it.confidence }
}

open class FuzzyDisjunction(open val atoms: Collection<FuzzyCondition>) : FuzzyCondition {
    override val confidence: Double
        //get() = atoms.maxOf { it.confidence }
        get() = 1.0 - atoms.productOf { 1.0 - it.confidence }
}

class FuzzyNegation(val atom: FuzzyCondition) : FuzzyCondition {
    override val confidence: Double
        get() = 1.0 - atom.confidence
}


class FuzzyImplication<V>(val condition: FuzzyCondition, val consequence: State<V>) : ValuedFuzzyCondition<V> {
    constructor(condition: FuzzyCondition, consequence: V) : this(condition, PersistentState(consequence))
    override val state: V
        get() = consequence.state
    override val confidence: Double
        get() = condition.confidence
}

class FuzzyDoubleDisjunction(val atoms: Collection<ValuedFuzzyCondition<Double>>) : ValuedFuzzyCondition<Double>, FuzzyCondition {
    // FIXME value and confidence should be at the same time! -> Confidence<>
    override val state: Double
        get() {
            return weightedAverage(atoms.map { Pair(it.state, it.confidence) })
        }
    override val confidence: Double
        get() {
            val wv = atoms.map { Pair(it.state, it.confidence) }
            val avg = weightedAverage(wv)
            val distances = wv.map { Pair(abs(it.first - avg), it.second) }
            val largestDistance = distances.maxBy { it.first }.first
            val relativeDistances = distances.map { Pair(it.first / largestDistance, it.second) }
            val result = 1.0 - relativeDistances.productOf { 1.0 - (it.second * (1.0 - it.first)) }
            return result
            //return weightedAverage(distances.map { Pair(it.second, 1.0 - (it.first / largestDistance)) })
        }

    // pair(value, weight)
    fun weightedAverage(wv: Collection<Pair<Double, Double>>): Double {
        return wv.sumOf { it.first * it.second } / wv.sumOf { it.second }
    }
}

inline fun <T> Iterable<T>.productOf(selector: (T) -> Double): Double {
    var product = 1.0
    for (element in this) {
        product *= selector(element)
    }
    return product
}


class FunctionOverStateFuzzyAtom<Q>(val function: Function<Q, Double>, val state: State<Q>) : FuzzyCondition {
    override val confidence: Double
        get() = function.value(state.state)

}
