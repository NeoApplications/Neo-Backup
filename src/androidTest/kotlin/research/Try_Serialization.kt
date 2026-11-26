package tests.research

import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import org.junit.Test

//@Serializable
//data class AnyType(var any: @Contextual Any): Any() {
//    constructor(value: String) : this(value as Any)
//    constructor(value: Int) : this(value as Any)
//    constructor(value: Boolean) : this(value as Any)
//}

typealias AnyType = Any
typealias MapAny = Map<String, AnyType>

private val supportedClasses = listOf(Int::class, Boolean::class, String::class)

private val valueSerializer = PolymorphicSerializer(Any::class)

private val valueSerMod = SerializersModule {
    supportedClasses.forEach { klass ->
        contextual(klass) { valueSerializer }
    }
}

object MapAnySerializer : KSerializer<Map<String, AnyType>> {

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("MapAnySerializer", PolymorphicKind.OPEN) {
            element<String>("key")
            element("value", valueSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        encoder.encodeStructure(descriptor) {
            for ((key, value) in value) {
                encodeStringElement(descriptor, 0, key)
                when (value) {
                    is Int     -> encodeIntElement(descriptor, 1, value)
                    is Boolean -> encodeBooleanElement(descriptor, 1, value)
                    is String  -> encodeStringElement(descriptor, 1, value)
                    else       -> encodeSerializableElement(
                        descriptor,
                        1,
                        PolymorphicSerializer(Any::class),
                        value
                    )
                }
            }
        }
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        return decoder.decodeStructure(descriptor) {
            val result = mutableMapOf<String, Any>()
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0                            -> {
                        val key = decodeStringElement(descriptor, 0)
                        val value = when (val elemIndex = decodeElementIndex(descriptor)) {
                            1    -> decodeIntElement(descriptor, 1)
                            2    -> decodeBooleanElement(descriptor, 2)
                            3    -> decodeStringElement(descriptor, 3)
                            else -> decodeSerializableElement(
                                descriptor,
                                elemIndex,
                                PolymorphicSerializer(Any::class)
                            )
                        }
                        result[key] = value
                    }

                    else                         -> throw SerializationException("Invalid index: $index")
                }
            }
            result
        }
    }
}

object MapSerializerAny : KSerializer<Map<String, AnyType>> {

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("MapSerializerAny", PolymorphicKind.OPEN) {
            element<String>("key")
            element("value", valueSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        encoder.encodeSerializableValue(
            MapSerializer(String.serializer(), valueSerializer),
            value
        )
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        return decoder.decodeSerializableValue(
            MapSerializer(String.serializer(), valueSerializer)
        ).mapValues { it.value }
    }

}

@Serializable(with = PrimitiveSerializer::class)
sealed class Primitive {

    abstract fun asAny(): Any

    @Serializable
    data class IntValue(val value: Int) : Primitive() {
        override fun asAny() = value
    }

    @Serializable
    data class BooleanValue(val value: Boolean) : Primitive() {
        override fun asAny() = value
    }

    @Serializable
    data class StringValue(val value: String) : Primitive() {
        override fun asAny() = value
    }

    companion object {
        fun <T : Any> from(value: T) =
            when (value) {
                is Int     -> IntValue(value)
                is Boolean -> BooleanValue(value)
                is String  -> StringValue(value)
                else       -> throw Exception("only Int, Boolean, String allowed")
            }
    }
}

typealias MapPrimitive = Map<String, Primitive>

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
object PrimitiveSerializer : KSerializer<Primitive> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("Primitive", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: Primitive) {
        when (value) {
            is Primitive.IntValue     -> encoder.encodeInt(value.value)
            is Primitive.BooleanValue -> encoder.encodeBoolean(value.value)
            is Primitive.StringValue  -> encoder.encodeString(value.value)
            //else -> encoder.encodeStructure(descriptor) {
            //    encodeSerializableElement(descriptor, 3, PolymorphicSerializer(Any::class), value)
            //}
            else                      -> error("only Int, Boolean, String allowed")
        }
    }

