/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2025  Antonios Hazim
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
package com.machiav3lli.backup.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.machiav3lli.backup.ACTION_RUN_SCHEDULE_SHORTCUT
import com.machiav3lli.backup.ALT_MODE_APK
import com.machiav3lli.backup.ALT_MODE_BOTH
import com.machiav3lli.backup.ALT_MODE_DATA
import com.machiav3lli.backup.EXTRA_SCHEDULE_ID
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.RESCUE_NAV
import com.machiav3lli.backup.data.entity.BackupsCache
import com.machiav3lli.backup.data.repository.AppExtrasRepository
import com.machiav3lli.backup.data.repository.BlocklistRepository
import com.machiav3lli.backup.data.repository.ExportsRepository
import com.machiav3lli.backup.data.repository.PackageRepository
import com.machiav3lli.backup.data.repository.ScheduleRepository
import com.machiav3lli.backup.data.repository.SelectionsRepository
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.unexpectedException
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.WorkHandler
import com.machiav3lli.backup.manager.tasks.AppActionWork
import com.machiav3lli.backup.manager.tasks.ScheduleWork
import com.machiav3lli.backup.ui.compose.ObservedEffect
import com.machiav3lli.backup.ui.compose.component.DevTools
import com.machiav3lli.backup.ui.compose.theme.AppTheme
import com.machiav3lli.backup.ui.dialogs.ActionsDialogUI
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.DialogKey
import com.machiav3lli.backup.ui.dialogs.GlobalBlockListDialogUI
import com.machiav3lli.backup.ui.navigation.AppNavDisplay
import com.machiav3lli.backup.ui.navigation.NavItem
import com.machiav3lli.backup.ui.navigation.NavRoute
import com.machiav3lli.backup.ui.navigation.navigateUnique
import com.machiav3lli.backup.ui.pages.RootMissing
import com.machiav3lli.backup.ui.pages.SplashPage
import com.machiav3lli.backup.ui.pages.persist_beenWelcomed
import com.machiav3lli.backup.ui.pages.persist_skippedEncryptionCounter
import com.machiav3lli.backup.ui.pages.pref_appTheme
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.TraceUtils.classAndId
import com.machiav3lli.backup.utils.allPermissionsGranted
import com.machiav3lli.backup.utils.altModeToMode
import com.machiav3lli.backup.utils.isBiometricLockAvailable
import com.machiav3lli.backup.utils.isBiometricLockEnabled
import com.machiav3lli.backup.utils.isDarkTheme
import com.machiav3lli.backup.utils.isDeviceLockEnabled
import com.machiav3lli.backup.utils.isEncryptionEnabled
import com.machiav3lli.backup.viewmodels.ActivityVM
import com.machiav3lli.backup.viewmodels.AppVM
import com.machiav3lli.backup.viewmodels.BackupBatchVM
import com.machiav3lli.backup.viewmodels.ExportsVM
import com.machiav3lli.backup.viewmodels.HomeVM
import com.machiav3lli.backup.viewmodels.LogsVM
import com.machiav3lli.backup.viewmodels.RestoreBatchVM
import com.machiav3lli.backup.viewmodels.ScheduleVM
import com.machiav3lli.backup.viewmodels.SchedulesVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber

@Composable
fun Rescue() {
    AppTheme {
        val expanded = remember { mutableStateOf(true) }
        DevTools(expanded = expanded)
    }
}

class NeoActivity : BaseActivity() {

    private val mScope: CoroutineScope = MainScope()
    private lateinit var navStack: NavBackStack<NavRoute>
    var freshStart: Boolean = true

    private lateinit var openDialog: MutableState<Boolean>
    private lateinit var dialogKey: MutableState<DialogKey?>

    private val viewModel: ActivityVM by viewModel()

    object LockNavigationState {
        var intendedDestination: NavRoute? = null
    }

    private val lockNavigationState = LockNavigationState

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        val mainChanged = (this != NeoApp.mainSaved.get())
        NeoApp.main = this

