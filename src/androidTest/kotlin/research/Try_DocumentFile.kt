package tests.research

import androidx.documentfile.provider.DocumentFile
import androidx.test.platform.app.InstrumentationRegistry
import com.machiav3lli.backup.MIME_TYPE_FILE
import com.machiav3lli.backup.ui.pages.pref_shadowRootFile
import com.machiav3lli.backup.utils.TraceUtils
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

val context = InstrumentationRegistry.getInstrumentation().targetContext
//val context = Utils.getDeContext(Utils.getContext())
//val context = Utils.getContext()

class MyFile(var document: DocumentFile) {

    val uri get() = document.uri
    val name get() = document.name

    val isDirectory: Boolean get() = document.isDirectory

    fun exists(): Boolean {
        return document.exists()
    }

    fun writeText(text: String) {
        context.contentResolver.openOutputStream(document.uri)!!.use {
            //it.writer().write(text)       // why doesn't this work?
            it.write(text.toByteArray())    // but this does?
        }
    }

    fun readText(): String {
        return context.contentResolver.openInputStream(document.uri)!!.use {
            val text = it.reader().readText()
            text
        }
    }

    companion object {
        fun fromFile(file: File): MyFile {
            return MyFile(DocumentFile.fromFile(file))
        }
    }

    fun createFile(name: String): MyFile? {
        val file = document.createFile(MIME_TYPE_FILE, name)
        file?.let {
            if (it.name != name)
                it.renameTo(name)   // need to delete existing target
            return MyFile(file)
        }
        return null
    }

    fun createDirectory(name: String): MyFile? {
        val dir = document.createDirectory(name)
        dir?.let {
            if (it.name != name)
                it.renameTo(name)   // need to delete existing target
            return MyFile(dir)
        }
        return null
    }

    fun delete(): Boolean {
        return !(isDirectory && document.listFiles().count() > 0) && document.delete()
    }

    fun deleteRecursive(): Boolean {
        return document.delete()
    }

    fun listFiles(): List<MyFile> {
        return document.listFiles().map { MyFile(it) }
    }
}

class Try_DocumentFile {

    val baseDirAsFile = (
            //context.cacheDir
            context.externalCacheDir!!
            //context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
            )
    val baseDir = MyFile.fromFile(baseDirAsFile)

    val testDirName get() = TraceUtils.classAndMethodName().replace(':', '_')
    val testFileName = "test.txt"
    val testText1 = "test text 1"
    val testText2 = "test text 2"
    val testText = "test text"

    @Before
    fun setup() {
        pref_shadowRootFile.value = false
    }

    @Test
    fun test_DeleteFile() {

        val dir = baseDir.createDirectory(testDirName)!!

        assertEquals(testDirName, dir.name)

        val file = dir.createFile(testFileName)!!

        assertEquals(testFileName, file.name)

        file.writeText(testText)

        assertEquals(true, dir.exists())
        assertEquals(true, dir.isDirectory)
        assertEquals(testText, file.readText())
        assertEquals(1, dir.listFiles().count())
        assertEquals(testFileName, dir.listFiles().first().name)

        file.delete()

        assertEquals(true, dir.exists())
        assertEquals(true, dir.isDirectory)
        assertEquals(0, dir.listFiles().count())

        file.delete()   // delete non existent

        assertEquals(true, dir.exists())
        assertEquals(true, dir.isDirectory)
        assertEquals(0, dir.listFiles().count())

        dir.deleteRecursive()

        assertEquals(false, dir.exists())
        assertEquals(false, dir.isDirectory)
    }

    @Test
    fun test_DuplicateDir() {

        val dir = baseDir.createDirectory(testDirName)!!

        assertEquals(testDirName, dir.name)

        val file = dir.createFile(testFileName)!!

        assertEquals(testFileName, file.name)

        file.writeText(testText)

        assertEquals(true, dir.exists())
        assertEquals(true, dir.isDirectory)
        assertEquals(testText, file.readText())
        assertEquals(1, dir.listFiles().count())
        assertEquals(testFileName, dir.listFiles().first().name)

        val dir2 = baseDir.createDirectory(testDirName)!!   // create existing dir

        assertEquals(true, dir2.exists())
        assertEquals(true, dir2.isDirectory)
        assertEquals(testText, file.readText())
        assertEquals(1, dir.listFiles().count())
        assertEquals(testFileName, dir.listFiles().first().name)

        dir.deleteRecursive()

        assertEquals(false, dir.exists())
        assertEquals(false, dir.isDirectory)
        assertEquals(false, dir2.exists())
        assertEquals(false, dir2.isDirectory)
    }


    @Test
    fun test_OverwriteFile() {

        val dir = baseDir.createDirectory(testDirName)!!

        assertEquals(testDirName, dir.name)

        val file1 = dir.createFile(testFileName)!!

        assertEquals(testFileName, file1.name)

        file1.writeText(testText1)

        assertEquals(true, dir.exists())
        assertEquals(true, dir.isDirectory)
        assertEquals(testText1, file1.readText())
        assertEquals(1, dir.listFiles().count())
        assertEquals(testFileName, dir.listFiles().first().name)

        val file2 = dir.createFile(testFileName)!!

        file2.writeText(testText2)

        assertEquals(true, dir.exists())
        assertEquals(true, dir.isDirectory)
        assertEquals(testText2, file2.readText())
        assertEquals(1, dir.listFiles().count())
        assertEquals(testFileName, dir.listFiles().first().name)

        val file = dir.createFile(testFileName)!!

        file.writeText(testText)

        assertEquals(true, dir.exists())
        assertEquals(true, dir.isDirectory)
        assertEquals(testText, file.readText())
        assertEquals(1, dir.listFiles().count())
        assertEquals(testFileName, dir.listFiles().first().name)

        dir.delete()

        assertEquals(true, dir.exists())
        assertEquals(true, dir.isDirectory)
        assertEquals(file.readText(), testText)
        assertEquals(1, dir.listFiles().count())
        assertEquals(testFileName, dir.listFiles().first().name)

        dir.deleteRecursive()

        assertEquals(false, dir.exists())
        assertEquals(false, dir.isDirectory)
    }
}
