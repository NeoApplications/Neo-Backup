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

import android.content.Context
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.MODE_DATA
import com.machiav3lli.backup.data.dbs.entity.SpecialInfo
import com.machiav3lli.backup.data.entity.ActionResult
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.ShellCommandFailedException
import com.machiav3lli.backup.manager.tasks.AppActionWork
import com.machiav3lli.backup.utils.CryptoSetupException
import timber.log.Timber
import java.io.File

class BackupSpecialAction(context: Context, work: AppActionWork?, shell: ShellHandler) :
    BackupAppAction(context, work, shell) {
    override fun run(app: Package, backupMode: Int): ActionResult {
        if (backupMode and MODE_APK == MODE_APK) {
            Timber.e("Special contents don't have APKs to backup. Ignoring")
        }
        return if (backupMode and MODE_DATA == MODE_DATA)
            super.run(app, MODE_DATA)
        else ActionResult(
            app, null,
            "Special backup only backups data, but data was not selected for backup",
            false
        )
    }

    @Throws(BackupFailedException::class, CryptoSetupException::class)
    override fun backupData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?
    ): Boolean {
        Timber.i("$app: Backup special data")
        require(app.packageInfo is SpecialInfo) { "Provided app is not an instance of SpecialAppMetaInfo" }
        val appInfo = app.packageInfo as SpecialInfo
        // TODO This can be optimized, because it's known, that special backups won't meet any symlinks
        //  since the list of files is fixed
        //  It would make sense to implement something like TarUtils.addFilepath with SuFileInputStream and
        val filesToBackup = mutableListOf<ShellHandler.FileInfo>()
        try {
            work?.setOperation("s")
            for (filePath in appInfo.specialFiles) {
                if (app.packageName == "special.smsmms.json") {
                    BackupSMSMMSJSONAction.backupData(context, filePath)
                }
                if (app.packageName == "special.calllogs.json") {
                    BackupCallLogsJSONAction.backupData(context, filePath)
                }
                val file = File(filePath)
                val isDirSource = filePath.endsWith("/")
                val fileInfos = mutableListOf<ShellHandler.FileInfo>()
                if (isDirSource) {
                    // directory
                    try {
                        // add contents
                        fileInfos.addAll(
                            shell.suGetDetailedDirectoryContents(
                                filePath.removeSuffix("/"),
                                isDirSource,
                                file.name
                            )
                        )
                    } catch (e: ShellCommandFailedException) {
                        LogsHandler.unexpectedException(e)
                        continue  //TODO hg42: avoid checking the error message text for now
                        //          alternative implementation, better replaced this by API, when root permissions available, e.g. via Shizuku
                        //              if(e.shellResult.err.toString().contains("No such file or directory", ignoreCase = true))
                        //                  continue
                        //              throw(e)
                    }
                    // add directory itself
                    filesToBackup.add(
                        shell.suGetFileInfo(file.absolutePath)
                    )
                } else {
                    // regular file
                    filesToBackup.add(
                        shell.suGetFileInfo(file.absolutePath)
                    )
                }
                filesToBackup.addAll(fileInfos)
            }
            genericBackupData(BACKUP_DIR_DATA, backupInstanceDir, filesToBackup, true, iv)
        } catch (e: RuntimeException) {
            throw BackupFailedException("${e.message}", e)
        } catch (e: ShellCommandFailedException) {
            val error = extractErrorMessage(e.shellResult)
            Timber.e("$app: Backup Special Data failed: $error")
            throw BackupFailedException(error, e)
        } catch (e: Throwable) {
            LogsHandler.unexpectedException(e, app)
            throw BackupFailedException("unhandled exception", e)
        }
        if (app.packageName == "special.smsmms.json" || app.packageName == "special.calllogs.json") {
            for (filePath in appInfo.specialFiles) {
                File(filePath).delete()
            }
        }
        return true
    }

    // Stubbing some functions, to avoid executing them with potentially dangerous results
    override fun backupPackage(app: Package, backupInstanceDir: StorageFile) {
        // stub
    }

    override fun backupDeviceProtectedData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?
    ): Boolean = // stub
        false

    override fun backupExternalData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?
    ): Boolean = // stub
        false

    override fun backupObbData(
        app: Package,
        backupInstanceDir: StorageFile,
        iv: ByteArray?
    ): Boolean = // stub
        false

    override fun pauseApp(type: String, wh: When, packageName: String) {
        // special packages are not apps
    }
}