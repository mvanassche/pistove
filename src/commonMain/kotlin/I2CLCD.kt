import kotlinx.coroutines.delay
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// adapted from https://github.com/Poduzov/PI4J-I2C-LCD/blob/master/I2CLCD.java
class I2CLCD(bus: Int, device: Int) {
    private var _device: I2CBusDevice
    init {
        _device = pi.i2c(bus, device)
    }
    // Write a single command
    suspend private fun write_cmd(cmd: Byte) {
        try {
            _device.write(byteArrayOf(cmd))
            delay(100.toDuration(DurationUnit.MICROSECONDS))
        } catch (ex: Exception) {
            println(ex.message)
        }
    }

    private val LCD_CLEARDISPLAY = 0x01
    private val LCD_RETURNHOME = 0x02
    private val LCD_ENTRYMODESET = 0x04
    private val LCD_DISPLAYCONTROL = 0x08
    private val LCD_CURSORSHIFT = 0x10
    private val LCD_FUNCTIONSET = 0x20
    private val LCD_SETCGRAMADDR = 0x40
    private val LCD_SETDDRAMADDR = 0x80

    // flags for display entry mode
    private val LCD_ENTRYRIGHT = 0x00
    private val LCD_ENTRYLEFT = 0x02
    private val LCD_ENTRYSHIFTINCREMENT = 0x01
    private val LCD_ENTRYSHIFTDECREMENT = 0x00

    // flags for display on/off control
    private val LCD_DISPLAYON = 0x04
    private val LCD_DISPLAYOFF = 0x00
    private val LCD_CURSORON = 0x02
    private val LCD_CURSOROFF = 0x00
    private val LCD_BLINKON = 0x01
    private val LCD_BLINKOFF = 0x00

    // flags for display/cursor shift
    private val LCD_DISPLAYMOVE = 0x08
    private val LCD_CURSORMOVE = 0x00
    private val LCD_MOVERIGHT = 0x04
    private val LCD_MOVELEFT = 0x00

    // flags for function set
    private val LCD_8BITMODE = 0x10
    private val LCD_4BITMODE = 0x00
    private val LCD_2LINE = 0x08
    private val LCD_1LINE = 0x00
    private val LCD_5x10DOTS = 0x04
    private val LCD_5x8DOTS = 0x00

    // flags for backlight control
    private val LCD_BACKLIGHT = 0x08
    private val LCD_NOBACKLIGHT = 0x00
    private val En = 4 // Enable bit
    private val Rw = 2 // Read/Write bit
    private val Rs = 1 // Register select bit

    //initializes objects and lcd
    suspend fun init() {
        try {
            lcd_write(0x03.toByte())
            lcd_write(0x03.toByte())
            lcd_write(0x03.toByte())
            lcd_write(0x02.toByte())
            lcd_write((LCD_FUNCTIONSET or LCD_2LINE or LCD_5x8DOTS or LCD_4BITMODE).toByte())
            lcd_write((LCD_DISPLAYCONTROL or LCD_DISPLAYON).toByte())
            lcd_write(LCD_CLEARDISPLAY.toByte())
            lcd_write((LCD_ENTRYMODESET or LCD_ENTRYLEFT).toByte())
            delay(200.toDuration(DurationUnit.MICROSECONDS))
        } catch (ex: Exception) {
            println(ex.message)
        }
    }

    // clocks EN to latch command
    private suspend fun lcd_strobe(data: Byte) {
        try {
            _device.write(byteArrayOf((data.toInt() or En or LCD_BACKLIGHT).toByte()))
            delay(500.toDuration(DurationUnit.MICROSECONDS))
            _device.write(byteArrayOf((data.toInt() and En.inv() or LCD_BACKLIGHT).toByte()))
            delay(100.toDuration(DurationUnit.MICROSECONDS))
        } catch (ex: Exception) {
            println(ex.message)
        }
    }

    private suspend fun lcd_write_four_bits(data: Byte) {
        try {
            _device.write(byteArrayOf((data.toInt() or LCD_BACKLIGHT).toByte()))
            lcd_strobe(data)
        } catch (ex: Exception) {
            println(ex.message)
        }
    }

    private suspend fun lcd_write(cmd: Byte, mode: Byte) {
        lcd_write_four_bits((mode.toInt() or (cmd.toInt() and 0xF0)).toByte())
        lcd_write_four_bits((mode.toInt() or (cmd.toInt() shl 4 and 0xF0)).toByte())
    }

    // write a command to lcd
    private suspend fun lcd_write(cmd: Byte) {
        lcd_write(cmd, 0.toByte())
    }

    // write a character to lcd
    suspend fun write_char(charvalue: Byte) {
        val mode = 1
        lcd_write_four_bits((mode or (charvalue.toInt() and 0xF0)).toByte())
        lcd_write_four_bits((mode or (charvalue.toInt() shl 4 and 0xF0)).toByte())
    }

    // put string function
    suspend fun display_string(string: String, line: Int) {
        when (line) {
            1 -> lcd_write(0x80.toByte())
            2 -> lcd_write(0xC0.toByte())
            3 -> lcd_write(0x94.toByte())
            4 -> lcd_write(0xD4.toByte())
        }
        for (i in 0 until string.length) {
            lcd_write(string[i].code.toByte(), Rs.toByte())
        }
    }

    // clear lcd and set to home
    suspend fun clear() {
        lcd_write(LCD_CLEARDISPLAY.toByte())
        lcd_write(LCD_RETURNHOME.toByte())
    }

    // define backlight on / off(lcd.backlight(1) off = lcd.backlight(0)
    suspend fun backlight(state: Boolean) {
        //for state, 1 = on, 0 = off
        if (state) {
            write_cmd(LCD_BACKLIGHT.toByte())
        } else {
            write_cmd(LCD_NOBACKLIGHT.toByte())
        }
    }

    // add custom characters(0 - 7)
    private suspend fun load_custom_chars(fontdata: Array<ByteArray>) {
        lcd_write(0x40.toByte())
        for (i in fontdata.indices) {
            for (j in 0 until fontdata[i].size) {
                write_char(fontdata[i][j])
            }
        }
    }

    // define precise positioning (addition from the forum)
    suspend fun display_string_pos(string: String, line: Int, pos: Int) {
        var pos_new: Byte = 0
        if (line == 1) {
            pos_new = pos.toByte()
        } else if (line == 2) {
            pos_new = (0x40 + pos).toByte()
        } else if (line == 3) {
            pos_new = (0x14 + pos).toByte()
        } else if (line == 4) {
            pos_new = (0x54 + pos).toByte()
        }
        lcd_write((0x80 + pos_new).toByte())
        for (i in 0 until string.length) {
            lcd_write(string[i].code.toByte(), Rs.toByte())
        }
    }

}