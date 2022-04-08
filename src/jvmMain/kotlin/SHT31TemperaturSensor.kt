import com.pi4j.Pi4J
import com.pi4j.io.i2c.I2C
import com.pi4j.io.i2c.I2CProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SHT31TemperaturSensor(bus: Int, device: Int) : BaseTemperatureSensor() {

    override val samplingPeriod = 1.toDuration(DurationUnit.SECONDS)
    val _device: I2C

    init {
        synchronized(I2CLock.synchOnMe) {
            val pi4j = Pi4J.newAutoContext()
            val config = I2C.newConfigBuilder(pi4j)
                .bus(bus)
                .device(device)
                .build()
            val i2CProvider = pi4j.provider<I2CProvider>("pigpio-i2c")
            _device = i2CProvider.create(config)
            _device.write(0x30.toByte(), 0xA2.toByte()) // not 100% sure !
            sleep(10)
        }
    }


    override suspend fun sampleValue(): Double {
        val data = ByteArray(6)
        // Send high repeatability measurement command
        // Command msb, command lsb
        val command = ByteArray(2)
        command[0] = 0x2C.toByte()
        command[1] = 0x06.toByte()
        synchronized (I2CLock.synchOnMe) {
            _device.write(command, 0, 2)
            sleep(15) // TODO use delay, but then the synchronized issue?

            // Read 6 bytes of data
            // temp msb, temp lsb, temp CRC, humidity msb, humidity lsb, humidity CRC
            _device.read(data, 0, 6)
        }

        val cTemp: Double = ((data[0].toUByte().toInt() shl(8)) + (data[1].toUByte().toInt())) * 175.0 / 65535.0 - 45.0
        //val humidity: Double = ((data[3].toUByte().toInt() shl(8)) + (data[4].toUByte().toInt())) * 100.0 / 65535.0
        return (cTemp * 10.0).roundToInt().toDouble() / 10.0 // rounding to 1 decimal?
    }
}


fun main(vararg args: String) {
    runBlocking {
        SHT31TemperaturSensor(args[0].toInt(), args[1].toInt(16)).let {
            println("$it: ${it.sampleValue()}°C")
        }
    }
}