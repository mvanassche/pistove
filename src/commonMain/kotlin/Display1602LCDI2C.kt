import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Display1602LCDI2C(override val id: String, val bus: Int, val device: Int) : Display, StringDisplay, BackLightDisplay, TestableDevice, State<String> {

    @Transient
    private var _lcd: I2CLCD? = null
    private suspend fun lcd(): I2CLCD {
        if(_lcd == null) {
            _lcd = I2CLCD(bus, device).apply {
                init()
                clear()
            }
        }
        return _lcd!!
    }

    override suspend fun display(value: String) {
        state = value
        if(value.length > 16) {
            var splitAt = value.substring(0, 16).indexOfLast { it.isWhitespace() }
            if (splitAt == -1) {
                splitAt = 16
            }
            lcd().display_string(value.substring(0, splitAt).padEnd(16, ' '), 1)
            lcd().display_string(value.substring(splitAt + 1).padEnd(16, ' '), 2)
        } else {
            lcd().display_string(value.padEnd(16, ' '), 1)
        }
    }

    override suspend fun backLight(illuminated: Boolean) {
        lcd().backlight(illuminated)
    }

    override suspend fun test() {
        display("THIS IS A TEST! SEE THIS MESSAGE?")
        delay(1000)
        backLight(false)
        delay(500)
        backLight(true)
        display("it works...")
    }

    override var state: String = ""
}
