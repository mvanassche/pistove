import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class SerializableTests {

    @Test
    fun test1() {
        val relay = LowActiveGPIOElectricRelay( 23)
        val encoded = Json { encodeDefaults = true }.encodeToString(relay)
        println(encoded)
        Json.decodeFromString<LowActiveGPIOElectricRelay>(encoded)
    }

}