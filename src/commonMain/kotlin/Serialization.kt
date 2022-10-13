import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

val module = SerializersModule {
}

val format = Json {
    //serializersModule = module
    encodeDefaults = true
    prettyPrint = true
}

val historyFormat = Json { prettyPrint = false }


fun <T> encodeToJsonElement(serializersModule: SerializersModule, serializer: SerializationStrategy<T>, value: T): JsonElement {
    val encoder = JSONGraphEncoder(serializersModule)
    //format.encodeToJsonElement(serializer, value)
    encoder.encodeSerializableValue(serializer, value)
    return encoder.element!!
}

inline fun <reified T> encodeToString(serializersModule: SerializersModule, value: T): String {
    return encodeToJsonElement(serializersModule, serializer<T>(), value).toString()
}

class JSONGraphEncoder(override val serializersModule: SerializersModule, val identifiedAlreadyProcessed: MutableSet<String> = mutableSetOf()) : Encoder {

    var element: JsonElement? = null

    override fun beginStructure(descriptor: SerialDescriptor): JSONGraphCompositeEncoder {
        return object : JSONGraphCompositeEncoder(serializersModule, identifiedAlreadyProcessed) {
            override fun endStructure(descriptor: SerialDescriptor) {
                element = JsonObject(jsonObjectMap)
            }
        }
    }

    override fun encodeBoolean(value: Boolean) {
        element = JsonPrimitive(value)
    }
    override fun encodeByte(value: Byte) {
        element = JsonPrimitive(value)
    }
    override fun encodeChar(value: Char) {
        element = JsonPrimitive(value.toString())
    }
    override fun encodeDouble(value: Double) {
        element = JsonPrimitive(value)
    }
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        element = JsonPrimitive(enumDescriptor.getElementName(index))
    }
    override fun encodeFloat(value: Float) {
        element = JsonPrimitive(value)
    }
    @ExperimentalSerializationApi
    override fun encodeInline(inlineDescriptor: SerialDescriptor): Encoder {
        return this
    }
    override fun encodeInt(value: Int) {
        element = JsonPrimitive(value)
    }
    override fun encodeLong(value: Long) {
        element = JsonPrimitive(value)
    }
    @ExperimentalSerializationApi
    override fun encodeNull() {
        element = JsonNull
    }
    override fun encodeShort(value: Short) {
        element = JsonPrimitive(value)
    }
    override fun encodeString(value: String) {
        element = JsonPrimitive(value)
    }
}

abstract class JSONGraphCompositeEncoder(override val serializersModule: SerializersModule, val identifiedAlreadyProcessed: MutableSet<String>) :
    CompositeEncoder {

    val jsonObjectMap = mutableMapOf<String, JsonElement>()

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value)
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value)
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value.toString())
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value.toString())
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value.toString())
    }

    @ExperimentalSerializationApi
    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
        TODO("Not yet implemented")
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value.toString())
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value.toString())
    }

    @ExperimentalSerializationApi
    override fun <T : Any> encodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) {
        if(value != null) { // TODO explicit nullable?
            if(value is Identifiable && value.id in identifiedAlreadyProcessed &&
                !(descriptor.getElementName(index) == "value" && descriptor.kind in listOf(PolymorphicKind.SEALED, PolymorphicKind.OPEN))) {
                jsonObjectMap[descriptor.getElementName(index)] = JsonObject(mapOf("@id" to JsonPrimitive(value.id)))
            } else {
                if(value is Identifiable) {
                    val id = value.id
                    identifiedAlreadyProcessed.add(id)
                }
                val encoder = JSONGraphEncoder(serializersModule, identifiedAlreadyProcessed)
                serializer.serialize(encoder, value)
                val subElement = encoder.element!!
                if(descriptor.getElementName(index) == "value" && descriptor.kind in listOf(PolymorphicKind.SEALED, PolymorphicKind.OPEN) && subElement is JsonObject) {
                    subElement.forEach { jsonObjectMap[it.key] = it.value }
                    //jsonObjectMap[descriptor.getElementName(index)] = encoder.element!!
                } else {
                    jsonObjectMap[descriptor.getElementName(index)] = encoder.element!!
                }
            }
        } else {
            //println("Not encode ${descriptor.getElementName(index)}")
        }
    }

    override fun <T> encodeSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        encodeNullableSerializableElement(descriptor, index, serializer, value)
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value.toString())
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        jsonObjectMap[descriptor.getElementName(index)] = JsonPrimitive(value.toString())
    }

}


