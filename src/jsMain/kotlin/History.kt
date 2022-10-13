import kotlinx.browser.document
import kotlinx.datetime.toJSDate
import org.w3c.dom.Element


fun showHistoryIn(historyElement: Element, historyDataSelectionElement: Element, history: Array<StoveControllerHistoryPoint>) {
    val keys = history.keys()
    val graphKeys = keys.map { it.first + "." + it.second }
    //val keys = listOf(Pair("stove-thermometer", "temperature"))
    val labels = listOf("time") + graphKeys
    val data = history.map { historyPoint ->
        arrayOf(historyPoint.atTimePoint.toJSDate()) + keys.map { historyPoint.samples[it.first]?.get(it.second)?.value }
    }.toTypedArray()
    val options = js("{}")
    options.labels = labels.toTypedArray()
    options.showRangeSelector = true
    //options.logscale =  js("{ x : false, y : true }")
    val d = Dygraph(historyElement, data, options)
    graphKeys.forEachIndexed { dataIndex, dataName ->
        document.createElement("span").also { span ->
            span.textContent = dataName
            historyDataSelectionElement.append(span)

            document.createElement("input").also {
                it.setAttribute("type", "checkbox")
                it.setAttribute("checked", "true")
                it.addEventListener("change", { event ->
                    d.setVisibility(dataIndex, it.asDynamic().checked);
                })
                span.append(it)
            }
        }
    }
}

fun Array<StoveControllerHistoryPoint>.keys(): List<Pair<String, String>> {
    // FIXME taking random for knowing values really isn't great!
    val keysFromRandoms = (0..50).flatMap { this.randomOrNull()?.samples?.flatMap { device -> device.value.map { Pair(device.key, it.key) } }.orEmpty() }
    val keysFromLast = this.last().samples.flatMap { device -> device.value.map { Pair(device.key, it.key) } }
    return (keysFromRandoms.toSet() + keysFromLast.toSet()).toList()
}


@JsModule("dygraphs/index.es5.js")
@JsNonModule
external class Dygraph(div: Element, data: Array<Array<Any?>>, attrs: Array<String>) {
    fun ready(onReady: () -> Unit)
    fun setAnnotations(annotations: Array<dynamic>)
    fun setVisibility(dataIndex: Int, visible: Boolean)
}

fun dygraphAnnotation(series: String, x: Any, shortText: String? = null, longText: String? = null): dynamic {
    val result = js("{}")
    result.series = series
    result.x = x
    result.time = x
    result.shortText = shortText
    result.longText = longText
    return result
}
