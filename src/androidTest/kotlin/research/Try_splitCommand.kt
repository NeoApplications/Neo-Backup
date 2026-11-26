package tests.research

import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.splitCommand
import junit.framework.TestCase.assertEquals
import org.junit.Test

class Try_splitCommand {

    @Test
    fun test_splitCommand_doubleQuotes() {
        val command = """ "with 'some' quirks" "with \"some\" quirks" """
        val cmd = splitCommand(command)
        assertEquals("""with 'some' quirks""", cmd[0])
        assertEquals("""with "some" quirks""", cmd[1])
    }

    @Test
    fun test_splitCommand_singleQuotes() {
        val command = """ 'with "some" quirks' 'with \"some\" quirks' """
        val cmd = splitCommand(command)
        assertEquals("""with "some" quirks""", cmd[0])
        assertEquals("""with \"some\" quirks""", cmd[1])
    }

}