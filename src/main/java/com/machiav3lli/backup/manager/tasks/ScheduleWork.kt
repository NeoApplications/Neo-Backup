package com.machiav3lli.backup.manager.tasks

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.compose.ui.util.fastForEach
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.machiav3lli.backup.ACTION_CANCEL_SCHEDULE
import com.machiav3lli.backup.EXTRA_NAME
import com.machiav3lli.backup.EXTRA_PERIODIC
import com.machiav3lli.backup.EXTRA_SCHEDULE_ID
import com.machiav3lli.backup.NOTIFICATION_CHANNEL_SCHEDULE
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.preferences.pref_autoLogAfterSchedule
import com.machiav3lli.backup.data.preferences.pref_autoLogSuspicious
import com.machiav3lli.backup.data.preferences.traceSchedule
import com.machiav3lli.backup.data.repository.AppExtrasRepository
import com.machiav3lli.backup.data.repository.BlocklistRepository
import com.machiav3lli.backup.data.repository.PackageRepository
import com.machiav3lli.backup.data.repository.ScheduleRepository
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.WorkHandler
import com.machiav3lli.backup.manager.services.CommandReceiver
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.pages.pref_fakeScheduleDups
import com.machiav3lli.backup.ui.pages.supportInfo
import com.machiav3lli.backup.ui.pages.textLog
import com.machiav3lli.backup.utils.FileUtils
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.calcRuntimeDiff
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.extensions.takeUntilSignal
import com.machiav3lli.backup.utils.filterPackages
import com.machiav3lli.backup.utils.getInstalledPackageList
import com.machiav3lli.backup.utils.showNotification
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ScheduleWork(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {
    private val packageRepo: PackageRepository by inject()
    private val scheduleRepo: ScheduleRepository by inject()
    private val blocklistRepo: BlocklistRepository by inject()
    private val appExtrasRepo: AppExtrasRepository by inject()

    private var scheduleId = inputData.getLong(EXTRA_SCHEDULE_ID, -1L)
    private val notificationId = SystemUtils.now.toInt()
    private var notification: Notification? = null
    private val scheduleJob = Job()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createForegroundNotification()
        return ForegroundInfo(
            notification.hashCode(),
            notification,
            if (Android.minSDK(Build.VERSION_CODES.Q)) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else 0
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO + scheduleJob) {
        try {
            scheduleId = inputData.getLong(EXTRA_SCHEDULE_ID, -1L)
            val name = inputData.getString(EXTRA_NAME) ?: ""

            NeoApp.wakelock(true)

            traceSchedule {
                buildString {
                    append("[$scheduleId] %%%%% ScheduleWork starting for name='$name'")
                }
            }

            if (scheduleId < 0) {
                return@withContext Result.failure()
            }

            if (runningSchedules[scheduleId] != null) {
                val message =
                    "[$scheduleId] duplicate schedule detected: $name (as designed, ignored)"
                Timber.w(message)
                if (pref_autoLogSuspicious.value) {
                    textLog(
                        listOf(
                            message,
                            "--- autoLogSuspicious $scheduleId $name"
                        ) + supportInfo()
                    )
                }
                return@withContext Result.failure()
            }

            if (isStopped) {
                traceSchedule { "[$scheduleId] Work cancelled before processing" }
                return@withContext Result.failure()
            }

            runningSchedules[scheduleId] = false

            repeat(1 + pref_fakeScheduleDups.value) {
                val now = SystemUtils.now
                runningSchedules[scheduleId] = true
                val result = processSchedule(name, now)
                if (!result) {
                    return@withContext Result.failure()
                }
            }

            NeoApp.wakelock(false)

            Result.success()
        } catch (e: Exception) {
            Timber.e(e)
            runningSchedules.remove(scheduleId)
            Result.failure()
        } finally {
            runningSchedules.remove(scheduleId)
            NeoApp.wakelock(false)
        }
    }

    private suspend fun processSchedule(name: String, now: Long): Boolean =
        coroutineScope {
            val finishSignal = MutableStateFlow(false)
            val schedule = scheduleRepo.getSchedule(scheduleId) ?: return@coroutineScope false

            val selectedItems = getFilteredPackages(schedule)

            if (selectedItems.isEmpty()) {
                handleEmptySelectedItems(name)
                return@coroutineScope false
            }

            val worksList = mutableListOf<OneTimeWorkRequest>()
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            val batchName = WorkHandler.getBatchName(name, now)
            get<WorkHandler>(WorkHandler::class.java).beginBatch(batchName)

            var errors = ""
            var resultsSuccess = true
            var finished = 0
            val queued = selectedItems.size

            val workJobs = selectedItems.map { packageName ->
                val oneTimeWorkRequest = AppActionWork.Request(
                    packageName = packageName,
                    mode = schedule.mode,
                    backupBoolean = true,
                    notificationId = notificationId,
                    batchName = batchName,
                    immediate = false
                )
                worksList.add(oneTimeWorkRequest)

                if (inputData.getBoolean(EXTRA_PERIODIC, false))
                    scheduleRepo.update(schedule.copy(timePlaced = now))

                async {
                    get<WorkManager>(WorkManager::class.java).getWorkInfoByIdFlow(oneTimeWorkRequest.id)
                        .takeUntilSignal(finishSignal)
                        .collectLatest { workInfo ->
                            when (workInfo?.state) {
                                androidx.work.WorkInfo.State.SUCCEEDED,
                                androidx.work.WorkInfo.State.FAILED,
                                androidx.work.WorkInfo.State.CANCELLED -> {
                                    finished++
                                    val succeeded =
                                        workInfo.outputData.getBoolean("succeeded", false)
                                    val packageLabel =
                                        workInfo.outputData.getString("packageLabel") ?: ""
                                    val error = workInfo.outputData.getString("error") ?: ""

                                    if (error.isNotEmpty()) {
                                        errors = "$errors$packageLabel: ${
                                            LogsHandler.handleErrorMessages(
                                                context,
                                                error
                                            )
                                        }\n"
                                    }
                                    resultsSuccess = resultsSuccess && succeeded

                                    if (finished >= queued) {
                                        finishSignal.update { true }
                                        endSchedule(name, "all jobs finished")
                                        selectedItems.fastForEach {
                                            packageRepo.updatePackage(it)
                                        }
                                    }
                                }

                                else                                   -> {
                                    if (finished >= queued) {
                                        finishSignal.update { true }
                                        endSchedule(name, "all jobs finished")
                                        selectedItems.fastForEach {
                                            packageRepo.updatePackage(it)
                                        }
                                    }
                                }
                            }
                        }
                }
            }

            if (worksList.isNotEmpty()) {
                if (beginSchedule(name, "queueing work")) {
                    get<WorkManager>(WorkManager::class.java)
                        .beginWith(worksList)
                        .enqueue()
                    workJobs.awaitAll()
                    true
                } else {
                    endSchedule(name, "duplicate detected")
                    false
                }
            } else {
                beginSchedule(name, "no work")
                endSchedule(name, "no work")
                false
            }
        }

    private suspend fun getFilteredPackages(schedule: Schedule): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                FileUtils.invalidateBackupLocation()
                NeoApp.startup = false

                val customBlocklist = schedule.blockList.toPersistentSet()
                val globalBlocklist = blocklistRepo.loadGlobalBlocklistOf().toPersistentSet()
                val blockList = globalBlocklist.plus(customBlocklist)
                val tagsMap = appExtrasRepo.getAll().associate { it.packageName to it.customTags }
                val allTags = appExtrasRepo.getAll().flatMap { it.customTags }.distinct()
                val tagsList = schedule.tagsList.filter { it in allTags }.toPersistentSet()

                val unfilteredPackages = context.getInstalledPackageList()

                filterPackages(
                    packages = unfilteredPackages,
                    tagsMap = tagsMap,
                    filter = schedule.filter,
                    specialFilter = schedule.specialFilter,
                    customList = schedule.customList,
                    blockList = blockList,
                    tagsList = tagsList,
                ).map { it.packageName }

            } catch (e: FileUtils.BackupLocationInAccessibleException) {
                Timber.e("Schedule failed: ${e.message}")
                emptyList()
            } catch (e: StorageLocationNotConfiguredException) {
                Timber.e("Schedule failed: ${e.message}")
                emptyList()
            }
        }
    }

    private fun handleEmptySelectedItems(name: String) {
        beginSchedule(name, "no work")
        endSchedule(name, "no work")
        context.showNotification(
            parentActivity = NeoActivity::class.java,
            title = context.getString(R.string.schedule_failed),
            text = context.getString(R.string.empty_filtered_list),
            autoCancel = false,
            id = notificationId,
        )
        traceSchedule { "[$scheduleId] no packages matching" }
    }

    private fun beginSchedule(name: String, details: String = ""): Boolean {
        return if (NeoApp.runningSchedules[scheduleId] != true) {
            NeoApp.runningSchedules[scheduleId] = true
            NeoApp.beginLogSection("schedule $name")
            true
        } else {
            val message =
                "duplicate schedule detected: id=$scheduleId name='$name' (late, ignored) $details"
            Timber.w(message)
            if (pref_autoLogSuspicious.value)
                textLog(
                    listOf(
                        message,
                        "--- autoLogAfterSchedule $scheduleId $name${if (details.isEmpty()) "" else " ($details)"}"
                    ) + supportInfo()
                )
            false
        }
    }

    private fun endSchedule(name: String, details: String = "") {
        if (NeoApp.runningSchedules[scheduleId] != null) {
            NeoApp.runningSchedules.remove(scheduleId)
            if (pref_autoLogAfterSchedule.value) {
                textLog(
                    listOf(
                        "--- autoLogAfterSchedule id=$scheduleId name=$name${if (details.isEmpty()) "" else " ($details)"}"
                    ) + supportInfo()
                )
            }
            NeoApp.endLogSection("schedule $name")
        } else {
            traceSchedule { "[$scheduleId] duplicate schedule end: name='$name'${if (details.isEmpty()) "" else " ($details)"}" }
        }
    }

    private fun createForegroundNotification(): Notification {
        if (notification != null) return notification!!

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, NeoActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(context, CommandReceiver::class.java).apply {
            action = ACTION_CANCEL_SCHEDULE
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_PERIODIC, inputData.getBoolean(EXTRA_PERIODIC, false))
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_SCHEDULE)
            .setContentTitle(context.getString(R.string.sched_notificationMessage))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_close,
                context.getString(R.string.dialogCancel),
                cancelPendingIntent
            )
            .build()
            .also { notification = it }
    }

    companion object {
        private const val SCHEDULE_ONETIME = "schedule_one_time_"
        private const val SCHEDULE_WORK = "schedule_work_"
        private val runningSchedules = ConcurrentHashMap<Long, Boolean>()

        fun enqueuePeriodic(schedule: Schedule, reschedule: Boolean = false) {
            if (!schedule.enabled) return
            val workManager = get<WorkManager>(WorkManager::class.java)

            val (timeToRun, initialDelay) = calcRuntimeDiff(schedule)

            // TODO implement pref for charging, battery, network
            val constraints = Constraints.Builder()
                .setRequiresCharging(false)
                .build()

            val scheduleWorkRequest = PeriodicWorkRequestBuilder<ScheduleWork>(
                schedule.interval.toLong(),
                TimeUnit.DAYS,
            )
                .setInitialDelay(
                    initialDelay,
                    TimeUnit.MILLISECONDS,
                )
                //.setConstraints(constraints)
                .addTag("schedule_periodic_${schedule.id}")
                .setInputData(
                    workDataOf(
                        EXTRA_SCHEDULE_ID to schedule.id,
                        EXTRA_NAME to schedule.name,
                        EXTRA_PERIODIC to true,
                    )
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                "$SCHEDULE_WORK${schedule.id}",
                if (reschedule) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                else ExistingPeriodicWorkPolicy.UPDATE,
                scheduleWorkRequest,
            )
            traceSchedule {
                "[${schedule.id}] schedule starting in: ${
                    TimeUnit.MILLISECONDS.toMinutes(
                        timeToRun - SystemUtils.now
                    )
                } minutes name=${schedule.name}"
            }
        }

        fun enqueueImmediate(schedule: Schedule) =
            enqueueOnce(schedule.id, schedule.name, false)

        fun enqueueScheduled(scheduleId: Long, scheduleName: String) =
            enqueueOnce(scheduleId, scheduleName, true)

        private fun enqueueOnce(scheduleId: Long, scheduleName: String, periodic: Boolean) {
            val scheduleWorkRequest = OneTimeWorkRequestBuilder<ScheduleWork>()
                .setInputData(
                    workDataOf(
                        EXTRA_SCHEDULE_ID to scheduleId,
                        EXTRA_NAME to scheduleName,
                        EXTRA_PERIODIC to periodic,
                    )
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("schedule_${scheduleId}")
                .build()

            get<WorkManager>(WorkManager::class.java).enqueueUniqueWork(
                "${if (periodic) SCHEDULE_WORK else SCHEDULE_ONETIME}$scheduleId",
                ExistingWorkPolicy.REPLACE,
                scheduleWorkRequest,
            )
            traceSchedule {
                "[${scheduleId}] schedule starting immediately, name=${scheduleName}"
            }
        }

        // TODO use when periodic runs are fixed
        suspend fun scheduleAll() = coroutineScope {
            val scheduleRepo = get<ScheduleRepository>(ScheduleRepository::class.java)
            val workManager = get<WorkManager>(WorkManager::class.java)
            scheduleRepo.getAll().forEach { schedule ->
                val scheduleAlreadyRuns = runningSchedules[schedule.id] == true
                val scheduled = workManager
                    .getWorkInfosForUniqueWork("$SCHEDULE_WORK${schedule.id}")
                    .get().any { !it.state.isFinished }
                when {
                    scheduleAlreadyRuns || scheduled -> {
                        traceSchedule { "[${schedule.id}]: ignore $schedule" }
                    }

                    schedule.enabled                 -> {
                        traceSchedule { "[${schedule.id}]: enable $schedule" }
                        enqueuePeriodic(schedule, false)
                    }

                    else                             -> {
                        traceSchedule { "[${schedule.id}]: cancel $schedule" }
                        cancel(schedule.id, true)
                    }
                }
            }
            workManager.pruneWork()
        }

        fun cancel(scheduleId: Long, periodic: Boolean = true) {
            traceSchedule { "[$scheduleId]: Canceling" }
            get<WorkManager>(WorkManager::class.java)
                .cancelUniqueWork(
                    "${if (periodic) SCHEDULE_WORK else SCHEDULE_ONETIME}$scheduleId"
                )
        }
    }
}