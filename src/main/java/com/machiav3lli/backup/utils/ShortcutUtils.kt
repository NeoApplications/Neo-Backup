/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2026  Antonios Hazim
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
package com.machiav3lli.backup.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.machiav3lli.backup.ACTION_RUN_SCHEDULE_SHORTCUT
import com.machiav3lli.backup.EXTRA_SCHEDULE_ID
import com.machiav3lli.backup.MAX_SHORTCUTS
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.ui.activities.NeoActivity
import timber.log.Timber

fun updateScheduleShortcuts(context: Context, schedules: List<Schedule>) {
    try {
        val enabledSchedules = schedules
            .filter { it.enabled }
            .sortedBy { it.name }
            .take(MAX_SHORTCUTS)

        val shortcuts = enabledSchedules.map { schedule ->
            createScheduleShortcut(context, schedule)
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        Timber.d("Updated ${shortcuts.size} schedule shortcuts")
    } catch (e: Exception) {
        Timber.e(e, "Failed to update schedule shortcuts")
    }
}

fun addScheduleShortcut(context: Context, schedule: Schedule) {
    if (!schedule.enabled) {
        removeScheduleShortcut(context, schedule.id)
        return
    }

    try {
        val shortcut = createScheduleShortcut(context, schedule)
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        Timber.d("Added/updated shortcut for schedule: ${schedule.name}")
    } catch (e: Exception) {
        Timber.e(e, "Failed to add schedule shortcut for: ${schedule.name}")
    }
}

fun removeScheduleShortcut(context: Context, scheduleId: Long) {
    try {
        val shortcutId = "schedule_$scheduleId"
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(shortcutId))
        Timber.d("Removed shortcut for schedule ID: $scheduleId")
    } catch (e: Exception) {
        Timber.e(e, "Failed to remove schedule shortcut for ID: $scheduleId")
    }
}

private fun createScheduleShortcut(context: Context, schedule: Schedule): ShortcutInfoCompat {
    val intent = Intent(context, NeoActivity::class.java).apply {
        action = ACTION_RUN_SCHEDULE_SHORTCUT
        putExtra(EXTRA_SCHEDULE_ID, schedule.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val shortLabel = schedule.name.take(10)
    val longLabel = context.getString(R.string.shortcut_run_schedule, schedule.name)

    return ShortcutInfoCompat.Builder(context, "schedule_${schedule.id}")
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
        .setIntent(intent)
        .build()
}
