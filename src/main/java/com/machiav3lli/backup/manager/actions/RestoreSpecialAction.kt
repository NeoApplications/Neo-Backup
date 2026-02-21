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
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.dbs.entity.SpecialInfo
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.entity.WifiNetwork
import com.machiav3lli.backup.data.entity.WifiNetworkParser
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBoxQ
import com.machiav3lli.backup.manager.handler.ShellHandler.ShellCommandFailedException
import com.machiav3lli.backup.manager.tasks.AppActionWork
import com.machiav3lli.backup.utils.CryptoSetupException
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class RestoreSpecialAction(context: Context, work: AppActionWork?, shell: ShellHandler) :
    RestoreAppAction(context, work, shell) {

    @Throws(CryptoSetupException::class, RestoreFailedException::class)
    override fun restoreAll(
        work: AppActionWork?,
        app: Package,
        backup: Backup,
        backupDir: StorageFile,
        backupMode: Int
    ) {
        restoreData(app, backup, backupDir)
    }

    @Throws(RestoreFailedException::class, CryptoSetupException::class)
    override fun restoreData(
        app: Package,
        backup: Backup,
        backupDir: StorageFile
    ) {
        Timber.i("%s: Restore special data", app)
        work?.setOperation("s")
        val metaInfo = app.packageInfo as SpecialInfo
        val tempPath = RootFile(context.cacheDir, backup.packageName)
        try {

            val dataType = BACKUP_DIR_DATA
            val backupArchive = findBackupArchive(dataType, backup, backupDir)
            tempPath.mkdir()
            val extractTo = tempPath.absolutePath

            genericRestoreFromArchive(
                dataType,
                backupArchive.file,
                extractTo,
                backupArchive.isCompressed,
                backupArchive.compressionType,
                backupArchive.isEncrypted,
                backup.iv,
                RootFile(context.cacheDir),
                isOldVersion(backup)
            )

            // check if all expected files are there
            val filesInBackup = tempPath.listFiles()
            val expectedFiles = metaInfo.specialFiles
                .map { pathname: String? -> RootFile(pathname ?: "") }
                .toTypedArray()
            if (filesInBackup != null
                && (filesInBackup.size != expectedFiles.size
                        || !areBasefilesSubsetOf(expectedFiles, filesInBackup))
            ) {
                val errorMessage =
                    "$app: Backup is missing files. Found ${filesInBackup.map { it.absolutePath }}; needed: ${expectedFiles.map { it.absolutePath }}"
                Timber.e(errorMessage)
                throw RestoreFailedException(errorMessage, null)
            }

            if (app.packageName == "special.wifi.access.points") {
                tempPath.listFiles()?.firstOrNull { it.name.endsWith("WifiConfigStore.xml") }
                    ?.let { configFile -> restoreWifiAccessPoints(app, configFile) }
                    ?: run {
                        Timber.w("$app: No WifiConfigStore.xml found in special files – skipping live injection")
                        return
                    }
            }
            val commands = buildList {
                for (restoreFile in expectedFiles) {
                    val (uid, gid, con) = try {
                        shell.suGetOwnerGroupContext(restoreFile.absolutePath)
                    } catch (e: Throwable) {
                        // fallback to permissions of parent directory
                        shell.suGetOwnerGroupContext(
                            restoreFile.parentFile?.absolutePath
                                ?: restoreFile.toPath().parent.toString()
                        )
                    }
                    add(
                        "$utilBoxQ mv -f ${
                            quote(
                                File(
                                    tempPath,
                                    restoreFile.name
                                )
                            )
                        } ${quote(restoreFile)}"
                    )
                    add(
                        "$utilBoxQ chmod 600 ${quote(restoreFile)}"
                    )
                    add(
                        "$utilBoxQ chown $uid:$gid ${quote(restoreFile)}"
                    )
                    if (con != "?") add("chcon -R -h -v '$con' ${quote(restoreFile)}")
                    // else null // "" ; restorecon -RF -v ${quote(restoreFile)}"  //TODO hg42 doesn't seem to work, probably because selinux unsupported in this case
                }
            }

            val command = commands.joinToString(" ; ")
            runAsRoot(command)

            if (app.packageName == "special.smsmms.json") {
                for (filePath in metaInfo.specialFiles) {
                    RestoreSMSMMSJSONAction.restoreData(context, filePath)
                }
            }
            if (app.packageName == "special.calllogs.json") {
                for (filePath in metaInfo.specialFiles) {
                    RestoreCallLogsJSONAction.restoreData(context, filePath)
                }
            }

        } catch (e: ShellCommandFailedException) {
            val error = extractErrorMessage(e.shellResult)
            Timber.e("$app: Restore $BACKUP_DIR_DATA failed. System might be inconsistent: $error")
            throw RestoreFailedException(error, e)
        } catch (e: FileNotFoundException) {
            throw RestoreFailedException("Could not find backup archive", e)
        } catch (e: IOException) {
            Timber.e("$app: Restore $BACKUP_DIR_DATA failed with IOException. System might be inconsistent: $e")
            throw RestoreFailedException("IOException", e)
        } catch (e: RuntimeException) {
            throw RestoreFailedException("${e.message}", e)
        } finally {
            val backupDeleted =
                FileUtils.deleteQuietly(tempPath)   // if deleteQuietly is missing, org.apache.commons.io is wrong (shitty version from 2003 that looks newer)
            Timber.d("$app: Uncompressed $BACKUP_DIR_DATA was deleted: $backupDeleted")
        }
        if (app.packageName == "special.smsmms.json" || app.packageName == "special.calllogs.json") {
            for (filePath in metaInfo.specialFiles) {
                File(filePath).delete()
            }
        }
    }

    /**
     * ### Why both file-restore AND cmd wifi?
     * The file copy should restore the networks persistently, but isn't working reliably
     * on latest Android versions, being overwritten by the data held in the Wi-Fi stack.
     * Calling `cmd wifi add-network` tells the *live* stack about the networks so both the in-
     * memory and file states are consistent.
     *
     * ### `cmd wifi` availability
     * The command has been available since Android 9 (API 28).
     */
    private fun restoreWifiAccessPoints(app: Package, xmlFile: RootFile) {
        val xmlBytes = runCatching {
            runAsRoot("cat ${quote(xmlFile)}")
                .out
                .joinToString("\n")
                .toByteArray(Charsets.UTF_8)
        }.onFailure { e ->
            Timber.e("$app: Could not read $xmlFile via root shell: $e")
        }.getOrElse { return }

        if (xmlBytes.isEmpty()) {
            Timber.w("$app: $xmlFile was empty or unreadable – skipping live injection")
            return
        }

        val networks = runCatching { WifiNetworkParser.parse(xmlBytes.inputStream()) }
            .onFailure { e -> Timber.e("$app: Failed to parse WifiConfigStore.xml: ${e.cause}") }
            .getOrElse { return }

        Timber.i("$app: Parsed ${networks.size} Wi-Fi network(s) from backup")

        val addCommands = WifiNetwork.buildRestoreCommands(networks)
        if (addCommands.isEmpty()) {
            Timber.i("$app: No networks eligible for live injection")
            return
        }

        addCommands.forEachIndexed { index, cmd ->
            runCatching { runAsRoot(cmd) }
                .onSuccess { Timber.d("$app: Wi-Fi inject [${index + 1}/${addCommands.size}] OK") }
                .onFailure { e ->
                    // errors are logged but not communicated to user for now TODO notify about failed network restores
                    Timber.w("$app: Wi-Fi inject [${index + 1}/${addCommands.size}] failed (non-fatal): $e")
                }
        }

        Timber.i("$app: Wi-Fi live injection complete (${addCommands.size} network(s) submitted)")
    }

    override fun restorePackage(backupDir: StorageFile, backup: Backup) {
        // stub
    }

    override fun restoreDeviceProtectedData(
        app: Package,
        backup: Backup,
        backupDir: StorageFile
    ) {
        // stub
    }

    override fun restoreExternalData(
        app: Package,
        backup: Backup,
        backupDir: StorageFile
    ) {
        // stub
    }

    override fun restoreObbData(
        app: Package,
        backup: Backup,
        backupDir: StorageFile
    ) {
        // stub
    }

    override fun refreshAppInfo(context: Context, app: Package) {
        // stub
    }

    companion object {
        private fun areBasefilesSubsetOf(
            set: Array<RootFile>,
            subsetList: Array<RootFile>
        ): Boolean {
            val baseCollection: Collection<String> = set.map { obj: File -> obj.name }.toHashSet()
            val subsetCollection: Collection<String> =
                subsetList.map { obj: File -> obj.name }.toHashSet()
            return baseCollection.containsAll(subsetCollection)
        }
    }
}
