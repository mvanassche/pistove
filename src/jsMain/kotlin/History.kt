import kotlinx.datetime.toJSDate
import org.w3c.dom.Element


fun showHistoryIn(element: Element, history: Array<StoveControllerHistoryPoint>) {
    val keys = history.keys()
    //val keys = listOf(Pair("stove-thermometer", "temperature"))
    val labels = listOf("time") + keys.map { it.first + "." + it.second }
    val data = history.map { historyPoint ->
        arrayOf(historyPoint.atTimePoint.toJSDate()) + keys.map { historyPoint.samples[it.first]?.get(it.second)?.value }
    }.toTypedArray()
    val options = js("{}")
    options.labels = labels.toTypedArray()
    options.showRangeSelector = true
    //options.logscale =  js("{ x : false, y : true }")
    val d = Dygraph(element, data, options)
}

fun Array<StoveControllerHistoryPoint>.keys(): List<Pair<String, String>> {
    // FIXME taking random for knowing values really isn't great!
    val keysFromRandom = this.random().samples.flatMap { device -> device.value.map { Pair(device.key, it.key) } }
    val keysFromLast = this.last().samples.flatMap { device -> device.value.map { Pair(device.key, it.key) } }
    return (keysFromRandom.toSet() + keysFromLast.toSet()).toList()
}


@JsModule("dygraphs/index.es5.js")
@JsNonModule
external class Dygraph(div: Element, data: Array<Array<Any?>>, attrs: Array<String>) {
    fun ready(onReady: () -> Unit)
    fun setAnnotations(annotations: Array<dynamic>)
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
