import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

fun main() {
    window.onload = {
        val socket = WebSocket("ws://${window.location.hostname}:${window.location.port}/stove")
        socket.onmessage = { event: MessageEvent ->
            event.data?.let {
                val stove = Json.decodeFromString<Stove>(it.toString()) // TODO no way to decode from dynamic directly?
                document.getElementById("status")?.textContent =
                    "\uD83D\uDD25 ${stove.fireTemperature}°C \uD83D\uDCA8 ${stove.airIntake} \uD83C\uDFE0 ${stove.inRoom.temperature}"
            }
        }

        val socket2 = WebSocket("ws://${window.location.hostname}:${window.location.port}/stove-controller")
        socket2.onmessage = { event: MessageEvent ->
            event.data?.let {
                val stoveController = it.toString().decode<StoveController>(module) // TODO no way to decode from dynamic directly?
                document.getElementById("config")?.textContent =
                    (stoveController.valve.openCloseRelay as LowActiveGPIOElectricRelay).bcm.toString()
            }
        }

        Unit
    }
}
