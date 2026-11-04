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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.machiav3lli.backup.R
import com.machiav3lli.backup.manager.handler.showNotification
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.blockBorderBottom
import com.machiav3lli.backup.ui.compose.component.ExportedScheduleRecycler
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.TopBar
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.CalendarPlus
import com.machiav3lli.backup.ui.compose.icons.phosphor.GearSix
import com.machiav3lli.backup.ui.navigation.NavItem
import com.machiav3lli.backup.utils.extensions.koinNeoViewModel
import com.machiav3lli.backup.viewmodels.ExportsVM

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SchedulesExportsPage(viewModel: ExportsVM = koinNeoViewModel(), navigateUp: () -> Unit) {
    val context = LocalContext.current
    val res = LocalResources.current
    val exports by viewModel.exportsList.collectAsState()

    SideEffect {
        viewModel.refreshList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::exportSchedules) {
                Icon(
                    imageVector = Phosphor.CalendarPlus,
                    contentDescription = stringResource(id = R.string.dialog_export_schedules)
                )
                Text(text = stringResource(id = R.string.dialog_export_schedules))
            }
        },
        topBar = {
            TopBar(
                title = stringResource(id = NavItem.Exports.title),
                navigationAction = {
                    RoundButton(
                        icon = Phosphor.GearSix,
                        description = stringResource(id = android.R.string.cancel),
                        onClick = navigateUp,
                    )
                }
            )
        }
    ) { paddingValues ->
        ExportedScheduleRecycler(
            modifier = Modifier
                .padding(paddingValues)
                .blockBorderBottom()
                .fillMaxSize(),
            schedsList = exports,
            onImport = {
                viewModel.importSchedule(it) { name, id ->
                    showNotification(
                        context, NeoActivity::class.java, id,
                        res.getString(R.string.sched_imported), name, false
                    )
                }
            },
            onDelete = { viewModel.deleteExport(it) }
        )
    }
}