import kotlinx.coroutines.delay

class Display1602LCDI2C(val bus: Int, val device: Int, illuminated: Boolean = false) : StringDisplay, BackLightDisplay, TestableDevice {
    val lcd = I2CLCD(bus, device).apply {
        init()
        clear()
    }

    override fun display(value: String) {
        if(value.length > 16) {
            var splitAt = value.substring(0, 16).indexOfLast { it.isWhitespace() }
            if (splitAt == -1) {
                splitAt = 16
            }
            lcd.display_string(value.substring(0, splitAt).padEnd(16, ' '), 1)
            lcd.display_string(value.substring(splitAt + 1).padEnd(16, ' '), 2)
        } else {
            lcd.display_string(value.padEnd(16, ' '), 1)
        }
    }

    override var illuminatedBackLight: Boolean = illuminated
        get() = field
        set(value) {
            field = value
            lcd.backlight(field)
        }

    override suspend fun test() {
        display("THIS IS A TEST! SEE THIS MESSAGE?")
        delay(1000)
        illuminatedBackLight = false
        delay(500)
        illuminatedBackLight = true
        display("it works...")
    }
}

fun main(vararg args: String) {
    Display1602LCDI2C(args[0].toInt(), args[1].toInt(16)).display(args[2])
}