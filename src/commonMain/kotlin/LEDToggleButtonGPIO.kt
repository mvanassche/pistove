import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * bcm led down -> LED on
 *
 */
@Serializable
class LEDToggleButtonGPIO(override val id: String, val bcmLED: Int, val bcmButton: Int) : ToggleButton(), TestableDevice {
    var led: GPIODigitalOutput? = null
    override suspend fun startSensing() {
        val input = pi.gpioDigitalInput(bcmButton, null, 100.toDuration(DurationUnit.MILLISECONDS))
        input.addOnChangeListener {
            if(it == DigitalState.low) {
                changed(false)
            }
            if(it == DigitalState.high) {
                changed(true)
            }
        }
        led = pi.gpioDigitalOutput(bcmLED, DigitalState.high)

    }

    fun changedLED(on: Boolean) {
        println("LED $led")
        led?.state = if(on) DigitalState.low else DigitalState.high
    }

    override suspend fun test() {
        startSensing()
        changedLED(true)
        delay(1000)
        changedLED(false)
        delay(1000)
        changedLED(true)
        delay(1000)
        changedLED(false)
        delay(1000)
        changedLED(true)
        delay(1000)
        changedLED(false)
        delay(1000)
        changedLED(true)
        delay(1000)
        changedLED(false)
        val m = Mutex(true)
        println("Click on $this")
        var i = 5
        addChangeListener {
            println("$this changed: $it")
            changedLED(it)
            if(i-- < 0) m.unlock()
        }
        m.lock()
    }
}
