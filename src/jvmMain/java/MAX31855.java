import com.pi4j.Pi4J;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiProvider;

import java.util.Arrays;
public class MAX31855 {

    public static final int THERMOCOUPLE_SIGN_BIT = 0x80000000; // D31
    public static final int INTERNAL_SIGN_BIT     = 0x8000;     // D15

    public static final int FAULT_BIT = 0x10000; // D16

    public static final byte FAULT_OPEN_CIRCUIT_BIT = 0x01; // D0
    public static final byte FAULT_SHORT_TO_GND_BIT = 0x02; // D1
    public static final byte FAULT_SHORT_TO_VCC_BIT = 0x04; // D2

    /**
     * 11 of the least most significant bits (big endian) set to 1.
     */
    public static final int LSB_11 = 0x07FF;

    /**
     * 13 of the least most significant bits (big endian) set to 1.
     */
    public static final int LSB_13 = 0x1FFF;

    private final byte[] BUFFER = new byte[4];

    private final int channel;

    Spi spi;

    public MAX31855(int channel) {
        this.channel = channel;

        // Initialize Pi4J with an auto context
        // An auto context includes AUTO-DETECT BINDINGS enabled
        // which will load all detected Pi4J extension libraries
        // (Platforms and Providers) in the class path
        var pi4j = Pi4J.newAutoContext();

        // create SPI config
        var config  = Spi.newConfigBuilder(pi4j)
                .id("my-spi-device")
                .name("My SPI Device")
                .address(channel)
                .baud(500000) //Spi.DEFAULT_BAUD)
                .build();

        // get a SPI I/O provider from the Pi4J context
        SpiProvider spiProvider = pi4j.provider("pigpio-spi");

        // use try-with-resources to auto-close SPI when complete
        spi = spiProvider.create(config);
        // open SPI communications
        spi.open();

    }

    /**
     * Read raw temperature data.
     *
     * @param raw Array of raw temperatures whereas index 0 = internal, 1 = themocouple
     * @return Returns any faults or 0 if there were no faults
     */
    public int readRaw(int[] raw) {
        if (raw.length != 2)
            throw new IllegalArgumentException("Temperature array must have a length of 2");

        // http://stackoverflow.com/a/9128762/196486
        Arrays.fill(BUFFER, (byte) 0); // clear buffer

        spi.transfer(BUFFER, 4);  //wiringPiSPIDataRW(channel, BUFFER, 4);

        int data = ((BUFFER[0] & 0xFF) << 24) |
                ((BUFFER[1] & 0xFF) << 16) |
                ((BUFFER[2] & 0xFF) <<  8) |
                (BUFFER[3] & 0xFF);

        int internal = (int) ((data >> 4) & LSB_11);
        if ((data & INTERNAL_SIGN_BIT) == INTERNAL_SIGN_BIT) {
            internal = -(~internal & LSB_11);
        }

        int thermocouple = (int) ((data >> 18) & LSB_13);
        if ((data & THERMOCOUPLE_SIGN_BIT) == THERMOCOUPLE_SIGN_BIT) {
            thermocouple = -(~thermocouple & LSB_13);
        }

        raw[0] = internal;
        raw[1] = thermocouple;

        if ((data & FAULT_BIT) == FAULT_BIT) {
            return data & 0x07;
        } else {
            return 0; // no faults
        }
    }

    /**
     * Converts raw internal temperature to actual internal temperature.
     *
     * @param raw Raw internal temperature
     * @return Actual internal temperature (C)
     */
    public float getInternalTemperature(int raw) {
        return raw * 0.0625f;
    }

    /**
     * Converts raw thermocouple temperature to actual thermocouple temperature.
     *
     * @param raw Raw thermocouple temperature
     * @return Actual thermocouple temperature (C)
     */
    public float getThermocoupleTemperature(int raw) {
        return raw * 0.25f;
    }

    public float getTemperature() {
        int[] raw = new int[2];
        int faults = readRaw(raw);

        if(faults == 0) {
            return getThermocoupleTemperature(raw[1]);
        } else {
            // TODO
            System.out.println(faults);
            return Float.NaN;
        }
    }



    public static void main(String[] args) {
        // https://projects.drogon.net/understanding-spi-on-the-raspberry-pi/
        // http://developer-blog.net/wp-content/uploads/2013/09/raspberry-pi-rev2-gpio-pinout.jpg
        // http://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus

        int channel = 0; // TODO args

        MAX31855 max31855 = new MAX31855(channel);

        int[] raw = new int[2];
        while (true) {
            int faults = max31855.readRaw(raw);

            float internal = max31855.getInternalTemperature(raw[0]);
            float thermocouple = max31855.getThermocoupleTemperature(raw[1]);

            System.out.println("Internal = " + internal + " C, Thermocouple = " + thermocouple + " C");
            if (faults != 0) {
                // TODO
                System.out.println(faults);
            }
        }
    }
}
