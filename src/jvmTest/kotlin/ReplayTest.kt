import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ReplayTest {

    @Test
    fun test1() {
        try {
            runBlocking {
                val valve = ElectricValveController(TestRelay("open"), TestRelay("close"))
                val chimney = TestTemperatureSensor("chimney")
                val room = TestTemperatureSensor("room")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController(valve, chimney, room, openButton, closeButton, autoButton)
                launch { stove.startControlling() }

                launch { closeButton.push() }
                delay(10000)
                assertTrue { valve.state == ValveState.closed }
                launch { openButton.push() }
                delay(10000)
                assertTrue { valve.state == ValveState.open }
                launch { autoButton.push() }
                delay(90000)
                assertTrue { valve.state == ValveState.closed }

                //cancel() ???

                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }

    @Test
    fun test2() {
        try {
            runBlocking {
                val valve = ElectricValveController(TestRelay("open"), TestRelay("close"))
                val chimney = TestTemperatureSensor("chimney")
                val room = TestTemperatureSensor("room")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController(valve, chimney, room, openButton, closeButton, autoButton)
                launch { stove.startControlling() }

                launch { closeButton.push() }
                launch { closeButton.push() }
                delay(100)
                launch { closeButton.push() }
                launch { closeButton.push() }
                delay(10000)
                assertTrue { valve.state == ValveState.closed }

                launch { closeButton.push() }
                assertTrue { valve.state == ValveState.closed }

                launch { openButton.push() }
                delay(100)
                launch { closeButton.push() }
                delay(100)
                launch { openButton.push() }
                delay(10000)
                assertTrue { valve.state == ValveState.open }

                //cancel() ???
                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }

    @Test
    fun test3() {
        try {
            runBlocking {
                val valve = ElectricValveController(TestRelay("open"), TestRelay("close"))
                val chimney = TestTemperatureSensor("chimney")
                val room = TestTemperatureSensor("room")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController(valve, chimney, room, openButton, closeButton, autoButton)
                launch { stove.startControlling() }

                launch { closeButton.push() }
                delay(10000)
                assertTrue { valve.state == ValveState.closed }
                launch { openButton.push() }
                delay(10000)
                assertTrue { valve.state == ValveState.open }
                launch { autoButton.push() }
                delay(1000)
                chimney.interruptFor(20.toDuration(DurationUnit.SECONDS))
                delay(20000)
                assertTrue { valve.state == ValveState.open }
                delay(80000)
                assertTrue { valve.state == ValveState.closed }

                //cancel() ???
                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }

    @Test
    fun test4() {
        try {
            runBlocking {
                val valve = ElectricValveController(TestRelay("open"), TestRelay("close"))
                val chimney = TestTemperatureSensor("chimney")
                val room = TestTemperatureSensor("room")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController(valve, chimney, room, openButton, closeButton, autoButton)
                launch { stove.startControlling() }

                val x = launch { closeButton.push() }
                delay(1000)
                x.cancel()
                delay(10000)
                assertTrue { valve.state == ValveState.closing }
                //cancel() ???
                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }

    @Test
    fun testXXX() {
        println(Json.encodeToString(XXXSerializer, object : XXX { override val x = 123.0 } ))
    }


}

@Serializable(with = XXXSerializer::class)
interface XXX {
    val x: Double
}

object XXXSerializer : KSerializer<XXX> {
    override fun deserialize(decoder: Decoder): XXX {
        return object : XXX { override val x = decoder.decodeDouble() }
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("XXX", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: XXX) {
        encoder.encodeDouble(value.x)
    }

}