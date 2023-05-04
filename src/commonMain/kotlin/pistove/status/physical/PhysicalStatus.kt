package pistove.status.physical

import InstantValue
import ValveState
import kotlinx.serialization.Serializable
import toString

@Serializable
data class Controller(
    val autoMode: Boolean,
    val messages: List<String>,
    val controlledEnvironment: Environment

) {
    override fun toString(): String {
        return "Auto: $autoMode\n" +
                "$controlledEnvironment\n" +
                messages.joinToString("\n")
    }
}

fun String?.orNot(): String {
    return this ?: "?"
}

fun InstantValue<Double>?.temperatureLabel(): String {
    return "${this?.value?.toString(1).orNot()}°C"
}

@Serializable
data class Environment(val temperature: InstantValue<Double>?, val house: House) {
    override fun toString(): String {
        return "\uD83C\uDF24: ${temperature.temperatureLabel()}\n$house"
    }
}


@Serializable
data class House(val temperature: InstantValue<Double>?, val stove: WoodStove) {
    override fun toString(): String {
        return "\uD83C\uDFE1: ${temperature.temperatureLabel()}\n$stove"
    }
}


@Serializable
data class WoodStove(val valve: ValveState?, val burningChamber: StoveBurningChamber, val accumulator: HeatAccumulator, val chimney: Chimney) {
    override fun toString(): String {
        return "$burningChamber\n" +
                "$accumulator\n" +
                "$valve"
    }
}

/*@Serializable
data class AirIntakeValve(val currentRate: Double?, val targetRate: Double?) {
    override fun toString(): String {
        return "Air intake: ${currentRate?.let { (it * 100.0) }?.toString(0) ?: "?"}%" +
            (if(targetRate != null && targetRate != currentRate) " → " + (targetRate * 100.0).toString(0) +"%" else "")
    }
}*/

@Serializable
data class StoveBurningChamber(val temperature: InstantValue<Double>?) {
    override fun toString(): String {
        return "\uD83D\uDD25: ${temperature.temperatureLabel()}"
    }
}

@Serializable
data class HeatAccumulator(val temperature: InstantValue<Double>?, val chargedRate: InstantValue<Double>?) {
    override fun toString(): String {
        return "\uD83D\uDD0B: ${chargedRate?.value?.let { it * 100.0 }?.toString(0)}% (${temperature.temperatureLabel()})"
    }
}

@Serializable
data class Chimney(val temperature: InstantValue<Double>?) {
    override fun toString(): String {
        return "░: ${temperature.temperatureLabel()}"
    }
}
