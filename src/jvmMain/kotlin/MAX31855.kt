import com.pi4j.Pi4J
import com.pi4j.io.spi.Spi
import com.pi4j.io.spi.SpiProvider
import java.util.*

class MAX31855(private val channel: Int) {
    private val BUFFER = ByteArray(4)
    var spi: Spi

    /**
     * Read raw temperature data.
     *
     * @param raw Array of raw temperatures whereas index 0 = internal, 1 = themocouple
     * @return Returns any faults or 0 if there were no faults
     */
    fun readRaw(raw: IntArray): Int {
        require(raw.size == 2) { "Temperature array must have a length of 2" }

        // http://stackoverflow.com/a/9128762/196486
        Arrays.fill(BUFFER, 0.toByte()) // clear buffer
        spi.transfer(BUFFER, 4) //wiringPiSPIDataRW(channel, BUFFER, 4);
        val data: Int = BUFFER[0].toInt() and 0xFF shl 24 or
                (BUFFER[1].toInt() and 0xFF shl 16) or
                (BUFFER[2].toInt() and 0xFF shl 8) or
                (BUFFER[3].toInt() and 0xFF)
        var internal = (data shr 4 and LSB_11)
        if (data and INTERNAL_SIGN_BIT == INTERNAL_SIGN_BIT) {
            internal = -(internal.inv() and LSB_11)
        }
        var thermocouple = (data shr 18 and LSB_13)
        if (data and THERMOCOUPLE_SIGN_BIT == THERMOCOUPLE_SIGN_BIT) {
            thermocouple = -(thermocouple.inv() and LSB_13)
        }
        raw[0] = internal
        raw[1] = thermocouple
        return if (data and FAULT_BIT == FAULT_BIT) {
            data and 0x07
        } else {
            0 // no faults
        }
    }

    /**
     * Converts raw internal temperature to actual internal temperature.
     *
     * @param raw Raw internal temperature
     * @return Actual internal temperature (C)
     */
    fun getInternalTemperature(raw: Int): Float {
        return raw * 0.0625f
    }

    /**
     * Converts raw thermocouple temperature to actual thermocouple temperature.
     *
     * @param raw Raw thermocouple temperature
     * @return Actual thermocouple temperature (C)
     */
    fun getThermocoupleTemperature(raw: Int): Float {
        return raw * 0.25f
    }

    // TODO
    val temperature: Float
        get() {
            val raw = IntArray(2)
            val faults = readRaw(raw)
            return if (faults == 0) {
                getThermocoupleTemperature(raw[1])
            } else {
                // TODO
                println(faults)
                Float.NaN
            }
        }

    companion object {
        const val THERMOCOUPLE_SIGN_BIT = -0x80000000 // D31
        const val INTERNAL_SIGN_BIT = 0x8000 // D15
        const val FAULT_BIT = 0x10000 // D16
        const val FAULT_OPEN_CIRCUIT_BIT: Byte = 0x01 // D0
        const val FAULT_SHORT_TO_GND_BIT: Byte = 0x02 // D1
        const val FAULT_SHORT_TO_VCC_BIT: Byte = 0x04 // D2

        /**
         * 11 of the least most significant bits (big endian) set to 1.
         */
        const val LSB_11 = 0x07FF

        /**
         * 13 of the least most significant bits (big endian) set to 1.
         */
        const val LSB_13 = 0x1FFF
    }

    init {

        // Initialize Pi4J with an auto context
        // An auto context includes AUTO-DETECT BINDINGS enabled
        // which will load all detected Pi4J extension libraries
        // (Platforms and Providers) in the class path
        val pi4j = Pi4J.newAutoContext()

        // create SPI config
        val config = Spi.newConfigBuilder(pi4j)
            .id("my-spi-device")
            .name("My SPI Device")
            .address(channel)
            .baud(500000) //Spi.DEFAULT_BAUD)
            .build()

        // get a SPI I/O provider from the Pi4J context
        val spiProvider = pi4j.provider<SpiProvider>("pigpio-spi")

        // use try-with-resources to auto-close SPI when complete
        spi = spiProvider.create(config)
        // open SPI communications
        spi.open()
    }
}