fun <T> JsonElement.decode(serializersModule: SerializersModule, serializer: DeserializationStrategy<T>): T {
    val decoder = JSONGraphDecoder(serializersModule, this, mutableMapOf())
    return decoder.decodeSerializableValue(serializer)
}

inline fun <reified T> String.decode(serializersModule: SerializersModule): T {
    return Json.parseToJsonElement(this).decode(serializersModule, serializer<T>())
}


class JSONGraphDecoder constructor(override val serializersModule: SerializersModule, val element: JsonElement, val identifieables: MutableMap<String, Identifiable>) :
    Decoder {

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if(element is JsonObject) {
            return JSONGraphCompositeDecoder(serializersModule, this, element, identifieables)
        } else {
            throw RuntimeException("TODO")
        }
    }

    override fun decodeBoolean(): Boolean {
        return element.jsonPrimitive.boolean
    }

    override fun decodeByte(): Byte {
        return element.jsonPrimitive.int.toByte()
    }

    override fun decodeChar(): Char {
        return element.jsonPrimitive.content[0]
    }

    override fun decodeDouble(): Double {
        return element.jsonPrimitive.double
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return enumDescriptor.elementNames.indexOf(this.element.jsonPrimitive.content)
    }

    override fun decodeFloat(): Float {
        return element.jsonPrimitive.float
    }

    @ExperimentalSerializationApi
    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder {
        return this
    }

    override fun decodeInt(): Int {
        return element.jsonPrimitive.int
    }

    override fun decodeLong(): Long {
        return element.jsonPrimitive.long
    }

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? {
        return null // ???
        //return element.jsonNull
    }

    override fun decodeShort(): Short {
        return element.jsonPrimitive.int.toShort()
    }

    override fun decodeString(): String {
        return element.jsonPrimitive.content
    }
}

class JSONGraphCompositeDecoder(override val serializersModule: SerializersModule, val decoder: JSONGraphDecoder, val element: JsonObject, val identifieables: MutableMap<String, Identifiable>) :
    CompositeDecoder {
    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.boolean
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.int.toByte()
    }

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.content[0]
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.double
    }

    private var elementIndex = 0
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.float
    }

    @ExperimentalSerializationApi
    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        TODO("Not yet implemented")
    }

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.int
    }

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.long
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        return element[descriptor.getElementName(index)]?.let {
            deserializer.deserialize(JSONGraphDecoder(serializersModule, it, identifieables))
        }
    }

    override fun <T> decodeSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, previousValue: T?): T {
        if((descriptor.getElementName(index) == "value" && descriptor.kind in listOf(PolymorphicKind.SEALED, PolymorphicKind.OPEN))) {
            return deserializer.deserialize(JSONGraphDecoder(serializersModule, element, identifieables))
        } else {
            val valueElement = element[descriptor.getElementName(index)]!!
            if (valueElement is JsonObject && valueElement["@id"] != null && valueElement["@id"] is JsonPrimitive &&
                valueElement["@id"]?.jsonPrimitive?.isString == true && identifieables[valueElement["@id"]!!.jsonPrimitive.content] != null
            ) {
                return identifieables[valueElement["@id"]!!.jsonPrimitive.content] as T
            }
            return deserializer.deserialize(JSONGraphDecoder(serializersModule, valueElement, identifieables)).also {
                if (it is Identifiable) {
                    identifieables[it.id] = it
                }
            }
        }
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.int.toShort()
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        return element[descriptor.getElementName(index)]!!.jsonPrimitive.content
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }
}
