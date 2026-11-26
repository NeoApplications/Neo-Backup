package tests.tests

import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRootPipeInCollectErr
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRootPipeOutCollectErr
import com.machiav3lli.backup.manager.handler.ShellHandler.ShellPipeInFailedException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class Test_ShellHandler {

    @Test
    fun test_fromLsOOutput_canHandleWhitespace() {
        val fileInfo = ShellHandler.FileInfo.fromLsOutput(
                "-rw------- 1 user0_a247 group0_a247 15951095 2021-01-19 01:03:29.000000000 +0100 Aurora Store-3.2.8.apk",
                "/data/data/org.fdroid.fdroid/files"
        )
        assertNotNull(fileInfo!!)
        assertEquals(
			    "Aurora Store-3.2.8.apk",
                fileInfo.filePath
        )
        assertEquals(
				"/data/data/org.fdroid.fdroid/files/Aurora Store-3.2.8.apk",
                fileInfo.absolutePath
        )
        assertEquals(
				15951095,
                fileInfo.fileSize
        )
        assertEquals(
				ShellHandler.FileType.REGULAR_FILE,
                fileInfo.fileType
        )
    }

    @Test
    fun test_fromLsOOutput_canHandleMultiWhitespace() {
        val fileInfo = ShellHandler.FileInfo.fromLsOutput(
                "-rw------- 1 user0_a247 group0_a247 15951095 2021-01-19 01:03:29.000000000 +0100 111   333.file",
                "/data/data/org.fdroid.fdroid/files"
        )
        assertNotNull(fileInfo!!)
        assertEquals(
				"111   333.file",
                fileInfo.filePath
        )
        assertEquals(
				"/data/data/org.fdroid.fdroid/files/111   333.file",
                fileInfo.absolutePath
        )
        assertEquals(
				15951095,
                fileInfo.fileSize
        )
        assertEquals(
				"user0_a247",
                fileInfo.owner
        )
        assertEquals(
				"group0_a247",
                fileInfo.group
        )
        assertEquals(
				1611014609000,
                fileInfo.fileModTime.time
        )
        assertEquals(
				0b0_110_000_000,
                fileInfo.fileMode
        )
        assertEquals(
				ShellHandler.FileType.REGULAR_FILE,
                fileInfo.fileType
        )
    }

    @Test
    fun test_fromLsOOutput_canHandleOldFormat() {
        val fileInfo = ShellHandler.FileInfo.fromLsOutput(
                "-rw------- 1 user0_a247 group0_a247 15951095 2021-01-19 01:03 111   333.file",
                "/data/data/org.fdroid.fdroid/files"
        )
        assertNotNull(fileInfo!!)
        assertEquals(
				"111   333.file",
                fileInfo.filePath
        )
        assertEquals(
				"/data/data/org.fdroid.fdroid/files/111   333.file",
                fileInfo.absolutePath
        )
        assertEquals(
				15951095,
                fileInfo.fileSize
        )
        assertEquals(
				"user0_a247",
                fileInfo.owner
        )
        assertEquals(
				"group0_a247",
                fileInfo.group
        )
        assertEquals(
            (1611014609000/60_000) * 60_000L, // only minutes, remove seconds
            fileInfo.fileModTime.time
        )
        assertEquals(
				0b0_110_000_000,
                fileInfo.fileMode
        )
        assertEquals(
				ShellHandler.FileType.REGULAR_FILE,
                fileInfo.fileType
        )
    }

    @Test
    fun test_fromLsOOutput_canHandleSpecialChars() {
        val fileInfo = ShellHandler.FileInfo.fromLsOutput(
                """-rw------- 1 user0_a247 group0_a247 15951095 2021-01-19 01:03:29.000000000 +0100 My|#$%^&*[](){}'"`:;?<~>,.file""",
                "/data/data/org.fdroid.fdroid/files"
        )
        assertNotNull(fileInfo!!)
        assertEquals(
				"""My|#$%^&*[](){}'"`:;?<~>,.file""",
                fileInfo.filePath
        )
        assertEquals(
				"""/data/data/org.fdroid.fdroid/files/My|#$%^&*[](){}'"`:;?<~>,.file""",
                fileInfo.absolutePath
        )
        assertEquals(
				15951095,
                fileInfo.fileSize
        )
        assertEquals(
				ShellHandler.FileType.REGULAR_FILE,
                fileInfo.fileType
        )
    }

    @Test
    fun test_fromLsOOutput_canHandleEscapedChars() {
        val fileInfo = ShellHandler.FileInfo.fromLsOutput(
            """-rw-r--r-- 1 root    root          0 2021-09-22 19:42:30.452216949 +0200 \123 \a\b\e\e\f\n\r\t\v\\.file""",
            "/data/data/org.fdroid.fdroid/files"
        )
        assertNotNull(fileInfo!!)
        assertEquals(
            "\u0053 \u0007\u0008\u001b\u001b\u000c\u000a\u000d\u0009\u000b\u005c.file",
            fileInfo.filePath
        )
    }

    @Test
    fun test_fromLsOOutput_canHandleLinks() {
        val fileInfo = ShellHandler.FileInfo.fromLsOutput(
            """lrwxrwxrwx 1 root system 50 2022-02-08 19:22:55.210000000 bugreports -> /data/user_de/0/com.android.shell/files/bugreports""",
            "/data/user_de/0/com.android.shell/"
        )
        assertNotNull(fileInfo!!)
        assertEquals(
            "bugreports",
            fileInfo.filePath
        )
        assertEquals(
            "/data/user_de/0/com.android.shell/files/bugreports",
            fileInfo.linkName
        )
    }

    @Test
    fun test_fromLsOOutput_canHandleLinksOfOldFormat() {
        val fileInfo = ShellHandler.FileInfo.fromLsOutput(
            """lrwxrwxrwx 1 root root     5       2022-02-09     07:03     link_sym_dir_rel -> files""",
            "/data/data/org.fdroid.fdroid/"
        )
        assertNotNull(fileInfo!!)
        assertEquals(
            "link_sym_dir_rel",
            fileInfo.filePath
        )
        assertEquals(
            "files",
            fileInfo.linkName
        )
    }

    @Test
    fun test_quote() {
        assertEquals(
				"""${'"'}My\\|\$&\"'\`[](){}   =:;?<~>-+!%^#*,.file${'"'}""",
                ShellHandler.quote("""My\|$&"'`[](){}   =:;?<~>-+!%^#*,.file""")
        )
    }

    @Test
    fun test_shell() {
        val result = runAsRoot("echo -n 1234567890 ; echo -n ERROR 1>&2")
        assertEquals(
            0,
            result.code
        )
        assertEquals(
            "1234567890",
            result.out.joinToString("\n")
        )
        assertEquals(
            "ERROR",
            result.err.joinToString("\n")
        )
    }

    @Test
    fun test_pipe_out() {
        val outStream = ByteArrayOutputStream()
        val (code, err) = runBlocking {
            runAsRootPipeOutCollectErr(outStream, "echo -n 1234567890 ; echo -n ERROR 1>&2")
        }
        assertEquals(
            "1234567890",
            outStream.toString()
        )
        assertEquals(
            "ERROR",
            err
        )
        assertEquals(
            0,
            code
        )
    }

    @Test
    fun test_pipe_in() {
        val inStream = ByteArrayInputStream("1234567890".encodeToByteArray())
        val (code, err) = runAsRootPipeInCollectErr(inStream, "md5sum 1>&2")
        assertEquals(
            "e807f1fcf82d132f9bb018ca6738a19f  -\n",
            err
        )
        assertEquals(
            0,
            code
        )
    }

    @Test
    fun test_pipe_in_large_input() {
        val size = 1024 * 1024
        val inStream = ByteArrayInputStream(ByteArray(size))
        val (code, err) = runAsRootPipeInCollectErr(inStream, "wc -c 1>&2")
        assertEquals(0, code)
        assertEquals(size.toLong(), err.trim().toLong())
    }

    @Test
    fun test_pipe_in_large_stdout() {
        val inStream = ByteArrayInputStream("1234567890".encodeToByteArray())
        val (code, err) = runAsRootPipeInCollectErr(
            inStream,
            "dd if=/dev/zero bs=1024 count=512 2>/dev/null; md5sum 1>&2"
        )
        assertEquals(0, code)
        assertEquals("e807f1fcf82d132f9bb018ca6738a19f  -\n", err)
    }

    @Test
    fun test_pipe_in_failing_input() {
        val inStream = object : InputStream() {
            override fun read(): Int = throw IOException("no more data")
        }
        try {
            runAsRootPipeInCollectErr(inStream, "cat >/dev/null")
            fail("expected ShellPipeInFailedException")
        } catch (e: ShellPipeInFailedException) {
            assertEquals("no more data", e.feedError.message)
        }
    }
}
