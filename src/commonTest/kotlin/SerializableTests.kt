import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.test.Test

class SerializableTests {

    @Test
    fun test1() {
        val relay = LowActiveGPIOElectricRelay( "test", 23)
        val encoded = format.encodeToString(relay)
        println(encoded)
        format.decodeFromString<LowActiveGPIOElectricRelay>(encoded)
    }

    @Test
    fun test2() {
        val stove = stoveController()
        val encoded = format.encodeToString(stove)
        println(encoded)
        val x = format.decodeFromString<StoveController>(encoded)
        println(x)
    }

    @Test
    fun test3() {
        val stove = stoveController()
        stove.devices
        val encoded = encodeToString(module, stove)
        println(encoded)
        val x = encoded.decode<StoveController>(module)
        println(x)
    }

}
