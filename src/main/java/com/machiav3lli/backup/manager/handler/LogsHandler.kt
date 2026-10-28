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
import com.machiav3lli.backup.LOG_INSTANCE
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.Log
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.entity.StorageFile.Companion.invalidateCache
import com.machiav3lli.backup.data.preferences.pref_autoLogExceptions
import com.machiav3lli.backup.data.preferences.pref_maxLogCount
import com.machiav3lli.backup.ui.pages.onErrorInfo
import com.machiav3lli.backup.ui.pages.textLog
import com.machiav3lli.backup.utils.BACKUP_DATE_TIME_FORMATTER
import com.machiav3lli.backup.utils.FileUtils.BackupLocationInAccessibleException
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.SystemUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

class LogsHandler {

    companion object {

        fun share(log: Log, asFile: Boolean = true) {
            MainScope().launch(Dispatchers.IO) {
                try {
                    getLogFile(log.logDate)?.let { file ->
                        SystemUtils.share(file, asFile)
                    }
                } catch (e: Throwable) {
                    unexpectedException(e)
                }
            }
        }

        @Throws(IOException::class)
        fun writeToLogFile(logText: String): StorageFile? {
            runCatching {
                val date = LocalDateTime.now()
                val logItem = Log(logText, date)
                val logFileName = String.format(
                    LOG_INSTANCE,
                    BACKUP_DATE_TIME_FORMATTER.format(date)
                )
                NeoApp.logsDirectory?.createFile(logFileName)?.let { logFile ->
                    BufferedOutputStream(logFile.outputStream()).use { logOut ->
                        logOut.write(
                            logItem.toSerialized().toByteArray(StandardCharsets.UTF_8)
                        )
                        //traceDebug { "Wrote $logFile file for $logItem" }
                    }
                    housekeepingLogs()
                    return logFile
                }
            }
            return null
        }

        @Throws(IOException::class)
        fun readLogs(): MutableList<Log> {
            val logs = mutableListOf<Log>()
            NeoApp.logsDirectory?.let { logsDir ->
                invalidateCache(logsDir)
                if (logsDir.isDirectory) {
                    logsDir.listFiles().forEach {

                        NeoApp.hitBusy(1000)

                        if (it.isFile) try {
                            logs.add(Log.fromFile(it))
                        } catch (e: Throwable) {
                            // avoid recursion! never use: logErrors(message) or throw
                            val message =
                                "incomplete log or wrong structure found in $it."
                            logException(e, it)
                            // create dummy log entry, that is deletable and shareable
                            runCatching {
                                val logDate =
                                    LocalDateTime.parse(
                                        it.name!!
                                            .replace(Regex(""".*?(\d+-\d+-\d+)-(\d+-\d+-\d+)-(\d+).*""")) {
                                                "${
                                                    it.groups[1]?.value ?: ""
                                                }T${
                                                    it.groups[2]?.value
                                                        ?.replace("-", ":")
                                                        ?: ""
                                                }.${
                                                    it.groups[3]?.value ?: ""
                                                }"
                                            }
                                    )
                                logs.add(Log(message(e), logDate))
                            }
                        }
                    }
                }
            }
            return logs
        }

        @Throws(IOException::class)
        fun housekeepingLogs() {
            try {
                NeoApp.logsDirectory?.let { logsDir ->
                    invalidateCache(logsDir)
                    if (logsDir.isDirectory) {
                        // must be ISO time format with sane sorted fields yyyy-mm-dd hh:mm:ss
                        val logs = logsDir.listFiles().sortedByDescending { it.name }
                        //traceDebug { "logs ${logs.map { it.name ?: "?" }.joinToString(" ")}" }
                        if (logs.size > pref_maxLogCount.value)
                            logs.subList(pref_maxLogCount.value, logs.size)
                                .forEach {
                                    try {
                                        //traceDebug { "delete ${it.path}" }
                                        it.delete()
                                    } catch (e: Throwable) {
                                        val message =
                                            "cannot delete log '${it.path}'"
                                        logException(e, message)    // only log -> no recursion!
                                    }
                                }
                    }
                }
            } catch (e: Throwable) {
                val message =
                    "housekeepingLogs failed"
                logException(e, message)    // only log -> no recursion!
            }
        }

