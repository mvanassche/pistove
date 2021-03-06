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
import kotlinx.html.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun main() {
    val stove = stoveController()
    val display = stove.devices.filterIsInstance<StringDisplay>().firstOrNull()
    val server = startWebServer(stove)

    Runtime.getRuntime().addShutdownHook(Thread() {
        if(!context.isShutdown) {
            runBlocking { stove.userCommunication.goodbye() }
        }
        server.stop(0, 100)
        context.shutdown()
    })

    runBlocking {
        stove.userCommunication.welcome()
        launch { stove.startControlling() }
        if(display != null) {
            launch {
                while (true) {
                    delay(5000)
                    display.display(stove.stateMessage())
                    /*if(valve.state != ValveState.closed && !display.illuminatedBackLight) {
                    display.illuminatedBackLight = true
                } else if(display.illuminatedBackLight) {
                    display.illuminatedBackLight = false
                }*/
                }
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
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title {
                            +"Stove controller"
                        }
                        script(src = "/static/pistove.js") {}
                    }
                    body {
                        div {
                            id = "status"
                        }
                        div {
                            button {
                                onClick = "fetch('/open')"
                                +"open"
                            }
                            button {
                                onClick = "fetch('/close')"
                                +"close"
                            }
                            button {
                                onClick = "fetch('/auto')"
                                +"auto"
                            }
                        }
                        div {
                            id = "config"
                        }
                    }
                }
            }
            get("/open") {
                stove.open()
                call.respondText("open")
            }
            get("/close") {
                stove.close()
                call.respondText("closed")
            }
            get("/auto") {
                launch { stove.auto() }
                call.respondText("auto engaged")
            }
            get("/shutdown") {
                System.exit(0)
            }
            webSocket("/stove") {
                while(true) {
                    try {
                        // TODO isn't there a direct way to send value serialized instead of using Frame.Text?
                        outgoing.trySend(Frame.Text(Json.encodeToString(stove.toStove())))
                        delay(3.seconds)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            webSocket("/stove-controller") {
                while(true) {
                    try {
                        outgoing.trySend(Frame.Text(encodeToString(module, stove)))
                        delay(3.seconds)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            webSocket("/display") {
                while(true) {
                    try {
                        outgoing.trySend(Frame.Text(stove.stateMessage()))
                        delay(3.seconds)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            static("/static") {
                resources()
            }
        }
    }.start(wait = false)
}

