import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TestTemperatureSensor(val name: String) : BaseTemperatureSensor() {

    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)

    fun interruptFor(duration: Duration) {
        interruptFor = duration
    }

    var interruptFor: Duration = Duration.ZERO

    var i = 0.0
    override suspend fun sampleValue(): Double {
        val delay = interruptFor.inWholeMilliseconds
        interruptFor = Duration.ZERO
        delay(delay)

        i += 0.05
        return (((sin(i) + 1) * 500.0) + Random.nextDouble(50.0)).roundToInt().toDouble()
             //.also { println("$name: $it°C") }
    }
}

class TestRelay(val name: String) : ElectricRelay {
    override var state: RelayState = RelayState.open_circuit

    override fun closeCircuit() {
        state = RelayState.closed_circuit
        println("activate $name")
    }
    override fun openCircuit() {
        state = RelayState.open_circuit
        println("deactivate $name")
    }
}


class TestButton(val name: String) : PushButton() {
    suspend fun push() {
        println("$name push")
        pushed()
        println("$name pushed")
    }

    override suspend fun startSensing() {
    }
}