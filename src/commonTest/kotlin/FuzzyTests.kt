import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class FuzzyTests {

    @Test
    fun test1() {

        val fumesTemperature = PersistentState(20.0)
        val fumesTemperatureSpeed = PersistentState(0.0)
        val timeSinceLastRechargingNotification = PersistentState(12.toDuration(DurationUnit.HOURS))


        val burningHot = FunctionOverStateFuzzyAtom(TrueFrom(250.0, 20.0), fumesTemperature)
        val hot = FunctionOverStateFuzzyAtom(TrueInRange(150.0, 250.0, 20.0), fumesTemperature)
        val warm = FunctionOverStateFuzzyAtom(TrueInRange(50.0, 150.0, 20.0), fumesTemperature)
        val cold = FunctionOverStateFuzzyAtom(TrueUntil(50.0, 20.0), fumesTemperature)

        val cooling = FunctionOverStateFuzzyAtom(TrueUntil(-20.0, 10.0), fumesTemperatureSpeed)
        val gentleCooling = FunctionOverStateFuzzyAtom(TrueInRange(-100.0, -20.0, 10.0), fumesTemperatureSpeed)
        val stable = FunctionOverStateFuzzyAtom(TrueInRange(-20.0, 20.0, 10.0), fumesTemperatureSpeed)
        val warming = FunctionOverStateFuzzyAtom(TrueFrom(20.0, 10.0), fumesTemperatureSpeed)

        val recharging = FunctionOverStateFuzzyAtom(TrueUntilDuration(30.toDuration(DurationUnit.MINUTES), 10.toDuration(DurationUnit.MINUTES)), timeSinceLastRechargingNotification)

        val ignition = recharging or warming
        val fullFire = burningHot
        val embers = hot and (stable or gentleCooling) and not(recharging)
        val discharging = warm and (stable or gentleCooling) and not(recharging)
        val idle = cold and stable and not(recharging)

        val inferredStates = listOf(idle, ignition, fullFire, embers, discharging)

        val tempBasedState = fumesTemperature.apply(LinearFunction(Pair(250.0, 0.5), Pair(100.0, 0.0))).map { min(max(it, 0.0), 0.5) }

        val r1 = ignition implies 1.0
        val r2 = fullFire implies 1.0
        val r3 = embers implies tempBasedState
        val r4 = (discharging or idle) implies 0.0

        val openRate = or(listOf(r1, r2, r3, r4))


        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(idle, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")

        fumesTemperature.state = 50.0
        fumesTemperatureSpeed.state = 40.0
        timeSinceLastRechargingNotification.state = 5.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(ignition, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")

        fumesTemperature.state = 260.0
        fumesTemperatureSpeed.state = 500.0
        timeSinceLastRechargingNotification.state = 60.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(ignition, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")

        fumesTemperature.state = 190.0
        fumesTemperatureSpeed.state = -54.0
        timeSinceLastRechargingNotification.state = 90.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(embers, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")

        fumesTemperature.state = 150.0
        fumesTemperatureSpeed.state = -30.0
        timeSinceLastRechargingNotification.state = 120.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(embers, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")

        fumesTemperature.state = 130.0
        fumesTemperatureSpeed.state = -23.0
        timeSinceLastRechargingNotification.state = 150.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(discharging, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")

        fumesTemperature.state = 110.0
        fumesTemperatureSpeed.state = -16.0
        timeSinceLastRechargingNotification.state = 240.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(discharging, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")

        fumesTemperature.state = 50.1
        fumesTemperatureSpeed.state = -6.0
        timeSinceLastRechargingNotification.state = 600.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(discharging, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")

        fumesTemperature.state = 48.0
        fumesTemperatureSpeed.state = -5.0
        timeSinceLastRechargingNotification.state = 620.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        assertEquals(idle, inferredStates.maxBy { it.confidence })
        println("Rate ${openRate.state} (${openRate.confidence})\n")


        fumesTemperature.state = 120.0
        fumesTemperatureSpeed.state = -500.0
        timeSinceLastRechargingNotification.state = 620.toDuration(DurationUnit.MINUTES)
        println("ignition: ${ignition.confidence} fullFire: ${fullFire.confidence} embers: ${embers.confidence} discharging: ${discharging.confidence} idle: ${idle.confidence}")
        println("Rate ${openRate.state} (${openRate.confidence})\n")

    }

}


