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
open class PushButtonGPIO(override val id: String, val bcm: Int, val activeState: DigitalState) : PushButton(), TestableDevice {
    val inactiveState = activeState.not()
    override suspend fun startSensing() {
        val pullUpDown = when(activeState) {
            DigitalState.low -> PullResistance.pull_up
            DigitalState.high -> PullResistance.pull_down
        }
        val input = pi.gpioDigitalInput(bcm, pullUpDown, 3.toDuration(DurationUnit.MILLISECONDS))
        var pushedAt: Instant? = null
        input.addOnChangeListener {
            if(it == inactiveState) {
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
            if(it == activeState) {
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
