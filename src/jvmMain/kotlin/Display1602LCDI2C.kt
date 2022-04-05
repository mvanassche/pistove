
class Display1602LCDI2C(val bus: Int, val device: Int, illuminated: Boolean = false) : StringDisplay, BackLightDisplay {
    val lcd = I2CLCD("LCD", "LCD", bus, device)

    override fun display(value: String) {
        //lcd.clear()
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
}

