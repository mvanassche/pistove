import kotlinx.datetime.Clock
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class FuzzyTests {

    @Test
    fun test1() {

        val fumesTemperature = PersistentState(20.0)
        val fumesTemperatureSpeed = PersistentState(0.0)
        val timeSinceLastRechargingNotification = PersistentState(12.toDuration(DurationUnit.HOURS))
        val lastUserValveRate: PersistentStateWithTimestamp<Double?> = PersistentStateWithTimestamp(null)


        val burningHot = FunctionOverStateFuzzyAtom("burning hot", FuzzyTrueFrom(250.0, 20.0), fumesTemperature)
        val hot = FunctionOverStateFuzzyAtom("hot", FuzzyTrueInRange(150.0, 250.0, 20.0), fumesTemperature)
        val warm = FunctionOverStateFuzzyAtom("warm", FuzzyTrueInRange(50.0, 150.0, 20.0), fumesTemperature)
        val cold = FunctionOverStateFuzzyAtom("cold", FuzzyTrueUntil(50.0, 20.0), fumesTemperature)

        val cooling = FunctionOverStateFuzzyAtom("cooling", FuzzyTrueUntil(-20.0, 10.0), fumesTemperatureSpeed)
        val fastCooling = FunctionOverStateFuzzyAtom("cooling fast", FuzzyTrueUntil(-100.0, 20.0), fumesTemperatureSpeed)
        val gentleCooling = FunctionOverStateFuzzyAtom("gentle cooling", FuzzyTrueInRange(-100.0, -20.0, 20.0), fumesTemperatureSpeed)
        val stable = FunctionOverStateFuzzyAtom("stable", FuzzyTrueInRange(-20.0, 20.0, 20.0), fumesTemperatureSpeed)
        val warming = FunctionOverStateFuzzyAtom("warming", FuzzyTrueFrom(20.0, 10.0), fumesTemperatureSpeed)

        val recharging = FunctionOverStateFuzzyAtom("recharged", FuzzyTrueUntilDuration(30.toDuration(DurationUnit.MINUTES), 10.toDuration(DurationUnit.MINUTES)), timeSinceLastRechargingNotification)

        val userRecentlyChangedOpenRate = FunctionOverStateFuzzyAtom("user said so", TrueUntilDuration(10.toDuration(DurationUnit.MINUTES)), lastUserValveRate.timeSinceLastChange)

        val ignition = recharging or warming
        val fullFire = burningHot
        val dyingFlames = hot and fastCooling and not(recharging)
        val embers = hot and (stable or gentleCooling) and not(recharging)
        val discharging = warm and (stable or gentleCooling) and not(recharging)
        val idle = cold and stable and not(recharging)

        val inferredStates = listOf(idle, ignition, fullFire, embers, discharging)

        val tempBasedState = fumesTemperature.apply(LinearFunction(Pair(250.0, 0.5), Pair(120.0, 0.0))).map { min(max(it, 0.0), 0.5) }
        val dyingFlamesTempBasedState = fumesTemperature.apply(LinearFunction(Pair(250.0, 0.9), Pair(180.0, 0.6))).map { min(max(it, 0.6), 0.9) }

        val ignitionSpeedBasedState = fumesTemperatureSpeed.apply(LinearFunction(Pair(0.0, 0.2), Pair(250.0, 1.0)))
            .map { max(it, tempBasedState.state) }
            .map { min(max(it, 0.2), 1.0) }

        val r1 = ignition and not(fullFire) implies ignitionSpeedBasedState //1.0 // implies ValveOpenRate(1.0)
        val r2 = fullFire and not(userRecentlyChangedOpenRate) implies 1.0
        val r2b = dyingFlames and not(userRecentlyChangedOpenRate) implies dyingFlamesTempBasedState
        val r3 = embers and not(userRecentlyChangedOpenRate) implies tempBasedState
        val r4 = (discharging or idle) and not(userRecentlyChangedOpenRate) implies 0.0
        val r5 = not(fullFire) and userRecentlyChangedOpenRate implies lastUserValveRate.map { it ?: 1.0 }
        val r5b = fullFire and userRecentlyChangedOpenRate implies lastUserValveRate.map { it ?: 1.0 }.map { max(it, 0.4) }

        val openRate = or(listOf(r1, r2, r2b, r3, r4, r5, r5b))


        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(idle, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 50.0
        fumesTemperatureSpeed.state = 40.0
        timeSinceLastRechargingNotification.state = 5.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(ignition, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 69.0
        fumesTemperatureSpeed.state = 385.0
        timeSinceLastRechargingNotification.state = 100000.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(ignition, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 260.0
        fumesTemperatureSpeed.state = 500.0
        timeSinceLastRechargingNotification.state = 40.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(ignition, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 280.0
        fumesTemperatureSpeed.state = -500.0
        timeSinceLastRechargingNotification.state = 60.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(fullFire, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 220.0
        fumesTemperatureSpeed.state = -120.0
        timeSinceLastRechargingNotification.state = 60.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(embers, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 190.0
        fumesTemperatureSpeed.state = -54.0
        timeSinceLastRechargingNotification.state = 90.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(embers, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 150.0
        fumesTemperatureSpeed.state = -30.0
        timeSinceLastRechargingNotification.state = 120.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(embers, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 130.0
        fumesTemperatureSpeed.state = -23.0
        timeSinceLastRechargingNotification.state = 150.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(discharging, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 110.0
        fumesTemperatureSpeed.state = -16.0
        timeSinceLastRechargingNotification.state = 240.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(discharging, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 50.1
        fumesTemperatureSpeed.state = -6.0
        timeSinceLastRechargingNotification.state = 600.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(discharging, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")

        fumesTemperature.state = 48.0
        fumesTemperatureSpeed.state = -5.0
        timeSinceLastRechargingNotification.state = 620.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        assertEquals(idle, inferredStates.maxBy { it.state.confidence })
        println("Rate ${openRate.state})\n")


        fumesTemperature.state = 120.0
        fumesTemperatureSpeed.state = -500.0
        timeSinceLastRechargingNotification.state = 620.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        println(openRate)
        println("Rate ${openRate.state})\n")


        fumesTemperature.state = 250.0
        fumesTemperatureSpeed.state = 0.0
        timeSinceLastRechargingNotification.state = 60.toDuration(DurationUnit.MINUTES)
        val rateAt250Stable = openRate.state.value
        println(openRate)
        println("Rate ${openRate.state})\n")

        // respect stupid user, but not too much, we are talking about a hot stove!
        fumesTemperature.state = 250.0
        fumesTemperatureSpeed.state = 0.0
        timeSinceLastRechargingNotification.state = 60.toDuration(DurationUnit.MINUTES)
        lastUserValveRate.state = 0.3
        lastUserValveRate.lastChanged = Clock.System.now() - 5.toDuration(DurationUnit.MINUTES)
        println(openRate)
        println("Rate ${openRate.state})\n")
        assertTrue { rateAt250Stable - openRate.state.value > 0.2 }


        // but not too long...
        fumesTemperature.state = 250.0
        fumesTemperatureSpeed.state = 0.0
        timeSinceLastRechargingNotification.state = 60.toDuration(DurationUnit.MINUTES)
        lastUserValveRate.state = 0.3
        lastUserValveRate.lastChanged = Clock.System.now() - 13.toDuration(DurationUnit.MINUTES)
        println(openRate)
        println("Rate ${openRate.state})\n")
        assertTrue { abs(rateAt250Stable - openRate.state.value) < 0.1  }


        // respect stupid user
        fumesTemperature.state = 30.0
        fumesTemperatureSpeed.state = 0.0
        timeSinceLastRechargingNotification.state = 600.toDuration(DurationUnit.MINUTES)
        lastUserValveRate.state = 1.0
        lastUserValveRate.lastChanged = Clock.System.now() - 5.toDuration(DurationUnit.MINUTES)
        println(openRate)
        println("Rate ${openRate.state})\n")
        assertTrue { abs(1.0 - openRate.state.value) < 0.1  }

        // but not too long...
        lastUserValveRate.lastChanged = Clock.System.now() - 11.toDuration(DurationUnit.MINUTES)
        println(openRate)
        println("Rate ${openRate.state})\n")
        assertTrue { openRate.state.value < 0.1  }

        fumesTemperature.state = 50.0
        fumesTemperatureSpeed.state = -45.0
        timeSinceLastRechargingNotification.state = 620.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        //assertEquals(idle, inferredStates.maxBy { it.state.confidence })
        println(openRate)
        println("Rate ${openRate.state})\n")


        fumesTemperature.state = 240.0
        fumesTemperatureSpeed.state = -90.0
        timeSinceLastRechargingNotification.state = 1.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.state.confidence} fullFire: ${fullFire.state.confidence} dying: ${dyingFlames.state.confidence} embers: ${embers.state.confidence} discharging: ${discharging.state.confidence} idle: ${idle.state.confidence}")
        //assertEquals(idle, inferredStates.maxBy { it.state.confidence })
        println(openRate)
        println("Rate ${openRate.state})\n")
        assertTrue { openRate.state.value > 0.5  }

    }

}


