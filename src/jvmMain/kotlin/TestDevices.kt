import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        stoveController().devices.filterIsInstance<TestableDevice>().forEach {
            println("Testing $it")
            it.test()
        }
    }
}