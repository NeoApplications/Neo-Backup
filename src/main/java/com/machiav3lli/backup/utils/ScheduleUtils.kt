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
package com.machiav3lli.backup.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.machiav3lli.backup.EXTRA_NAME
import com.machiav3lli.backup.EXTRA_SCHEDULE_ID
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.STATEFLOW_SUBSCRIBE_BUFFER
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.preferences.pref_autoLogSuspicious
import com.machiav3lli.backup.data.preferences.traceSchedule
import com.machiav3lli.backup.data.repository.ScheduleRepository
import com.machiav3lli.backup.manager.services.ScheduleReceiver
import com.machiav3lli.backup.manager.tasks.ScheduleWork
import com.machiav3lli.backup.ui.pages.onErrorInfo
import com.machiav3lli.backup.ui.pages.pref_fakeScheduleMin
import com.machiav3lli.backup.ui.pages.pref_useAlarmClock
import com.machiav3lli.backup.ui.pages.pref_useExactAlarm
import com.machiav3lli.backup.ui.pages.textLog
import com.machiav3lli.backup.utils.extensions.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

fun calcRuntimeDiff(schedule: Schedule): Pair<Long, Long> {
    val now = Calendar.getInstance()
    val c = Calendar.getInstance()
    var nIncrements = 0

    val fakeMin = pref_fakeScheduleMin.value
    if (fakeMin > 1) {
        //c[Calendar.HOUR_OF_DAY] = schedule.timeHour
        c[Calendar.MINUTE] = (c[Calendar.MINUTE] / fakeMin + 1) * fakeMin % 60
        c[Calendar.SECOND] = 0
        c[Calendar.MILLISECOND] = 0
        while (c.before(now)) {
            c.add(Calendar.MINUTE, fakeMin)
            nIncrements++
        }
        traceSchedule { "[${schedule.id}] added $nIncrements * ${schedule.interval} min" }
    } else if (fakeMin == 1) {
        //c[Calendar.HOUR_OF_DAY] = schedule.timeHour
        c[Calendar.MINUTE] = schedule.timeHour
        c[Calendar.SECOND] = schedule.timeMinute
        c[Calendar.MILLISECOND] = 0
        while (c.before(now)) {
            c.add(Calendar.HOUR, schedule.interval)
            nIncrements++
        }
        traceSchedule { "[${schedule.id}] added $nIncrements * ${schedule.interval} hours" }
    } else {
        c[Calendar.HOUR_OF_DAY] = schedule.timeHour
        c[Calendar.MINUTE] = schedule.timeMinute
        c[Calendar.SECOND] = 0
        c[Calendar.MILLISECOND] = 0
        while (c.before(now)) {
            c.add(Calendar.DAY_OF_MONTH, schedule.interval)
            nIncrements++
        }
        traceSchedule { "[${schedule.id}] added $nIncrements * ${schedule.interval} days" }
    }

    traceSchedule {
        "[${schedule.id}] calculateTimeToRun: next: ${
            ISO_DATE_TIME_FORMAT.format(c.timeInMillis)
        } now: ${
            ISO_DATE_TIME_FORMAT.format(now.timeInMillis)
        } placed: ${
            ISO_DATE_TIME_FORMAT.format(schedule.timePlaced)
        } interval: ${
            schedule.interval
        }"
    }
    return Pair(c.timeInMillis, c.timeInMillis - now.timeInMillis)
}

const val updateInterval = 1_000L
const val useSeconds = updateInterval < 60_000

fun calcTimeLeft(schedule: Schedule): Pair<String, String> {
    var absTime = ""
    var relTime = ""
    val (at, diff) = calcRuntimeDiff(schedule)
    absTime = ISO_DATE_TIME_FORMAT_MIN.format(at)
    val timeDiff = max(diff, 0)
    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff).toInt()
    val days = TimeUnit.MILLISECONDS.toDays(timeDiff).toInt()
    if (days != 0) {
        relTime +=
            "${NeoApp.context.resources.getQuantityString(R.plurals.days_left, days, days)} + "
    }
    val hours = TimeUnit.MILLISECONDS.toHours(timeDiff).toInt() % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff).toInt() % 60
    if (useSeconds && remainingMinutes < 10) {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiff).toInt() % 60
        relTime += LocalTime.of(hours, minutes, seconds).toString()
    } else {
        relTime += LocalTime.of(hours, minutes).toString()
    }
    return Pair(absTime, relTime)
}

