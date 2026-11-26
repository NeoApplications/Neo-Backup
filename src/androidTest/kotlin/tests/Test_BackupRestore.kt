package tests.tests

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.machiav3lli.backup.ENCRYPTION
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.entity.UndeterminedStorageFile
import com.machiav3lli.backup.manager.actions.BackupAppAction
import com.machiav3lli.backup.manager.actions.RestoreAppAction
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBox
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBoxQ
import com.machiav3lli.backup.ui.pages.pref_encryption_mode
import com.topjohnwu.superuser.ShellUtils.fastCmd
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

//val timeCompareFormat = "yyyy-MM-dd'T'HH:mm:ss"
const val TIME_COMPARE_FORMAT = "yyyy-MM-dd'T'HH:mm"

class Test_BackupRestore {

    companion object {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        //val context = Utils.getDeContext(Utils.getContext())
        //val context = Utils.getContext()

        lateinit var tempDir: RootFile
        lateinit var testDir: RootFile

        lateinit var shellHandler: ShellHandler
        lateinit var tempStorage: StorageFile
        lateinit var backupDirTarApi: StorageFile
        lateinit var backupDirTarCmd: StorageFile
        lateinit var restoreCache: File

        var backupCreated = false
        var contentLs = listOf<String>()
        var contentCount = 0

        init {
            ShellHandler.FileInfo.utilBoxInfo().forEach { Timber.w(it) }
        }

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            shellHandler = NeoApp.shellHandler!!
            //tempDir = RootFile(context.cacheDir, "test_backup_restore")
            tempDir = RootFile(context.dataDir, "test_backup_restore")
            //tempDir = RootFile("/data/local/tmp/test_backup_restore")
            tempDir.deleteRecursive()
            tempDir.mkdirs()
            testDir = prepareDirectory()
        }

        @AfterClass
        @JvmStatic
        fun cleanupClass() {
            tempDir.deleteRecursive()
        }

        fun listContent(dir: RootFile): List<String> {
            //return runAsRoot("ls -bAlZ ${dir.quoted} | grep -v total | sort").out.joinToString("\n")
            return runAsRoot(
                """cd ${dir.escapedPath} ; stat -c '%-20n %y %A %f %-15U %-15G %8s %F' .* * | grep -v '\.\$'"""
            ).out
                .filterNotNull()
                .map { it.replace(Regex(""":\d\d\.\d{9}\b"""), "") } // :seconds.millis
                .map { it.replace(Regex(""" link +\w+ +\w+"""), " link") } // to target
                .sorted()
        }

        var checks = mutableMapOf<String, (file: RootFile) -> Boolean>()
        fun compareTime(want: String, real: Long) =
            want.startsWith(
                SimpleDateFormat(
                    TIME_COMPARE_FORMAT,
                    Locale.getDefault()
                ).format(real)
            )

