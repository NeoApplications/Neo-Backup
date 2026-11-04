package tests.research

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.entity.getCursorString
import com.machiav3lli.backup.manager.handler.LogsHandler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test
import timber.log.Timber
import kotlin.system.measureTimeMillis

class Try_readProperties {

    val backupRoot = "/sdcard/NB"

    @Test
    fun test_readText_SAF() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var size = -1L
        var text = ""
        val time = measureTimeMillis {
            val file = StorageFile(RootFile("/sdcard/test.txt"))
            size = file.size
            Timber.i("file ${file.path} size: $size")
            text = file.readText()
        }
        Timber.i("text size: ${text.length}")
        Timber.i("time SAF: $time ms")
        //assertEquals(size, text.length)
    }

    @Test
    fun test_readText_RootFile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var size = -1L
        var text = ""
        val time = measureTimeMillis {
            val file = RootFile("/sdcard/test.txt")
            size = file.length()
            Timber.i("file ${file.path} size: $size")
            text = file.readText()
        }
        Timber.i("text size: ${text.length}")
        Timber.i("time RootFile: $time ms")
        //assertEquals(size, text.length)
    }

    @Test
    fun test_scanReadProperties() {
        val backups = mutableMapOf<String, MutableList<Backup>>()
        val time = measureTimeMillis {
            var backupDir = StorageFile(RootFile(backupRoot))
            var text = ""
            backupDir.listFiles().forEach { packageDir ->
                val packageName = packageDir.name
                packageDir.listFiles().forEach { file ->
                    if (file.isPropertyFile)
                        text += file.readText() + "\n\n\n"
                }
            }
        }
        Timber.i("time scanning (read): $time ms")
    }

    @Test
    fun test_scanPropertiesSAF() {
        val backups = mutableMapOf<String, MutableList<Backup>>()
        val time = measureTimeMillis {
            val backupRoot = NeoApp.backupRoot
            backupRoot?.listFiles()?.forEach { packageDir ->
                val packageName = packageDir.name
                val backupList = mutableListOf<Backup>()
                packageDir.listFiles().forEach { file ->
                    if (file.isPropertyFile)
                        Backup.createFrom(file)?.let {
                            backupList.add(it)
                        }
                }
                if (!backupList.isEmpty())
                    packageName?.let { backups.put(it, backupList) }
            }
        }
        //val serialized = OABX.serializer.encodeToString(backups)
        //StorageFile(File("/sdcard/test.map")).outputStream()?.write(serialized.toByteArray())
        //Timber.i("backups: $serialized")
        Timber.i("packages: ${backups.size} backups: ${backups.map { it.value.size }.sum()}")
        Timber.w("time scanning (create backups): $time ms")
    }

    @Test
    fun test_scanPropertiesSAF_querySearchDocuments() {
        val backups = mutableMapOf<String, MutableList<Backup>>()
        val backupList = mutableListOf<Backup>()
        val time = measureTimeMillis {
            val backupRoot = NeoApp.backupRoot
            val treeUri = DocumentsContract
                .buildDocumentUriUsingTree(backupRoot!!.uri, DocumentsContract.getTreeDocumentId(backupRoot.uri))

            val authority = treeUri.authority
                            //"com.machiav3lli.backup.provider"
                            //"androidx.core.content.FileProvider"
                            //"com.android.externalstorage.documents" // this is the value baund to the treeUri
            val searchUri = DocumentsContract
                .buildSearchDocumentsUri(
                    authority,
                    treeUri.path,
                    ".properties"
                )

            val cursor = NeoApp.context.contentResolver
                .query(searchUri, null, Bundle.EMPTY, null)

            var documentUri: Uri
            while (cursor?.moveToNext() == true) {
                try {
                    val id = getCursorString(
                        cursor,
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID
                    ) ?: "???"
                    val name = getCursorString(
                        cursor,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    ) ?: "???"
                    documentUri =
                        DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            id
                        )
                    val file = StorageFile(null, documentUri, name)
                    if (file.isPropertyFile)
                        Backup.createFrom(file)?.let {
                            backupList.add(it)
                        }
                } catch (e: Throwable) {
                    LogsHandler.unexpectedException(e)
                }
            }
        }

        //val serialized = OABX.serializer.encodeToString(backups)
        //StorageFile(File("/sdcard/test.map")).outputStream()?.write(serialized.toByteArray())
        //Timber.i("backups: $serialized")
        Timber.i("backups: ${backupList.map { it.size }.sum()}")
        Timber.i("packages: ${backups.size} backups: ${backups.map { it.value.size }.sum()}")
        Timber.w("time scanning (create backups): $time ms")
    }

    @Test
    fun test_scanPropertiesRootFile() {
        val backups = mutableMapOf<String, MutableList<Backup>>()
        val time = measureTimeMillis {
            val backupDir = StorageFile(RootFile(backupRoot))
            backupDir.listFiles().forEach { packageDir ->
                val packageName = packageDir.name
                val backupList = mutableListOf<Backup>()
                packageDir.listFiles().forEach { file ->
                    if (file.isPropertyFile)
                        Backup.createFrom(file)?.let {
                            backupList.add(it)
                        }
                }
                if (!backupList.isEmpty())
                    packageName?.let { backups.put(it, backupList) }
            }
        }
        val serialized = NeoApp.mainSerializer.encodeToString(backups)
        //StorageFile(File("/sdcard/test.map")).outputStream()?.write(serialized.toByteArray())
        //Timber.i("backups: $serialized")
        Timber.i("packages: ${backups.size} backups: ${backups.map { it.value.size }.sum()}")
        Timber.w("time scanning (create backups): $time ms")
    }

    @Test
    fun test_readPropertiesFromSingleFile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backupList = mutableListOf<Backup>()
        var size = -1L
        var text = ""
        val time = measureTimeMillis {
            val file = StorageFile(RootFile("/sdcard/test.txt"))
            size = file.size
            Timber.i("file ${file.path} size: $size")
            text = file.readText()
            Timber.i("text size: ${text.length}")
            text.substring(1, text.length-1).split("}{").forEach {
                Backup.fromSerialized("{$it}").let { backupList.add(it) }
            }
        }
        Timber.i("backups: ${backupList.size}")
        Timber.w("time backups from single file: $time ms")
    }

    @Test
    fun test_readPropertiesFromSingleMapFile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var backups: MutableMap<String, MutableList<Backup>>
        val time = measureTimeMillis {
            val file = StorageFile(RootFile("/sdcard/test.map"))
            val size = file.size
            Timber.i("file ${file.path} size: $size")
            val text = file.readText()
            Timber.i("text size: ${text.length}")
            backups = NeoApp.mainSerializer.decodeFromString(text)
        }
        Timber.i("packages: ${backups.size} backups: ${backups.map { it.value.size }.sum()}")
        Timber.w("time backups from single map file: $time ms")
    }
}
