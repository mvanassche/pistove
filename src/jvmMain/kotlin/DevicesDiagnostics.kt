import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        stoveController().devices.filterIsInstance<TestableDevice>().forEach {
            println("Testing $it")
            try {
                it.test()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        println("Tests completed")
    }
    context.shutdown()
}