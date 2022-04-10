import kotlinx.serialization.Serializable

interface RaspberryPi {

    fun gpioDigitalOutput(bcm: Int, defaultState: DigitalState): GPIODigitalOutput

}

interface GPIOProtocol {
    fun <T> transact(process: GPIOProtocol.() -> T): T {
        return process()
    }
}

enum class DigitalState { low, high }
interface GPIODigitalOutput : GPIOProtocol {
    var state: DigitalState?
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

}