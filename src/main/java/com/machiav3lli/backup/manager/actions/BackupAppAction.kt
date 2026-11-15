/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.manager.actions

import android.annotation.SuppressLint
import android.content.Context
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.MODE_DATA
import com.machiav3lli.backup.MODE_DATA_DE
import com.machiav3lli.backup.MODE_DATA_EXT
import com.machiav3lli.backup.MODE_DATA_MEDIA
import com.machiav3lli.backup.MODE_DATA_OBB
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.batchModes
import com.machiav3lli.backup.batchOperations
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.ActionResult
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.plugins.InternalShellScriptPlugin
import com.machiav3lli.backup.data.preferences.traceAccess
import com.machiav3lli.backup.manager.handler.BackupBuilder
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.PGPHandler
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.isFileNotFoundException
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRootPipeOutCollectErr
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBoxQ
import com.machiav3lli.backup.manager.handler.ShellHandler.ShellCommandFailedException
import com.machiav3lli.backup.manager.tasks.AppActionWork
import com.machiav3lli.backup.ui.pages.pref_backupCache
import com.machiav3lli.backup.ui.pages.pref_backupPauseApps
import com.machiav3lli.backup.ui.pages.pref_backupTarCmd
import com.machiav3lli.backup.ui.pages.pref_enableStorageCheck
import com.machiav3lli.backup.ui.pages.pref_fakeBackupSeconds
import com.machiav3lli.backup.utils.CIPHER_ALGORITHM
import com.machiav3lli.backup.utils.CryptoSetupException
import com.machiav3lli.backup.utils.FileUtils.BackupLocationInAccessibleException
import com.machiav3lli.backup.utils.FileUtils.checkAvailableStorage
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.TraceUtils.canonicalName
import com.machiav3lli.backup.utils.copyRootFileToDocument
import com.machiav3lli.backup.utils.encryptStream
import com.machiav3lli.backup.utils.getCompressionLevel
import com.machiav3lli.backup.utils.getCompressionType
import com.machiav3lli.backup.utils.getCryptoSalt
import com.machiav3lli.backup.utils.getEncryptionPassword
import com.machiav3lli.backup.utils.initIv
import com.machiav3lli.backup.utils.isCompressionEnabled
import com.machiav3lli.backup.utils.isEncryptionEnabled
import com.machiav3lli.backup.utils.isPGPEncryptionEnabled
import com.machiav3lli.backup.utils.isPasswordEncryptionEnabled
import com.machiav3lli.backup.utils.suAddFiles
import com.topjohnwu.superuser.ShellUtils
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import org.koin.java.KoinJavaComponent.get
import org.pgpainless.algorithm.SymmetricKeyAlgorithm
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream

