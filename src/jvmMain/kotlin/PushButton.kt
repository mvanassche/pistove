import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalState
import com.pi4j.io.gpio.digital.PullResistance
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class PushButtonGPIO(val gpioPin: Int): PushButton() {
    override suspend fun startSensing() {
        val pi4j = Pi4J.newAutoContext()
        val buttonConfig = DigitalInput.newConfigBuilder(pi4j)
            .id("button")
            .name("Press button")
            .address(gpioPin)
            .pull(PullResistance.PULL_DOWN)
            .debounce(3000L)
            .provider("pigpio-digital-input")
        val button: DigitalInput = pi4j.create(buttonConfig)
        button.addListener({ e ->
            println("Pushbutton $gpioPin state ${e.state()}")
            if (e.state() === DigitalState.LOW) {
                pushed()
            }
        })
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