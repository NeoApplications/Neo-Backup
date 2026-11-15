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
package com.machiav3lli.backup.manager.handler

import android.content.Context
import android.os.Binder
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBoxQ
import com.machiav3lli.backup.manager.handler.ShellHandler.ShellCommandFailedException
import com.machiav3lli.backup.utils.FileUtils
import timber.log.Timber
import java.io.File

class ShellCommands {

    class ShellActionFailedException(val command: String, message: String, cause: Throwable?) :
        Exception(message, cause)

    companion object {

        // using reflection to get id of calling user since method getCallingUserId of UserHandle is hidden
        // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/os/UserHandle.java#L123
        val currentProfile: Int
            get() {
                //TODO hg42 another possibility RootFile.cmd("echo \$USER_ID").toInt()
                try {
                    // using reflection to get id of calling user since method getCallingUserId of UserHandle is hidden
                    // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/os/UserHandle.java#L123
                    val userHandle = Class.forName("android.os.UserHandle")
                    val muEnabled = userHandle.getField("MU_ENABLED").getBoolean(null)
                    val range = userHandle.getField("PER_USER_RANGE").getInt(null)
                    if (muEnabled) return Binder.getCallingUid() / range
                } catch (_: ClassNotFoundException) {
                } catch (_: NoSuchFieldException) {
                } catch (_: IllegalAccessException) {
                }
                return 0
            }

        @Throws(ShellActionFailedException::class)
        fun uninstall(
            users: List<String?>?,
            packageName: String?,
            sourceDir: String?,
            dataDir: String?,
            isSystem: Boolean
        ) {
            if (!isSystem) {
                // Uninstalling if user app
                if (!users.isNullOrEmpty()) {
                    val commands = mutableListOf<String>()
                    for (user in users) {
                        commands.add("pm uninstall --user $user $packageName")
                    }
                    val command = commands.joinToString(" ; ")  // no dependency
                    try {
                        runAsRoot(command)
                    } catch (e: ShellCommandFailedException) {
                        throw ShellActionFailedException(
                            command,
                            e.shellResult.err.joinToString("\n"),
                            e
                        )
                    } catch (e: Throwable) {
                        LogsHandler.unexpectedException(e, command)
                        throw ShellActionFailedException(command, "unhandled exception", e)
                    }
                    // don't care for the result here, it likely fails due to file not found
                    try {
                        if (!packageName.isNullOrEmpty()) { // IMPORTANT!!! otherwise removing all in parent(!) directory
                            val command = "$utilBoxQ rm -rf /data/lib/$packageName/*"
                            runAsRoot(command)
                        }
                    } catch (e: ShellCommandFailedException) {
                        Timber.d("Command '$command' failed: ${e.shellResult.err.joinToString(" ")}")
                    } catch (e: Throwable) {
                        LogsHandler.unexpectedException(e, command)
                    }
                }
            } else {
                // Deleting if system app
                // it seems that busybox mount sometimes fails silently so use toolbox instead
                var apkSubDir = FileUtils.getName(sourceDir!!)
                apkSubDir = apkSubDir.substring(0, apkSubDir.lastIndexOf('.'))
                if (apkSubDir.isEmpty()) {
                    val error = ("Variable apkSubDir in uninstall method is empty. This is used "
                            + "in a recursive rm call and would cause catastrophic damage!")
                    Timber.wtf(error)
                    throw IllegalArgumentException(error)
                }
                // TODO what about dedata etc.?
                var command = "mount -o remount,rw /system && ("
                if (sourceDir.isNotEmpty())    // IMPORTANT!!! otherwise removing all in parent(!) directory
                    command += " ; $utilBoxQ rm -rf ${quote(sourceDir)}"
                if (apkSubDir.isNotEmpty())        // IMPORTANT!!! otherwise removing all in parent(!) directory
                    command += " ; $utilBoxQ rm -rf ${quote("/system/app/$apkSubDir")}"
                command += ") ; mount -o remount,ro /system"
                if (!dataDir.isNullOrEmpty())      // IMPORTANT!!! otherwise removing all in parent(!) directory
                    command += " ; $utilBoxQ rm -rf ${quote(dataDir)}"
                if (!packageName.isNullOrEmpty())  // IMPORTANT!!! otherwise removing all in parent(!) directory
                    command += " ; $utilBoxQ rm -rf ${quote("/data/app-lib/${packageName}")}/*"
                try {
                    runAsRoot(command)
                } catch (e: ShellCommandFailedException) {
                    throw ShellActionFailedException(
                        command,
                        e.shellResult.err.joinToString("\n"),
                        e
                    )
                } catch (e: Throwable) {
                    LogsHandler.unexpectedException(e, command)
                    throw ShellActionFailedException(command, "unhandled exception", e)
                }
            }
        }

        @Throws(ShellActionFailedException::class)
        fun enableDisable(users: List<String?>?, packageName: String?, enable: Boolean) {
            val option = if (enable) "enable" else "disable"
            if (!users.isNullOrEmpty()) {
                val commands = mutableListOf<String>()
                for (user in users) {
                    commands.add("pm $option --user $user $packageName")
                }
                val command = commands.joinToString(" ; ")  // no dependency
                try {
                    runAsRoot(command)
                } catch (e: ShellCommandFailedException) {
                    throw ShellActionFailedException(
                        command,
                        "Could not $option package $packageName",
                        e
                    )
                } catch (e: Throwable) {
                    LogsHandler.unexpectedException(e, command)
                    throw ShellActionFailedException(
                        command,
                        "Could not $option package $packageName",
                        e
                    )
                }
            }
        }

        @Throws(ShellActionFailedException::class)
        fun getUsers(): List<String> {
            // do not cache because users can change at runtime
            //if (users.isNotEmpty()) {
            //    return users
            //}
            val command = "pm list users | $utilBoxQ sed -nr 's/.*\\{([0-9]+):.*/\\1/p'"
            return try {
                val result = runAsRoot(command)
                result.out
                    .map { obj: String -> obj.trim { it <= ' ' } }
                    .filter { it.isNotEmpty() }
                    .toList()
            } catch (e: ShellCommandFailedException) {
                throw ShellActionFailedException(command, "Could not fetch list of users", e)
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e, command)
                throw ShellActionFailedException(command, "Could not fetch list of users", e)
            }
        }

