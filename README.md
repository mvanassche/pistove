

Raspberry pi

```
sudo apt-get install openjdk-11-jdk
```
```
sudo apt-get install pigpio
```

# Hardware

## Stove


- [Manual air valve register](https://www.jpvos.be/nl/dixneuf-warmeluchtroosters-toe-afvoer/9882-register-met-kabel-diam160-met-messing-knop.html?controllerUri=product)
- [Belimo LM230A](https://www.ventilatieland.be/fr_BE/p/belimo-servomotor-230v-o-d-5-nm-lm230a-tp/8749/) note: you can probably go with a much smaller and cheaper model
- [Air valve register](https://www.ventilatieland.be/nl_BE/p/motorbediende-regelklep-o-160mm-voor-spirobuis/8945/)

## Raspberry Pi

There are 2 versions, a cheaper version with limited display, and a more expensive with a touch screen. For both versions, a web page exposes a user interface (the same as the touch screen).  

- [ceramic oven probe, k type thermocouple](https://www.amazon.com.be/ceramic-mounting-thermocouple-sensor-temperature/dp/B09XX7Y9TY) for the hot fumes
- [MAX31855 - V2.0](https://shop.mchobby.be/fr/temperature/302-amplificateur-thermocouple-max31855-v20-3232100003026-adafruit.html)
- [Thermocouple Type-K](https://shop.mchobby.be/fr/temperature/301-thermocouple-type-k-3232100003019.html) for the cold fumes and the accumulator t°
- [SHT31-F](https://shop.mchobby.be/fr/environnemental-press-temp-hrel-gaz/1882-sht31-f-capteur-d-humidite-et-temperature-3232100018822-dfrobot.html) for indoor t°
- [Piezo buzzer](https://shop.mchobby.be/fr/autres-capteurs/57-piezo-buzzer-3232100000575.html)
- [DS18B20](https://shop.mchobby.be/fr/environnemental-press-temp-hrel-gaz/151-capteur-temperature-ds18b20-etanche-extra-3232100001510.html) for outside t°
- [Relay 5v | 2-canals](https://www.bol.com/be/nl/p/otronic-relais-module-5v-2-kanaals-arduino-esp32-esp8266-raspberry-pi-wemos/9300000011396590/) to control the register

### Version pi zero 2 W

- [Raspberry Pi Zero 2 with HEADER](https://shop.mchobby.be/fr/pi-zero-12wwh/2334-raspberry-pi-zero-2-w-avec-header-wireless-cam-port-3232100023345.html)
- [LCD 16x2 I2C](https://shop.mchobby.be/fr/afficheur-lcd-tft-oled/1807-afficheur-lcd-16x2-i2c-3232100018075-dfrobot.html)
- push buttons

### Version pi 4B

- Raspberry 4B 1G
- [4.3inch HDMI LCD (B) - 800x480 - IPS - Capacitive touch](https://www.kiwi-electronics.com/nl/4-3inch-hdmi-lcd-b-800x480-ips-capacitive-touch-4065)
- Rotary button



### Pi wiring

# Software
