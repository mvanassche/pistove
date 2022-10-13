import kotlinx.serialization.Serializable


sealed interface BasicUserCommunication {

    suspend fun welcome()
    suspend fun goodbye()

    suspend fun alert()
    suspend fun acknowledge()
    val devices: Set<Device>

}

@Serializable
class DisplayAndBuzzerUserCommunication(override val id: String, val display: StringDisplay, val buzzer: Buzzer) : BasicUserCommunication, StringDisplay by display {
    override suspend fun welcome() {
        (display as? BackLightDisplay)?.backLight(true)
        buzzer.dodadi()
    }

    override suspend fun goodbye() {
        (display as? BackLightDisplay)?.backLight(false)
        buzzer.didado()
    }

    override suspend fun alert() {
        buzzer.rrrrrr()
    }

    override suspend fun acknowledge() {
        buzzer.bipBip()
    }

    override val devices: Set<Device>
        get() = setOf(display, buzzer)

}
