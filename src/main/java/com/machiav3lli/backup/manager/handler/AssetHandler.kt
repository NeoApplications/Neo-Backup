package com.machiav3lli.backup.manager.handler

import android.content.Context
import android.content.res.AssetManager
import com.machiav3lli.backup.ui.pages.pref_backupCache
import com.machiav3lli.backup.ui.pages.pref_backupNoBackupData
import com.machiav3lli.backup.ui.pages.pref_restoreCache
import com.machiav3lli.backup.ui.pages.pref_restoreNoBackupData
import com.machiav3lli.backup.utils.SystemUtils.updateId
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class AssetHandler(context: Context) {
    companion object {
        const val UPDATE_ID_FILE = "__update_id__"
    }

    var directory: File
        private set

    init {
        // copy asset files to file storage
        // but not if the version file exists and is already from the current app version
        // the version file is written at the end of the copy to validate the transaction
        // this may upgrade or downgrade files, but always all at once

        directory = File(context.filesDir, "assets")
        directory.mkdirs()
        val updateId = context.updateId   //versionName
        val lastId = try {
            File(directory, UPDATE_ID_FILE).readText()
        } catch (_: Throwable) {
            ""
        }
        if (lastId != updateId) {
            try {
                // cleans each directory and then copies the files
                context.assets.copyRecursively("files", directory)
                // add some generated files because the app version changed
                updateExcludeFiles()
                // finish transaction by writing the version file
                File(directory, UPDATE_ID_FILE).writeText(updateId)
            } catch (_: Throwable) {
                Timber.w("cannot copy asset files to ${directory}")
            }
        }
    }

    val EXCLUDE_CACHE_FILE get() = File(directory, "tar_EXCLUDE_CACHE").toString()
    val BACKUP_EXCLUDE_FILE get() = File(directory, "tar_BACKUP_EXCLUDE").toString()
    val RESTORE_EXCLUDE_FILE get() = File(directory, "tar_RESTORE_EXCLUDE").toString()

    val DATA_EXCLUDED_CACHE_DIRS
        get() = listOf(
            "cache",
            "code_cache"
        )

    // libs are generally created while installing the app. Backing them up
    // would result in a compatibility problem between devices with different cpu_arch
    val LIB_DIRS
        //TODO hg42 what about architecture dependent names? or may be application specific? lib* ???
        get() = listOf("lib")

    // these need to be dynamic, becasue the preferences can change at runtime
    val DATA_BACKUP_EXCLUDED_BASENAMES
        get() = (
                LIB_DIRS
                        //TODO hg42 use Context.getNoBackupFilesDir() ??? tricky, because it's an absolute path (remove common part...)
                        + if (pref_backupNoBackupData.value) listOf() else listOf("no_backup")
                        + if (pref_backupCache.value) listOf() else DATA_EXCLUDED_CACHE_DIRS
                )

    val DATA_RESTORE_EXCLUDED_BASENAMES
        get() = (
                LIB_DIRS
                        //TODO hg42 use Context.getNoBackupFilesDir() ??? tricky, because it's an absolute path (remove common part...)
                        + if (pref_restoreNoBackupData.value) listOf() else listOf("no_backup")
                        + if (pref_restoreCache.value) listOf() else DATA_EXCLUDED_CACHE_DIRS
                )

    val DATA_EXCLUDED_NAMES
        get() = listOfNotNull(
            "com.google.android.gms.appid.xml", // appid needs to be recreated
            "com.machiav3lli.backup.xml", // NB's encrypted prefs file
            "trash",
            ".thumbnails",
            if (ShellHandler.utilBox.hasBug("DotDotDirHang")) "..*" else null
        )

    fun updateExcludeFiles() {

        File(BACKUP_EXCLUDE_FILE)
            .writeText(
                (DATA_BACKUP_EXCLUDED_BASENAMES.map { "./$it" }
                        + DATA_EXCLUDED_NAMES)
                    .joinToString("") { it + "\n" }
            )
        File(RESTORE_EXCLUDE_FILE)
            .writeText(
                (DATA_RESTORE_EXCLUDED_BASENAMES.map { "./$it" }
                        + DATA_EXCLUDED_NAMES)
                    .joinToString("") { it + "\n" }
            )
        File(EXCLUDE_CACHE_FILE)
            .writeText(
                DATA_EXCLUDED_CACHE_DIRS.map { "./$it" }
                    .joinToString("") { it + "\n" }
            )
    }
}

fun AssetManager.copyRecursively(assetPath: String, targetFile: File) {
    list(assetPath)?.let { list ->
        if (list.isEmpty()) { // assetPath is file
            open(assetPath).use { input ->
                FileOutputStream(targetFile.absolutePath).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

        } else { // assetPath is folder
            targetFile.deleteRecursively()
            targetFile.mkdir()

            list.forEach {
                copyRecursively("$assetPath/$it", File(targetFile, it))
            }
        }
    }
}
