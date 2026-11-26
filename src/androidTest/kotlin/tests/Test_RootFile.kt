package tests.tests

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBoxQ
import com.topjohnwu.superuser.ShellUtils.fastCmd
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class Test_RootFile {

    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    //val context = Utils.getDeContext(Utils.getContext())
    //val context = Utils.getContext()
    val baseDir: String = context.cacheDir.absolutePath
    val fileJ: File = File.createTempFile("Test_RootFile", ".test")
    val fileX = RootFile(baseDir, "Test_RootFile.test")

    @Test
    fun test_RootFile() {

        fastCmd("touch ${fileJ.absolutePath}")
        fastCmd("touch ${fileX.absolutePath}")

        listOf(
            // 000
            { it: File ->
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

        fileJ.delete()
        fileX.delete()
    }

}