import kotlin.test.Test
import kotlin.test.assertEquals

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

}