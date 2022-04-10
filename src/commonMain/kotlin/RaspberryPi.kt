

interface RaspberryPi {

    fun gpioDigitalOutput(bcm: Int, defaultState: DigitalState): GPIODigitalOutput

}

interface GPIOProtocol {
    fun <T> transact(process: GPIOProtocol.() -> T): T
}

enum class DigitalState { low, high }
interface GPIODigitalOutput : GPIOProtocol {
    var state: DigitalState?
}


expect fun raspberryPiFromEnvironment(): RaspberryPi
