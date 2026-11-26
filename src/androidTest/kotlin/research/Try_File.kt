package tests.research

import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBoxQ
import com.topjohnwu.superuser.ShellUtils.fastCmd
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class Try_File {

    val file: File = File.createTempFile("Try_File", ".test")

    @Test
    fun test_File_ownerOnly() {

        fastCmd("touch ${file.absolutePath}")

        // set writable

        fastCmd("chmod 0 ${file.absolutePath}")
        assertEquals(
            "0",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 0 ${file.absolutePath}")
        file.setWritable(false, true)
        assertEquals(
            "0",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 0 ${file.absolutePath}")
        file.setWritable(false, false)
        assertEquals(
            "0",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 0 ${file.absolutePath}")
        file.setWritable(true, true)
        assertEquals(
            "200",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 0 ${file.absolutePath}")
        file.setWritable(true, false)
        assertEquals(
            "222",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        // reset writable

        fastCmd("chmod 777 ${file.absolutePath}")
        assertEquals(
            "777",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 777 ${file.absolutePath}")
        file.setWritable(false, true)
        assertEquals(
            "577",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 777 ${file.absolutePath}")
        file.setWritable(false, false)
        assertEquals(
            "555",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        file.delete()
    }

    @Test
    fun test_File_readOnly() {

        fastCmd("touch ${file.absolutePath}")

        fastCmd("chmod 0 ${file.absolutePath}")
        assertEquals(
            "0",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 0 ${file.absolutePath}")
        file.setReadOnly()
        assertEquals(
            "0",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 777 ${file.absolutePath}")
        assertEquals(
            "777",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        fastCmd("chmod 777 ${file.absolutePath}")
        file.setReadOnly()
        assertEquals(
            "555",
            fastCmd("$utilBoxQ stat -c '%a' ${quote(file)}")
        )

        file.delete()
    }

}