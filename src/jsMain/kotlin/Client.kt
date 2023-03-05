import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.hasClass
import kotlinx.dom.removeClass
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import pistove.status.physical.temperatureLabel

fun main() {
    val wsProtocol = if(window.location.protocol.startsWith("https")) "wss" else "ws"
    window.onload = {

        val status = ReconnectingWebSocket("$wsProtocol://${window.location.hostname}:${window.location.port}/ws/status") { event: MessageEvent ->
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

        document.addEventListener("visibilitychange", {
            when(document.asDynamic().visibilityState) {
                "hidden" -> status.pause()
                "visible" -> status.resume()
                else -> status.resume()
            }
        })

        // history

        document.getElementById("show-history")?.addEventListener("click", {
            document.getElementById("history")?.let {
                if(it.hasClass("visible")) {
                    it.removeClass("visible")
                } else {
                    it.addClass("visible")
                }
            }
        })
        val historyDiv = document.getElementById("history-graph")
        val historyDataSelectionDiv = document.getElementById("history-data-selection")


        if(historyDiv != null && historyDataSelectionDiv != null) {
            val select = document.createElement("select")
            select.append(document.createElement("option"))
            document.getElementById("history-periods")?.append(select)
            select.addEventListener("change", {
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


class ReconnectingWebSocket(val url: String, val onMessage: (event: MessageEvent) -> Unit) {
    var socket: WebSocket? = null
    var running = false

    init {
        resume()
    }

    fun pause() {
        running = false
        socket?.close()
        socket = null
    }

    fun resume() {
        if (!running) {
            running = true
            socket = WebSocket(url).apply {
                onmessage = onMessage
                onclose = {
                    if (running) {
                        close()
                        window.setTimeout({ resume() }, 10000)
                    }
                }
                onerror = {
                    if (running) {
                        close()
                        window.setTimeout({ resume() }, 10000)
                    }
                }
            }
        }
    }
}