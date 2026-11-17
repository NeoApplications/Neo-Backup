/*
 * OAndBackupX: open-source apps backup and restore app.
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
package com.machiav3lli.backup.manager.handler

import android.content.Context
import android.content.pm.PackageManager
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.MODE_DATA
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.ActionResult
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.data.entity.StorageFile.Companion.invalidateCache
import com.machiav3lli.backup.manager.actions.BackupAppAction
import com.machiav3lli.backup.manager.actions.BackupSpecialAction
import com.machiav3lli.backup.manager.actions.RestoreAppAction
import com.machiav3lli.backup.manager.actions.RestoreSpecialAction
import com.machiav3lli.backup.manager.actions.RestoreSystemAppAction
import com.machiav3lli.backup.manager.handler.ShellHandler.ShellCommandFailedException
import com.machiav3lli.backup.manager.tasks.AppActionWork
import com.machiav3lli.backup.ui.pages.pref_numBackupRevisions
import com.machiav3lli.backup.ui.pages.pref_paranoidHousekeeping
import com.machiav3lli.backup.utils.FileUtils.BackupLocationInAccessibleException
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.TraceUtils.canonicalName
import com.machiav3lli.backup.utils.copyRootFileToDocument
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException

object BackupRestoreHelper {

    suspend fun backup(
        context: Context,
        work: AppActionWork?,
        shell: ShellHandler,
        packageItem: Package,
        backupMode: Int,
    ): ActionResult {
        var reBackupMode = backupMode

        // Select and prepare the action to use
        val action: BackupAppAction = when {
            packageItem.isSpecial -> {
                if (reBackupMode and MODE_APK == MODE_APK) {
                    Timber.e("<${packageItem.packageName}> Special Backup called with MODE_APK or MODE_BOTH. Masking invalid settings.")
                    reBackupMode = reBackupMode and MODE_DATA
                    Timber.d("<${packageItem.packageName}> New backup mode: $reBackupMode")
                }
                BackupSpecialAction(context, work, shell)
            }

            else                  -> {
                BackupAppAction(context, work, shell)
            }
        }
        Timber.d("<${packageItem.packageName}> Using ${action::class.simpleName} class")

        // create the new backup
        val result = action.run(packageItem, reBackupMode)

        if (result.succeeded)
            Timber.i("<${packageItem.packageName}> Backup succeeded: ${result.succeeded}")
        else
            Timber.i("<${packageItem.packageName}> Backup FAILED: ${result.succeeded} ${result.message}")

        housekeepingPackageBackups(packageItem)

        return result
    }

    suspend fun restore(
        context: Context, work: AppActionWork?, shellHandler: ShellHandler, appInfo: Package,
        mode: Int, backup: Backup
    ): ActionResult {
        val action: RestoreAppAction = when {
            appInfo.isSpecial -> RestoreSpecialAction(context, work, shellHandler)
            appInfo.isSystem  -> RestoreSystemAppAction(context, work, shellHandler)
            else              -> RestoreAppAction(context, work, shellHandler)
        }
        val result = action.run(appInfo, backup, mode)
        Timber.i("<${appInfo.packageName}> Restore succeeded: ${result.succeeded}")
        return result
    }

    @Throws(IOException::class)
    fun copySelfApk(context: Context, shell: ShellHandler): Boolean {
        val filename = SystemUtils.packageName + '-' + SystemUtils.versionName + ".apk"
        try {
            val backupRoot = NeoApp.backupRoot ?: return false
            val apkFile = backupRoot.findFile(filename)
            apkFile?.delete()
            try {
                val apkDir = (
                        context.packageManager.getPackageInfo(
                            SystemUtils.packageName,
                            0
                        ).applicationInfo
                            ?: context.applicationInfo).sourceDir
                val fileInfos =
                    shell.suGetDetailedDirectoryContents(apkDir, false)
                if (apkDir == null || fileInfos.size != 1) {
                    throw FileNotFoundException("Could not find Neo Backup's own apk file")
                }
                copyRootFileToDocument(
                    fileInfos[0].absolutePath,
                    backupRoot,
                    RootFile(fileInfos[0].absolutePath).name
                )
                // Invalidating cache, otherwise the next call will fail
                // Can cost a lot time, but this function won't be run that often
                invalidateCache()
                val baseApkFile = backupRoot.findFile(fileInfos[0].filename)
                if (baseApkFile != null) {
                    baseApkFile.renameTo(filename)
                } else {
                    Timber.e("Cannot find just created file '${fileInfos[0].filename}' in backup dir for renaming. Skipping")
                    return false
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.wtf("${e::class.canonicalName}! This should never happen! Message: $e")
                return false
            } catch (e: ShellCommandFailedException) {
                throw IOException(e.shellResult.err.joinToString(" "), e)
            }
        } catch (e: StorageLocationNotConfiguredException) {
            Timber.e("${e::class.simpleName}: $e")
            return false
        } catch (e: BackupLocationInAccessibleException) {
            Timber.e("${e::class.simpleName}: $e")
            return false
        }
        return true
    }

    suspend fun housekeepingPackageBackups(app: Package) {

        if (pref_paranoidHousekeeping.value)
            app.refreshBackupList()

        val numBackupRevisions =
            pref_numBackupRevisions.value
        if (numBackupRevisions == 0) {
            Timber.i("<${app.packageName}> Infinite backup revisions configured. Not deleting any backup.")
            return
        }

        app.deleteOldestBackups(numBackupRevisions)
    }

    enum class ActionType {
        BACKUP, RESTORE
    }
}