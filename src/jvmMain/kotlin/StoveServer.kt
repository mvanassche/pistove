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

    val powerRelay = LowActiveGPIOElectricRelay(5)
    val openCloseRelay = LowActiveGPIOElectricRelay(6)
    //val openRelay = TestRelay("open")
    //val closeRelay = TestRelay("close")
    val valve = ElectricValveController(powerRelay = powerRelay, openCloseRelay = openCloseRelay)
    //val chimney = TestTemperatureSensor("chimney")
    val chimney = MAX31855TemperaturSensor(0)
    //val room = TMPDS18B20TemperatureSensor()
    val room = TestTemperatureSensor("room")
    /*val openButton = TestButton("open button")
    val closeButton = TestButton("close button")
    val autoButton = TestButton("auto button")*/
    val buzzer = PassivePiezoBuzzerHardwarePWM(12)
    val openButton = PushButtonGPIO(13)
    val closeButton = PushButtonGPIO(26)
    val autoButton = PushButtonGPIO(19)
    val display = Display1602LCDI2C(1, 0x27)
    val stove = StoveController(valve, chimney, room, openButton, closeButton, autoButton, DisplayAndBuzzerUserCommunication(display, buzzer))
    startWebServer(stove)
    runBlocking {
        launch { stove.startControlling() }
        launch {
            while(true) {
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


fun startWebServer(stove: StoveController) {
    val port = 8080
    embeddedServer(Netty, port = port) {
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
