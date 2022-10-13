import io.ktor.application.*
import kotlinx.datetime.Clock
import java.io.File
import java.nio.file.Path
import java.time.ZonedDateTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun historyPeriods(): List<String> {
    return Path.of("/stove/data/history").listDirectoryEntries().map { it.fileName.name.removeSuffix(".json") }
}

fun storeSampleForHistory(stove: StoveController) {
    val now = ZonedDateTime.now()
    File("/stove/data/history/${now.year}-${now.month.value}.json").apply {
        parentFile.mkdirs()
        createNewFile()
    }
        .appendText(
            StoveControllerHistoryPoint(
                Clock.System.now(),
                stove.identifieables.filterIsInstance<Sampleable>().map { it.id to it.sample(5.toDuration(DurationUnit.MINUTES)) }.toMap()).toJsonOneLineString() + "\n"
            //format.encodeToString(StoveControllerHistoryPoint(Clock.System.now(), stove))
        )
}


fun jsonStringForPeriod(period: String): String {
    return "[" + (period.let {
        File("/stove/data/history/${it}.json").readLines().joinToString(",")
    }) + "]"

}