        private fun prepareDirectory(): RootFile {

            contentCount = 0

            tempDir.setWritable(writable = true, ownerOnly = false)

            var dir: RootFile
            var files: RootFile

            run {
                val name = "test_data"
                dir = RootFile(tempDir, name)
                dir.mkdirs()
            }

            run {
                val name = "dir"
                val fileTime = "2000-11-22T12:34:01"
                val file = RootFile(dir, name)
                files = file
                file.mkdirs()
                fastCmd("$utilBoxQ touch -d $fileTime ${file.escapedPath}")
                checks["$name/type"] = { it.isDirectory }
                checks["#off#$name/time"] = { compareTime(fileTime, it.lastModified()) }
                contentCount++
            }

            run {
                val name = "regular"
                val fileContent = "TEST"
                val fileTime = "2000-11-22T12:34:01"
                val file = RootFile(dir, name)
                fastCmd("$utilBoxQ echo -n '$fileContent' >${file.escapedPath}")
                fastCmd("$utilBoxQ touch -d $fileTime ${file.escapedPath}")
                checks["$name/type"] = { it.isFile }
                checks["$name/content"] = { it.readText() == fileContent }
                checks["$name/time"] = { compareTime(fileTime, it.lastModified()) }
                contentCount++
            }

            run {
                val name = "regular_system"
                val fileContent = "SYSTEM"
                val fileTime = "2000-11-22T12:34:02"
                val fileOwner = "system:system"
                val file = RootFile(dir, name)
                fastCmd("$utilBoxQ echo -n '$fileContent' >${file.escapedPath}")
                fastCmd("$utilBoxQ chown $fileOwner ${file.escapedPath}")
                fastCmd("$utilBoxQ touch -d $fileTime ${file.escapedPath}")
                checks["$name/type"] = { it.isFile }
                checks["$name/content"] = { it.readText() == fileContent }
                checks["$name/time"] = { compareTime(fileTime, it.lastModified()) }
                contentCount++
            }

            run {
                val name = "fifo"
                val file = RootFile(dir, name)
                val fileTime = "2000-11-22T12:34:03"
                fastCmd("$utilBoxQ mkfifo ${file.escapedPath}")
                fastCmd("$utilBoxQ touch -d $fileTime ${file.escapedPath}")
                checks["#off#$name/time"] = { compareTime(fileTime, it.lastModified()) }
                contentCount++
            }

            run {
                val name = "socket"
                val file = RootFile(dir, name)
                fastCmd("$utilBoxQ rm ${file.escapedPath} ; ($utilBoxQ nc -U -q 1 -W 1 -l -s ${file.escapedPath} & exit) ")
                checks["#off#$name/notexist"] = { !it.exists() }
                contentCount++
            }

            run {
                val name = "link_sym_dir_rel"
                val file = RootFile(dir, name)
                fastCmd("cd ${dir.escapedPath} && $utilBoxQ ln -s ${files.name} ${file.name}")
                checks["$name/type"] = { it.isSymlink }
                contentCount++
            }

            run {
                val name = "link_sym_dir_abs"
                val file = RootFile(dir, name)
                fastCmd("$utilBoxQ ln -s ${files.absolutePath} ${file.escapedPath}")
                checks["$name/type"] = { it.isSymlink }
                contentCount++
            }

            if (!utilBox.hasBug("DotDotDirHang") && !utilBox.hasBug("DotDotDirExtract")) {
                run {
                    val name = "..dir"
                    val file = RootFile(dir, name)
                    fastCmd("$utilBoxQ mkdir -p ${file.escapedPath}")
                    checks["$name/type"] = { it.isDirectory }
                    contentCount++
                }
            }

            contentLs = listContent(dir)

            return dir
        }
    }

    //val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivityX::class.java)
    //    .putExtra("title", "Testing rules!")

    //@get:Rule
    //val activityRule = activityScenarioRule<MainActivityX>( /* intent */ )
    //var activityRule = ActivityScenarioRule(MainActivityX::class.java)

    @Before
    fun setup() {
        tempStorage = StorageFile(tempDir)
        backupDirTarApi = tempStorage.createDirectory("backup_tarapi")
        backupDirTarCmd = tempStorage.createDirectory("backup_tarcmd")
        //restoreCache = RootFile(tempDir, "restore").also { it.mkdirs() }
        //restoreCache = RootFile(context.cacheDir)
        restoreCache = File(context.cacheDir, "restore").also { it.mkdirs() }
    }

    @After
    fun cleanup() {
        RootFile(restoreCache).deleteRecursive()
    }

    fun archiveTarApi(compress: Boolean) =
        UndeterminedStorageFile(backupDirTarApi, "data.tar${if (compress) ".gz" else ""}")

    fun archiveTarCmd(compress: Boolean) =
        UndeterminedStorageFile(backupDirTarCmd, "data.tar${if (compress) ".gz" else ""}")

    fun checkContent(dir: RootFile) {
        val want = contentLs
        val real = listContent(dir)
        assertTrue(want.isNotEmpty())
        assertTrue(real.isNotEmpty())
        //assertEquals(want, real)
        checks.forEach { (name, check) ->
            if (name.startsWith("#"))
                return@forEach
            val fileName = name.split("/").first()
            val result = check(RootFile(dir, fileName))
            if (!result)
                Timber.w("checkDirectory: FAILED: $name")
            else
                Timber.w("checkDirectory: OK:     $name")
            Timber.w("checkDirectory/want: ${want.firstOrNull { it.substringBefore(" ") == fileName } ?: "<missing>"}")
            Timber.w("checkDirectory/real: ${real.firstOrNull { it.substringBefore(" ") == fileName } ?: "<missing>"}")
            assertTrue("check $name failed", result)
        }
    }

    @Test
    fun test_sourceContents() {
        checkContent(testDir)
    }

    fun backup(compress: Boolean) {
        if (!backupCreated) {
            pref_encryption_mode.value = ENCRYPTION.NONE.ordinal
            val iv = null     //initIv(CIPHER_ALGORITHM)
            val backupAction = BackupAppAction(context, null, shellHandler)
            val fromDir = StorageFile(testDir)
            backupAction.genericBackupDataTarApi(
                "data", backupDirTarApi, fromDir.toString(),
                compress, iv
            )
            backupAction.genericBackupDataTarCmd(
                "data", backupDirTarCmd, fromDir.toString(),
                compress, iv
            )
            backupCreated = true
        }
    }

    /*
    fun listTar(archive: StorageFile): String {
        val result = runAsRoot(
            // toybox doesn't have awk
            //"tar -tvzf ${quote(archive.file!!.absolutePath)} | awk '{if($8) {print $1,$2,$3,$6,$7,$8} else {print $1,$2,$3,$6}}'"
            "tar -tvzf ${quote(archive.path!!)}"
        ).out
            .filterNotNull()
            .map { it.replace(Regex("""/$"""), "") }
            .map { it.replace(Regex(""" \./"""), " ") }
            .sorted()
            .joinToString("\n")
        return result
    }
    */

    fun checkRestore(toType: String, fromType: String) {
        val compress = false

        backup(compress)

        val archive = (
                if (fromType == "tarapi")
                    archiveTarApi(compress)
                else
                    archiveTarCmd(compress)
                ).findFile()!!

        Timber.w("#################### TAR ${archive.path}")
        runAsRoot("tar -tvzf ${quote(archive.path!!)}")
        val restoreAction = RestoreAppAction(context, null, shellHandler)
        val restoreDir = RootFile(tempDir, "restore_$toType")
        restoreDir.mkdirs()
        runBlocking {
            if (toType == "tarapi")
                restoreAction.genericRestoreFromArchiveTarApi(
                    "data", archive, restoreDir.toString(),
                    compress, null, false, null, restoreCache
                )
            else
                restoreAction.genericRestoreFromArchiveTarCmd(
                    "data", archive, restoreDir.toString(),
                    compress, null, false, null
                )
        }
        Timber.w("#################### RES ${archive.path}")
        runAsRoot("ls -bAlZ ${restoreDir.escapedPath} | grep -v total | sort")
        checkContent(restoreDir)
    }

    @Test
    fun test_restore_tarapi_from_tarapi() {
        checkRestore("tarapi", "tarapi")
    }

    @Test
    fun test_restore_tarcmd_from_tarapi() {
        checkRestore("tarcmd", "tarapi")
    }

    @Test
    fun test_restore_tarapi_from_tarcmd() {
        checkRestore("tarapi", "tarcmd")
    }

    @Test
    fun test_restore_tarcmd_from_tarcmd() {
        checkRestore("tarcmd", "tarcmd")
    }
}
