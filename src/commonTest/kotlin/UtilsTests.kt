import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTests {

    @Test
    fun padStringElementsToFit1() {
        println(padStringElementsToFit(16, listOf("12°", "19°", "851°", "185°")))
        println(padStringElementsToFit(16, listOf("12°", "19°", "51°", "?°")))
        println(padStringElementsToFit(16, listOf("19°", "51°", "??°")))
        println(padStringElementsToFit(16, listOf("19°", "closing", "??%")))
    }

    @Test
    fun doubleFormat() {
        assertEquals("12346", 12345.6789.toString(0))
        assertEquals("12345.7", 12345.6789.toString(1))
        assertEquals("12345.68", 12345.6789.toString(2))
    }

    @Test
    fun linearTest1() {
        val f = LinearFunction(Pair(250.0, 0.7), Pair(100.0, 0.0))
        assertTrue { (f.y(100.0) - 0.0).absoluteValue < 0.000001 }
        assertTrue { (f.y(250.0) - 0.7).absoluteValue < 0.000001 }
        assertTrue { (f.y(175.0) - 0.35).absoluteValue < 0.000001 }
        println("140 -> " + f.y(140.0))
        println("120 -> " + f.y(120.0))
    }

}