package tests.tests
import androidx.test.platform.app.InstrumentationRegistry
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.topjohnwu.superuser.Shell
import org.jetbrains.annotations.TestOnly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class Test_shell {

    companion object {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        //val context = Utils.getDeContext(Utils.getContext())
        //val context = Utils.getContext()
        val baseDir: String = context.cacheDir.absolutePath

        const val TEST_EXT = ".test"
        const val SAFE_COMMAND_LENGTH = 130_000

        @TestOnly
        fun checkCommandLength(commandStub: String, length: Int, check: String = "api"): Boolean {
            var shellResult: Shell.Result? = null
            try {
                if (check == "api") {
                    val shellInput = "x".repeat(length)
                    shellResult = runAsRoot("$commandStub '$shellInput' | wc -c")
                }
                if (check == "shell") {
                    shellResult = runAsRoot($$"$$commandStub $(yes xxxxxxxxxx|tr -d \"\\n\"|head -c $$length) | wc -c")
                }
                shellResult?.let {
                    return@checkCommandLength it.out.joinToString("").toInt() == length
                }
            } catch (e: OutOfMemoryError) {
                LogsHandler.logException(e, "length: $length")
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e, "length: $length err: ${shellResult?.err}")
            }
            return false
        }
    }

    @Test
    fun test_safeCommandLength() {          // test a line length everyone should be able to process
        assertTrue(
            checkCommandLength("toybox echo -n", SAFE_COMMAND_LENGTH)
        )
        assertTrue(
            checkCommandLength("echo -n", SAFE_COMMAND_LENGTH)
        )
    }

    @TestOnly
    fun dump(str : String, separator: CharSequence = " ",  prefix: CharSequence = "",  postfix: CharSequence = ""): String {
        return str.toByteArray(StandardCharsets.UTF_8)
            .joinToString(separator, prefix, postfix) {
                it.toUByte().toString(16).padStart(2, '0')
            }
    }

    @Test
    fun test_dump() {
        assertEquals(
            "00 01 02 03 04 05 06 07 08",
            dump("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008")
        )
        assertEquals(
            "07 08 0c 0a 0d 09 12 1b f0 9f 8e 83 f0 9f 8e 83",
            dump("\u0007\b\u000c\n\r\t\u0012\u001b🎃\uD83C\uDF83")   // pumpkin as char and codes
        )
    }

    @TestOnly
    internal fun test_quote1(filename : String) {
        val uniqPart = UUID.randomUUID()    // should not need quoting
        val testDir = "$baseDir/test-$uniqPart"
        val testFilePath = "$testDir/$filename$TEST_EXT"
        val testPattern = "$testDir/*$TEST_EXT"

        File(testDir).mkdirs()

        try {

            val file = File(testFilePath)
            file.createNewFile()

            assertEquals(
                dump(testFilePath),
                dump(
                    ShellHandler.FileInfo.unescapeLsOutput(
                        runAsRoot("toybox ls -bdAlZ $testPattern")
                            .out.joinToString("\n")
                            .split(Regex(" +"), limit = 9)
                                [8]
                    )
                )
            )
        } finally {
            try {
                runAsRoot("toybox rm -r $testDir")
            } catch(_ : Throwable) {
            }
        }
    }

    @Test
    fun test_quote_special() {
        test_quote1("""test    ,\|$&"'`[](){}=:;?<~>-+!%^#*""")
    }

    @Test
    fun test_quote_escaped() {
        test_quote1("\u0007\b\u000c\n\r\t\u0012\u001b🎃\uD83C\uDF83")
    }

    @Test
    fun test_quote_0_32() {
        test_quote1((0..32).map { it.toChar() }.filter { c -> !(c.code == 0 || c in "\n\r") }
            .joinToString(""))
    }

    @Test
    fun test_quote_33_127() {
        test_quote1((33..127).map { it.toChar() }
            .filter { c -> !(c.isLetterOrDigit() || c == '/') }
            .joinToString(""))
    }

    // fails: (0..32).map { (128+it).toChar() }.filter { c -> ! (c.toInt() == 0 || c.toInt() == 173 || (c.toInt().and(127)).toChar() in "\t\n\r ") }.joinToString("") + ext,

    @Test
    fun test_quote_128_plus_33_127() {
        test_quote1((33..127).map { (128 + it).toChar() }.joinToString(""))
    }
}
