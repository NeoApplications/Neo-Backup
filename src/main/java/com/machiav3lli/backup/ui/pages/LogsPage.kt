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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.machiav3lli.backup.data.entity.Log
import com.machiav3lli.backup.ui.compose.blockBorderBottom
import com.machiav3lli.backup.ui.compose.component.LogRecycler
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.TopBar
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.GearSix
import com.machiav3lli.backup.ui.navigation.NavItem
import com.machiav3lli.backup.utils.extensions.koinNeoViewModel
import com.machiav3lli.backup.viewmodels.LogsVM
import kotlinx.collections.immutable.toPersistentList

@Composable
fun LogsPage(navigateUp: () -> Unit) {

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                title = stringResource(id = NavItem.Logs.title),
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
        Logs(modifier = Modifier.padding(paddingValues))
    }
}

@Composable
fun Logs(modifier: Modifier = Modifier, viewModel: LogsVM = koinNeoViewModel()) {

    val logs = remember(viewModel) { viewModel.logsList }

    LaunchedEffect(viewModel) {
        viewModel.refreshList()
    }

    LogRecycler(
        modifier = modifier
            .blockBorderBottom()
            .fillMaxSize(),
        logsList = logs.sortedByDescending(Log::logDate).toPersistentList(),
        onShare = { viewModel.shareLog(it, pref_shareAsFile.value) },
        onDelete = { viewModel.deleteLog(it) }
    )
}