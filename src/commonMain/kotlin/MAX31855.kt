import mu.KotlinLogging

// inspired by https://github.com/twyatt/pi-max31855/blob/master/src/com/traviswyatt/pi/max31855/MAX31855.java


class MAX31855(val bus: Int, val channel: Int) {
    private val logger = KotlinLogging.logger {}

    var spi: GPIOSPI = pi.spi(bus = bus, channel = channel)

    /**
     * Read raw temperature data.
     *
     * @param raw Array of raw temperatures whereas index 0 = internal, 1 = themocouple
     * @return Returns any faults or 0 if there were no faults
     */
    fun readRaw(raw: IntArray): Int? {
        require(raw.size == 2) { "Temperature array must have a length of 2" }

        val BUFFER = ByteArray(4) // no need: it is initialized to 0 { 0.toByte() }
        val transferResult = spi.transfer(BUFFER)
        if(BUFFER.all { it == 0x00.toByte() }) return null // TODO no other way to detect issue? seems not.
        when(transferResult) {
            is OKResult -> {
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
            is ErrorResult -> {
                logger.error { "Error writing to SPI $channel: ${transferResult.errorCode}" }
                return null
            }
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
                if(faults != null) {
                    if ((faults and FAULT_OPEN_CIRCUIT_BIT.toInt()) == FAULT_OPEN_CIRCUIT_BIT.toInt())
                        logger.error { "MAX31855 channel $channel: open circuit" }
                    if ((faults and FAULT_SHORT_TO_GND_BIT.toInt()) == FAULT_SHORT_TO_GND_BIT.toInt())
                        logger.error { "MAX31855 channel $channel: shortcut to ground" }
                    if ((faults and FAULT_SHORT_TO_VCC_BIT.toInt()) == FAULT_SHORT_TO_VCC_BIT.toInt())
                        logger.error { "MAX31855 channel $channel: shortcut to VCC" }
                }
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
}