import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalState
import com.pi4j.io.gpio.digital.PullResistance
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex


class PushButtonGPIO(val gpioPin: Int): PushButton(), TestableDevice {
    override suspend fun startSensing() {
        val buttonConfig = DigitalInput.newConfigBuilder(context)
            .address(gpioPin)
            .pull(PullResistance.PULL_DOWN)
            .debounce(3000L)
            .provider("pigpio-digital-input")
        val button: DigitalInput = context.create(buttonConfig)
        button.addListener({ e ->
            if (e.state() === DigitalState.LOW) {
                pushed()
            }
        })
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

fun main(vararg args: String) {
    runBlocking {
        println("Testing push buttons")
        args.forEach {
            println("Create push button $it")
            val button = PushButtonGPIO(it.toInt())
            button.addOnClickListener { println("Pushed $it") }
            launch {
                println("Starting push button $it")
                button.startSensing()
            }
        }
        while (true) delay(1000)
    }
}