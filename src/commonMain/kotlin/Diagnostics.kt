import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDiagnostic(
    val id: String,
    val type: String,
    val lastValue: String?,
    val lastValueAgeSeconds: Double?,
    val stale: Boolean,
    val lastFault: String?,
    val lastFaultAgeSeconds: Double?
)

private const val DEFAULT_STALE_AFTER_SECONDS = 30.0

@Serializable
data class DiagnosticsSnapshot(
    val operational: Boolean,
    val messages: List<String>,
    val devices: List<DeviceDiagnostic>
)

/**
 * Snapshot of every known device, meant for a human debugging the physical setup (e.g. telling apart a
 * wiring/hardware fault on one sensor from the whole thing going quiet), plus the overall operational
 * status (see [StoveController.fumesOperational]). Not used by the normal stove UI.
 */
fun StoveController.diagnostics(): DiagnosticsSnapshot {
    val now = Clock.System.now()
    val deviceDiagnostics = devices.sortedBy { it.id }.map { device ->
        val lastValue = (device as? SensorWithState<*>)?.lastValue
        val lastFault = (device as? FaultReporting)?.lastFault
        val staleAfterSeconds = (device as? BaseSamplingValuesSensor<*>)?.samplingPeriod
            ?.inWholeMilliseconds?.times(3)?.div(1000.0)
            ?: DEFAULT_STALE_AFTER_SECONDS

        val lastValueAgeSeconds = lastValue?.let { (now - it.time).inWholeMilliseconds / 1000.0 }
        DeviceDiagnostic(
            id = device.id,
            type = device::class.simpleName ?: "?",
            lastValue = lastValue?.value?.toString(),
            lastValueAgeSeconds = lastValueAgeSeconds,
            stale = device is Sensor && (lastValueAgeSeconds == null || lastValueAgeSeconds > staleAfterSeconds),
            lastFault = lastFault?.value,
            lastFaultAgeSeconds = lastFault?.let { (now - it.time).inWholeMilliseconds / 1000.0 }
        )
    }
    return DiagnosticsSnapshot(fumesOperational, operationalMessages, deviceDiagnostics)
}