        fun getLogFile(date: LocalDateTime): StorageFile? {
            try {
                NeoApp.logsDirectory?.let { logsDir ->
                    invalidateCache(logsDir)
                    val timeStr = BACKUP_DATE_TIME_FORMATTER.format(date)
                    //val logFileName = String.format(  //TODO WECH
                    //    LOG_INSTANCE,
                    //    BACKUP_DATE_TIME_FORMATTER.format(date)
                    //)
                    //val file = logsDir.findFile(logFileName)
                    val files = logsDir.listFiles().filter { it.name!!.contains(timeStr) }
                    if (files.isNotEmpty()) {
                        val file = files.first()
                        if (file.exists())
                            return file
                    }
                }
            } catch (e: Throwable) {
                unexpectedException(e)
            }
            return null
        }

        fun logErrors(errors: String) {
            try {
                val logText = errors + "\n\n" + onErrorInfo().joinToString("\n")
                writeToLogFile(logText)
            } catch (e: IOException) {
                logException(e, backTrace = true)
            } catch (e: StorageLocationNotConfiguredException) {
                logException(e, backTrace = true)
            } catch (e: BackupLocationInAccessibleException) {
                logException(e, backTrace = true)
            }
        }

        fun stackTrace(e: Throwable) = e.stackTrace.joinToString("\nat ", "at ")
        fun message(e: Throwable, backTrace: Boolean = false) =
            "${e::class.simpleName}${
                if (e.message != null)
                    "\n${e.message}"
                else
                    ""
            }${
                if (e.cause != null)
                    "\n${e.cause!!::class.simpleName}\ncause: ${e.cause!!.message}"
                else
                    ""
            }${
                if (backTrace)
                    "\n${stackTrace(e)}"
                else
                    ""
            }${
                if (backTrace && e.cause != null)
                    "\n${stackTrace(e.cause!!)}"
                else
                    ""
            }"

        fun logException(
            e: Throwable,
            what: Any? = null,
            backTrace: Boolean = false,
            prefix: String = "",
            unhandled: Boolean = false,
        ) {
            var whatStr = ""
            if (what != null) {
                whatStr = what.toString()
                whatStr = if (whatStr.contains("\n") || whatStr.length > 20)
                    "{\n$whatStr\n}"
                else
                    "$whatStr : "
            }
            Timber.e("$prefix$whatStr\n${message(e, backTrace)}")
            if (unhandled && pref_autoLogExceptions.value) {
                textLog(
                    listOf(
                        whatStr,
                        message(e, backTrace),
                        ""
                    ) + onErrorInfo()
                )
            }
        }

        fun unexpectedException(e: Throwable, what: Any? = null) {
            logException(e, what, backTrace = true, prefix = "unexpected: ", unhandled = true)
        }

        fun handleErrorMessages(context: Context, errorText: String?): String? {
            return when {
                errorText?.contains("bytes specified in the header were written")
                    ?: false -> context.getString(R.string.error_datachanged) + "\n(" + errorText + ")"

                errorText?.contains("Input is not in the .gz format")
                    ?: false -> context.getString(R.string.error_encryptionpassword) + "\n" + errorText + ")"

                else         -> errorText
            }
        }

        fun <T> runOrLog(todo: () -> T): T? {
            return try {
                todo()
            } catch (e: Throwable) {
                unexpectedException(e)
                null
            }
        }

        fun <T> runOrLog(default: T, todo: () -> T): T {
            return try {
                todo()
            } catch (e: Throwable) {
                unexpectedException(e)
                default
            }
        }

        suspend fun <T> runsOrLog(todo: suspend () -> T): T? {
            return try {
                todo()
            } catch (e: Throwable) {
                unexpectedException(e)
                null
            }
        }

        suspend fun <T> runsOrLog(default: T, todo: suspend () -> T): T {
            return try {
                todo()
            } catch (e: Throwable) {
                unexpectedException(e)
                default
            }
        }

        fun <T> runOr(todo: () -> T): T? {
            return try {
                todo()
            } catch (e: Throwable) {
                null
            }
        }

        fun <T> runOr(default: T, todo: () -> T): T {
            return try {
                todo()
            } catch (e: Throwable) {
                default
            }
        }

        suspend fun <T> runsOr(todo: suspend () -> T): T? {
            return try {
                todo()
            } catch (e: Throwable) {
                null
            }
        }

        suspend fun <T> runsOr(default: T, todo: suspend () -> T): T {
            return try {
                todo()
            } catch (e: Throwable) {
                default
            }
        }
    }
}
