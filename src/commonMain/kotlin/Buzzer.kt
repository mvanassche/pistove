import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
sealed interface Buzzer: Actuator {

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

    suspend fun dodadi() {
        beep(3130, 50.toDuration(DurationUnit.MILLISECONDS))
        delay(20)
        beep(3520, 100.toDuration(DurationUnit.MILLISECONDS))
        delay(20)
        beep(3951, 150.toDuration(DurationUnit.MILLISECONDS))
    }

    suspend fun didado() {
        beep(3951, 50.toDuration(DurationUnit.MILLISECONDS))
        delay(20)
        beep(3520, 100.toDuration(DurationUnit.MILLISECONDS))
        delay(20)
        beep(3130, 150.toDuration(DurationUnit.MILLISECONDS))
    }

}