

interface BasicUserCommunication {

    suspend fun alert()
    suspend fun acknowledge()

}

class DisplayAndBuzzerUserCommunication(val display: Display, val buzzer: Buzzer) : BasicUserCommunication {
    override suspend fun alert() {
        buzzer.rrrrrr()
    }

    override suspend fun acknowledge() {
        buzzer.bipBip()
    }

}
