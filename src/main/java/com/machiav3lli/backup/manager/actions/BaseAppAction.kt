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
import android.content.pm.PackageManager
import com.machiav3lli.backup.data.plugins.InternalRegexPlugin.Companion.findRegex
import com.machiav3lli.backup.data.plugins.InternalShellScriptPlugin
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.ShellCommands.Companion.currentProfile
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBoxQ
import com.machiav3lli.backup.manager.handler.ShellHandler.ShellCommandFailedException
import com.machiav3lli.backup.manager.tasks.AppActionWork
import com.machiav3lli.backup.ui.pages.pref_backupSuspendApps
import com.machiav3lli.backup.ui.pages.pref_restoreKillApps
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.TraceUtils.traceBold
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import timber.log.Timber

abstract class BaseAppAction protected constructor(
    protected val context: Context,
    protected val work: AppActionWork?,
    protected val shell: ShellHandler,
) {

    protected val deviceProtectedStorageContext: Context =
        context.createDeviceProtectedStorageContext()

    fun getBackupArchiveFilename(
        what: String,
        isCompressed: Boolean,
        compressionType: String?,
        isEncrypted: Boolean,
    ): String {
        val extension = buildString {
            if (isCompressed) {
                append(
                    when (compressionType) {
                        null, "no" -> ""
                        "gz"       -> ".gz"
                        "zst"      -> ".zst"
                        else       -> ".gz"
                    }
                )
            }
            if (isEncrypted) {
                append(".enc")
            }
        }
        return "$what.tar$extension"
    }

    abstract class AppActionFailedException : Exception {
        protected constructor(message: String?) : super(message)
        protected constructor(message: String?, cause: Throwable?) : super(message, cause)
    }

    private fun pauseOptions(wh: String) =
        when (wh) {
            "pre-backup"   -> listOfNotNull(
                if (pref_backupSuspendApps.value) "--suspend" else null,
            )

            "post-backup"  -> listOfNotNull(
                if (pref_backupSuspendApps.value) "--suspend" else null,
            )

            "pre-restore"  -> listOfNotNull(
                if (pref_restoreKillApps.value) "--kill" else null
            )

            "post-restore" -> listOfNotNull(
            )

            else           -> listOf()

        }.joinToString(" ")

    enum class When { PRE, POST }

    open fun pauseApp(type: String, wh: When, packageName: String) {
        if (packageName.matches(doNotStop)) return // will stop most activity, needs a good blacklist
        try {

            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val appuid = applicationInfo.uid
            if (appuid < android.os.Process.FIRST_APPLICATION_UID) { // exclude several system users, e.g. system, radio
                Timber.w("$type $packageName: ignore processes of system user UID < ${android.os.Process.FIRST_APPLICATION_UID}")
                return
            }
            val profileId = currentProfile
            val script = InternalShellScriptPlugin.findScript("pause").toString()

            val options = pauseOptions("$wh-$type")

            var stoppedPids: List<String>? = null     // pids
            if (wh == When.POST) {
                stoppedPids = preprocessResults["$type:$packageName:$profileId"]
                Timber.w("pids: ${stoppedPids?.joinToString(" ")}")
            }
            val params = stoppedPids?.joinToString("", " ") ?: ""

            val cmd =
                "sh $script $wh-$type $profileId $utilBoxQ $options $packageName $appuid$params"
            traceBold { "$type $packageName: $cmd" }

            val shellResult = runAsRoot(cmd)

            if (wh == When.PRE) {
                // save the stopped pids
                preprocessResults["$type:$packageName:$profileId"] =
                    shellResult.out.asSequence()
                        .filter { line: String -> line.isNotEmpty() }
                        .toMutableList()
                Timber.w(
                    "$type $packageName: pre-results: ${
                        preprocessResults["$type:$packageName:$profileId"]?.joinToString(
                            " "
                        )
                    }"
                )
            }
            if (wh == When.POST) {
                preprocessResults.remove("$type:$packageName:$profileId")
            }

        } catch (_: PackageManager.NameNotFoundException) {
            Timber.i("$type $packageName: cannot ${wh}-process: package does not exist")
        } catch (e: ShellCommandFailedException) {
            Timber.i("$type $packageName: cannot ${wh}-process: ${e.shellResult.err.joinToString(" ")}")
        } catch (e: Throwable) {
            LogsHandler.unexpectedException(e)
        }
    }

    class ScriptException(text: String) :
        AppActionFailedException(text)

    companion object {
        const val BACKUP_DIR_DATA = "data"
        const val BACKUP_DIR_DEVICE_PROTECTED_FILES = "device_protected_files"
        const val BACKUP_DIR_EXTERNAL_FILES = "external_files"
        const val BACKUP_DIR_OBB_FILES = "obb_files"
        const val BACKUP_DIR_MEDIA_FILES = "media_files"

        val replacements = mapOf(
            "<ownPackage>" to SystemUtils.packageName.replace(
                ".",
                """\."""
            )
        )

        val ignoredPackages = findRegex("ignored_packages", replacements)
        val doNotStop = findRegex("do_not_stop", replacements)

        init {
            Timber.i("ignoredPackages = $ignoredPackages")
            Timber.i("doNotStop = $doNotStop")
        }

        private val preprocessResults = mutableMapOf<String, List<String>>()

        fun extractErrorMessage(shellResult: Shell.Result): String {
            // if stderr does not say anything, try stdout
            val err = if (shellResult.err.isEmpty()) shellResult.out else shellResult.err
            return if (err.isEmpty()) {
                "Unknown Error"
            } else err[err.lastIndex]
        }

        fun isSuspended(packageName: String): Boolean {
            val profileId = currentProfile
            return ShellUtils.fastCmdResult("pm dump --user $profileId $packageName | grep suspended=true")
        }

        fun cleanupSuspended(packageName: String) {
            val profileId = currentProfile
            Timber.i("cleanup $packageName")
            try {
                runAsRoot("pm dump --user $profileId $packageName | grep suspended=true && pm unsuspend --user $profileId ${packageName}")
            } catch (e: Throwable) {
            }
        }
    }
}