        freshStart = (savedInstanceState == null)   //TODO use some lifecycle method?

        Timber.w(
            listOfNotNull(
                if (freshStart) "fresh start" else null,
                if (mainChanged && (!freshStart || (NeoApp.mainSaved.get() != null)))
                    "main changed (was ${classAndId(NeoApp.mainSaved.get())})"
                else
                    null,
            ).joinToString(", ")
        )

        super.onCreate(savedInstanceState)

        //TODO wech begin ??? or is this necessary with resume or similar?

        //TODO here or in MainPage? MainPage seems to be weird at least for each recomposition
        NeoApp.appsSuspendedChecked = false

        //if (pref_catchUncaughtException.value) {               //TODO wech ???
        //    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        //        try {
        //            Timber.i("\n\n" + "=".repeat(60))
        //            LogsHandler.unexpectedException(e)
        //            LogsHandler.logErrors("uncaught: ${e.message}")
        //            if (pref_uncaughtExceptionsJumpToPreferences.value) {
        //                context.restartApp(RESCUE_NAV)
        //            }
        //            object : Thread() {
        //                override fun run() {
        //                    Looper.prepare()
        //                    Looper.loop()
        //                }
        //            }.start()
        //        } catch (_: Throwable) {
        //            // ignore
        //        } finally {
        //            exitProcess(2)
        //        }
        //    }
        //}

        //TODO wech Shell.getShell() // should be handled in ShellHandler

        //TODO wech end ???

        setContent {
            AppTheme {
                SplashPage()
            }
            navStack = rememberNavBackStack(NavRoute.Permissions) as NavBackStack<NavRoute>
        }

        if (doIntent(intent, "beforeContent"))
            return

        if (!ShellHandler.checkRootEquivalent()) {
            setContent {
                AppTheme {
                    RootMissing(this)
                }
            }
            return
        }

