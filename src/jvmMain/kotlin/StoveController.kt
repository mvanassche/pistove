


fun stoveController(pi: RaspberryPi): StoveController {
    val powerRelay = LowActiveGPIOElectricRelay(pi, 5)
    val openCloseRelay = LowActiveGPIOElectricRelay(pi, 6)
    //val openRelay = TestRelay("open")
    //val closeRelay = TestRelay("close")
    val valve = ElectricValveController(powerRelay = powerRelay, openCloseRelay = openCloseRelay)
    //val chimney = TestTemperatureSensor("chimney")
    val chimney = MAX31855TemperaturSensor(0)
    //val room = TMPDS18B20TemperatureSensor()
    //val room = TestTemperatureSensor("room")
    val room = SHT31TemperaturSensor(1, 0x45)
    /*val openButton = TestButton("open button")
    val closeButton = TestButton("close button")
    val autoButton = TestButton("auto button")*/
    val buzzer = PassivePiezoBuzzerHardwarePWM(12)
    val openButton = PushButtonGPIO(13)
    val closeButton = PushButtonGPIO(26)
    val autoButton = PushButtonGPIO(19)
    val display = Display1602LCDI2C(1, 0x27)
    return StoveController(valve, chimney, room, openButton, closeButton, autoButton, DisplayAndBuzzerUserCommunication(display, buzzer))
}