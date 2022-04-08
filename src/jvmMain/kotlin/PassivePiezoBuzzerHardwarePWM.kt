import com.pi4j.Pi4J
import com.pi4j.io.pwm.Pwm
import com.pi4j.io.pwm.PwmType
import com.pi4j.plugin.pigpio.provider.pwm.PiGpioPwmProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.time.Duration


class PassivePiezoBuzzerHardwarePWM(val gpioPin: Int) : Buzzer, TestableDevice {

    val pwm: Pwm
    init {
        val config = Pwm.newConfigBuilder(context)
            .id("my-pwm-pin")
            .name("My Test PWM Pin")
            .address(gpioPin)
            .pwmType(PwmType.HARDWARE) // USE HARDWARE PWM
            //.frequency(4000) // optionally pre-configure the desired frequency to 1KHz
            .shutdown(0) // optionally pre-configure a shutdown duty-cycle value (on terminate)
            .initial(50)     // optionally pre-configure an initial duty-cycle value (on startup)
            .build()

        pwm = context.providers().get(PiGpioPwmProvider::class.java).create(config)
        pwm.off() // TODO put in config?
    }

    override suspend fun beep(duration: Duration) {
        beep(4000, duration)
    }

    override suspend fun beep(frequency: Int, duration: Duration) {
        pwm.frequency(frequency)
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