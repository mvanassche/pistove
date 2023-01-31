import kotlinx.coroutines.delay
import kotlinx.serialization.Transient
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SH1107OLED(override val id: String, val bus: Int = 1, val device: Int = 0x3c) : Display, TestableDevice {

    val width = 128
    val height = 128
    val numberOfPages = height / 8
    val buffer = ByteArray(width * height / 8)



    @Transient
    private var _device: I2CBusDevice
    init {
        _device = pi.i2c(bus, device)
    }

    suspend fun write(vararg bytes: Int) {
        _device.write(bytes.map { it.toByte() }.toByteArray())
        //delay(100.toDuration(DurationUnit.MICROSECONDS))
    }
    suspend fun write(vararg bytes: Byte) {
        _device.write(bytes.map { it }.toByteArray())
        //delay(100.toDuration(DurationUnit.MICROSECONDS))
    }



    suspend fun initialize() {
        //val multiplex = 0x7F.toByte() // 128 FIXME not sure!
        val multiplex = 0xFF.toByte() // 128 FIXME not sure!
        val comPins = 0x12.toByte() // FIXME not sure

        val initSeq = arrayOf<Byte>(
                DISPLAY_OFF,
                SETHIGHCOLUMN, 0xB0.toByte(), 0xC8.toByte(),
                SETLOWCOLUMN, 0x10.toByte(), 0x40.toByte(),
                SET_DISPLAY_CLOCK_DIV, 0x80.toByte(),
                SET_MULTIPLEX, multiplex, // set the last value dynamically based on screen size requirement
                //SET_DISPLAY_OFFSET, 0x00.toByte(), // sets offset pro to 0
                SET_DISPLAY_OFFSET, 0x02.toByte(), // sets offset pro to 0
                SET_START_LINE,
                CHARGE_PUMP, 0x14.toByte(), // charge pump val
                MEMORY_MODE, 0x00.toByte(), // 0x0 act like ks0108
                SEG_REMAP, // screen orientation
                COM_SCAN_DEC, // screen orientation change to INC to flip
                SET_COM_PINS, comPins, // com pins val sets dynamically to match each screen size requirement
                SET_CONTRAST, 0xFF.toByte(), // contrast val
                SET_PRECHARGE, 0xF1.toByte(), // precharge val
                SET_VCOM_DETECT, 0x40, // vcom detect
                DISPLAY_ALL_ON_RESUME,
                NORMAL_DISPLAY,
                DISPLAY_ON
        )
        initSeq.forEach {
            write(0x00, it)
        }


        // display something!
        /*write(0x00, COLUMN_ADDR)
        write(0x00, 0x00)
  //      write(0x00, width - 1)
        write(0x00, (height / 8) -1 )
        write(0x00, PAGE_ADDR)
        write(0x00, 0x00)
//        write(0x00, (height / 8) -1 )
        write(0x00, width - 1)*/

       /* (0 until (128 * 128 / 8)).forEach {
            write(0x40, 0xFF)
        }*/
    }

    suspend fun displayRandom(byte: Byte) {
        val startColumn = 0x00
        val endColumn = startColumn + Random.nextInt(16)
        val startPage = 0xB0
        val endPage = startPage + Random.nextInt(16)
        //val byte = Random.nextInt(256)

        //println("display random end-column: $endColumn, end-page: $endPage, byte: $byte")

        /*write(0x00, COLUMN_ADDR)
        write(0x00, 0x00)
        write(0x00, endColumn)
        write(0x00, PAGE_ADDR)
        write(0x00, 0x00)
        write(0x00, endPage)

        (0 until (128 * 128 / 8)).forEach {
            write(0x40, byte)
        }
        */
        (0 until 16).forEach { page ->
            write(0x00, startPage + page)
            write(0x00, 0x02)
            write(0x00, 0x10)
            (0 until 128).forEach {
                write(0x40, byte)
            }
        }
        /*write(0x00, startPage)
        write(0x00, 0x02)
        write(0x00, 0x10)
        (0 until 128).forEach {
            write(0x40, byte)
        }
        write(0x00, startPage + 2)
        write(0x00, 0x02)
        write(0x00, 0x10)
        (0 until 128).forEach {
            write(0x40, byte)
        }*/
    }

    suspend fun displayimage(data: ByteArray) {
        val startPage = 0xB0
        (0 until 16).forEach { page ->
            write(0x00, startPage + page)
            write(0x00, 0x02) // write the low 4 bits for start of coumn
            write(0x00, 0x10) // write the high 4 bits for start of coumn
            (0 until 128).forEach {
                write(0x40, data[(page * 128) + it])
            }
        }
    }

    suspend fun clearArea() {
        /*val startPage = 0xB0
        (4 until 5).forEach { page ->
            write(0x00, startPage + page)
            write(0x00, 0x0E)
            write(0x00, 0x13)
            (0 until 64).forEach {
                write(0x40, 0xFF)
            }
        }*/
    }


    override suspend fun test() {
        initialize()

        displayimage(resourceByeArray("background-oled.bmp"))
        delay(3000)
        clearArea()
        //

        /*(0..10).forEach {
            displayRandom((25*it).toByte())
            delay(3000)
        }*/
    }



    val DISPLAY_OFF: Byte = 0xAE.toByte()
    val DISPLAY_ON: Byte = 0xAF.toByte()
    val SET_DISPLAY_CLOCK_DIV: Byte = 0xD5.toByte()
    val SET_MULTIPLEX: Byte = 0xA8.toByte()
    val SET_DISPLAY_OFFSET: Byte = 0xD3.toByte()
    val SET_START_LINE: Byte = 0x00.toByte()
    val CHARGE_PUMP: Byte = 0x8D.toByte()
    val MEMORY_MODE: Byte = 0x20.toByte()
    val SEG_REMAP: Byte = 0xA1.toByte() // using 0xA0 will flip screen
    val COM_SCAN_DEC: Byte = 0xC8.toByte()
    val COM_SCAN_INC: Byte = 0xC0.toByte()
    val SET_COM_PINS: Byte = 0xDA.toByte()
    val SET_CONTRAST: Byte = 0x81.toByte()
    val SET_PRECHARGE: Byte = 0xd9.toByte()
    val SET_VCOM_DETECT: Byte = 0xDB.toByte()
    val DISPLAY_ALL_ON_RESUME: Byte = 0xA4.toByte()
    val NORMAL_DISPLAY: Byte = 0xA6.toByte()
    val COLUMN_ADDR: Byte = 0x21.toByte()
    val PAGE_ADDR: Byte = 0x22.toByte()
    val INVERT_DISPLAY: Byte = 0xA7.toByte()
    val ACTIVATE_SCROLL: Byte = 0x2F.toByte()
    val DEACTIVATE_SCROLL: Byte = 0x2E.toByte()
    val SET_VERTICAL_SCROLL_AREA: Byte = 0xA3.toByte()
    val RIGHT_HORIZONTAL_SCROLL: Byte = 0x26.toByte()
    val LEFT_HORIZONTAL_SCROLL: Byte = 0x27.toByte()
    val VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL: Byte = 0x29.toByte()
    val VERTICAL_AND_LEFT_HORIZONTAL_SCROLL: Byte = 0x2A.toByte()
    val SETHIGHCOLUMN = 0x10.toByte()
    val SETLOWCOLUMN = 0x00.toByte()

}

expect fun resourceByeArray(name: String): ByteArray
