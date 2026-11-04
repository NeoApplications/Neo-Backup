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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.machiav3lli.backup.DialogMode
import com.machiav3lli.backup.ICON_SIZE_SMALL
import com.machiav3lli.backup.MODE_UNSET
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.preferences.traceCompose
import com.machiav3lli.backup.manager.tasks.ScheduleWork
import com.machiav3lli.backup.ui.compose.component.ScheduleRecycler
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.CalendarPlus
import com.machiav3lli.backup.ui.dialogs.ActionsDialogUI
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.utils.extensions.koinNeoViewModel
import com.machiav3lli.backup.utils.getStartScheduleMessage
import com.machiav3lli.backup.utils.specialBackupsEnabled
import com.machiav3lli.backup.viewmodels.SchedulesVM
import kotlinx.coroutines.launch
import okhttp3.internal.toLongOrDefault

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SchedulerPage(viewModel: SchedulesVM = koinNeoViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openDialog = remember { mutableStateOf(false) }
    val dialogProps: MutableState<Pair<DialogMode, Schedule>> = remember {
        mutableStateOf(Pair(DialogMode.NONE, Schedule()))
    }

    val state by viewModel.state.collectAsState()
    val paneNavigator = rememberListDetailPaneScaffoldNavigator<Any>()
    val scheduleSheetId = remember { mutableLongStateOf(-1L) }

    traceCompose {
        "SchedulerPage schedules=${
            state.enabledSchedules.size + state.disabledSchedules.size
        } blocklist=${
            state.blocklist.size
        } language=${pref_languages.value}"
    }

    NavigableListDetailPaneScaffold(
        navigator = paneNavigator,
        listPane = {
            AnimatedPane {
                Scaffold(
                    containerColor = Color.Transparent,
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            text = { Text(stringResource(id = R.string.sched_add)) },
                            icon = {
                                Icon(
                                    modifier = Modifier.size(ICON_SIZE_SMALL),
                                    imageVector = Phosphor.CalendarPlus,
                                    contentDescription = stringResource(id = R.string.sched_add)
                                )
                            },
                            onClick = { viewModel.addSchedule(specialBackupsEnabled) }
                        )
                    }
                ) { _ ->
                    ScheduleRecycler(
                        enabledSchedules = state.enabledSchedules,
                        disabledSchedules = state.disabledSchedules,
                        onClick = { item ->
                            scope.launch {
                                paneNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item.id)
                            }
                        },
                        onRun = { item ->
                            dialogProps.value = Pair(DialogMode.SCHEDULE_RUN, item)
                            openDialog.value = true
                        },
                        onCheckChanged = { item: Schedule, b: Boolean ->
                            viewModel.updateSchedule(
                                item.copy(enabled = b),
                                true,
                            )
                        }
                    )
                }
            }
        },
        detailPane = {
            scheduleSheetId.longValue = paneNavigator.currentDestination
                ?.takeIf { it.pane == this.paneRole }?.contentKey
                .toString().toLongOrDefault(-1L)

            scheduleSheetId.longValue.takeIf { it != -1L }?.let { id ->
                AnimatedPane {
                    SchedulePage(
                        scheduleId = id,
                        onDismiss = {
                            scope.launch {
                                paneNavigator.navigateBack()
                            }
                        }
                    )
                }
            }
        }
    )

    if (openDialog.value) BaseDialog(onDismiss = { openDialog.value = false }) {
        dialogProps.value.let { (dialogMode, schedule) ->
            when (dialogMode) {
                DialogMode.SCHEDULE_RUN
                    -> ActionsDialogUI(
                    titleText = "${schedule.name}: ${stringResource(R.string.sched_activateButton)}?",
                    messageText = context.getStartScheduleMessage(
                        schedule = schedule,
                        globalBlockList = state.blocklist,
                        tagsMap = state.tagsMap,
                        allTags = state.tagsList,
                    ),
                    onDismiss = { openDialog.value = false },
                    primaryText = stringResource(R.string.dialogOK),
                    primaryAction = {
                        if (schedule.mode != MODE_UNSET)
                            ScheduleWork.enqueueImmediate(schedule)
                    },
                )

                else -> {}
            }
        }
    }
}
