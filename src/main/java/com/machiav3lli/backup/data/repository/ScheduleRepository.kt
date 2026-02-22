package com.machiav3lli.backup.data.repository

import android.app.Application
import com.machiav3lli.backup.data.dbs.Converters
import com.machiav3lli.backup.data.dbs.dao.ScheduleDao
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.preferences.traceSchedule
import com.machiav3lli.backup.manager.tasks.ScheduleWork
import com.machiav3lli.backup.utils.addScheduleShortcut
import com.machiav3lli.backup.utils.cancelScheduleAlarm
import com.machiav3lli.backup.utils.removeScheduleShortcut
import com.machiav3lli.backup.utils.scheduleNextAlarm
import com.machiav3lli.backup.utils.updateScheduleShortcuts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleRepository(
    private val dao: ScheduleDao,
    private val appContext: Application,
) {
    fun getAllFlow() = dao.getAllFlow()

    suspend fun getAll() = dao.getAll()

    fun getScheduleFlow(id: Long) = dao.getByIdFlow(id)

    suspend fun getSchedule(id: Long) = dao.getById(id)

    suspend fun getSchedule(name: String) = dao.getByName(name)

    fun getCustomListFlow(id: Flow<Long>): Flow<Set<String>> = id.flatMapLatest {
        dao.getCustomListFlow(it)
            .map { string -> Converters().toStringSet(string) }
    }

    fun getBlockListFlow(id: Flow<Long>): Flow<Set<String>> = id.flatMapLatest {
        dao.getBlockListFlow(it)
            .map { string -> Converters().toStringSet(string) }
    }

    suspend fun addNew(withSpecial: Boolean) {
        dao.insert(
            Schedule.Builder() // Set id to 0 to make the database generate a new id
                .withId(0)
                .withSpecial(withSpecial)
                .randomName()
                .build()
        )
        // update to avoid ignoring the shortcut when the number of shortcuts surpasses 4
        updateScheduleShortcuts(appContext, dao.getAll())
    }

    suspend fun update(value: Schedule) = dao.update(value)

    suspend fun updateSchedule(schedule: Schedule, rescheduleBoolean: Boolean) {
        dao.update(schedule)
        if (schedule.enabled) {
            traceSchedule { "[$schedule.id] ScheduleViewModel.updateS -> ${if (rescheduleBoolean) "re-" else ""}schedule" }
            scheduleNextAlarm(
                appContext,
                schedule.id,
                rescheduleBoolean
            )
            addScheduleShortcut(appContext, schedule)
        } else {
            traceSchedule { "[$schedule.id] ScheduleViewModel.updateS -> cancelAlarm" }
            cancelScheduleAlarm(appContext, schedule.id, schedule.name)
            ScheduleWork.cancel(schedule.id)
            removeScheduleShortcut(appContext, schedule.id)
        }
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
        ScheduleWork.cancel(id)
        removeScheduleShortcut(appContext, id)
    }
}