    override fun deserialize(decoder: Decoder): Primitive {
        return decoder.decodeStructure(descriptor) {
            val intValue: Int? = runCatching { decoder.decodeInt() }.getOrNull()
            val booleanValue: Boolean? = runCatching { decoder.decodeBoolean() }.getOrNull()
            val stringValue: String? = runCatching { decoder.decodeString() }.getOrNull()
            val anyValue: Any? = null
            when {
                intValue != null     -> Primitive.IntValue(intValue)
                booleanValue != null -> Primitive.BooleanValue(booleanValue)
                stringValue != null  -> Primitive.StringValue(stringValue)
                //anyValue != null -> Primitive.AnyValue(anyValue)
                else                 -> error("No values found")
            }
        }
    }
}

val mapPrimitiveSerializer = MapSerializer(String.serializer(), PrimitiveSerializer)


class Try_Serialization {

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    //val context = Utils.getDeContext(Utils.getContext())
    //val context = Utils.getContext()

    fun <K, T> mapAnyOf(vararg args: Pair<K, T>) = mapOf(*args)  //.mapValues { AnyType(it) }

    val mapAny: MapAny = mapAnyOf(
        "int" to 123,
        "boolean" to false,
        "string" to "abc",
    )

    fun <K, T : Any> mapPrimitiveOf(vararg args: Pair<K, T>) =
        mapOf(*args).mapValues { Primitive.from(it.value) }

    val mapPrimitive: MapPrimitive = mapPrimitiveOf(
        "int" to 123,
        "boolean" to false,
        "string" to "abc",
    )

    @Serializable
    data class aClass(val int: Int = 123, val flag: Boolean = false, val str: String = "abc")

    val aObj = aClass(int = 456, flag = true, str = "str")


    val serMod: SerializersModule = SerializersModule {
        //contextual(Int.serializer())
        //contextual(Boolean.serializer())
        //contextual(String.serializer())
        //polymorphic(Any::class) {
        //    subclass(Int.serializer())
        //    subclass(Boolean.serializer())
        //    subclass(String.serializer())
        //}
        //polymorphic(Any::class) {
        //    subclass(Int::class)
        //    subclass(Boolean::class)
        //    subclass(String::class)
        //}
        //polymorphic(Any::class) {
        //    subclass(Int::class, Int.serializer())
        //    subclass(Boolean::class, Boolean.serializer())
        //    subclass(String::class, String.serializer())
        //}
        //polymorphic(Any::class, Int::class, Int.serializer())
        //polymorphic(Any::class, Boolean::class, Boolean.serializer())
        //polymorphic(Any::class, String::class, String.serializer())
    } + valueSerMod

    val json = Json {
        serializersModule = serMod
        prettyPrint = true
    }

    val serializer = json

    @Test
    fun test_mapAnySerializer() {
        val ser = serializer.encodeToString(MapAnySerializer, mapAny)
        println("ser: '\n$ser\n'")
        val obj = serializer.decodeFromString(MapAnySerializer, ser)
        assertEquals(mapAny, obj)
    }

    @Test
    fun test_mapSerializerAny() {
        val ser = serializer.encodeToString(MapSerializerAny, mapAny)
        println("ser: '\n$ser\n'")
        val obj = serializer.decodeFromString(MapSerializerAny, ser)
        assertEquals(mapAny, obj)
    }

    @Test
    fun test_mapPrimitiveSerializer() {
        val ser = serializer.encodeToString(mapPrimitiveSerializer, mapPrimitive)
        println("ser: '\n$ser\n'")
        val obj =
            serializer.decodeFromString(mapPrimitiveSerializer, ser).mapValues { it.value.asAny() }
        assertEquals(mapAny, obj)
    }

    @Test
    fun test_map() {
        val ser = serializer.encodeToString(mapAny)
        println("ser: '\n$ser\n'")
        val obj: MapAny = serializer.decodeFromString(ser)
        assertEquals(mapAny, obj)
    }

    @Test
    fun test_obj() {
        val ser = serializer.encodeToString(aObj)
        println("ser: '\n$ser\n'")
        val obj: aClass = serializer.decodeFromString(ser)
        assertEquals(aObj, obj)
    }

    @Test
    fun test_jsonElement() {
        val ele = json.encodeToJsonElement(mapAny)
        println("ele: '\n$ele\n'")
        val prim = ele.jsonObject.mapValues { it.value.jsonPrimitive }
        val ser = prim.toString()
        val obj: MapAny = serializer.decodeFromString(ser)
        assertEquals(mapAny, obj)
    }
}
