import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Transient
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * [CD]    [D ]
 *
 * [C ]    [  ]
 * clock wise: [CD] -> [ D] -> [  ] -> [C ] -> [CD]
 * counter clock wise: [CD] -> [C ] -> [  ] -> [ D] -> [CD]
 * some rotary encoders are stable at [CD], some at [CD] and [  ].
 * This will count +/-1 only once a the [  ] has been passed, typically back to [CD].
 * +1
 */
// TODO debouncing rotary?
class RotaryPushButtonGPIO(override val id: String, val bcmClk: Int, val bcmDT: Int, val bcmSW: Int): PushButtonGPIO(id, bcmSW, DigitalState.low), RotatyButton, TestableDevice {

    @Transient
    override val changeListeners = mutableListOf<(Int) -> Unit>()
    var sensing = false

    override suspend fun startSensing() {
        if(!sensing) { // FIXME concurrency!
            sensing = true
            super.startSensing()
            val input1 = pi.gpioDigitalInput(bcmClk, PullResistance.pull_down, 1.toDuration(DurationUnit.MILLISECONDS))
            val input2 = pi.gpioDigitalInput(bcmDT, PullResistance.pull_down, 1.toDuration(DurationUnit.MILLISECONDS))
            input1.addOnChangeListener { rotaryStateChanged(it, input2.state) }
            input2.addOnChangeListener { rotaryStateChanged(input1.state, it) }
        }
    }

    override var counter = 0
    private var trend = 0 // -1 0 1
    private var commit = false
    fun rotaryStateChanged(clk: DigitalState?, dt: DigitalState?) {
        // do update previous as quickly as possible to limit the concurrency potential issues
        if(clk != null) {
            if(dt != null) {
                //print(stateString(clk, dt))

                when (clk) {
                    DigitalState.low -> {
                        when (dt) {
                            DigitalState.low -> {   // [  ]
                                commit = true
                            }
                            DigitalState.high -> {  // [ D]
                                if (commit && trend == -1) {
                                    counter += trend
                                    changed(trend)
                                }
                                commit = false
                                trend = 1
                            }
                        }
                    }
                    DigitalState.high -> {
                        when (dt) {
                            DigitalState.low -> { // [C ]
                                if (commit && trend == 1) {
                                    counter += trend
                                    changed(trend)
                                }
                                commit = false
                                trend = -1
                            }
                            DigitalState.high -> { // [CD]
                                if (commit) {
                                    counter += trend
                                    changed(trend)
                                }
                                commit = false
                                trend = 0
                            }
                        }
                    }
                }
            }
        }
    }

    fun stateString(clk: DigitalState?, dt: DigitalState?): String {
        return when(clk) {
            null -> {
                when(dt) {
                    null -> "[??]"
                    DigitalState.low -> "[? ]"
                    DigitalState.high -> "[?D]"
                }
            }
            DigitalState.low -> {
                when(dt) {
                    null -> "[ ?]"
                    DigitalState.low -> "[  ]"
                    DigitalState.high -> "[ D]"
                }
            }
            DigitalState.high -> {
                when(dt) {
                    null -> "[C?]"
                    DigitalState.low -> "[C ]"
                    DigitalState.high -> "[CD]"
                }
            }
        }
    }

    override suspend fun test() {
        val m = Mutex(true)
        println("Click on $this")
        var i = 5
        addOnClickListener {
            println("OK: $this clicked")
            if(i-- < 0) m.unlock()
        }
        addChangeListener {
            println(it)
        }
        coroutineScope { launch { startSensing() } }
        m.lock()
    }
}