open class BackupAppAction(context: Context, work: AppActionWork?, shell: ShellHandler) :
    BaseAppAction(context, work, shell) {

    // TODO migrate to Arrow
    open fun run(app: Package, backupMode: Int): ActionResult {
        var backup: Backup? = null
        var ok = false
        val fakeSeconds = pref_fakeBackupSeconds.value

        fun handleException(e: Throwable): ActionResult {
            val message =
                "${e::class.simpleName}: ${e.message}${e.cause?.message?.let { " - $it" } ?: ""}"
            Timber.e("Backup failed: $message")
            return ActionResult(app, null, message, false)
        }

        try {
            Timber.i("Backing up: ${app.packageName} (${app.packageLabel})")
            work?.setOperation("B")

            if (fakeSeconds > 0) {

                val step = 1000L * 1
                val startTime = SystemUtils.msSinceBoot
                do {
                    val now = SystemUtils.msSinceBoot
                    val seconds = (now - startTime) / 1000.0
                    work?.setOperation((seconds / 10).toInt().toString().padStart(3, '0'))
                    Thread.sleep(step)
                } while (seconds < fakeSeconds)

                val succeeded = true // random() < 0.75

                return if (succeeded) {
                    Timber.w("package: ${app.packageName} faking success")
                    ActionResult(app, null, "faked backup succeeded", true)
                } else {
                    Timber.w("package: ${app.packageName} faking failure")
                    ActionResult(app, null, "faked backup failed", false)
                }
            }

            val appBackupBaseDir: StorageFile = try {
                app.getAppBackupBaseDir(create = true)!!
            } catch (e: BackupLocationInAccessibleException) {
                // Usually, this should never happen, but just in case...
                return handleException(BackupFailedException(STORAGE_LOCATION_INACCESSIBLE, e))
            } catch (e: StorageLocationNotConfiguredException) {
                return handleException(BackupFailedException(STORAGE_LOCATION_INACCESSIBLE, e))
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e, app)
                // Usually, this should never happen, but just in case...
                return handleException(BackupFailedException(STORAGE_LOCATION_INACCESSIBLE, e))
            }

            if (pref_enableStorageCheck.value) try {
                checkAvailableStorage(context, app, backupMode)
            } catch (e: BackupFailedException) {
                return handleException(e)
            }

            val backupBuilder = try {
                BackupBuilder(app.packageInfo, appBackupBaseDir)
            } catch (e: Throwable) {
                return handleException(BackupFailedException(STORAGE_LOCATION_INACCESSIBLE, e))
            }
            val iv = initIv(CIPHER_ALGORITHM) // as we're using a static Cipher Algorithm
            backupBuilder.setIv(iv)

            val backupInstanceDir = backupBuilder.backupDir
            val pauseApp = pref_backupPauseApps.value
            if (pauseApp)
                pauseApp(type = "backup", wh = When.PRE, packageName = app.packageName)

            try {
                fun doBackup(mode: Int, todo: () -> Unit) {
                    if ((backupMode and mode) != 0) {
                        Timber.i("$app: Backing up ${batchModes[mode]}")
                        work?.setOperation(batchOperations[mode]!!)
                        todo()
                    }
                }
                doBackup(MODE_APK) {
                    backupPackage(app, backupInstanceDir)
                    backupBuilder.setHasApk(true)
                }
                doBackup(MODE_DATA) {
                    backupBuilder.setHasAppData(
                        backupData(app, backupInstanceDir, iv)
                    )
                }
                doBackup(MODE_DATA_DE) {
                    backupBuilder.setHasDevicesProtectedData(
                        backupDeviceProtectedData(app, backupInstanceDir, iv)
                    )
                }
                doBackup(MODE_DATA_EXT) {
                    backupBuilder.setHasExternalData(
                        backupExternalData(app, backupInstanceDir, iv)
                    )
                }
                doBackup(MODE_DATA_OBB) {
                    backupBuilder.setHasObbData(
                        backupObbData(app, backupInstanceDir, iv)
                    )
                }
                doBackup(MODE_DATA_MEDIA) {
                    backupBuilder.setHasMediaData(
                        backupMediaData(app, backupInstanceDir, iv)
                    )
                }
                if (isCompressionEnabled()) {
                    Timber.i("$app: Compressing backup using ${getCompressionType()}")
                    backupBuilder.setCompressionType(getCompressionType())
                }
                when {
                    isPasswordEncryptionEnabled() ->
                        backupBuilder.setCipherType(CIPHER_ALGORITHM)

                    isPGPEncryptionEnabled()      ->
                        backupBuilder.setCipherType(SymmetricKeyAlgorithm.AES_256.name)
                }
                StorageFile.invalidateCache(backupInstanceDir)
                val backupSize = backupInstanceDir.listFiles().sumOf { it.size }
                backupBuilder.setSize(backupSize)

                backup = backupBuilder.createBackup()

                ok = backup.file != null

            } catch (e: BackupFailedException) {
                return handleException(e)
            } catch (e: CryptoSetupException) {
                return handleException(e)
            } catch (e: IOException) {
                return handleException(e)
            } finally {
                work?.setOperation("======")
                if (pauseApp)
                    pauseApp(type = "backup", wh = When.POST, packageName = app.packageName)
                if (backup == null)
                    backup = backupBuilder.createBackup()
                if (ok)
                    app.addNewBackup(backup)
                else {
                    Timber.d("Backup failed -> deleting it")
                    app.removeBackup(backup)
                }
            }
        } catch (e: Throwable) {
            return handleException(e)
        } finally {
            work?.setOperation("======>")
            Timber.i("${app.packageName}: Backup done: $backup")
        }
        return ActionResult(app, backup, "", true)
    }

    fun createArchiveFile(
        dataType: String,
        backupInstanceDir: StorageFile,
        compress: Boolean,
        iv: ByteArray?
    ): OutputStream {
        val shouldCompress = compress && isCompressionEnabled()

        val backupFilename = getBackupArchiveFilename(
            dataType,
            shouldCompress,
            getCompressionType(),
            isEncryptionEnabled()
        )
        val backupFile = backupInstanceDir.createFile(backupFilename)

        var outStream: OutputStream = backupFile.outputStream()!!

        when {
            isPasswordEncryptionEnabled() -> {
                val password = getEncryptionPassword()
                if (iv == null) throw CryptoSetupException(Exception("IV is null"))
                if (password.isEmpty()) throw CryptoSetupException(Exception("password is empty"))
                outStream = outStream.encryptStream(password, getCryptoSalt(), iv)
            }

            isPGPEncryptionEnabled()      -> {
                get<PGPHandler>(PGPHandler::class.java).encryptStream(outStream).fold(
                    onSuccess = { it?.let { outStream = it } },
                    onFailure = { throw CryptoSetupException(it) },
                )
            }
        }

        if (shouldCompress) {
            val compressionLevel = getCompressionLevel()
            when (getCompressionType()) {
                "no" -> {}
                "gz" -> {
                    val gzipParams = GzipParameters()
                    gzipParams.compressionLevel = compressionLevel

                    outStream = GzipCompressorOutputStream(
                        outStream,
                        gzipParams
                    )
                }

                "zst" -> {
                    outStream = ZstdCompressorOutputStream.builder()
                        .setOutputStream(outStream)
                        .setLevel(compressionLevel)
                        .get()
                }

                else -> throw UnsupportedOperationException("Unsupported compression algorithm: ${getCompressionType()}")
            }
        }
        return outStream
    }

    @Throws(IOException::class, CryptoSetupException::class)
    protected fun createBackupArchiveTarApi(
        backupInstanceDir: StorageFile,
        dataType: String,
        allFilesToBackup: List<ShellHandler.FileInfo>,
        compress: Boolean,
        iv: ByteArray?,
    ) {
        Timber.i("Creating $dataType backup via API")

        val outStream = createArchiveFile(dataType, backupInstanceDir, compress, iv)

        try {
            TarArchiveOutputStream(outStream).use { archive ->
                archive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                archive.suAddFiles(allFilesToBackup)
            }
        } finally {
            Timber.d("Done compressing. Closing archive stream.")
            outStream.close()
        }
    }

    @Throws(BackupFailedException::class)
    private fun assembleFileList(sourcePath: String): List<ShellHandler.FileInfo> {
        return try {
            val excludeCache = !pref_backupCache.value
            val allFilesToBackup =
                shell.suGetDetailedDirectoryContents(sourcePath, true, sourcePath)
                    .filterNot { f: ShellHandler.FileInfo ->
                        f.filename in NeoApp.assets.DATA_BACKUP_EXCLUDED_BASENAMES
                                || f.filename in NeoApp.assets.DATA_EXCLUDED_NAMES
                                || (excludeCache && f.filename in NeoApp.assets.DATA_EXCLUDED_CACHE_DIRS)
                    }
            allFilesToBackup
        } catch (e: ShellCommandFailedException) {
            throw BackupFailedException("Could not list contents of $sourcePath", e)
        } catch (e: Throwable) {
            LogsHandler.unexpectedException(e, sourcePath)
            throw BackupFailedException("Could not list contents of $sourcePath", e)
        }
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    protected fun genericBackupDataTarApi(
        dataType: String,
        backupInstanceDir: StorageFile,
        filesToBackup: List<ShellHandler.FileInfo>,
        compress: Boolean,
        iv: ByteArray?,
    ): Boolean {
        Timber.i(
            "Backing up %s got %d files to backup",
            dataType,
            filesToBackup.size
        )
        if (filesToBackup.isEmpty()) {
            Timber.i("Nothing to backup for $dataType. Skipping")
            return false
        }
        try {
            createBackupArchiveTarApi(backupInstanceDir, dataType, filesToBackup, compress, iv)
        } catch (e: IOException) {
            val message = "${e::class.canonicalName} occurred on $dataType backup: $e"
            Timber.e(message)
            throw BackupFailedException(message, e)
        } catch (e: Throwable) {
            val message = "${e::class.canonicalName} occurred on $dataType backup: $e"
            LogsHandler.unexpectedException(e, message)
            throw BackupFailedException(message, e)
        }
        return true
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    fun genericBackupDataTarApi(
        dataType: String,
        backupInstanceDir: StorageFile,
        sourcePath: String,
        compress: Boolean,
        iv: ByteArray?,
    ): Boolean {
        val filesToBackup = assembleFileList(sourcePath)
        return genericBackupData(dataType, backupInstanceDir, filesToBackup, compress, iv)
    }

    @SuppressLint("RestrictedApi")
    @Throws(BackupFailedException::class, CryptoSetupException::class)
    fun genericBackupDataTarCmd(
        dataType: String,
        backupInstanceDir: StorageFile,
        sourcePath: String,
        compress: Boolean,
        iv: ByteArray?,
    ): Boolean {
        if (!ShellUtils.fastCmdResult("test -d ${quote(sourcePath)}"))
            return false

        Timber.i("Creating $dataType backup via tar")

        val outStream = createArchiveFile(dataType, backupInstanceDir, compress, iv)

        try {
            val tarScript = InternalShellScriptPlugin.findScript("tar").toString()

            var options = ""
            options += " --exclude ${quote(NeoApp.assets.BACKUP_EXCLUDE_FILE)}"
            if (!pref_backupCache.value) {
                options += " --exclude ${quote(NeoApp.assets.EXCLUDE_CACHE_FILE)}"
            }

            val cmd = "sh ${quote(tarScript)} create $utilBoxQ $options ${quote(sourcePath)}"

            Timber.i("SHELL: $cmd")

            val (code, err) = runAsRootPipeOutCollectErr(outStream, cmd)

            //---------- ignore error code, because sockets may trigger it
            // if (err != "") {
            //     Timber.i(err)
            //     if (code != 0)
            //         throw ScriptException(err)
            // }
            //---------- instead look at error output and ignore some of the messages
            if (code != 0)
                Timber.i("tar returns: code $code: $err") // at least log the full error

            val errLines = err
                .split("\n")
                .filterNot { line ->
                    line.isBlank()
                            || line.contains("tar: unknown file type") // e.g. socket 140000
                            || line.contains("tar: had errors") // summary at the end
                }

            // Ignoring the error code looks problematic, but it was checked.
            // 1. The unknown file type is a known thing and is about sockets or in general files that
            //    cannot be added to an archive or unsupported by tar.
            //    You know, tar is able to pack the most of all archivers.
            // 2. The "had errors" was added in this commit of toybox tar:
            //    https://github.com/landley/toybox/commit/3b71ff9d7e4cab52b9d421bc8daf2bdd7810731d
            //    it is additional to the real error messages.
            //    If any error was found, this message is added as a summary at the end.

            // so if there are remaining lines *and* the code is non-zero, we throw an exception
            if (errLines.isNotEmpty()) {
                val errFiltered = errLines.joinToString("\n")
                Timber.i(errFiltered)
                if (code != 0)
                    throw ScriptException(errFiltered)
            }
        } catch (e: IOException) {
            val message = "${e::class.canonicalName} occurred on $dataType backup: $e"
            Timber.e(message)
            throw BackupFailedException(message, e)
        } catch (e: Throwable) {
            val message = "${e::class.canonicalName} occurred on $dataType backup: $e"
            LogsHandler.unexpectedException(e, message)
            throw BackupFailedException(message, e)
        } finally {
            Timber.d("Done compressing. Closing archive stream.")
            outStream.close()
        }
        return true
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    protected fun genericBackupData(
        dataType: String,
        backupInstanceDir: StorageFile,
        filesToBackup: List<ShellHandler.FileInfo>,
        compress: Boolean,
        iv: ByteArray?,
    ): Boolean {
        return genericBackupDataTarApi(dataType, backupInstanceDir, filesToBackup, compress, iv)
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    protected fun genericBackupData(
        dataType: String,
        backupInstanceDir: StorageFile,
        sourcePath: String,
        compress: Boolean,
        iv: ByteArray?,
    ): Boolean {
        Timber.i("${NeoApp.NB.packageName} <- $sourcePath")
        traceAccess { runAsRoot("echo '$sourcePath: '  '$sourcePath'/*").out.joinToString("\n") }
        if (pref_backupTarCmd.value) {
            return genericBackupDataTarCmd(
                dataType,
                backupInstanceDir,
                sourcePath,
                compress,
                iv
            )
        } else {
            return genericBackupDataTarApi(
                dataType,
                backupInstanceDir,
                sourcePath,
                compress,
                iv
            )
        }
    }

    @Throws(BackupFailedException::class)
    protected open fun backupPackage(app: Package, backupInstanceDir: StorageFile) {
        Timber.i("<${app.packageName}> Backup package apks")
        var apksToBackup = arrayOf(app.apkPath)
        if (app.apkSplits.isEmpty()) {
            Timber.d("<${app.packageName}> The app is a normal apk")
        } else {
            apksToBackup += app.apkSplits.drop(0)
            Timber.d("<${app.packageName}> Package is split into ${apksToBackup.size} apks")
        }
        Timber.d(
            "[%s] Backing up package (%d apks: %s)",
            app.packageName,
            apksToBackup.size,
            apksToBackup.joinToString(" ") { s: String -> RootFile(s).name }
        )
        for (apk in apksToBackup) {
            try {
                Timber.i("${app.packageName}: $apk")
                copyRootFileToDocument(apk, backupInstanceDir, RootFile(apk).name)
            } catch (e: IOException) {
                Timber.e("$app: Could not backup apk $apk: $e")
                throw BackupFailedException("Could not backup apk $apk", e)
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e, app)
                throw BackupFailedException("Could not backup apk $apk", e)
            }
        }
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    protected open fun backupData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?,
    ): Boolean {
        val dataType = BACKUP_DIR_DATA
        Timber.i(LOG_START_BACKUP, app.packageName, dataType)
        return genericBackupData(
            dataType,
            backupInstanceDir,
            app.dataPath,
            isCompressionEnabled(),
            iv
        )
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    protected open fun backupExternalData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?,
    ): Boolean {
        val dataType = BACKUP_DIR_EXTERNAL_FILES
        Timber.i(LOG_START_BACKUP, app.packageName, dataType)
        return try {
            genericBackupData(
                dataType,
                backupInstanceDir,
                app.getExternalDataPath(),
                isCompressionEnabled(),
                iv
            )
        } catch (ex: BackupFailedException) {
            when (ex.cause) {
                is ShellCommandFailedException -> {
                    if (isFileNotFoundException(ex.cause as ShellCommandFailedException)) {
                        // no such data found
                        Timber.i(LOG_NO_THING_TO_BACKUP, dataType, app.packageName)
                        return false
                    }
                }
            }
            throw ex
        }
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    protected open fun backupObbData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?,
    ): Boolean {
        val dataType = BACKUP_DIR_OBB_FILES
        Timber.i(LOG_START_BACKUP, app.packageName, dataType)
        return try {
            genericBackupData(
                dataType,
                backupInstanceDir,
                app.getObbFilesPath(),
                isCompressionEnabled(),
                iv
            )
        } catch (ex: BackupFailedException) {
            when (ex.cause) {
                is ShellCommandFailedException -> {
                    if (isFileNotFoundException(ex.cause as ShellCommandFailedException)) {
                        // no such data found
                        Timber.i(LOG_NO_THING_TO_BACKUP, dataType, app.packageName)
                        return false
                    }
                }
            }
            throw ex
        }
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    protected open fun backupMediaData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?,
    ): Boolean {
        val dataType = BACKUP_DIR_MEDIA_FILES
        Timber.i(LOG_START_BACKUP, app.packageName, dataType)
        return try {
            genericBackupData(
                dataType,
                backupInstanceDir,
                app.getMediaFilesPath(),
                isCompressionEnabled(),
                iv
            )
        } catch (ex: BackupFailedException) {
            when (ex.cause) {
                is ShellCommandFailedException -> {
                    if (isFileNotFoundException(ex.cause as ShellCommandFailedException)) {
                        // no such data found
                        Timber.i(LOG_NO_THING_TO_BACKUP, dataType, app.packageName)
                        return false
                    }
                }
            }
            throw ex
        }
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    protected open fun backupDeviceProtectedData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?,
    ): Boolean {
        val dataType = BACKUP_DIR_DEVICE_PROTECTED_FILES
        Timber.i(LOG_START_BACKUP, app.packageName, dataType)
        return try {
            genericBackupData(
                dataType,
                backupInstanceDir,
                app.devicesProtectedDataPath,
                isCompressionEnabled(),
                iv
            )
        } catch (ex: BackupFailedException) {
            when (ex.cause) {
                is ShellCommandFailedException -> {
                    if (isFileNotFoundException(ex.cause as ShellCommandFailedException)) {
                        // no such data found
                        Timber.i(LOG_NO_THING_TO_BACKUP, dataType, app.packageName)
                        return false
                    }
                }
            }
            throw ex
        }
    }

    class BackupFailedException(message: String?, cause: Throwable?) :
        AppActionFailedException(message, cause)

    companion object {
        const val LOG_START_BACKUP = "[%s] Starting %s backup"
        const val LOG_NO_THING_TO_BACKUP = "[%s] No %s to backup available"
        const val STORAGE_LOCATION_INACCESSIBLE =
            "Cannot backup data. Storage location not set or inaccessible"
        const val STORAGE_LOCATION_NOTWRITABLE =
            "Cannot backup data. Storage location not writable"
    }
}
