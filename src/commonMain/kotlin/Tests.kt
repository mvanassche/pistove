import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


interface TestableDevice {
    suspend fun test()
}

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
    override var state: RelayState = RelayState.inactive

    override fun activate() {
        state = RelayState.activated
        println("activate $name")
    }
    override fun deactivate() {
        state = RelayState.inactive
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

object TestBasicUserCommunication : BasicUserCommunication {
    override suspend fun alert() {
        println("ALERT")
    }

    override suspend fun acknowledge() {
        println("OK")
    }

    override val devices = emptySet<Device>()

}