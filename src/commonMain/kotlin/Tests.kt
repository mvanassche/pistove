import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


sealed interface TestableDevice {
    suspend fun test()
}

@Serializable
class TestTemperatureSensor(override val id: String) : BaseTemperatureSensor() {

    override var lastValue: InstantValue<Double>? = null

    @Transient
    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)

    fun interruptFor(duration: Duration) {
        interruptFor = duration
    }

    @Transient
    var interruptFor: Duration = Duration.ZERO

    var i = 0.0
    override suspend fun sampleValue(): Double {
        val delay = interruptFor.inWholeMilliseconds
        interruptFor = Duration.ZERO
        delay(delay)

        i += 0.05
        return (((sin(i) + 1) * 500.0) + Random.nextDouble(50.0)).roundToInt().toDouble()
             //.also { println("id: $it°C") }
    }
}

@Serializable
class EmptyTemperatureSensor(override val id: String) : BaseTemperatureSensor() {

    override var lastValue: InstantValue<Double>? = null

    @Transient
    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)

    override suspend fun sampleValue() = null
}


@Serializable
class TestRelay(override val id: String) : ElectricRelay {
    override var state: RelayState = RelayState.inactive
        set(value) {
            println("Relay $id -> $value  (${Clock.System.now()})")
            field = value
        }
    get() = field
}


@Serializable
class TestButton(override val id: String) : PushButton() {
    suspend fun push() {
        println("$id push")
        pushed()
        println("$id pushed")
    }

    suspend fun longPush() {
        println("$id long push")
        longPushed()
        println("$id long pushed")
    }
    override suspend fun startSensing() {
    }
}

@Serializable
object TestBasicUserCommunication : BasicUserCommunication {
    override suspend fun welcome() {}
    override suspend fun goodbye() {}
    override suspend fun notify() {
        println("NOTIFY")
    }

    override suspend fun alert() {
        println("ALERT")
    }

    override suspend fun acknowledge() {
        println("OK")
    }

    override val devices = emptySet<Device>()

}