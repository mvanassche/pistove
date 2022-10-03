import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ReplayTest {

    @Test
    fun test1() {
        try {
            runBlocking {
                val valve = object : ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay")) {
                    override val timeForFullMotion = 1.5.seconds
                    override val extraTimeForSafety = 0.1.seconds
                }
                val chimney = TestTemperatureSensor("chimney")
                val accumulator = TestTemperatureSensor("accumulator-thermometer")
                val room = TestTemperatureSensor("room")
                val outside = TestTemperatureSensor("outside")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController("stove",valve, chimney, accumulator, room, outside, openButton, closeButton, autoButton, TestBasicUserCommunication)
                launch { stove.startControlling() }

                launch { closeButton.longPush() }
                delay(3400)
                assertTrue { valve.isClosed() == true }
                launch { openButton.longPush() }
                delay(1700)
                assertTrue { valve.isClosed() == false }
                //cancel() ???

                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }

    @Test
    fun test2() {
        try {
            runBlocking {
                val valve = object : ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay")) {
                    override val timeForFullMotion = 1.5.seconds
                    override val extraTimeForSafety = 0.1.seconds
                }
                val chimney = TestTemperatureSensor("chimney")
                val accumulator = TestTemperatureSensor("accumulator-thermometer")
                val room = TestTemperatureSensor("room")
                val outside = TestTemperatureSensor("outside")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController("stove",valve, chimney, accumulator, room, outside, openButton, closeButton, autoButton, TestBasicUserCommunication)
                launch { stove.startControlling() }

                launch { closeButton.longPush() }
                launch { closeButton.longPush() }
                delay(10)
                launch { closeButton.longPush() }
                launch { closeButton.longPush() }
                delay(3400)
                assertTrue { valve.isClosed() == true }

                launch { closeButton.longPush() }
                assertTrue { valve.isClosed() == true }

                launch { openButton.longPush() }
                delay(10)
                launch { closeButton.longPush() }
                delay(10)
                launch { openButton.longPush() }
                delay(1700)
                assertTrue { valve.state is NotMovingValveState && valve.openRate == 1.0 }

                //cancel() ???
                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }

    @Test
    fun test3() {
        try {
            runBlocking {
                val valve = object : ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay")) {
                    override val timeForFullMotion = 1.5.seconds
                    override val extraTimeForSafety = 0.1.seconds
                }
                val chimney = TestTemperatureSensor("chimney")
                val accumulator = TestTemperatureSensor("accumulator-thermometer")
                val room = TestTemperatureSensor("room")
                val outside = TestTemperatureSensor("outside")
                val openButton = TestButton("open button")
                val closeButton = TestButton("close button")
                val autoButton = TestButton("auto button")
                val stove = StoveController("stove",valve, chimney, accumulator, room, outside, openButton, closeButton, autoButton, TestBasicUserCommunication)
                launch { stove.startControlling() }

                launch { closeButton.longPush() }
                delay(3400)
                assertTrue { valve.state is NotMovingValveState && valve.openRate == 0.0 }
                launch { openButton.longPush() }
                delay(17000)
                assertTrue { valve.state is NotMovingValveState && valve.openRate == 1.0 }
                /*launch { autoButton.longPush() }
                delay(1000)
                chimney.interruptFor(20.toDuration(DurationUnit.SECONDS))
                delay(20000)
                assertTrue { valve.state is OpenValve }
                delay(45000)
                assertTrue { valve.state is ClosedValve }*/

                //cancel() ???
                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }


    @Test
    fun test5() {
        try {
            runBlocking {
                val valve = object : ElectricValveController("air-valve", TestRelay("power-relay"), TestRelay("direction-relay")) {
                    override val timeForFullMotion = 1.5.seconds
                    override val extraTimeForSafety = 0.1.seconds
                }
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
                delay(300)
                assertTrue { valve.state is ClosingValveState }
                delay(1000)
                println(valve.openRate)
                assertTrue { valve.openRate!! in (0.45..0.55) }
                println("open to 0.4")
                launch { valve.setOpenRateTo(0.4) }
                delay(500)
                assertTrue { valve.openRate!! in (0.35..0.45) }

                launch { valve.setOpenRateTo(0.1) }
                delay(10)
                launch { valve.setOpenRateTo(0.3) }
                delay(10)
                launch { valve.setOpenRateTo(0.2) }
                delay(10)
                launch { valve.setOpenRateTo(0.9) }
                delay(10)
                launch { valve.setOpenRateTo(0.1) }
                delay(3000)
                assertTrue { valve.openRate!! in (0.05..0.15) }

                //cancel() ???
                throw InterruptedException()
            }
        } catch (e: InterruptedException) {}
    }

}
