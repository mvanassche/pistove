import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
class PushButtonGPIO(override val id: String, val bcm: Int) : PushButton(), TestableDevice {
    override suspend fun startSensing() {
        val input = pi.gpioDigitalInput(bcm, PullResistance.pull_down, 3.toDuration(DurationUnit.MILLISECONDS))
        var pushedAt: Instant? = null
        input.addOnChangeListener {
            if(it == DigitalState.low) {
                val now = Clock.System.now()
                pushedAt = now
                GlobalScope.launch {
                    delay(2.0.seconds)
                    if(pushedAt == now) {
                        pushedAt = null
                        longPushed()
                    }

                }
            }
            if(it == DigitalState.high) {
                if(pushedAt != null) {
                    pushed()
                }
                pushedAt = null
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