        setContent {

            navStack = rememberNavBackStack(NavRoute.Permissions) as NavBackStack<NavRoute>

            DisposableEffect(pref_appTheme.value) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkTheme },
                )
                onDispose {}
            }

            AppTheme {
                openDialog = remember { mutableStateOf(false) }
                dialogKey = remember { mutableStateOf(null) }
                val openBlocklist = remember { mutableStateOf(false) }
                val blocklist by viewModel.blockList.collectAsState()

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    ObservedEffect {
                        resumeMain()
                    }

                    Box {
                        AppNavDisplay(
                            backStack = navStack,
                            modifier = Modifier.imePadding(),
                        )

                        if (openBlocklist.value)
                            BaseDialog(onDismiss = { openBlocklist.value = false }) {
                                GlobalBlockListDialogUI(
                                    currentBlocklist = blocklist,
                                    openDialogCustom = openBlocklist,
                                ) { newSet ->
                                    viewModel.updateBlocklist(newSet)
                                }
                            }
                    }
                }

                if (openDialog.value) {
                    BaseDialog(onDismiss = { openDialog.value = false }) {
                        when (dialogKey.value) {
                            is DialogKey.Encryption -> {
                                ActionsDialogUI(
                                    titleText = stringResource(id = R.string.enable_encryption_title),
                                    messageText = stringResource(id = R.string.enable_encryption_message),
                                    onDismiss = { openDialog.value = false },
                                    primaryText = stringResource(id = R.string.dialog_approve),
                                    primaryAction = {
                                        openDialog.value = false
                                        moveTo(NavRoute.Prefs(1))
                                    }
                                )
                            }

                            is DialogKey.Error      -> {
                                val message = (dialogKey.value as DialogKey.Error).message
                                ActionsDialogUI(
                                    titleText = stringResource(id = R.string.errorDialogTitle),
                                    messageText = message,
                                    onDismiss = { openDialog.value = false },
                                    primaryText = stringResource(id = R.string.dialogSave),
                                    primaryAction = { LogsHandler.logErrors(message) },
                                    secondaryText = stringResource(id = R.string.dialogOK)
                                )
                            }

                            else                    -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        NeoApp.main = this
        super.onResume()
    }

    override fun onDestroy() {
        NeoApp.mainSaved = NeoApp.mainRef
        NeoApp.main = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        doIntent(intent, "newIntent")
        super.onNewIntent(intent)
    }

    private fun doIntent(intent: Intent?, at: String): Boolean {
        if (intent == null) return false
        val command = intent.action
        val data = intent.data
        Timber.i("Main: command $command -> $data")
        when (at) {

            "beforeContent"             -> {
                when (command) {
                    null                         -> {
                        return false
                    }

                    Intent.ACTION_MAIN           -> {
                        if (data == null)
                            return false
                        when (data.toString()) {
                            RESCUE_NAV -> {
                                setContent {
                                    Rescue()
                                }
                                return true
                            }
                        }
                    }

                    ACTION_RUN_SCHEDULE_SHORTCUT -> {
                        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                        if (scheduleId != -1L) {
                            handleScheduleShortcut(scheduleId)
                        }
                    }

                    else                         -> {
                        return false
                    }
                }
            }

            "afterContent", "newIntent" -> {
                when (command) {
                    null               -> {
                        return false
                    }

                    Intent.ACTION_MAIN -> {
                        if (data == null)
                            return false
                        //moveTo(data.toString())
                        Timber.w("Received a newIntent with command:$command and didn't handle it!")
                    }

                    else               -> {
                        NeoApp.addInfoLogText("Main: command '$command'")
                    }
                }
            }

        }
        return false
    }

    private fun handleScheduleShortcut(scheduleId: Long) {
        mScope.launch {
            try {
                val scheduleRepo = get<ScheduleRepository>(ScheduleRepository::class.java)
                val schedule = scheduleRepo.getSchedule(scheduleId)
                if (schedule != null && schedule.enabled) {
                    Timber.i("Running schedule from shortcut: ${schedule.name}")
                    ScheduleWork.enqueueImmediate(schedule)
                    showSnackBar(getString(R.string.sched_activateButton) + ": ${schedule.name}")
                } else {
                    Timber.w("Schedule not found or disabled: $scheduleId")
                    showError(getString(R.string.schedule_not_found_or_disabled))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error running schedule from shortcut")
                showError(e.message)
            }
        }
    }

    fun showEncryptionDialog() {
        val dontShowAgain = isEncryptionEnabled()
        if (dontShowAgain) return
        val dontShowCounter = persist_skippedEncryptionCounter.value
        if (dontShowCounter > 30) return    // don't increment further (useless touching file)
        persist_skippedEncryptionCounter.value = dontShowCounter + 1
        if (dontShowCounter % 10 == 0) {
            showDialog(DialogKey.Encryption())
        }
    }

    fun updatePackage(packageName: String) {
        viewModel.updatePackage(packageName)
    }

    fun enableSpecials(enable: Boolean) {
        viewModel.onEnableSpecials(enable)
    }

    fun refreshPackagesAndBackups() {
        viewModel.refreshBackups()
    }

    fun showSnackBar(message: String) { // TODO reimplement this?
    }

    fun dismissSnackBar() {
    }

    fun showDialog(key: DialogKey) {
        dialogKey.value = key
        openDialog.value = true
    }

    fun showError(message: String?) {
        message?.let {
            showDialog(DialogKey.Error(it))
        }
    }

    // TODO track and reduce usage
    fun moveTo(destination: NavRoute) {
        try {
            persist_beenWelcomed.value = destination != NavRoute.Welcome
            if (!isOnLockScreen()) navStack.navigateUnique(destination)
        } catch (e: IllegalArgumentException) {
            Timber.e("cannot navigate to '$destination'")
        } catch (e: Throwable) {
            unexpectedException(e)
        }
    }

    fun whileShowingSnackBar(message: String, todo: () -> Unit) {
        runOnUiThread {
            showSnackBar(message)
        }
        todo()
        runOnUiThread {
            dismissSnackBar()
        }
    }

    fun startBatchAction(
        backupBoolean: Boolean,
        selectedPackageNames: List<String?>,
        selectedModes: List<Int>,
    ) {
        val now = SystemUtils.now
        val notificationId = now.toInt()
        val batchType = getString(if (backupBoolean) R.string.backup else R.string.restore)
        val batchName = WorkHandler.getBatchName(batchType, now)
        val workManager = get<WorkManager>(WorkManager::class.java)

        val selectedItems = selectedPackageNames
            .mapIndexed { i, packageName ->
                if (packageName.isNullOrEmpty()) null
                else Pair(packageName, selectedModes[i])
            }
            .filterNotNull()

        var errors = ""
        var resultsSuccess = true
        var counter = 0
        val worksList: MutableList<OneTimeWorkRequest> = mutableListOf()
        get<WorkHandler>(WorkHandler::class.java).beginBatch(batchName)
        selectedItems.forEach { (packageName, mode) ->

            val oneTimeWorkRequest =
                AppActionWork.Request(
                    packageName = packageName,
                    mode = mode,
                    backupBoolean = backupBoolean,
                    notificationId = notificationId,
                    batchName = batchName,
                    immediate = true
                )
            worksList.add(oneTimeWorkRequest)

            val oneTimeWorkLiveData = workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            oneTimeWorkLiveData.observeForever(
                object : Observer<WorkInfo?> {    //TODO WECH hg42
                    override fun onChanged(value: WorkInfo?) {
                        when (value?.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                counter += 1

                                val (succeeded, packageLabel, error) = AppActionWork.getOutput(value)
                                if (error.isNotEmpty()) errors =
                                    "$errors$packageLabel: ${      //TODO hg42 add to WorkHandler
                                        LogsHandler.handleErrorMessages(
                                            this@NeoActivity,
                                            error
                                        )
                                    }\n"

                                resultsSuccess = resultsSuccess and succeeded
                                updatePackage(packageName)
                                oneTimeWorkLiveData.removeObserver(this)
                            }

                            else                     -> {}
                        }
                    }
                }
            )
        }

        if (worksList.isNotEmpty()) {
            workManager
                .beginWith(worksList)
                .enqueue()
        }
    }

    fun startBatchRestoreAction(
        selectedPackageNames: List<String>,
        selectedApk: Map<String, Int>,
        selectedData: Map<String, Int>,
    ) {
        val now = SystemUtils.now
        val notificationId = now.toInt()
        val batchType = getString(R.string.restore)
        val batchName = WorkHandler.getBatchName(batchType, now)
        val workManager = get<WorkManager>(WorkManager::class.java)

        val selectedItems = buildList {
            selectedPackageNames.forEach { pn ->
                when {
                    selectedApk[pn] == selectedData[pn] && selectedApk[pn] != null -> add(
                        Triple(pn, selectedApk[pn]!!, altModeToMode(ALT_MODE_BOTH, false))
                    )

                    else                                                           -> {
                        if ((selectedApk[pn] ?: -1) != -1) add(
                            Triple(pn, selectedApk[pn]!!, altModeToMode(ALT_MODE_APK, false))
                        )
                        if ((selectedData[pn] ?: -1) != -1) add(
                            Triple(pn, selectedData[pn]!!, altModeToMode(ALT_MODE_DATA, false))
                        )
                    }
                }
            }
        }

        var errors = ""
        var resultsSuccess = true
        var counter = 0
        val worksList: MutableList<OneTimeWorkRequest> = mutableListOf()
        get<WorkHandler>(WorkHandler::class.java).beginBatch(batchName)
        selectedItems.forEach { (packageName, bi, mode) ->
            val oneTimeWorkRequest = AppActionWork.Request(
                packageName = packageName,
                mode = mode,
                backupBoolean = false,
                backupIndex = bi,
                notificationId = notificationId,
                batchName = batchName,
                immediate = true,
            )
            worksList.add(oneTimeWorkRequest)

            val oneTimeWorkLiveData = workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            oneTimeWorkLiveData.observeForever(
                object : Observer<WorkInfo?> {
                    override fun onChanged(value: WorkInfo?) {
                        when (value?.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                counter += 1

                                val (succeeded, packageLabel, error) = AppActionWork.getOutput(value)
                                if (error.isNotEmpty()) errors =
                                    "$errors$packageLabel: ${
                                        LogsHandler.handleErrorMessages(
                                            this@NeoActivity,
                                            error
                                        )
                                    }\n"

                                resultsSuccess = resultsSuccess and succeeded
                                oneTimeWorkLiveData.removeObserver(this)
                            }

                            else                     -> {}
                        }
                    }
                }
            )
        }

        if (worksList.isNotEmpty()) {
            workManager
                .beginWith(worksList)
                .enqueue()
        }
    }

    internal fun navigateSortFilterSheet(page: NavItem) {
        navStack.navigateUnique(NavRoute.SortFilter(page.destination))
    }

    internal fun navigateBatchPrefsSheet(backup: Boolean) {
        navStack.navigateUnique(NavRoute.BatchPrefs(backup))
    }

    fun resumeMain() {
        when {
            !persist_beenWelcomed.value
                 -> if (navStack.lastOrNull() != NavRoute.Welcome) {
                navStack.clear()
                navStack.navigateUnique(NavRoute.Welcome)
            }

            allPermissionsGranted && this::navStack.isInitialized
                 -> launchMain()

            else -> navStack.navigateUnique(NavRoute.Permissions)
        }
    }

    private fun launchMain() {
        when {
            shouldShowLock()   -> {
                val currentDestination = navStack.lastOrNull() ?: NavRoute.Main
                if (!isOnLockScreen()) lockNavigationState.intendedDestination = currentDestination
                navStack.navigateUnique(NavRoute.Lock)
                launchBiometricPrompt()
            }

            isOnSystemScreen() -> {
                navStack.navigateUnique(NavRoute.Main)
            }
        }
    }

    private fun launchBiometricPrompt() {
        try {
            val biometricPrompt = createBiometricPrompt()
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.prefs_biometriclock))
                .setConfirmationRequired(true)
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or (if (isBiometricLockEnabled()) BiometricManager.Authenticators.BIOMETRIC_WEAK else 0))
                .build()
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            navigateToStoredDestination()
        }
    }

    private fun createBiometricPrompt(): BiometricPrompt {
        return BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToStoredDestination()
                }
            })
    }

    private fun shouldShowLock() = isBiometricLockAvailable() && isDeviceLockEnabled()

    private fun isOnSystemScreen() = navStack.lastOrNull() in listOf(
        NavRoute.Welcome,
        NavRoute.Permissions,
        NavRoute.Lock,
    )

    private fun isOnLockScreen() =
        navStack.lastOrNull() == NavRoute.Lock

    private fun navigateToStoredDestination() {
        lockNavigationState.intendedDestination?.let { destination ->
            navStack.navigateUnique(destination)
            navStack.remove(NavRoute.Lock)
            lockNavigationState.intendedDestination = null
        }
    }
}

val viewModelsModule = module {
    singleOf(::BackupsCache)
    singleOf(::PackageRepository)
    singleOf(::BlocklistRepository)
    singleOf(::ScheduleRepository)
    singleOf(::AppExtrasRepository)
    singleOf(::ExportsRepository)
    single { SelectionsRepository(NeoApp.backupRoot) }
    viewModelOf(::ActivityVM)
    viewModelOf(::HomeVM)
    viewModelOf(::BackupBatchVM)
    viewModelOf(::RestoreBatchVM)
    viewModelOf(::SchedulesVM)
    viewModelOf(::ScheduleVM)
    viewModelOf(::AppVM)
    viewModelOf(::ExportsVM)
    viewModelOf(::LogsVM)
}