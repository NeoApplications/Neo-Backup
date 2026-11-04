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
package com.machiav3lli.backup.ui.pages

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.ALT_MODE_APK
import com.machiav3lli.backup.ALT_MODE_BOTH
import com.machiav3lli.backup.ALT_MODE_DATA
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.BooleanPref
import com.machiav3lli.backup.data.entity.ColoringState
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.preferences.traceCompose
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.component.ActionButton
import com.machiav3lli.backup.ui.compose.component.ActionChip
import com.machiav3lli.backup.ui.compose.component.BatchPackageRecycler
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.StateChip
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArchiveTray
import com.machiav3lli.backup.ui.compose.icons.phosphor.ClockCounterClockwise
import com.machiav3lli.backup.ui.compose.icons.phosphor.DiamondsFour
import com.machiav3lli.backup.ui.compose.icons.phosphor.FloppyDisk
import com.machiav3lli.backup.ui.compose.icons.phosphor.FunnelSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.GameController
import com.machiav3lli.backup.ui.compose.icons.phosphor.HardDrives
import com.machiav3lli.backup.ui.compose.icons.phosphor.Nut
import com.machiav3lli.backup.ui.compose.icons.phosphor.PlayCircle
import com.machiav3lli.backup.ui.compose.icons.phosphor.Prohibit
import com.machiav3lli.backup.ui.compose.icons.phosphor.ProhibitInset
import com.machiav3lli.backup.ui.compose.icons.phosphor.ShieldCheckered
import com.machiav3lli.backup.ui.compose.theme.ColorAPK
import com.machiav3lli.backup.ui.compose.theme.ColorData
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.BatchActionDialogUI
import com.machiav3lli.backup.ui.dialogs.GlobalBlockListDialogUI
import com.machiav3lli.backup.ui.navigation.NavItem
import com.machiav3lli.backup.utils.altModeToMode
import com.machiav3lli.backup.viewmodels.BatchVM

