import kotlinx.serialization.Serializable
import kotlin.time.Duration

interface RaspberryPi {

    fun gpioDigitalOutput(bcm: Int, defaultState: DigitalState): GPIODigitalOutput

    fun gpioDigitalInput(bcm: Int, pullResistance: PullResistance?, debounce: Duration?): GPIODigitalInput

}

interface GPIOProtocol {
    fun <T> transact(process: GPIOProtocol.() -> T): T {
        return process()
    }
}

enum class DigitalState { low, high }

enum class PullResistance { pull_up, pull_down }

interface GPIODigitalOutput : GPIOProtocol {
    var state: DigitalState?
}

interface GPIODigitalInput : GPIOProtocol {
    val state: DigitalState?
    fun addOnChangeListener(listener: (DigitalState?) -> Unit)
    fun removeOnChangeListener(listener: (DigitalState?) -> Unit)
}




val pi by lazy { raspberryPiFromEnvironment() }
expect fun raspberryPiFromEnvironment(): RaspberryPi

@Serializable
object DummyPi : RaspberryPi {
    override fun gpioDigitalOutput(bcm: Int, defaultState: DigitalState): GPIODigitalOutput {
        return object : GPIODigitalOutput {
            override var state: DigitalState? = null
        }
    }

    override fun gpioDigitalInput(bcm: Int, pullResistance: PullResistance?, debounce: Duration?): GPIODigitalInput {
        return object : GPIODigitalInput {
            override val state: DigitalState? = null
            override fun addOnChangeListener(listener: (DigitalState?) -> Unit) {}
            override fun removeOnChangeListener(listener: (DigitalState?) -> Unit) {}
        }
    }

}