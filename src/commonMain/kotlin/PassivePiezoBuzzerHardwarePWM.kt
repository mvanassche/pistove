import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration

@Serializable
class PassivePiezoBuzzerHardwarePWM(override val id: String, val bcm: Int, val hardware: Boolean) : Buzzer, TestableDevice {
    constructor(id: String, bcm: Int) : this(id, bcm, (bcm in listOf(12, 13, 18, 19))) // see https://github.com/Kotlin/kotlinx.serialization/issues/1904

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

/*fun main(vararg args: String) {
    runBlocking {
        PassivePiezoBuzzerHardwarePWM(args[0].toInt()).apply {
            bipBip()
            rrrrrr()
        }
    }
}*/