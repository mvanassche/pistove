import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration

@Serializable
class PassivePiezoBuzzerHardwarePWM(override val id: String, val bcm: Int, val hardware: Boolean = (bcm in listOf(12, 13, 18, 19))) : Buzzer, TestableDevice {

    @Transient
    val pwm: GPIOPWM = pi.pwm(bcm, hardware).also {
        it.dutyCycle = 50.0
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