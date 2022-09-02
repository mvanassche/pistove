import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class StoveControllerHistoryPoint(val atTimePoint: Instant, val controller: StoveController)