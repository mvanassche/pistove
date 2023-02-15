import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import kotlinx.html.*
import kotlinx.html.dom.*
import org.w3c.dom.HTMLElement
import pistove.status.physical.temperatureLabel

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

        /*val socket2 = WebSocket("$wsProtocol://${window.location.hostname}:${window.location.port}/ws/stove-controller")
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
        }*/

        reconnectingWebSocket("$wsProtocol://${window.location.hostname}:${window.location.port}/ws/status") { event: MessageEvent ->
            event.data?.let {
                val controller = Json.decodeFromString<pistove.status.physical.Controller>(it.toString())
                val status = document.getElementById("status")
                //status?.clear()

                status?.querySelector("#auto-mode")?.let { it.textContent = if(controller.autoMode) "Auto" else "Manual" }
                status?.querySelector("#environment .temperature")?.let { it.textContent = controller.controlledEnvironment.temperature.temperatureLabel() }
                status?.querySelector("#house .temperature")?.let { it.textContent = controller.controlledEnvironment.house.temperature.temperatureLabel() }
                status?.querySelector("#valve")?.let {
                    it.textContent = controller.controlledEnvironment.house.stove.valve.toString()
                    it.setAttribute("style", "--open-rate: ${controller.controlledEnvironment.house.stove.valve?.nominalRate};")
                }
                status?.querySelector("#fire .temperature")?.let { it.textContent = controller.controlledEnvironment.house.stove.burningChamber.temperature.temperatureLabel() }
                status?.querySelector("#accumulator .temperature")?.let { it.textContent = controller.controlledEnvironment.house.stove.accumulator.temperature.temperatureLabel() }
                status?.querySelector("#accumulator .charge")?.let { it.textContent = "${controller.controlledEnvironment.house.stove.accumulator.chargedRate?.value?.let { it * 100.0 }?.toString(0)}%" }
                status?.querySelector("#accumulator")?.let { it.setAttribute("style", "--charged-rate: ${controller.controlledEnvironment.house.stove.accumulator.chargedRate?.value};") }

                //status?.innerHTML = controller.toString()

                document.getElementById("air-intake-valve-rate-input")?.let {
                    val nominalRate = controller.controlledEnvironment.house.stove.valve?.nominalRate
                    if(nominalRate != null && !it.asDynamic().changing as Boolean) {
                        it.asDynamic().value = nominalRate
                    }
                }
            }
        }


        val historyDiv = document.getElementById("history")
        val historyDataSelectionDiv = document.getElementById("history-data-selection")


        if(historyDiv != null && historyDataSelectionDiv != null) {
            val select = document.createElement("select")
            select.append(document.createElement("option"))
            document.getElementById("history-periods")?.append(select)
            select.addEventListener("change", {
                println(select.asDynamic().value)
                window.fetch("/history/${select.asDynamic().value}").then {
                    it.text().then {
                        showHistoryIn(historyDiv, historyDataSelectionDiv, historyFormat.decodeFromString(it))
                    }
                }
            })

            window.fetch("/history").then {
                it.text().then {
                    format.decodeFromString<List<String>>(it).forEach { period ->
                        select.append(
                            document.createElement("option").also {
                                it.textContent = period
                                it.setAttribute("value", period)
                            }
                        )
                    }
                }
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

fun reconnectingWebSocket(url: String, onMessage: (event: MessageEvent) -> Unit) {
    var socketStatus = WebSocket(url)
    socketStatus.onmessage = onMessage
    socketStatus.onclose = {
        socketStatus.close()
        window.setTimeout({ reconnectingWebSocket(url, onMessage) }, 10000)
    }
    socketStatus.onerror = {
        socketStatus.close()
        window.setTimeout({ reconnectingWebSocket(url, onMessage) }, 10000)
    }
}