@Composable
fun BatchPage(
    viewModel: BatchVM,
    backupBoolean: Boolean
) {
    val neoActivity = LocalActivity.current as NeoActivity
    val state by viewModel.state.collectAsState()
    val openBatchDialog = remember { mutableStateOf(false) }
    val openBlocklist = retain { mutableStateOf(false) }

    val allApkChecked by remember(state.filteredPackages, viewModel.apkBackupCheckedList) {
        derivedStateOf {
            viewModel.apkBackupCheckedList.filterValues { it != -1 }.size == state.filteredPackages
                .filter { !it.isSpecial && (backupBoolean || it.latestBackup?.hasApk == true) }
                .size
        }
    }
    val allDataChecked by remember(state.filteredPackages, viewModel.dataBackupCheckedList) {
        derivedStateOf {
            viewModel.dataBackupCheckedList.filterValues { it != -1 }.size == state.filteredPackages
                .filter { backupBoolean || it.latestBackup?.hasData == true }
                .size
        }
    }

    traceCompose {
        "${if (backupBoolean) "BackupPage" else "RestorePage"} filtered=${
            state.filteredPackages.size
        } updated=${
            state.updatedPackages.size
        } selection=${
            state.selection.size
        } language=${pref_languages.value}"
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChip(
                        modifier = Modifier.weight(1f),
                        icon = Phosphor.Prohibit,
                        text = stringResource(id = R.string.sched_blocklist),
                        positive = false,
                        fullWidth = true,
                    ) {
                        openBlocklist.value = true
                    }
                    ActionChip(
                        modifier = Modifier.weight(1f),
                        icon = Phosphor.FunnelSimple,
                        text = stringResource(id = R.string.sort_filter),
                        positive = true,
                        fullWidth = true,
                    ) {
                        neoActivity.navigateSortFilterSheet(
                            if (backupBoolean) NavItem.Backup
                            else NavItem.Restore
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Card(
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row {
                        StateChip(
                            icon = Phosphor.DiamondsFour,
                            text = stringResource(id = R.string.all_apk),
                            checked = allApkChecked,
                            color = ColorAPK,
                        ) { checkBoolean ->
                            when {
                                checkBoolean -> state.filteredPackages
                                    .filter { (backupBoolean && !it.isSpecial) || it.latestBackup?.hasApk == true }
                                    .map(Package::packageName)
                                    .forEach {
                                        viewModel.apkBackupCheckedList[it] = 0
                                    }

                                else         -> state.filteredPackages
                                    .filter { backupBoolean || it.latestBackup?.hasApk == true }
                                    .map(Package::packageName)
                                    .forEach {
                                        viewModel.apkBackupCheckedList[it] = -1
                                    }
                            }
                        }
                        StateChip(
                            icon = Phosphor.HardDrives,
                            text = stringResource(id = R.string.all_data),
                            checked = allDataChecked,
                            color = ColorData,
                        ) { checkBoolean ->
                            when {
                                checkBoolean -> state.filteredPackages
                                    .filter { backupBoolean || it.latestBackup?.hasData == true }
                                    .map(Package::packageName)
                                    .forEach {
                                        viewModel.dataBackupCheckedList[it] = 0
                                    }

                                else         -> state.filteredPackages
                                    .filter { backupBoolean || it.latestBackup?.hasData == true }
                                    .map(Package::packageName)
                                    .forEach {
                                        viewModel.dataBackupCheckedList[it] = -1
                                    }
                            }
                        }
                    }
                    RoundButton(icon = Phosphor.Nut) {
                        neoActivity.navigateBatchPrefsSheet(backupBoolean)
                    }
                    ActionButton(
                        text = stringResource(id = if (backupBoolean) R.string.backup else R.string.restore),
                        icon = if (backupBoolean) Phosphor.ArchiveTray
                        else Phosphor.ClockCounterClockwise,
                        modifier = Modifier.weight(1f),
                        coloring = ColoringState.Positive,
                    ) {
                        if (viewModel.apkBackupCheckedList.filterValues { it != -1 }.isNotEmpty()
                            || viewModel.dataBackupCheckedList.filterValues { it != -1 }
                                .isNotEmpty()
                        ) openBatchDialog.value = true
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BatchPackageRecycler(
                modifier = Modifier
                    .weight(1f, true)
                    .fillMaxSize(),
                productsList = state.filteredPackages,
                restore = !backupBoolean,
                apkBackupCheckedList = viewModel.apkBackupCheckedList,
                dataBackupCheckedList = viewModel.dataBackupCheckedList,
                onBackupApkClick = { packageName: String, b: Boolean, i: Int ->
                    if (b) viewModel.apkBackupCheckedList[packageName] = i
                    else if (viewModel.apkBackupCheckedList[packageName] == i)
                        viewModel.apkBackupCheckedList[packageName] = -1
                },
                onBackupDataClick = { packageName: String, b: Boolean, i: Int ->
                    if (b) viewModel.dataBackupCheckedList[packageName] = i
                    else if (viewModel.dataBackupCheckedList[packageName] == i)
                        viewModel.dataBackupCheckedList[packageName] = -1
                },
            ) { item, checkApk, checkData ->
                when (checkApk) {
                    true -> viewModel.apkBackupCheckedList[item.packageName] = 0
                    else -> viewModel.apkBackupCheckedList[item.packageName] = -1
                }
                when (checkData) {
                    true -> viewModel.dataBackupCheckedList[item.packageName] = 0
                    else -> viewModel.dataBackupCheckedList[item.packageName] = -1
                }
            }
        }

        if (openBlocklist.value) BaseDialog(onDismiss = { openBlocklist.value = false }) {
            GlobalBlockListDialogUI(
                currentBlocklist = state.blocklist,
                openDialogCustom = openBlocklist,
            ) { newSet ->
                viewModel.updateBlocklist(newSet)
            }
        }
        if (openBatchDialog.value) BaseDialog(onDismiss = { openBatchDialog.value = false }) {
            val selectedApk = viewModel.apkBackupCheckedList.filterValues { it != -1 }
            val selectedData = viewModel.dataBackupCheckedList.filterValues { it != -1 }
            val selectedPackageNames = selectedApk.keys.plus(selectedData.keys).distinct()

            BatchActionDialogUI(
                backupBoolean = backupBoolean,
                selectedPackageInfos = state.filteredPackages
                    .filter { it.packageName in selectedPackageNames }
                    .map(Package::packageInfo),
                selectedApk = selectedApk,
                selectedData = selectedData,
                onDismiss = { openBatchDialog.value = false },
            ) {
                if (pref_singularBackupRestore.value && !backupBoolean) neoActivity.startBatchRestoreAction(
                    selectedPackageNames = selectedPackageNames,
                    selectedApk = selectedApk,
                    selectedData = selectedData,
                )
                else neoActivity.startBatchAction(
                    backupBoolean,
                    selectedPackageNames = selectedPackageNames,
                    selectedModes = selectedPackageNames.map { pn ->
                        altModeToMode(
                            when {
                                selectedData[pn] == 0 && selectedApk[pn] == 0 -> ALT_MODE_BOTH
                                selectedData[pn] == 0                         -> ALT_MODE_DATA
                                else                                          -> ALT_MODE_APK
                            }, backupBoolean
                        )
                    }
                )
            }
        }
    }
}

val pref_backupDeviceProtectedData = BooleanPref(
    key = "srv-bkp.backupDeviceProtectedData",
    titleId = R.string.prefs_deviceprotecteddata,
    summaryId = R.string.prefs_deviceprotecteddata_summary,
    icon = Phosphor.ShieldCheckered,
    defaultValue = true
)

val pref_backupExternalData = BooleanPref(
    key = "srv-bkp.backupExternalData",
    titleId = R.string.prefs_externaldata,
    summaryId = R.string.prefs_externaldata_summary,
    icon = Phosphor.FloppyDisk,
    defaultValue = true
)

val pref_backupObbData = BooleanPref(
    key = "srv-bkp.backupObbData",
    titleId = R.string.prefs_obbdata,
    summaryId = R.string.prefs_obbdata_summary,
    icon = Phosphor.GameController,
    defaultValue = true
)

val pref_backupMediaData = BooleanPref(
    key = "srv-bkp.backupMediaData",
    titleId = R.string.prefs_mediadata,
    summaryId = R.string.prefs_mediadata_summary,
    icon = Phosphor.PlayCircle,
    defaultValue = true
)

val pref_backupNoBackupData = BooleanPref(
    key = "srv-bkp.backupNoBackupData",
    titleId = R.string.prefs_nobackupdata,
    summaryId = R.string.prefs_nobackupdata_summary,
    icon = Phosphor.ProhibitInset,
    defaultValue = false,
    onChanged = { NeoApp.assets.updateExcludeFiles() },
)

val pref_backupCache = BooleanPref(
    key = "srv-bkp.backupCache",
    titleId = R.string.prefs_backupcache,
    summaryId = R.string.prefs_backupcache_summary,
    icon = Phosphor.Prohibit,
    defaultValue = false
)


val pref_restoreDeviceProtectedData = BooleanPref(
    key = "srv-rst.restoreDeviceProtectedData",
    titleId = R.string.prefs_deviceprotecteddata_rst,
    summaryId = R.string.prefs_deviceprotecteddata_rst_summary,
    icon = Phosphor.ShieldCheckered,
    defaultValue = true
)

val pref_restoreExternalData = BooleanPref(
    key = "srv-rst.restoreExternalData",
    titleId = R.string.prefs_externaldata_rst,
    summaryId = R.string.prefs_externaldata_rst_summary,
    icon = Phosphor.FloppyDisk,
    defaultValue = true
)

val pref_restoreObbData = BooleanPref(
    key = "srv-rst.restoreObbData",
    titleId = R.string.prefs_obbdata_rst,
    summaryId = R.string.prefs_obbdata_rst_summary,
    icon = Phosphor.GameController,
    defaultValue = true
)

val pref_restoreMediaData = BooleanPref(
    key = "srv-rst.restoreMediaData",
    titleId = R.string.prefs_mediadata_rst,
    summaryId = R.string.prefs_mediadata_rst_summary,
    icon = Phosphor.PlayCircle,
    defaultValue = true
)

val pref_restoreNoBackupData = BooleanPref(
    key = "srv-rst.restoreNoBackupData",
    titleId = R.string.prefs_nobackupdata_rst,
    summaryId = R.string.prefs_nobackupdata_rst_summary,
    icon = Phosphor.ProhibitInset,
    defaultValue = false,
    onChanged = { NeoApp.assets.updateExcludeFiles() },
)

val pref_restoreCache = BooleanPref(
    key = "srv-rst.restoreCache",
    titleId = R.string.prefs_restorecache,
    summaryId = R.string.prefs_restorecache_summary,
    icon = Phosphor.Prohibit,
    defaultValue = false
)