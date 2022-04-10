import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) {
    runBlocking {
        println("Testing push buttons")
        args.forEach {
            println("Create push button $it")
            val button = PushButtonGPIO(it.toInt())
            button.addOnClickListener { println("Pushed $it") }
            launch {
                println("Starting push button $it")
                button.startSensing()
            }
        }
        while (true) delay(1000)
    }
}