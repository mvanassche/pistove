import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

fun main() {
    val wsProtocol = if(window.location.protocol.startsWith("https")) "wss" else "ws"
    window.onload = {
        /*val socket = WebSocket("$wsProtocol://${window.location.hostname}:${window.location.port}/ws/stove")
        socket.onmessage = { event: MessageEvent ->
            event.data?.let {
                val stove = Json.decodeFromString<Stove>(it.toString()) // TODO no way to decode from dynamic directly?
                document.getElementById("status")?.textContent =
                    "\uD83D\uDD25 ${stove.fireTemperature}°C \uD83D\uDCA8 ${stove.airIntake} \uD83C\uDFE0 ${stove.inRoom.temperature}"
            }
        }*/

        val socket2 = WebSocket("$wsProtocol://${window.location.hostname}:${window.location.port}/ws/stove-controller")
        socket2.onmessage = { event: MessageEvent ->
            event.data?.let {
                val stoveController = it.toString().decode<StoveController>(module) // TODO no way to decode from dynamic directly?
                val config = document.getElementById("config")
                config?.clear()
                stoveController.identifieables.forEach { identifiable ->
                    identifiable.prettyString()?.also { label ->
                        document.createElement("div").also {
                            it.textContent = label
                            config?.append(it)
                        }
                    }
                }
                /*document.getElementById("config")?.textContent =
                    "Fumes: ${stoveController.fumes.lastValue?.value} (${stoveController.fumes.lastValue?.time})"*/
            }
        }

        Unit
    }
}

fun Identifiable.prettyString() : String? {
    if(this is SensorWithState<*>) {
        this.lastValue?.also {
            return "$id: ${it.value} (${it.time}) "
        }
    }
    if(this is State<*>) {
        return "$id: ${this.state}"
    }
    return null
}
