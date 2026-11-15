package com.machiav3lli.backup.manager.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.machiav3lli.backup.ACTION_CANCEL
import com.machiav3lli.backup.ACTION_CANCEL_SCHEDULE
import com.machiav3lli.backup.ACTION_CRASH
import com.machiav3lli.backup.ACTION_RE_SCHEDULE
import com.machiav3lli.backup.ACTION_RUN_SCHEDULE
import com.machiav3lli.backup.EXTRA_PERIODIC
import com.machiav3lli.backup.EXTRA_SCHEDULE_ID
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.preferences.traceSchedule
import com.machiav3lli.backup.data.repository.ScheduleRepository
import com.machiav3lli.backup.manager.handler.WorkHandler
import com.machiav3lli.backup.manager.tasks.ScheduleWork
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.scheduleNextAlarm
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

class CommandReceiver :
//TODO hg42 how to maintain security?
//      machiav3lli by making the receiver only internally accessible (not exported)
//      hg42 but it's one of the purposes to be remotely controllable from other apps like Tasker
//      hg42 no big prob for now: cancel, starting or changing schedule isn't very critical
    BroadcastReceiver(), KoinComponent {
    private val scheduleRepo: ScheduleRepository by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val command = intent.action
        Timber.i("Command: command $command")

        val pendingResult = goAsync()
        val appScope = (context.applicationContext as NeoApp).applicationScope
        appScope.launch {
            try {
                when (command) {
                    ACTION_CANCEL          -> {
                        val batchName = intent.getStringExtra("name")
                        Timber.d("################################################### command intent cancel -------------> name=$batchName")
                        NeoApp.addInfoLogText("$command $batchName")
                        get<WorkHandler>(WorkHandler::class.java).cancel(batchName)
                    }

                    ACTION_RUN_SCHEDULE    -> {
                        intent.getStringExtra("name")?.let { name ->
                            NeoApp.addInfoLogText("$command $name")
                            Timber.d("################################################### command intent schedule -------------> name=$name")
                            scheduleRepo.getSchedule(name)?.let { schedule ->
                                ScheduleWork.enqueueImmediate(schedule)
                            }
                        }
                    }

                    ACTION_CANCEL_SCHEDULE -> {
                        intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L).takeIf { it != -1L }
                            ?.let { id ->
                                Timber.d("################################################### command cancel schedule -------------> id=$id")
                                ScheduleWork.cancel(
                                    id,
                                    intent.getBooleanExtra(EXTRA_PERIODIC, false)
                                )
                            }
                    }

                    // TODO reconsider when ScheduleWork is fully implemented
                    ACTION_RE_SCHEDULE     -> {
                        intent.getStringExtra("name")?.let { name ->
                            val now = SystemUtils.now
                            val time = intent.getStringExtra("time")
                            val setTime = time ?: SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(now + 120)
                            NeoApp.addInfoLogText("$command $name $time -> $setTime")
                            Timber.d("################################################### command intent reschedule -------------> name=$name time=$time -> $setTime")
                            scheduleRepo.getSchedule(name)?.let { schedule ->
                                val (hour, minute) = setTime.split(":").map { it.toInt() }
                                traceSchedule { "[${schedule.id}] command receiver -> re-schedule to hour=$hour minute=$minute" }
                                val newSched = schedule.copy(
                                    timeHour = hour,
                                    timeMinute = minute,
                                )
                                scheduleRepo.update(newSched)
                                scheduleNextAlarm(context, newSched.id, true)
                            }
                        }
                    }

                    ACTION_CRASH           -> {
                        throw Exception("this is a crash via command intent")
                    }

                    null                   -> {}
                    else                   -> {
                        NeoApp.addInfoLogText("Command: command '$command'")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}