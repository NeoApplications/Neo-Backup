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
package com.machiav3lli.backup.utils

import android.content.Context
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.MODE_DATA
import com.machiav3lli.backup.MODE_DATA_DE
import com.machiav3lli.backup.MODE_DATA_EXT
import com.machiav3lli.backup.MODE_DATA_MEDIA
import com.machiav3lli.backup.MODE_DATA_OBB
import com.machiav3lli.backup.data.dbs.entity.SpecialInfo
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.manager.actions.BackupAppAction
import com.machiav3lli.backup.manager.handler.BackupsHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber
import java.io.File
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

object FileUtils {

    // TODO Change to StorageFile-based
    fun getExternalStorageDirectory(context: Context): File? {
        return context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.parentFile
    }

    fun getName(fullPath: String): String {
        var path = fullPath
        if (path.endsWith(File.separator)) path = path.substring(0, path.length - 1)
        return path.substring(path.lastIndexOf(File.separator) + 1)
    }

    fun translatePosixPermissionToMode(permissions: String): Int {
        var str = permissions.takeLast(9)
        str = str.replace('s', 'x', false)
        str = str.replace('S', '-', false)
        val set = PosixFilePermissions.fromString(str)
        return translatePosixPermissionToMode(set)
    }

    fun translatePosixPermissionToMode(permissions: Set<PosixFilePermission?>): Int {
        var mode = 0
        PosixFilePermission.values().forEach {
            mode = mode shl 1
            mode += if (permissions.contains(it)) 1 else 0
        }
        return mode
    }

    // TODO revamp & cleanup reads of backups
    /**
     * Invalidates the cached value for the backup location URI so that the next call to
     * `getBackupDir` will set it again.
     */
    suspend fun invalidateBackupLocation() {
        SpecialInfo.clearCache()
        get<BackupsHandler>(BackupsHandler::class.java).refreshAll()
    }

    class BackupLocationInAccessibleException : Exception {
        constructor() : super()
        constructor(message: String?) : super(message)
        constructor(message: String?, cause: Throwable?) : super(message, cause)
    }

    @Throws(BackupAppAction.BackupFailedException::class)
    fun checkAvailableStorage(context: Context, app: Package, backupMode: Int) {
        val storageStatsCheck =
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? android.app.usage.StorageStatsManager
                ?: throw BackupAppAction.BackupFailedException(
                    "Cannot access storage stats service",
                    null
                )

        val storageManagerCheck =
            context.getSystemService(Context.STORAGE_SERVICE) as? android.os.storage.StorageManager
                ?: throw BackupAppAction.BackupFailedException(
                    "Cannot access storage manager",
                    null
                )

        try {
            val backupDir = app.getAppBackupBaseDir(create = false)
                ?: throw BackupAppAction.BackupFailedException(
                    "Cannot determine backup location",
                    null
                )

            var estimatedSize = 0L

            if ((backupMode and MODE_APK) != 0) {
                estimatedSize += app.apkPath.let { RootFile(it).length() }
                estimatedSize += app.apkSplits.sumOf { RootFile(it).length() }
            }
            if ((backupMode and MODE_DATA) != 0)
                estimatedSize += estimateDirectorySize(app.dataPath)
            if ((backupMode and MODE_DATA_DE) != 0)
                estimatedSize += estimateDirectorySize(app.devicesProtectedDataPath)
            if ((backupMode and MODE_DATA_EXT) != 0)
                estimatedSize += estimateDirectorySize(app.getExternalDataPath())
            if ((backupMode and MODE_DATA_OBB) != 0)
                estimatedSize += estimateDirectorySize(app.getObbFilesPath())
            if ((backupMode and MODE_DATA_MEDIA) != 0)
                estimatedSize += estimateDirectorySize(app.getMediaFilesPath())

            estimatedSize = (estimatedSize * 1.2).toLong() // 20% safety buffer

            val availableBytes = backupDir.freeSpace

            if (availableBytes < estimatedSize) {
                val requiredMB = estimatedSize / (1024 * 1024)
                val availableMB = availableBytes / (1024 * 1024)
                throw BackupAppAction.BackupFailedException(
                    "Insufficient storage space. Required: ${requiredMB}MB, Available: ${availableMB}MB",
                    null
                )
            }

            Timber.i("Storage check passed. Estimated: ${estimatedSize / (1024 * 1024)}MB, Available: ${availableBytes / (1024 * 1024)}MB")
        } catch (e: BackupAppAction.BackupFailedException) {
            throw e
        } catch (e: Throwable) {
            // don't fail on other Exceptions
            Timber.w("Storage check failed with error: ${e.message}")
        }
    }

    private fun estimateDirectorySize(path: String): Long {
        return try {
            val result = runAsRoot("du -sb ${quote(path)} 2>/dev/null | cut -f1")
            if (result.isSuccess) {
                result.out.firstOrNull()?.toLongOrNull() ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            Timber.w("Could not estimate size of $path: ${e.message}")
            0L
        }
    }
}
