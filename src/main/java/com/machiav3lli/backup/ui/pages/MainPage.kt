/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2023  Antonios Hazim
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

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.LINK_KEEP_ANDROID_OPEN
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.manager.tasks.RefreshBackupsWorker
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.blockBorderBottom
import com.machiav3lli.backup.ui.compose.component.MainTopBar
import com.machiav3lli.backup.ui.compose.component.RefreshButton
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.devToolsSearch
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.CircleWavyWarning
import com.machiav3lli.backup.ui.compose.icons.phosphor.GearSix
import com.machiav3lli.backup.ui.compose.icons.phosphor.MagnifyingGlass
import com.machiav3lli.backup.ui.compose.icons.phosphor.Prohibit
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.GlobalBlockListDialogUI
import com.machiav3lli.backup.ui.navigation.NavItem
import com.machiav3lli.backup.ui.navigation.NavRoute
import com.machiav3lli.backup.ui.navigation.NeoNavigationSuiteScaffold
import com.machiav3lli.backup.ui.navigation.SlidePager
import com.machiav3lli.backup.utils.TraceUtils.traceBold
import com.machiav3lli.backup.utils.extensions.koinNeoViewModel
import com.machiav3lli.backup.utils.launchView
import com.machiav3lli.backup.viewmodels.BackupBatchVM
import com.machiav3lli.backup.viewmodels.HomeVM
import com.machiav3lli.backup.viewmodels.RestoreBatchVM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainPage(
    navigator: (NavRoute) -> Unit,
    homeVM: HomeVM = koinNeoViewModel(),
    backupVM: BackupBatchVM = koinNeoViewModel(),
    restoreVM: RestoreBatchVM = koinNeoViewModel(),
) {
    val activity = LocalActivity.current as NeoActivity
    val scope = rememberCoroutineScope()
    val pages = persistentListOf(
        NavItem.Home,
        NavItem.Backup,
        NavItem.Restore,
        NavItem.Scheduler,
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val currentPageIndex = remember { derivedStateOf { pagerState.currentPage } }
    val currentPage by remember { derivedStateOf { pages[currentPageIndex.value] } }
    val mainState by homeVM.state.collectAsState()
    val showBanner by remember(pref_ignoreKeepAndroidOpenNotice.value) {
        mutableStateOf(!pref_ignoreKeepAndroidOpenNotice.value)
    }

    BackHandler {
        activity.finishAffinity()
    }

    LaunchedEffect(homeVM) {
        launch(Dispatchers.IO) {
            if (activity.freshStart) {
                activity.freshStart = false
                traceBold { "******************** freshStart && Main ********************" }
                RefreshBackupsWorker.enqueueFullRefresh()
                NeoApp.startup = false // ensure backups no more reported as empty

                withContext(Dispatchers.Main) {
                    devToolsSearch.value =
                        TextFieldValue("")   //TODO hg42 hide implementation details
                    activity.showEncryptionDialog()
                }
            }
        }
    }

    NeoNavigationSuiteScaffold(
        pages = pages,
        currentState = currentPageIndex,
        onItemClick = { index ->
            scope.launch {
                pagerState.animateScrollToPage(index)
            }
        }
    ) {
        val openBlocklist = retain { mutableStateOf(false) }
        val searchExpanded = remember {
            mutableStateOf(false)
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                Column {
                    MainTopBar(
                        title = stringResource(id = currentPage.title),
                        expanded = searchExpanded,
                        query = mainState.searchQuery,
                        onQueryChanged = { newQuery ->
                            homeVM.setSearchQuery(newQuery)
                            backupVM.setSearchQuery(newQuery)
                            restoreVM.setSearchQuery(newQuery)
                        },
                        onClose = {
                            homeVM.setSearchQuery("")
                            backupVM.setSearchQuery("")
                            restoreVM.setSearchQuery("")
                        }
                    ) {
                        when (currentPage.destination) {
                            NavItem.Scheduler.destination -> {
                                RoundButton(
                                    icon = Phosphor.Prohibit,
                                    description = stringResource(id = R.string.sched_blocklist)
                                ) { openBlocklist.value = true }
                                RoundButton(
                                    description = stringResource(id = R.string.prefs_title),
                                    icon = Phosphor.GearSix
                                ) { navigator(NavRoute.Prefs()) }
                            }

                            else                          -> {
                                RoundButton(
                                    icon = Phosphor.MagnifyingGlass,
                                    description = stringResource(id = R.string.search),
                                    onClick = { searchExpanded.value = true }
                                )
                                RefreshButton { activity.refreshPackagesAndBackups() }
                                RoundButton(
                                    description = stringResource(id = R.string.prefs_title),
                                    icon = Phosphor.GearSix
                                ) { navigator(NavRoute.Prefs()) }
                            }
                        }
                    }
                    AnimatedVisibility(showBanner) {
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    activity.launchView(LINK_KEEP_ANDROID_OPEN)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                ),
                                leadingContent = {
                                    Icon(
                                        imageVector = Phosphor.CircleWavyWarning,
                                        contentDescription = stringResource(R.string.keep_android_open),
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        text = stringResource(R.string.keep_android_open_notice),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                trailingContent = {
                                    RoundButton(
                                        icon = Phosphor.X,
                                        description = stringResource(R.string.dialog_ignore),
                                    ) {
                                        pref_ignoreKeepAndroidOpenNotice.value = true
                                    }
                                }
                            )
                        }
                    }
                }
            },
        ) { paddingValues ->
            SlidePager(
                modifier = Modifier
                    .padding(paddingValues)
                    .blockBorderBottom()
                    .fillMaxSize(),
                pagerState = pagerState,
                pageItems = pages,
            )
        }

        if (openBlocklist.value) BaseDialog(onDismiss = { openBlocklist.value = false }) {
            GlobalBlockListDialogUI(
                currentBlocklist = mainState.blocklist,
                openDialogCustom = openBlocklist,
            ) { newSet ->
                homeVM.updateBlocklist(newSet)
            }
        }
    }
}
