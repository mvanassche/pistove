import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface Buzzer: Actuator {

    suspend fun beep(duration: Duration)
    suspend fun beep(frequency: Int, duration: Duration)

    suspend fun bipBip() {
        beep(50.toDuration(DurationUnit.MILLISECONDS))
        delay(40)
        beep(50.toDuration(DurationUnit.MILLISECONDS))
    }

    suspend fun rrrrrr() {
        beep(1000, 1.toDuration(DurationUnit.SECONDS))
    }

}