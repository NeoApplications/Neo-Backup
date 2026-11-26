package tests.tests

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.entity.Pref.Companion.escape
import com.machiav3lli.backup.data.entity.Pref.Companion.fromSimpleFormat
import com.machiav3lli.backup.data.entity.Pref.Companion.toSimpleFormat
import com.machiav3lli.backup.data.entity.Pref.Companion.unescape
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.TestOnly
import org.junit.Test

class Test_Serialization {

    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    //val context = Utils.getDeContext(Utils.getContext())
    //val context = Utils.getContext()

    val strings = mapOf(
        "special" to
                """test  , \ | $ & " ' ` [ ] ( ) { } = : ; ? < ~ > - + ! % ^ # * """,
        "escaped" to
                "\u0007\b\u000c\n\r\t\u0012\u001b🎃\uD83C\uDF83",
        "0..32" to
                (0..32).map { it.toChar() }
                    //.filter { c -> !(c.code == 0 || c in "\n\r") }
                    .joinToString(""),
        "33..127" to
                (33..127).map { it.toChar() }
                    //.filter { c -> !(c.isLetterOrDigit() || c == '/') }
                    .joinToString(""),
        "128+(0..32)" to
                (0..32).map { (128 + it).toChar() }
                    //.filter { c ->
                    //    !(c.code == 0 ||
                    //            c.code == 173 ||
                    //            (c.code.and(127)).toChar() in "\t\n\r "
                    //            )
                    //}
                    .joinToString(""),
        "128+(33..127)" to
                (33..127).map { (128 + it).toChar() }
                    .joinToString(""),
    )

    val aMap = mapOf<String, Any>(
        "int" to 123,
        "boolean" to false,
    ) + strings

    @Serializable
    private data class SomeClass(
        val int: Int = 123,
        val flag: Boolean = false,
        val str: String = "abc"
    )

    private val aObj = SomeClass(int = 456, flag = true, str = "str")

    @TestOnly
    internal fun test_esc(value: String) {
        val esc = escape(value)
        println("'$esc'")
        val unesc = unescape(esc)
        assertEquals(value, unesc)
    }

    @Test
    fun test_simple_escape() {
        strings.forEach { (_, value) ->
            test_esc(value)
        }
    }

    @Test
    fun test_simple() {
        val ser = toSimpleFormat(aMap)
        println("simple: '\n$ser\n'")
        val obj = fromSimpleFormat(ser)
        assertEquals(aMap, obj)
    }

    @Test
    fun test_json_obj() {
        val ser = NeoApp.toSerialized(NeoApp.JsonPretty, aObj)
        println("json: '\n$ser\n'")
        val obj = NeoApp.fromSerialized<SomeClass>(ser)
        assertEquals(aObj, obj)
    }

    @Test
    fun test_json_map() {
        val ser = NeoApp.toSerialized(NeoApp.JsonPretty, aMap)
        println("json: '\n$ser\n'")
        val obj = NeoApp.fromSerialized<Map<String, Any>>(ser)
        assertEquals(aMap, obj)
    }

    @Test
    fun test_yaml_obj() {
        val ser = NeoApp.toSerialized(NeoApp.YamlDefault, aObj)
        println("yaml: '\n$ser\n'")
        val obj = NeoApp.fromSerialized<SomeClass>(ser)
        assertEquals(aObj, obj)
    }

    @Test
    fun test_yaml_map() {
        val ser = NeoApp.toSerialized(NeoApp.YamlDefault, aMap)
        println("yaml: '\n$ser\n'")
        val obj = NeoApp.fromSerialized<Map<String, Any>>(ser)
        assertEquals(aMap, obj)
    }

}
