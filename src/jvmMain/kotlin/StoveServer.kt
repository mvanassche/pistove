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

    val valve = ElectricValveController(TestRelay("open"), TestRelay("close"))
    val chimney = TestTemperatureSensor("chimney")
    //val room = TMPDS18B20TemperatureSensor()
    val room = TestTemperatureSensor("room")
    val openButton = TestButton("open button")
    val closeButton = TestButton("close button")
    val autoButton = TestButton("auto button")
    val stove = StoveController(valve, chimney, room, openButton, closeButton, autoButton)
    startWebServer(stove)
    runBlocking {
        stove.startControlling()
    }



    /*
    val xxx = MutableSharedFlow<InstantValue<BigDecimal>>(100, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    var sx = 125.25
    (0..1000).map { Random.nextDouble(10.0) }
    val x = InstantValue(125.25, Instant.now())
    println(Json.encodeToString(x))



    var pi4j = Pi4J.newAutoContext();
    val platforms = pi4j.platforms()

    platforms.describe().print(System.out)

    val PIN_LED = 22; // PIN 15 = BCM 22

    var ledConfig = DigitalOutput.newConfigBuilder(pi4j)
        .id("led")
        .name("LED Flasher")
        .address(PIN_LED)
        .shutdown(DigitalState.LOW)
        .initial(DigitalState.LOW)
    //.provider("pigpio-digital-output");

    var led = pi4j.create(ledConfig);
    led.blink(3, TimeUnit.SECONDS)

    pi4j.shutdown();

     */
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
                        script(src = "/static/stove.js") {}
                    }
                    body {
                        div {
                            id = "status"
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
