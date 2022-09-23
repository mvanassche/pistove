import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.time.Duration


@Serializable
data class StoveControllerHistoryPoint(val atTimePoint: Instant,
                                        val samples: Map<String, Map<String, SampleValue>>) {
    //val controller: StoveController)

    fun toJsonOneLineString(): String {
        return historyFormat.encodeToString(this)
    }
}

interface Sampleable : Identifiable {
    fun sample(validityPeriod: Duration): Map<String, SampleValue>
}

@Serializable
sealed interface SampleValue
{
    val value: Any
}

@Serializable
data class SampleDoubleValue(override val value: Double): SampleValue

@Serializable
data class SampleStringValue(override val value: String): SampleValue