@Composable
fun Schedule.timeLeft(): StateFlow<Pair<String, String>> = flow {
    while (true) {
        if (this@timeLeft.enabled) emit(calcTimeLeft(this@timeLeft))
        delay(updateInterval.milliseconds)
    }
}
    .stateIn(
        scope = rememberCoroutineScope(),
        started = SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
        initialValue = calcTimeLeft(this)
    )

// TODO fix for future replacement of Alarm
suspend fun scheduleNextWork(context: Context, scheduleId: Long, rescheduleBoolean: Boolean) {
    if (scheduleId >= 0) {
        val scheduleRepo = get<ScheduleRepository>(ScheduleRepository::class.java)
        var schedule = scheduleRepo.getSchedule(scheduleId)

        if (schedule?.enabled == true) {
            val now = SystemUtils.now

            if (rescheduleBoolean) {
                schedule = schedule.copy(timePlaced = now)
                traceSchedule { "[${schedule.id}] re-scheduling $schedule" }
                scheduleRepo.update(schedule)
            }
            ScheduleWork.enqueuePeriodic(schedule, rescheduleBoolean)
        }
    } else {
        Timber.e("[$scheduleId] got id from $context")
    }
}

suspend fun scheduleNextAlarm(context: Context, scheduleId: Long, rescheduleBoolean: Boolean) {
    if (scheduleId >= 0) {
        coroutineScope {
            val scheduleRepo = get<ScheduleRepository>(ScheduleRepository::class.java)
            var schedule = scheduleRepo.getSchedule(scheduleId) ?: return@coroutineScope
            if (schedule.enabled) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val now = SystemUtils.now
                val (timeToRun, timeLeft) = calcRuntimeDiff(schedule)

                if (rescheduleBoolean) {
                    schedule = schedule.copy(
                        timePlaced = now,
                        timeToRun = timeToRun,
                    )
                    traceSchedule { "[${schedule.id}] re-scheduling $schedule" }
                    scheduleRepo.update(schedule)
                } else {
                    if (timeLeft <= TimeUnit.MINUTES.toMillis(1)) {
                        schedule = schedule.copy(
                            timeToRun = now + TimeUnit.MINUTES.toMillis(1)
                        )
                        scheduleRepo.update(schedule)
                        val message = "timeLeft < 1 min -> set schedule $schedule"
                        traceSchedule { "[${schedule.id}] **************************************** $message" }
                        if (!NeoApp.isRelease || pref_autoLogSuspicious.value)
                            textLog(
                                listOf(
                                    message,
                                    ""
                                ) + onErrorInfo()
                            )
                    }
                }

                val hasPermission: Boolean =
                    if (Android.minSDK(Build.VERSION_CODES.S)) {
                        alarmManager.canScheduleExactAlarms()
                    } else {
                        true
                    }

                val pendingIntent = context.createPendingIntent(scheduleId, schedule.name)

                when {
                    hasPermission && pref_useAlarmClock.value -> {
                        traceSchedule { "[${schedule.id}] alarmManager.setAlarmClock $schedule" }
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(schedule.timeToRun, null),
                            pendingIntent
                        )
                    }

                    hasPermission && pref_useExactAlarm.value -> {
                        traceSchedule { "[${schedule.id}] alarmManager.setExactAndAllowWhileIdle $schedule" }
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            schedule.timeToRun,
                            pendingIntent
                        )
                    }

                    else                                      -> {
                        traceSchedule { "[${schedule.id}] alarmManager.setAndAllowWhileIdle $schedule" }
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            schedule.timeToRun,
                            pendingIntent
                        )
                    }
                }
                traceSchedule {
                    "[$scheduleId] schedule starting in: ${
                        TimeUnit.MILLISECONDS.toMinutes(schedule.timeToRun - SystemUtils.now)
                    } minutes name=${schedule.name}"
                }
            } else
                traceSchedule { "[$scheduleId] schedule is disabled. Nothing to schedule!" }
        }
    } else {
        Timber.e("[$scheduleId] got id from $context")
    }
}

fun cancelScheduleAlarm(context: Context, scheduleId: Long, scheduleName: String) {
    traceSchedule { "[$scheduleId] cancelled schedule" }
    context.createPendingIntent(scheduleId, scheduleName).let {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(it)
        it.cancel()
    }
}

