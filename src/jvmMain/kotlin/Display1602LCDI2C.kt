
class Display1602LCDI2C(val bus: Int, val device: Int) : StringDisplay {
    val lcd = I2CLCD("LCD", "LCD", bus, device)

    override fun display(value: String) {
        lcd.clear()
        if(value.length > 16) {
            var splitAt = value.substring(0, 16).indexOfLast { it.isWhitespace() }
            if (splitAt == -1) {
                splitAt = 16
            }
            lcd.display_string(value.substring(0, splitAt), 1)
            lcd.display_string(value.substring(splitAt + 1), 2)
        } else {
            lcd.display_string(value, 1)
        }
    }
}

