import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SHT31TemperaturSensor(bus: Int, device: Int) : BaseTemperatureSensor(), TestableDevice {

    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)
    val _device: I2CBusDevice

    init {
        _device = pi.i2c(bus, device)
        _device.write(byteArrayOf(0x30.toByte(), 0xA2.toByte())) // not 100% sure !
        sleep(10)
    }

    override suspend fun sampleValue(): Double {
        val data = ByteArray(6)
        // Send high repeatability measurement command
        // Command msb, command lsb
        val command = ByteArray(2)
        command[0] = 0x2C.toByte()
        command[1] = 0x06.toByte()
        _device.transact {
            write(command)
            sleep(15) // TODO use delay, but then the synchronized issue?
            // Read 6 bytes of data
            // temp msb, temp lsb, temp CRC, humidity msb, humidity lsb, humidity CRC
            read(data)
        }
        // TODO check CRC!!

        val cTemp: Double = ((data[0].toUByte().toInt() shl(8)) + (data[1].toUByte().toInt())) * 175.0 / 65535.0 - 45.0
        //val humidity: Double = ((data[3].toUByte().toInt() shl(8)) + (data[4].toUByte().toInt())) * 100.0 / 65535.0
        return (cTemp * 10.0).roundToInt().toDouble() / 10.0 // rounding to 1 decimal?
    }

    override suspend fun test() {
        repeat(10) {
            println("$this: ${sampleValue()}°C")
            delay(500)
        }
    }
}


fun main(vararg args: String) {
    runBlocking {
        SHT31TemperaturSensor(args[0].toInt(), args[1].toInt(16)).let {
            println("$it: ${it.sampleValue()}°C")
        }
    }
}