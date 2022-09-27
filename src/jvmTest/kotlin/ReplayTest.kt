import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ReplayTest {

    @Test
    fun test1() {
        try {
            runBlocking {
                val valve = ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay"))
                val chimney = TestTemperatureSensor("chimney")
                val accumulator = TestTemperatureSensor("accumulator-thermometer")
                val room = TestTemperatureSensor("room")
                val outside = TestTemperatureSensor("outside")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController("stove",valve, chimney, accumulator, room, outside, openButton, closeButton, autoButton, TestBasicUserCommunication)
                launch { stove.startControlling() }

                launch { closeButton.push() }
                delay(10000)
                assertEquals(ValveState.closed, valve.state)
                launch { openButton.push() }
                delay(10000)
                assertTrue { valve.state == ValveState.open }
                launch { autoButton.push() }
                delay(200000)
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
                val valve = ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay"))
                val chimney = TestTemperatureSensor("chimney")
                val accumulator = TestTemperatureSensor("accumulator-thermometer")
                val room = TestTemperatureSensor("room")
                val outside = TestTemperatureSensor("outside")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController("stove",valve, chimney, accumulator, room, outside, openButton, closeButton, autoButton, TestBasicUserCommunication)
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
                val valve = ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay"))
                val chimney = TestTemperatureSensor("chimney")
                val accumulator = TestTemperatureSensor("accumulator-thermometer")
                val room = TestTemperatureSensor("room")
                val outside = TestTemperatureSensor("outside")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController("stove",valve, chimney, accumulator, room, outside, openButton, closeButton, autoButton, TestBasicUserCommunication)
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
                val valve = ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay"))
                val chimney = TestTemperatureSensor("chimney")
                val accumulator = TestTemperatureSensor("accumulator-thermometer")
                val room = TestTemperatureSensor("room")
                val outside = TestTemperatureSensor("outside")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController("stove",valve, chimney, accumulator, room, outside, openButton, closeButton, autoButton, TestBasicUserCommunication)
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
    fun test5() {
        try {
            runBlocking {
                val valve = ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay"))
                val chimney = TestTemperatureSensor("chimney")
                val accumulator = TestTemperatureSensor("accumulator-thermometer")
                val room = TestTemperatureSensor("room")
                val outside = TestTemperatureSensor("outside")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController("stove",valve, chimney, accumulator, room, outside, openButton, closeButton, autoButton, TestBasicUserCommunication)
                launch { stove.startControlling() }

                println("open")
                valve.open()
                println("open to 0.5")
                launch { valve.setOpenRateTo(0.5) }
                delay(10000)
                assertEquals(ValveState.closing, valve.state)
                delay(80000)
                assertTrue { valve.openRate == 0.5 }
                println("open to 0.4")
                launch { valve.setOpenRateTo(0.4) }
                delay(20000)
                assertTrue { valve.openRate == 0.4 }

                //cancel() ???
                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }
}
