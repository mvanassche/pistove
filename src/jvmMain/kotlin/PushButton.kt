import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalState
import com.pi4j.io.gpio.digital.PullResistance
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


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
        coroutineScope {
            button.addListener({ e ->
                if (e.state() === DigitalState.LOW) {
                    this.launch { pushed() }
                }
            })
        }
    }
}