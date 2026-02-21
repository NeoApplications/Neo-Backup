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
import android.content.Intent
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.PROP_NAME
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.entity.uriFromFile
import com.machiav3lli.backup.manager.handler.LogsHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object BackupShareUtils {
    private const val SHARE_CACHE_DIR = "backup_share"
    private const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
    private const val MAX_CACHE_FILES = 10

    private fun getShareCacheDir(context: Context): File {
        val cacheDir = File(context.cacheDir, SHARE_CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    fun cleanupShareCache(context: Context) {
        try {
            val cacheDir = getShareCacheDir(context)
            if (!cacheDir.exists()) return

            val files = cacheDir.listFiles() ?: return
            val now = System.currentTimeMillis()

            // Remove old cached backups
            files.forEach { file ->
                if (now - file.lastModified() > MAX_CACHE_AGE_MS) {
                    Timber.d("Deleting old share cached backup: ${file.name}")
                    file.delete()
                }
            }

            // Limit number of cached backups
            val remainingFiles = cacheDir.listFiles() ?: return
            if (remainingFiles.size > MAX_CACHE_FILES) {
                remainingFiles
                    .sortedByDescending { it.lastModified() }
                    .drop(MAX_CACHE_FILES)
                    .forEach { file ->
                        Timber.d("Deleting excess number of cached backup: ${file.name}")
                        file.delete()
                    }
            }
        } catch (e: Exception) {
            Timber.e("Error cleaning up share cache: ${e.message}")
            LogsHandler.logException(e, backTrace = false)
        }
    }

    suspend fun createBackupArchive(backup: Backup, context: Context): File? =
        withContext(Dispatchers.IO) {
            try {
                cleanupShareCache(context)

                val cacheDir = getShareCacheDir(context)
                val dateStr = backup.backupDate.format(BACKUP_DATE_TIME_FORMATTER)
                val archiveFileName = "${backup.packageName}_${dateStr}.tar"
                val archiveFile = File(cacheDir, archiveFileName)
                if (archiveFile.exists()) archiveFile.delete()

                Timber.i("Creating backup archive: ${archiveFile.absolutePath}")
                FileOutputStream(archiveFile).use { fileOut ->
                    TarArchiveOutputStream(fileOut).use { tarOut ->
                        tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                        val backupDirName = backup.dir?.name ?: "backup"

                        backup.file?.let { propsFile ->
                            addFileToTar(tarOut, propsFile, propsFile.name ?: "backup.$PROP_NAME")
                        }
                        backup.dir?.let { backupDir ->
                            val files = backupDir.listFiles()
                            files.forEach { file ->
                                if (file.isFile) {
                                    val entryName = "$backupDirName/${file.name ?: "unknown"}"
                                    addFileToTar(tarOut, file, entryName)
                                }
                            }
                        }
                    }
                }

                Timber.i("Successfully created backup archive: ${archiveFile.absolutePath} (${archiveFile.length()} bytes)")
                archiveFile
            } catch (e: Exception) {
                Timber.e("Error creating backup archive: ${e.message}")
                LogsHandler.logException(e, backTrace = true)
                null
            }
        }

    private fun addFileToTar(
        tarOut: TarArchiveOutputStream,
        storageFile: StorageFile,
        entryName: String
    ) {
        try {
            val fileSize = storageFile.size
            val tarEntry = TarArchiveEntry(entryName)
            tarEntry.size = fileSize
            tarOut.putArchiveEntry(tarEntry)
            storageFile.inputStream()?.use { input ->
                input.copyTo(tarOut)
            }
            tarOut.closeArchiveEntry()
        } catch (e: Exception) {
            Timber.e("Error adding file to tar: $entryName - ${e.message}")
            throw e
        }
    }

    fun shareBackup(backup: Backup, context: Context) {
        MainScope().launch(Dispatchers.IO) {
            try {
                val archiveFile = createBackupArchive(backup, context)

                if (archiveFile == null) {
                    Timber.e("Failed to create backup archive")
                    return@launch
                }

                val uri = context.uriFromFile(archiveFile)
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "application/x-tar"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Backup: ${backup.packageLabel}")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Backup of ${backup.packageLabel} (${backup.packageName})\n" +
                                "Version: ${backup.versionName} (${backup.versionCode})\n" +
                                "Date: ${backup.backupDate.getFormattedDate(true)}\n" +
                                "Size: ${archiveFile.length()} bytes"
                    )
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                val chooserIntent =
                    Intent.createChooser(shareIntent, "Share Backup: ${backup.packageLabel}")

                withContext(Dispatchers.Main) {
                    NeoApp.activity?.startActivity(chooserIntent)
                }

                Timber.i("Backup share intent launched successfully")
            } catch (e: Exception) {
                Timber.e("Error sharing backup: ${e.message}")
                LogsHandler.logException(e, backTrace = true)
            }
        }
    }
}
