import kotlinx.coroutines.runBlocking
import java.lang.reflect.Parameter

fun main(vararg args: String) {
    val clazz = Class.forName(args[0])
    if(TestableDevice::class.java.isAssignableFrom(clazz)) {
        val params = args.drop(1)
        val constructor = clazz.constructors.filter { it.parameters.size == params.size }.first()
        val device = constructor.newInstance(*params.castFor(constructor.parameters).toTypedArray())
        runBlocking { (device as TestableDevice).test() }
    }
}

fun List<String>.castFor(params: Array<Parameter>): List<Any> {
    return this.zip(params)
        .map {
            when(it.second.type) {
                String::class.java -> it.first
                Int::class.java -> it.first.toInt()
                else -> it.second.type.getConstructor(String::class.java).newInstance(it.first)
            }
    }
}

/*fun KClass<*>.sealedTransitiveSubClasses(): Set<KClass<*>> {
    val result = this.sealedSubclasses
    return (result + result.flatMap { it.sealedTransitiveSubClasses() }).toSet()
}*/