import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
class PushButtonGPIO(override val id: String, val bcm: Int) : PushButton(), TestableDevice {
    override suspend fun startSensing() {
        val input = pi.gpioDigitalInput(bcm, PullResistance.pull_down, 3.toDuration(DurationUnit.MILLISECONDS))
        input.addOnChangeListener {
            if(it == DigitalState.low) {
                pushed()
            }
        }
    }

    override suspend fun test() {
        val m = Mutex(true)
        println("Click on $this")
        addOnClickListener {
            println("OK: $this clicked")
            m.unlock()
        }
        coroutineScope { launch { startSensing() } }
        m.lock()
    }
}