        val isMultiuserEnabled: Boolean
            get() {
                val users = getUsers()
                return users.isNotEmpty() && users.size > 1
            }

        @get:Throws(ShellActionFailedException::class)
        val disabledPackages: List<String>
            get() {
                val profileId = currentProfile
                val command = "pm list packages --user $profileId -d"
                return try {
                    val result = runAsRoot(command)
                    result.out
                        .filter { line: String -> line.contains("s") }
                        .map { line: String ->
                            line.substring(line.indexOf(':') + 1).trim { it <= ' ' }
                        }
                        .toList()
                } catch (e: ShellCommandFailedException) {
                    throw ShellActionFailedException(
                        command,
                        "Could not fetch disabled packages",
                        e
                    )
                } catch (e: Throwable) {
                    LogsHandler.unexpectedException(e, command)
                    throw ShellActionFailedException(
                        command,
                        "Could not fetch disabled packages",
                        e
                    )
                }
            }

        @Throws(ShellActionFailedException::class)
        fun wipeCache(context: Context, app: Package) {
            Timber.i("${app.packageName}: Wiping cache")
            val commands = mutableListOf<String>()
            // Normal app cache always exists
            val dataPath = app.dataPath
            if (dataPath.isNotEmpty())
                commands.add("$utilBoxQ rm -rf ${quote(dataPath)}/cache/* ${quote(dataPath)}/code_cache/*")

            fun conditionalDeleteCommand(directory: String): String {
                return if (directory.isNotEmpty())
                    "if [ -d ${quote(directory)} ]; then $utilBoxQ rm -rf ${quote(directory)}/* ; fi"
                else
                    ""
            }

            // device protected data cache, might exist or not
            if (app.devicesProtectedDataPath.isNotEmpty()) {
                val cacheDir = File(app.devicesProtectedDataPath, "cache").absolutePath
                val codeCacheDir = File(app.devicesProtectedDataPath, "code_cache").absolutePath
                commands.add(conditionalDeleteCommand(cacheDir))
                commands.add(conditionalDeleteCommand(codeCacheDir))
            }

            // external cache dirs are added dynamically, the bash if-else will handle the logic
            for (myCacheDir in context.externalCacheDirs) {
                val cacheDirName = myCacheDir.name
                val appsCacheDir = File(
                    File(myCacheDir.parentFile?.parentFile, app.packageName),
                    cacheDirName
                ).absolutePath
                commands.add(conditionalDeleteCommand(appsCacheDir))
            }
            val command = commands.joinToString(" ; ")  // no dependency
            try {
                runAsRoot(command)
            } catch (e: ShellCommandFailedException) {
                throw ShellActionFailedException(command, e.shellResult.err.joinToString("\n"), e)
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e, command)
                throw ShellActionFailedException(command, "unhandled exception", e)
            }
        }
    }
}