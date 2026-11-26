package tests.research

import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBoxQ
import com.topjohnwu.superuser.ShellUtils.fastCmd
import com.topjohnwu.superuser.io.SuFile
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class Try_SuFile {

    val fileJ: File = File.createTempFile("Try_File", ".test")
    val fileX = SuFile("/cache/Try_SuFile.test")

    @Test
    fun test_SuFile() {

        listOf(
            // init
            { it: File ->
                fastCmd("touch ${it.absolutePath}")
                fastCmd("chmod 000 ${it.absolutePath}")
            },
            // 000
            {
                fastCmd("chmod 000 ${it.absolutePath}")
                it.setWritable(false, false)
            },
            {
                fastCmd("chmod 000 ${it.absolutePath}")
                it.setWritable(false, true)
            },
            {
                fastCmd("chmod 000 ${it.absolutePath}")
                it.setWritable(true, false)
            },
            {
                fastCmd("chmod 000 ${it.absolutePath}")
                it.setWritable(true, true)
            },
            // 777
            {
                fastCmd("chmod 777 ${it.absolutePath}")
                it.setWritable(false, false)
            },
            {
                fastCmd("chmod 777 ${it.absolutePath}")
                it.setWritable(false, true)
            },
            {
                fastCmd("chmod 777 ${it.absolutePath}")
                it.setWritable(true, false)
            },
            {
                fastCmd("chmod 777 ${it.absolutePath}")
                it.setWritable(true, true)
            },
            // readonly
            {
                fastCmd("chmod 000 ${it.absolutePath}")
                it.setReadOnly()
            },
            {
                fastCmd("chmod 777 ${it.absolutePath}")
                it.setReadOnly()
            },
        ).forEach { todo ->
            todo(fileJ)
            todo(fileX)
            val resultJ = fastCmd("$utilBoxQ stat -c '%a' ${quote(fileJ)}")
            val resultX = fastCmd("$utilBoxQ stat -c '%a' ${quote(fileX)}")
            //println("${if(resultJ==resultX) "ok  " else "FAIL"} $resultJ $resultX")
            assertEquals(resultJ, resultX)
        }

    }
}