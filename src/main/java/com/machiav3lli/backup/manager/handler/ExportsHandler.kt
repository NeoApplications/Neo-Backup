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
import com.machiav3lli.backup.EXPORTS_FOLDER_NAME
import com.machiav3lli.backup.EXPORTS_FOLDER_NAME_ALT
import com.machiav3lli.backup.EXPORTS_INSTANCE
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.entity.StorageFile.Companion.invalidateCache
import com.machiav3lli.backup.data.repository.ScheduleRepository
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logErrors
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.unexpectedException
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.showNotification
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class ExportsHandler(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository = get(ScheduleRepository::class.java),
) {
    private var exportsDirectory: StorageFile? = null

    init {
        NeoApp.backupRoot?.let { backupRoot ->
            exportsDirectory = backupRoot.ensureDirectory(EXPORTS_FOLDER_NAME)
            backupRoot.findFile(EXPORTS_FOLDER_NAME_ALT)?.let { oldFolder ->
                oldFolder.listFiles().forEach {
                    exportsDirectory?.createFile(it.name!!)
                        ?.writeText(it.readText())
                }
                oldFolder.deleteRecursive()
            }
        }
    }

    @Throws(IOException::class)
    suspend fun exportSchedules() {
        scheduleRepository.getAll().forEach {
            val fileName = String.format(EXPORTS_INSTANCE, it.name)
            exportsDirectory?.createFile(fileName)?.let { exportFile ->
                BufferedOutputStream(exportFile.outputStream())
                    .use { exportOut ->
                        exportOut.write(
                            it.toSerialized().toByteArray(StandardCharsets.UTF_8)
                        )
                        Timber.i("Exported the schedule ${it.name} to $exportFile")
                    }
            }
        }
        context.showNotification(
            NeoActivity::class.java, SystemUtils.now.toInt(),
            context.getString(R.string.sched_exported), null, false
        )
    }

    @Throws(IOException::class)
    fun readExports(): List<Pair<Schedule, StorageFile>> {
        val exports = mutableListOf<Pair<Schedule, StorageFile>>()
        invalidateCache { it.contains(EXPORTS_FOLDER_NAME) }
        exportsDirectory?.listFiles()?.forEach {
            if (it.isFile) try {
                val schedule = Schedule.Builder(it).build()
                exports.add(Pair(schedule, it))
            } catch (_: NullPointerException) {
                val message = "(Null) Incomplete schedule or wrong structure found in ${it}."
                Timber.w(message)
                logErrors(message)
            } catch (e: Throwable) {
                val message = "(catchall) Incomplete schedule or wrong structure found in ${it}."
                unexpectedException(e, it)
                logErrors(message)
            }
        }
        return exports
    }
}
