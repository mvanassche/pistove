import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun main() {
    val stove = stoveController()
    val display = stove.devices.filterIsInstance<StringDisplay>().firstOrNull()
    val server = startWebServer(stove)

    pi.addBeforeShutdown {
        runBlocking { stove.userCommunication.goodbye() }
        server.stop(0, 100)
    }

    runBlocking {
        stove.userCommunication.welcome()
        launch { stove.startControlling() }
        if(display != null) {
            launch {
                while (true) {
                    delay(5000)
                    display.displayTable(stove.stateDisplayString())
                    /*if(valve.state != ValveState.closed && !display.illuminatedBackLight) {
                    display.illuminatedBackLight = true
                } else if(display.illuminatedBackLight) {
                    display.illuminatedBackLight = false
                }*/
                }
            }
        }
        launch {
            while (true) {
                if(stove.valve.isClosed() != false) {
                    delay(15.0.minutes)
                } else {
                    delay(2.0.minutes)
                }
                storeSampleForHistory(stove)
            }
        }
    }
}


fun startWebServer(stove: StoveController): ApplicationEngine {
    val port = 8080
    return embeddedServer(Netty, port = port) {
        install(CORS) {
            anyHost()
        }
        install(WebSockets) {
            //timeout = Duration.ofSeconds(15)
            pingPeriod = Duration.ofSeconds(30)
        }
        install(ContentNegotiation) {
        }

        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title {
                            +"Stove controller"
                        }
                        script(src = "/static/pistove.js") {}
                    }
                    body {
                        style= "min-height: 95vh;" +
                                "display: flex;" +
                                "flex-direction: column;"
                        div {
                            button {
                                onClick = "var r = window.prompt('open rate', '0.5'); fetch('/rate/' + r);"
                                +"set valve open rate"
                            }
                        }
                        div {
                            id = "status"
                        }
                        div {
                            id = "config"
                        }
                        div {
                            id = "history-periods"
                        }
                        div {
                            id = "history-data-selection"
                        }
                        div {
                            id = "history"
                            style ="width: 100%;" +
                                    "flex-grow: 1;"
                        }
                    }
                }
            }
            get("/shutdown") {
                System.exit(0)
            }
            webSocket("/ws/stove-controller") {
                while(true) {
                    try {
                        outgoing.trySend(Frame.Text(encodeToString(module, stove)))
                        delay(3.seconds)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            get("/history") {
                call.respondText(contentType = ContentType.Application.Json) {
                    format.encodeToString(historyPeriods())
                }
            }
            get("/history/{year}") {
                call.respondText(contentType = ContentType.Application.Json) {
                    "[" + (call.parameters["year"]?.toIntOrNull()
                        ?.let {
                            File("/stove/data/history/${it}.json").readLines().joinToString(",")
                        } ?: "") + "]"
                }
            }
            static("/static") {
                resources()
            }
        }
    }.start(wait = false)
}


fun historyPeriods(): List<String> {
    return Path.of("/stove/data/history").listDirectoryEntries().map { it.fileName.name.removeSuffix(".json") }
}

// TODO move to JVM history
fun storeSampleForHistory(stove: StoveController) {
    val now = ZonedDateTime.now()
    File("/stove/data/history/${now.year}.json").apply {
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

fun samplesFromYear(year: Int): List<StoveControllerHistoryPoint> {
    return File("/stove/data/history/${year}.json").readLines()
        .map { historyFormat.decodeFromString<StoveControllerHistoryPoint>(it) }
}