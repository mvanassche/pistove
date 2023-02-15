import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import io.ktor.server.netty.*
import kotlinx.html.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
        launch {
            delay(30000)
            stove.userCommunication.notify()
        }
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
                if(stove.valve.isMoving() == true || stove.valve.isClosed() == false) {
                    delay(2.0.minutes)
                } else {
                    delay(15.0.minutes)
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

        routing {
            get("/") {
                call.respondText(
                    this::class.java.classLoader.getResource("index.html")!!.readText(),
                    ContentType.Text.Html
                )
            }
            // move to client side!!??
            /*get("/") {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title {
                            +"Stove controller"
                        }
                        script(src = "/static/pistove.js") {}
                    }
                    body {
                        style= "min-height: 90vh;" +
                                "display: flex;" +
                                "flex-direction: column;" +
                                "margin-top: 20px;"
                        div {
                            input {
                                id = "air-intake-valve-rate"
                                style = "width: 200px;"
                                type = InputType.range
                                onChange = "fetch('/rate/' + this.value);"
                                onInput = "this.changing = true;"
                                onMouseUp = "this.changing = false;"
                                onTouchStart = "this.changing = true;"
                                onTouchEnd = "this.changing = false;"
                                min = "0"
                                max = "1"
                                step = "0.1"
                                list = "open-rates"
                            }
                            dataList {
                                style = "display: flex;" +
                                        "flex-direction: column;" +
                                        "justify-content: space-between;" +
                                        "writing-mode: vertical-lr;" +
                                        "width: 200px;"
                                id="open-rates"
                                (0..10).forEach {
                                    option {
                                        value = "${it * 10}"
                                        label = "${it * 10}%"
                                        style = "padding: 0;"
                                    }
                                }
                            }
                            button {
                                onClick = "fetch('/command/recharged');"
                                +"Recharge"
                            }
                            /*button {
                                onClick = "var r = window.prompt('open rate', '0.5'); fetch('/rate/' + r);"
                                +"set valve open rate"
                            }*/
                        }
                        div {
                            id = "status"
                            style = "white-space: break-spaces;"
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
            }*/
            get("/shutdown") {
                System.exit(0)
            }
            // TODO: GET vs POST vs WS message with command classes?
            get("/rate/{rate}") {
                call.parameters["rate"]?.toDoubleOrNull()?.let {
                    stove.setOpenRateTo(it)
                }
                call.respondText("ok")
            }
            get("/command/recharged") {
                stove.onRecharged()
                call.respondText("ok")
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
            webSocket("/ws/status") {
                while(true) {
                    try {
                        outgoing.trySend(Frame.Text(Json.encodeToString(stove.physicalStatus)))
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
            get("/history/{period}") {
                call.respondText(contentType = ContentType.Application.Json) {
                    call.parameters["period"]?.let { jsonStringForPeriod(it) } ?: "[]"
                }
            }
            static("/static") {
                resources()
            }
        }
    }.start(wait = false)
}



