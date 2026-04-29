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
package com.machiav3lli.backup.ui.pages

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.machiav3lli.backup.DialogMode
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.Permission
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.blockBorderBottom
import com.machiav3lli.backup.ui.compose.component.PermissionItem
import com.machiav3lli.backup.ui.dialogs.ActionsDialogUI
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.utils.SystemUtils.packageName
import com.machiav3lli.backup.utils.checkBatteryOptimization
import com.machiav3lli.backup.utils.checkUsageStatsPermission
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.getStoragePermission
import com.machiav3lli.backup.utils.hasStoragePermissions
import com.machiav3lli.backup.utils.isStorageDirSetAndOk
import com.machiav3lli.backup.utils.requireCallLogsPermission
import com.machiav3lli.backup.utils.requireContactsPermission
import com.machiav3lli.backup.utils.requireSMSMMSPermission
import com.machiav3lli.backup.utils.requireStorageLocation
import com.machiav3lli.backup.utils.setBackupDir
import com.machiav3lli.backup.utils.specialBackupsEnabled
import org.koin.compose.koinInject
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingPermsPage(
    powerManager: PowerManager = koinInject(),
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val mainActivity = LocalActivity.current as NeoActivity
    val openDialog = remember { mutableStateOf(false) }
    val dialogProp: MutableState<DialogMode> = remember {
        mutableStateOf(DialogMode.NONE)
    }

    val permissionsList = remember {
        mutableStateMapOf<Permission, () -> Unit>()
    }

    val standardPermissions = remember {
        listOfNotNull(
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                && specialBackupsEnabled
            ) Triple(
                Permission.SMSMMS,
                listOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.RECEIVE_MMS,
                    Manifest.permission.RECEIVE_WAP_PUSH
                ),
                DialogMode.PERMISSION_SMS_MMS
            ) else null,
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                && specialBackupsEnabled
            ) Triple(
                Permission.CallLogs,
                listOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG
                ),
                DialogMode.PERMISSION_CALL_LOGS
            ) else null,
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                && specialBackupsEnabled
            ) Triple(
                Permission.Contacts,
                listOf(Manifest.permission.READ_CONTACTS),
                DialogMode.PERMISSION_CONTACTS
            ) else null,
            Triple(
                Permission.PostNotifications,
                if (Android.minSDK(Build.VERSION_CODES.TIRAMISU)) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else emptyList(),
                DialogMode.NONE
            )
        )
    }

    // standard permissions' states
    val permissionStates = standardPermissions.associate { (permissionType, permissions, _) ->
        permissionType to permissions.mapNotNull { permission ->
            if (permission.isNotEmpty()) {
                rememberPermissionState(permission)
            } else null
        }
    }

    val askForDirectory =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                result.data?.let {
                    val uri = it.data ?: return@rememberLauncherForActivityResult
                    val flags = it.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    setBackupDir(uri)
                }
            }
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsList.apply {
                    if (!context.hasStoragePermissions && none { it.key == Permission.StorageAccess })
                        set(Permission.StorageAccess) { mainActivity.getStoragePermission() }

                    if (!context.isStorageDirSetAndOk && none { it.key == Permission.StorageLocation })
                        set(Permission.StorageLocation) {
                            requireStorageLocation(askForDirectory) {
                                dialogProp.value = DialogMode.NO_SAF
                                openDialog.value = true
                            }
                        }

                    if (!context.checkBatteryOptimization(powerManager)
                        && none { it.key == Permission.BatteryOptimization }
                    )
                        set(Permission.BatteryOptimization) {
                            dialogProp.value = DialogMode.PERMISSION_BATTERY_OPTIMIZATION
                            openDialog.value = true
                        }

                    if (!context.checkUsageStatsPermission && none { it.key == Permission.UsageStats })
                        set(Permission.UsageStats) {
                            dialogProp.value = DialogMode.PERMISSION_USAGE_STATS
                            openDialog.value = true
                        }

                    // Handle standard permissions
                    standardPermissions.forEach { (permissionType, permissions, dialogMode) ->
                        val permissionState = permissionStates[permissionType]
                        if (permissions.isNotEmpty() &&
                            permissionState?.any { !it.status.isGranted } == true &&
                            none { it.key == permissionType }
                        ) {
                            set(permissionType) {
                                if (dialogMode != DialogMode.NONE) {
                                    dialogProp.value = dialogMode
                                    openDialog.value = true
                                } else {
                                    permissionState.forEach { it.launchPermissionRequest() }
                                }
                            }
                        }
                    }

                    // Remove granted permissions
                    if (context.hasStoragePermissions) remove(Permission.StorageAccess)
                    if (context.isStorageDirSetAndOk) remove(Permission.StorageLocation)
                    if (context.checkBatteryOptimization(powerManager)) remove(Permission.BatteryOptimization)
                    if (context.checkUsageStatsPermission) remove(Permission.UsageStats)

                    standardPermissions.forEach { (permissionType, permissions, _) ->
                        val permissionState = permissionStates[permissionType]
                        if (permissions.isNotEmpty() &&
                            permissionState?.all { it.status.isGranted } == true
                        ) remove(permissionType)
                    }
                }

                if (permissionsList.isEmpty()) onNext()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    })

    LazyColumn(
        modifier = Modifier
            .blockBorderBottom()
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(permissionsList.toList(), key = { it.first.nameId }) { (permission, onClick) ->
            PermissionItem(
                item = permission,
                modifier = Modifier.animateItem(),
                onClick = onClick
            )
        }
    }

    if (openDialog.value) BaseDialog(onDismiss = { openDialog.value = false }) {
        dialogProp.value.let { dialogMode ->
            when (dialogMode) {
                DialogMode.NO_SAF
                     -> ActionsDialogUI(
                    titleText = stringResource(R.string.no_file_manager_title),
                    messageText = stringResource(R.string.no_file_manager_message),
                    onDismiss = { openDialog.value = false },
                    primaryText = stringResource(R.string.dialogOK),
                    primaryAction = {
                        mainActivity.finishAffinity()
                    },
                )

                DialogMode.PERMISSION_USAGE_STATS
                     -> ActionsDialogUI(
                    titleText = stringResource(R.string.grant_usage_access_title),
                    messageText = stringResource(R.string.grant_usage_access_message),
                    onDismiss = { openDialog.value = false },
                    primaryText = stringResource(R.string.dialog_approve),
                    primaryAction = {
                        mainActivity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                )

                DialogMode.PERMISSION_SMS_MMS
                     -> ActionsDialogUI(
                    titleText = stringResource(R.string.smsmms_permission_title),
                    messageText = stringResource(R.string.grant_smsmms_message),
                    onDismiss = { openDialog.value = false },
                    primaryText = stringResource(R.string.dialog_approve),
                    primaryAction = {
                        mainActivity.requireSMSMMSPermission()
                    },
                )

                DialogMode.PERMISSION_CALL_LOGS
                     -> ActionsDialogUI(
                    titleText = stringResource(R.string.calllogs_permission_title),
                    messageText = stringResource(R.string.grant_calllogs_message),
                    onDismiss = { openDialog.value = false },
                    primaryText = stringResource(R.string.dialog_approve),
                    primaryAction = {
                        mainActivity.requireCallLogsPermission()
                    },
                )

                DialogMode.PERMISSION_CONTACTS
                     -> ActionsDialogUI(
                    titleText = stringResource(R.string.contacts_permission_title),
                    messageText = stringResource(R.string.grant_contacts_message),
                    onDismiss = { openDialog.value = false },
                    primaryText = stringResource(R.string.dialog_approve),
                    primaryAction = {
                        mainActivity.requireContactsPermission()
                    },
                )

                DialogMode.PERMISSION_BATTERY_OPTIMIZATION
                     -> ActionsDialogUI(
                    titleText = stringResource(R.string.ignore_battery_optimization_title),
                    messageText = stringResource(R.string.ignore_battery_optimization_message),
                    onDismiss = { openDialog.value = false },
                    primaryText = stringResource(R.string.dialog_approve),
                    primaryAction = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:$packageName")
                        try {
                            mainActivity.startActivity(intent)
                            persist_ignoreBatteryOptimization.value =
                                powerManager.isIgnoringBatteryOptimizations(packageName) == true
                        } catch (e: ActivityNotFoundException) {
                            Timber.w(e, "Ignore battery optimizations not supported")
                            Toast.makeText(
                                context,
                                R.string.ignore_battery_optimization_not_supported,
                                Toast.LENGTH_LONG
                            ).show()
                            persist_ignoreBatteryOptimization.value = true
                        }
                    },
                )

                else -> {}
            }
        }
    }
}
