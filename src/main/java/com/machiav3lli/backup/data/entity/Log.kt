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
package com.machiav3lli.backup.data.entity

import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.utils.LocalDateTimeSerializer
import com.machiav3lli.backup.utils.TraceUtils.canonicalName
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.extensions.Quintuple
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime

@Serializable
data class Log(
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable(with = LocalDateTimeSerializer::class)
    val logDate: LocalDateTime = LocalDateTime.parse("2020-01-01T00:00:00"),
    val deviceName: String = "",
    val sdkCodename: String = "",
    val cpuArch: String = "",
    val logText: String = "",
) {
    constructor(text: String, date: LocalDateTime) : this(
        logDate = date,
        deviceName = Android.deviceName,
        sdkCodename = Android.name,
        cpuArch = Android.mainPlatform,
        logText = text
    )

    override fun toString(): String {
        return "LogItem{" +
                "logDate=$logDate" +
                ", deviceName='$deviceName'" +
                ", sdkCodename='$sdkCodename'" +
                ", cpuArch='$cpuArch'" +
                ", logText:\n$logText" +
                '}'
    }

    fun delete(): Boolean? {
        val logFile = LogsHandler.getLogFile(this.logDate)
        return logFile?.delete()
    }

    fun toSerialized() = """
        logDate: $logDate
        deviceName: $deviceName
        sdkCodename: $sdkCodename
        cpuArch: $cpuArch
    """.trimIndent() + "\n\n" + logText

    companion object {
        fun fromFile(logFile: StorageFile): Log {
            try {
                logFile.inputStream()!!.use { inputStream ->
                    val text = inputStream.reader().readText()
                    initFromText(text).let {
                        return Log(it.first, it.second, it.third, it.fourth, it.fifth)
                    }
                }
            } catch (e: Backup.BrokenBackupException) {
                throw Backup.BrokenBackupException(
                    "Cannot read $logFile",
                    e
                )
            } catch (e: FileNotFoundException) {
                throw Backup.BrokenBackupException(
                    "Cannot open $logFile",
                    e
                )
            } catch (e: IOException) {
                throw Backup.BrokenBackupException(
                    "Cannot read $logFile",
                    e
                )
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e, logFile)
                throw Backup.BrokenBackupException("Unable to process $logFile. (${e::class.canonicalName}) $e")
            }
        }

        private fun initFromText(text: String): Quintuple<LocalDateTime, String, String, String, String> =
            try {
                var valid = false
                val lines = text.lines().toMutableList()
                var logDate: LocalDateTime? = null
                var deviceName = ""
                var sdkCodename = ""
                var cpuArch = ""
                var logText = ""
                while (lines.isNotEmpty()) {
                    val line = lines.removeAt(0)
                    if (line.isBlank()) {
                        logText = lines.joinToString("\n")
                        lines.clear()
                    } else {
                        try {
                            val (field, value) = line.split(Regex(""":\s*"""), limit = 2)
                            when (field) {
                                "logDate"     -> {  // minimum data we need
                                    logDate = LocalDateTime.parse(value)
                                    valid = true
                                }

                                "deviceName"  -> {
                                    deviceName = value
                                }

                                "sdkCodename" -> {
                                    sdkCodename = value
                                }

                                "cpuArch"     -> {
                                    cpuArch = value
                                }
                            }
                        } catch (_: Throwable) {
                            if (valid) {
                                // be tolerant, first non-header line, read remaining lines as text
                                logText = lines.joinToString("\n")
                                lines.clear()
                            }
                        }
                    }
                }
                if (logDate != null) Quintuple(
                    logDate,
                    deviceName,
                    sdkCodename,
                    cpuArch,
                    logText
                )
                else throw Backup.BrokenBackupException(
                    "Log file is neither ${NeoApp.mainSerializer::class.simpleName} nor text header format"
                )
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e)
                throw e
            }
    }
}