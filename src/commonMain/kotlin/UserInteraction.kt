

interface BasicUserCommunication {

    suspend fun alert()
    suspend fun acknowledge()
    val devices: Set<Device>

}

class DisplayAndBuzzerUserCommunication(val display: Display, val buzzer: Buzzer) : BasicUserCommunication {
    override suspend fun alert() {
        buzzer.rrrrrr()
    }

    override suspend fun acknowledge() {
        buzzer.bipBip()
    }

    override val devices: Set<Device>
        get() = setOf(display, buzzer)

}
