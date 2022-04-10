import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration


class PassivePiezoBuzzerHardwarePWM(val bcm: Int, val hardware: Boolean = (bcm in listOf(12, 13, 18, 19))) : Buzzer, TestableDevice {

    val pwm: GPIOPWM
    init {
        pwm = pi.pwm(bcm, hardware)
        pwm.dutyCycle = 50.0
    }

    override suspend fun beep(duration: Duration) {
        beep(4000, duration)
    }

    override suspend fun beep(frequency: Int, duration: Duration) {
        pwm.frequency = frequency
        pwm.on()
        delay(duration.inWholeMilliseconds)
        pwm.off()
    }

    override suspend fun test() {
        delay(500)
        rrrrrr()
        delay(1000)
        bipBip()
        delay(500)
    }
}

fun main(vararg args: String) {
    runBlocking {
        PassivePiezoBuzzerHardwarePWM(args[0].toInt()).apply {
            bipBip()
            rrrrrr()
        }
    }
}