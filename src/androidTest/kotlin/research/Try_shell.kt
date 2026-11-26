package tests.research
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import org.jetbrains.annotations.TestOnly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tests.tests.Test_shell
import timber.log.Timber
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class Try_shell {

    @TestOnly
    fun findShellCommandLength(commandStub: String, check: String = "api", maxLengthChecked: Int = 999_999): Int {
        var maxWorked = 0
        var minFailed = 0
        var n = 10000
        var fastIncrease = true
        while (true) {

            Timber.i("$maxWorked < $n < $minFailed")

            val ok = Test_shell.checkCommandLength(commandStub, n, check = check)
            if (ok) {
                maxWorked = n
            } else {
                fastIncrease = false
                minFailed = n
                n = maxWorked
            }

            if (fastIncrease) {
                n *= 10
                minFailed = n
            } else {
                val precision = max(1, 10.0.pow((floor(log10(minFailed.toDouble())) - 3)).toInt())
                if (maxWorked + precision >= minFailed)
                    break
                n = ((maxWorked + minFailed) / 2 / precision).toInt() * precision
            }
            if (n >= maxLengthChecked)
                n = maxLengthChecked

            if (maxWorked >= maxLengthChecked)
                break
        }
        Timber.i("maxWorked: $maxWorked minFailed: $minFailed")
        return maxWorked
    }

    @Test
    fun test_shellCommandLength_API() {     // test on API level (runAsRoot -> Magisk Shell.sh -> ...), with a long runAsRoot command
        val results = mutableMapOf<String, Int>()
        for (stub in listOf(
                "echo -n",
                "toybox echo -n"
        )
        ) {
            val length = findShellCommandLength(stub, "api")
            Timber.i("$stub -> $length")
            results[stub] = length
        }
        results.forEach {
            Timber.i("${it.key.padEnd(20)} -> ${it.value.toString().padStart(10)}")
        }
        assertTrue(
                results.values.all { it >= Test_shell.SAFE_COMMAND_LENGTH }
        )
    }

    @Test
    fun test_shellCommandLength_Shell() {   // test on shell level (generate arguments in command line), with a short runAsRoot command
        val results = mutableMapOf<String, Int>()
        for (stub in listOf(
                "echo -n",
                "toybox echo -n"
        )
        ) {
            val length = findShellCommandLength(stub, "shell")
            Timber.i("$stub -> $length")
            results[stub] = length
        }
        results.forEach {
            Timber.i("${it.key.padEnd(20)} -> ${it.value.toString().padStart(10)}")
        }
        assertTrue(
                results.values.all { it >= Test_shell.SAFE_COMMAND_LENGTH }
        )
    }

    @Test
    fun test_escapes() {    // about backslash expansion in shell and echo
        assertEquals(       // mksh echo
            " 07 08 0c 0a 0d 09 0b 1b 1b e1 88 b4 ef bf bd 5c",
            runAsRoot(
                """echo -n -e '\a\b\f\n\r\t\v\e\E\u1234\U123456\\' | toybox od -t x1 -A none"""
            ).out.joinToString("")
        )
        assertEquals(       // toybox echo (not expanding \E \u1234 \U123456)
            " 07 08 0c 0a 0d 09 0b 1b 5c 45 5c 75 31 32 33 34 5c 55 31 32 33 34 35 36 5c",
            runAsRoot(
                """toybox echo -n -e '\a\b\f\n\r\t\v\e\E\u1234\U123456\\' | toybox od -t x1 -A none"""
            ).out.joinToString("")
        )
        assertEquals(       // toybox echo (not expanding \E \u1234 \U123456)
            """\a\b\f\n\r\t\v\e\E\u1234\U123456\\""",
            runAsRoot(
                """toybox echo -n '\a\b\f\n\r\t\v\e\E\u1234\U123456\\'"""
            ).out.joinToString("")
        )
    }

    @Test
    fun test_shell_stdout_stderr_are_separate() {
        val result = runAsRoot("toybox echo stdout ; toybox echo stderr 1>&2")
        assertEquals(
            "stdout",
            result.out.joinToString("\n")
        )
        assertEquals(
            "stderr",
            result.err.joinToString("\n")
        )
    }
}
