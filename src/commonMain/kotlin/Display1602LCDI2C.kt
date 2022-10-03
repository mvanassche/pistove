import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Display1602LCDI2C(override val id: String, val bus: Int, val device: Int) : Display, StringDisplay, BackLightDisplay, TestableDevice, State<String> {

    @Transient
    private var _lcd: I2CLCD? = null
    @Transient
    private val mutex = Mutex()
    private suspend fun lcd(): I2CLCD {
        if (_lcd == null) {
            _lcd = I2CLCD(bus, device).apply {
                init()
                clear()
            }
        }
        return _lcd!!
    }

    fun sanitizeString(s: String): String {
        return s.replace('°', 223.toChar())
    }

    override suspend fun display(value: String) {
        if ('\n' in value) {
            displayLines(value.lines())
        } else {
            state = value
            // TODO _lcd.clear() once in a while
            if (value.length > 16) {
                var splitAt = value.substring(0, 16).indexOfLast { it.isWhitespace() }
                if (splitAt == -1) {
                    splitAt = 16
                }
                displayLine(value.substring(0, splitAt).padEnd(16, ' '), 1)
                displayLine(value.substring(splitAt + 1).padEnd(16, ' '), 2)
            } else {
                displayLine(value.padEnd(16, ' '), 1)
            }
        }
    }

    override suspend fun displayLines(lines: List<String>) {
        state = lines.joinToString("\n")
        lines.forEachIndexed { index, line ->
            displayLine(line, index + 1)
        }
    }

    override suspend fun displayTable(linesOfElements: List<List<String>>) {
        displayLines(linesOfElements.map { strings ->
            padStringElementsToFit(16, strings)
        })
    }

    suspend fun displayLine(line: String, index: Int) {
        mutex.withLock {
            lcd().display_string(sanitizeString(line), index)
        }
    }

    override suspend fun backLight(illuminated: Boolean) {
        lcd().backlight(illuminated)
    }

    override suspend fun test() {
        display("THIS IS A TEST! SEE THIS MESSAGE?")
        delay(1000)
        backLight(false)
        delay(500)
        backLight(true)
        display("it works...")
    }

    override var state: String = ""
}