private val scheduleAdjectives = setOf(
    "Fast", "Modern", "Jumpy", "Smart", "Agile",
    "Dynamic", "Swift", "Calm", "Daily", "Weekly",
    "Flexible", "Reliable", "Steady", "Balanced", "Rapid",
    "Focused", "Lean", "Quiet", "Reactive", "Active",
    "Robust", "Simple", "Clean", "Neat", "Turbo",
    "Express", "Hybrid", "Adaptive", "Fresh", "Bright",
    "Sharp", "Prime", "Bold", "Smooth", "Compact",
    "Instant", "Light", "Quick", "Efficient", "Clever",
    "Stealth", "Ultra", "Core", "Gravity", "Nova",
    "Pulse", "Spark", "Turbocharged", "Elegant", "Crisp",
    "Zippy", "Bouncy", "Funky", "Groovy", "Sneaky",
    "Spicy", "Goofy", "Rocket", "Ninja", "Hyper",
    "Whiz", "Lucky", "Cheeky", "Quirky", "Cosmic",
    "Loopy", "Epic", "Magic", "Sparkly", "Wild",
    "Crazy", "Silly", "Snappy", "Zesty", "Jammy",
    "Spooky", "Chunky", "Noisy", "Zoomy", "Zigzag",
    "Breezy", "Fizzing", "Popping", "Blazing", "Glitchy",
    "Party", "Chaotic", "Frenzy", "Rocketship", "Pixel",
    "Disco", "Mischief", "Banana", "Madcap", "Outrageous"
)

fun randomScheduleName(): String = "${scheduleAdjectives.random()} Schedule"

private const val ALARM_REQUEST_CODE_PREFIX = 8000
private fun Context.createPendingIntent(scheduleId: Long, scheduleName: String): PendingIntent {
    val alarmIntent = Intent(this, ScheduleReceiver::class.java).apply {
        action = "schedule"
        putExtra(EXTRA_SCHEDULE_ID, scheduleId)
        putExtra(EXTRA_NAME, scheduleName)
        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    }
    return PendingIntent.getBroadcast(
        this,
        ALARM_REQUEST_CODE_PREFIX + scheduleId.toInt(),
        alarmIntent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

var alarmsHaveBeenScheduled = false

// TODO long-term: replace with ScheduleWorker.scheduleAll()
fun scheduleAlarmsOnce(context: Context) {
    if (alarmsHaveBeenScheduled)
        return
    alarmsHaveBeenScheduled = true
    CoroutineScope(Dispatchers.IO).launch {
        scheduleAll(context)
    }
}

private suspend fun scheduleAll(context: Context) = coroutineScope {
    val scheduleRepo = get<ScheduleRepository>(ScheduleRepository::class.java)

    scheduleRepo.getAll().forEach { schedule ->
        when {
            !schedule.enabled -> cancelScheduleAlarm(context, schedule.id, schedule.name)
            else              -> scheduleNextAlarm(context, schedule.id, false)
        }
    }
}

fun Context.getStartScheduleMessage(
    schedule: Schedule,
    globalBlockList: Set<String>,
    tagsMap: Map<String, Set<String>>,
    allTags: Set<String>,
) = StringBuilder()
    .append(
        "\n${getString(R.string.sched_mode)} ${
            modesToString(
                this,
                modeToModes(schedule.mode)
            )
        }"
    )
    .append(
        "\n${getString(R.string.backup_filters)} ${
            filterToString(
                this,
                schedule.filter
            )
        }"
    )
    .append(
        "\n${getString(R.string.other_filters_options)} ${
            specialFilterToString(
                this,
                schedule.specialFilter
            )
        }"
    )
    // TODO list the CL packages
    .append(
        "\n" + getString(
            R.string.sched_customlist_dialog_message,
            if (schedule.customList.isNotEmpty()) resources.getQuantityString(
                R.plurals.package_num,
                schedule.customList.size
            ) else getString(R.string.dialogNo)
        )
    )
    // TODO list the BL packages
    .append(
        "\n" + getString(
            R.string.sched_blocklist_dialog_message,
            if (schedule.blockList.isNotEmpty()) resources.getQuantityString(
                R.plurals.package_num,
                schedule.blockList.size
            ) else getString(R.string.dialogNo)
        )
    )
    .append(
        "\n${getString(R.string.filters_tags)}: ${
            if (schedule.tagsList.isNotEmpty()) schedule.tagsList.joinToString(",")
            else getString(R.string.dialogNo)
        }"
    )
    .append(
        "\n" + getString(
            R.string.packagesToBackup, filterPackages(
                packages = getInstalledPackageList(),
                tagsMap = tagsMap,
                filter = schedule.filter,
                specialFilter = schedule.specialFilter,
                customList = schedule.customList,
                blockList = globalBlockList.plus(schedule.blockList.toSet()),
                tagsList = schedule.tagsList.filter { it in allTags }.toSet(),
            ).size
        )
    )
    .toString()