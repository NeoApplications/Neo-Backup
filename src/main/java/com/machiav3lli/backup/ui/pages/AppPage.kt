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

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.imageLoader
import com.machiav3lli.backup.DialogMode
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.preferences.traceCompose
import com.machiav3lli.backup.exodusUrl
import com.machiav3lli.backup.manager.handler.ShellCommands
import com.machiav3lli.backup.manager.handler.ShellCommands.Companion.currentProfile
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.tasks.BackupActionTask
import com.machiav3lli.backup.manager.tasks.RestoreActionTask
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.component.BackupItem
import com.machiav3lli.backup.ui.compose.component.IconTextButton
import com.machiav3lli.backup.ui.compose.component.InfoChipsBlock
import com.machiav3lli.backup.ui.compose.component.OutlinedIconTextButton
import com.machiav3lli.backup.ui.compose.component.PackageIcon
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.TagsBlock
import com.machiav3lli.backup.ui.compose.component.TitleText
import com.machiav3lli.backup.ui.compose.icons.Icon
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.icon.Exodus
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArchiveTray
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowSquareOut
import com.machiav3lli.backup.ui.compose.icons.phosphor.Broom
import com.machiav3lli.backup.ui.compose.icons.phosphor.Hash
import com.machiav3lli.backup.ui.compose.icons.phosphor.Info
import com.machiav3lli.backup.ui.compose.icons.phosphor.Leaf
import com.machiav3lli.backup.ui.compose.icons.phosphor.MagnifyingGlass
import com.machiav3lli.backup.ui.compose.icons.phosphor.NotePencil
import com.machiav3lli.backup.ui.compose.icons.phosphor.Prohibit
import com.machiav3lli.backup.ui.compose.icons.phosphor.ProhibitInset
import com.machiav3lli.backup.ui.compose.icons.phosphor.TrashSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.Warning
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import com.machiav3lli.backup.ui.compose.show
import com.machiav3lli.backup.ui.dialogs.ActionsDialogUI
import com.machiav3lli.backup.ui.dialogs.BackupDialogUI
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.RestoreDialogUI
import com.machiav3lli.backup.ui.dialogs.StringInputDialogUI
import com.machiav3lli.backup.utils.BackupShareUtils
import com.machiav3lli.backup.utils.TraceUtils
import com.machiav3lli.backup.utils.extensions.koinNeoViewModel
import com.machiav3lli.backup.utils.infoChips
import com.machiav3lli.backup.viewmodels.AppVM
import timber.log.Timber

@Composable
fun AppPage(
    packageName: String,
    viewModel: AppVM = koinNeoViewModel(),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val mActivity = LocalActivity.current as NeoActivity
    val imageLoader = LocalContext.current.imageLoader
    val openDialog = remember { mutableStateOf(false) }
    val dialogProps: MutableState<Pair<DialogMode, Any>> = remember {
        mutableStateOf(Pair(DialogMode.NONE, Schedule()))
    }

    val state by viewModel.appState.collectAsState()
    val snackbarText by viewModel.snackbarText.collectAsState("")
    val dismissNow by viewModel.dismissNow
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarVisible = snackbarText.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()
    val columns = 4

    LaunchedEffect(packageName) {
        viewModel.setApp(packageName)
    }

    state.pkg?.let { pkg ->
        val iconVals by derivedStateOf {
            Triple(pkg.iconData, pkg.isSpecial, pkg.isSystem)
        }

        traceCompose {
            "AppPage ${pkg.packageName} ${
                TraceUtils.formatBackups(
                    pkg.backupsNewestFirst
                )
            }"
        }

        if (dismissNow) {
            viewModel.dismissNow.value = false
            onDismiss()
        }
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(pkg.packageName)

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PackageIcon(
                            imageData = iconVals.first,
                            isSpecial = iconVals.second,
                            isSystem = iconVals.third,
                            imageLoader = imageLoader,
                        )

                        Column(
                            modifier = Modifier
                                .wrapContentHeight()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = pkg.packageLabel,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = pkg.packageName,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        RoundButton(
                            icon = Phosphor.X,
                            description = stringResource(R.string.dialogCancel)
                        ) {
                            onDismiss()
                        }
                    }
                    AnimatedVisibility(visible = snackbarVisible) { // TODO move to MainPage
                        Text(
                            text = snackbarText,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    InfoChipsBlock(list = pkg.infoChips())
                    if (snackbarVisible)
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surface,
                            color = MaterialTheme.colorScheme.primary,
                            gapSize = 2.dp,
                        )
                    HorizontalDivider(
                        thickness = 2.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LazyVerticalGrid(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                columns = GridCells.Fixed(columns),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                if (launchIntent != null) item {
                    AnimatedVisibility(visible = launchIntent != null) {
                        IconTextButton(
                            icon = Phosphor.ArrowSquareOut,
                            description = stringResource(id = R.string.launch_app),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            context.startActivity(launchIntent)
                        }
                    }
                }
                if (!pkg.isInstalled && !pkg.isSpecial) item {
                    AnimatedVisibility(visible = !pkg.isInstalled && !pkg.isSpecial) {
                        IconTextButton(
                            icon = Phosphor.MagnifyingGlass,
                            description = stringResource(id = R.string.search_package),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "market://search?q=${pkg.packageName}".toUri()
                                )
                            )
                        }
                    }
                }
                if (pkg.isInstalled && !pkg.isSpecial) item {
                    AnimatedVisibility(visible = pkg.isInstalled && !pkg.isSpecial) {
                        IconTextButton(
                            icon = Phosphor.Info,
                            description = stringResource(id = R.string.app_info),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data =
                                Uri.fromParts(
                                    "package",
                                    pkg.packageName,
                                    null
                                )
                            context.startActivity(intent)
                        }
                    }
                }
                if (!pkg.isSpecial) item {
                    AnimatedVisibility(visible = !pkg.isSpecial) {
                        IconTextButton(
                            icon = Icon.Exodus,
                            description = stringResource(id = R.string.exodus_report),
                            containerColor = colorResource(id = R.color.ic_exodus),
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    exodusUrl(pkg.packageName).toUri()
                                )
                            )
                        }
                    }
                }
                item {
                    IconTextButton(
                        icon = Phosphor.Prohibit,
                        description = stringResource(id = R.string.global_blocklist_add),
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ) {
                        dialogProps.value = Pair(DialogMode.BLOCKLIST, pkg)
                        openDialog.value = true
                    }
                }
                if (pkg.isInstalled && !pkg.isSpecial) item(
                    span = { GridItemSpan(columns) }
                ) {
                    TitleText(textId = R.string.title_app_management)
                }
                if (pkg.isInstalled && !pkg.isSpecial
                    && ((pkg.storageStats?.dataBytes ?: 0L) >= 0L)
                ) item {
                    AnimatedVisibility(
                        visible = pkg.isInstalled &&
                                !pkg.isSpecial &&
                                ((pkg.storageStats?.dataBytes ?: 0L) >= 0L)
                    ) {
                        OutlinedIconTextButton(
                            icon = Phosphor.TrashSimple,
                            description = stringResource(id = R.string.clear_cache),
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ) {
                            dialogProps.value = Pair(DialogMode.CLEAN_CACHE, pkg)
                            openDialog.value = true
                        }
                    }
                }
                if (pkg.isInstalled && !pkg.isSpecial) item {
                    AnimatedVisibility(visible = pkg.isInstalled && !pkg.isSpecial) {
                        OutlinedIconTextButton(
                            icon = Phosphor.Warning,
                            description = stringResource(id = R.string.forceKill),
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ) {
                            dialogProps.value = Pair(DialogMode.FORCE_KILL, pkg)
                            openDialog.value = true
                        }
                    }
                }
                if (pkg.isInstalled && !pkg.isSpecial) item {
                    AnimatedVisibility(visible = pkg.isInstalled && !pkg.isSpecial) {
                        OutlinedIconTextButton(
                            icon = if (pkg.isDisabled) Phosphor.Leaf
                            else Phosphor.ProhibitInset,
                            containerColor = if (pkg.isDisabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary,
                            description = stringResource(
                                id = if (pkg.isDisabled) R.string.enablePackage
                                else R.string.disablePackage
                            ),
                            onClick = {
                                dialogProps.value =
                                    Pair(DialogMode.ENABLE_DISABLE, pkg.isDisabled)
                                openDialog.value = true
                            }
                        )
                    }
                }
                if (pkg.isInstalled && !pkg.isSystem) item {
                    AnimatedVisibility(visible = pkg.isInstalled && !pkg.isSystem) {
                        OutlinedIconTextButton(
                            icon = Phosphor.TrashSimple,
                            description = stringResource(id = R.string.uninstall),
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                dialogProps.value = Pair(DialogMode.UNINSTALL, pkg)
                                openDialog.value = true
                            }
                        )
                    }
                }
                item(span = { GridItemSpan(columns) }) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TitleText(textId = R.string.title_tags)
                        TagsBlock(
                            tags = state.appExtras.customTags,
                            onRemove = {
                                viewModel.setExtras(
                                    packageName,
                                    state.appExtras.copy(
                                        customTags = state.appExtras.customTags.minus(it)
                                    )
                                )
                            },
                            onAdd = {
                                dialogProps.value = Pair(DialogMode.ADD_TAG, "")
                                openDialog.value = true
                            }
                        )
                    }
                }
                item(span = { GridItemSpan(columns) }) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card {
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                ),
                                trailingContent = {
                                    RoundButton(
                                        icon = Phosphor.NotePencil,
                                        description = stringResource(R.string.edit_note),
                                    ) {
                                        dialogProps.value = Pair(DialogMode.NOTE, state.appExtras.note)
                                        openDialog.value = true
                                    }
                                },
                                overlineContent = {
                                    Text(
                                        text = stringResource(id = R.string.title_note),
                                        maxLines = 1,
                                    )
                                },
                                headlineContent = {
                                    Text(state.appExtras.note)
                                }
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(columns) }) {
                    TitleText(textId = R.string.title_backup_management)
                }
                if (pkg.isInstalled || pkg.isSpecial) item(span = { GridItemSpan(2) }) {
                    AnimatedVisibility(visible = pkg.isInstalled || pkg.isSpecial) {
                        IconTextButton(
                            icon = Phosphor.ArchiveTray,
                            description = stringResource(id = R.string.backup),
                            enabled = !snackbarVisible,
                            aspectRatio = 4f,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            dialogProps.value = Pair(DialogMode.BACKUP, pkg)
                            openDialog.value = true
                        }
                    }
                }
                if (pkg.hasBackups) item {
                    AnimatedVisibility(visible = pkg.hasBackups) {
                        OutlinedIconTextButton(
                            icon = Phosphor.Broom,
                            description = stringResource(id = R.string.delete_all_backups),
                            enabled = !snackbarVisible,
                            containerColor = MaterialTheme.colorScheme.tertiary,
                        ) {
                            dialogProps.value = Pair(DialogMode.DELETE_ALL, pkg)
                            openDialog.value = true
                        }
                    }
                }
                if (pkg.hasBackups) item {
                    AnimatedVisibility(visible = pkg.hasBackups) {
                        OutlinedIconTextButton(
                            icon = Phosphor.Hash,
                            description = stringResource(id = R.string.enforce_backups_limit),
                            enabled = !snackbarVisible,
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ) {
                            dialogProps.value = Pair(DialogMode.ENFORCE_LIMIT, pkg)
                            openDialog.value = true
                        }
                    }
                }
                item(span = { GridItemSpan(columns) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TitleText(textId = R.string.stats_backups)
                        if (pref_numBackupRevisions.value > 0) Text(
                            text = "(${
                                stringResource(
                                    id = R.string.backup_revisions_limit,
                                    pref_numBackupRevisions.value
                                )
                            })"
                        )
                    }
                }
                this.items(
                    items = pkg.backupsNewestFirst,
                    span = { GridItemSpan(columns) }) {
                    BackupItem(
                        it,
                        onRestore = { item ->
                            pkg.let { app ->
                                if (!app.isSpecial && !app.isInstalled
                                    && !item.hasApk && item.hasAppData
                                ) {
                                    snackbarHostState.show(
                                        coroutineScope = coroutineScope,
                                        message = context.getString(R.string.notInstalledModeDataWarning)
                                    )
                                } else {
                                    dialogProps.value = Pair(DialogMode.RESTORE, item)
                                    openDialog.value = true
                                }
                            }
                        },
                        onDelete = { item ->
                            dialogProps.value = Pair(DialogMode.DELETE, item)
                            openDialog.value = true
                        },
                        onNote = { item ->
                            dialogProps.value = Pair(DialogMode.NOTE_BACKUP, item)
                            openDialog.value = true
                        },
                        onShare = { item ->
                            BackupShareUtils.shareBackup(item, context)
                        },
                        rewriteBackup = { backup, changedBackup ->
                            viewModel.rewriteBackup(backup, changedBackup)
                        }
                    )
                }
            }

            if (openDialog.value) BaseDialog(onDismiss = { openDialog.value = false }) {
                dialogProps.value.let { (dialogMode, obj) ->
                    when (dialogMode) {
                        DialogMode.BACKUP         -> {
                            BackupDialogUI(
                                appPackage = pkg,
                                openDialogCustom = openDialog,
                            ) { mode ->
                                if (pref_useWorkManagerForSingleManualJob.value) {
                                    mActivity.startBatchAction(
                                        true,
                                        listOf(packageName),
                                        listOf(mode)
                                    )
                                } else {
                                    BackupActionTask(
                                        pkg, mActivity, NeoApp.shellHandler!!, mode,
                                    ) { message ->
                                        viewModel.setSnackbarText(message)
                                    }.execute()
                                }
                            }
                        }

                        DialogMode.RESTORE        -> {
                            RestoreDialogUI(
                                appPackage = pkg,
                                backup = obj as Backup,
                                openDialogCustom = openDialog,
                            ) { mode ->
                                if (pref_useWorkManagerForSingleManualJob.value) {
                                    mActivity.startBatchAction(
                                        false,
                                        listOf(packageName),
                                        listOf(mode)
                                    )
                                } else {
                                    obj.let {
                                        RestoreActionTask(
                                            pkg, mActivity, NeoApp.shellHandler!!, mode,
                                            it
                                        ) { message ->
                                            viewModel.setSnackbarText(message)
                                        }.execute()
                                    }
                                }
                            }
                        }

                        DialogMode.BLOCKLIST      -> {
                            ActionsDialogUI(
                                titleText = pkg.packageLabel,
                                messageText = stringResource(id = R.string.addBlocklistDialogMessage),
                                onDismiss = { openDialog.value = false },
                                primaryText = stringResource(id = R.string.dialogYes),
                                primaryAction = {
                                    viewModel.addToBlocklist(
                                        pkg.packageName
                                    )
                                }
                            )
                        }

                        DialogMode.DELETE         -> {
                            ActionsDialogUI(
                                titleText = pkg.packageLabel,
                                messageText = stringResource(id = R.string.deleteBackupDialogMessage),
                                onDismiss = { openDialog.value = false },
                                primaryText = stringResource(id = R.string.dialogYes),
                                primaryAction = {
                                    snackbarHostState.show(
                                        coroutineScope = coroutineScope,
                                        message = "${pkg.packageLabel}: ${
                                            context.getString(
                                                R.string.deleteBackup
                                            )
                                        }"
                                    )
                                    if (!pkg.hasBackups) {
                                        Timber.w("UI Issue! Tried to delete backups for app without backups.")
                                        openDialog.value = false
                                    }
                                    viewModel.deleteBackup(dialogProps.value.second as Backup)
                                }
                            )
                        }

                        DialogMode.DELETE_ALL     -> {
                            ActionsDialogUI(
                                titleText = pkg.packageLabel,
                                messageText = stringResource(id = R.string.delete_all_backups),
                                onDismiss = { openDialog.value = false },
                                primaryText = stringResource(id = R.string.dialogYes),
                                primaryAction = {
                                    viewModel.deleteAllBackups()
                                    snackbarHostState.show(
                                        coroutineScope = coroutineScope,
                                        message = "${pkg.packageLabel}: ${
                                            context.getString(
                                                R.string.delete_all_backups
                                            )
                                        }"
                                    )
                                }
                            )
                        }

                        DialogMode.CLEAN_CACHE    -> {
                            ActionsDialogUI(
                                titleText = pkg.packageLabel,
                                messageText = stringResource(id = R.string.clear_cache),
                                onDismiss = { openDialog.value = false },
                                primaryText = stringResource(id = R.string.dialogYes),
                                primaryAction = {
                                    try {
                                        Timber.i("${pkg.packageLabel}: Wiping cache")
                                        ShellCommands.wipeCache(context, pkg)
                                        viewModel.updatePackage(pkg.packageName)
                                    } catch (e: ShellCommands.ShellActionFailedException) {
                                        // Not a critical issue
                                        val errorMessage: String =
                                            when (val cause = e.cause) {
                                                is ShellHandler.ShellCommandFailedException -> {
                                                    cause.shellResult.err.joinToString(
                                                        " "
                                                    )
                                                }

                                                else                                        -> {
                                                    cause?.message ?: "unknown error"
                                                }
                                            }
                                        Timber.w("Cache couldn't be deleted: $errorMessage")
                                    }
                                }
                            )
                        }

                        DialogMode.FORCE_KILL     -> {
                            val profileId = currentProfile
                            ActionsDialogUI(
                                titleText = pkg.packageLabel,
                                messageText = stringResource(id = R.string.forceKillMessage),
                                onDismiss = { openDialog.value = false },
                                primaryText = stringResource(id = R.string.dialogYes),
                                primaryAction = {
                                    //TODO hg42 force-stop, force-close, ... ? I think these are different ones, and I don't know which.
                                    //TODO hg42 killBackgroundProcesses seems to be am kill
                                    //TODO in api33 A13 there is am stop-app which doesn't kill alarms and
                                    runAsRoot("am stop-app --user $profileId ${pkg.packageName} || am force-stop --user $profileId ${pkg.packageName}")
                                }
                            )
                        }

                        DialogMode.ENABLE_DISABLE -> {
                            val enable = dialogProps.value.second as Boolean

                            ActionsDialogUI(
                                titleText = pkg.packageLabel,
                                messageText = stringResource(
                                    id = if (enable) R.string.enablePackage
                                    else R.string.disablePackage
                                ),
                                onDismiss = { openDialog.value = false },
                                primaryText = stringResource(id = R.string.dialogYes),
                                primaryAction = {
                                    try {
                                        viewModel.enableDisableApp(packageName, enable)
                                        // TODO (re-)add user selection support
                                    } catch (e: ShellCommands.ShellActionFailedException) {
                                        mActivity.showError(e.message)
                                    }
                                }
                            )
                        }

                        DialogMode.UNINSTALL      -> {
                            ActionsDialogUI(
                                titleText = pkg.packageLabel,
                                messageText = stringResource(id = R.string.uninstallDialogMessage),
                                onDismiss = { openDialog.value = false },
                                primaryText = stringResource(id = R.string.dialogYes),
                                primaryAction = {
                                    viewModel.uninstallApp()
                                    snackbarHostState.show(
                                        coroutineScope = coroutineScope,
                                        message = "${pkg.packageLabel}: ${
                                            context.getString(
                                                R.string.uninstallProgress
                                            )
                                        }"
                                    )
                                }
                            )
                        }

                        DialogMode.NOTE           -> {
                            StringInputDialogUI(
                                titleText = stringResource(id = R.string.edit_note),
                                initValue = dialogProps.value.second as String,
                                openDialogCustom = openDialog,
                            ) {
                                viewModel.setExtras(
                                    packageName,
                                    state.appExtras.copy(note = it)
                                )
                            }
                        }

                        DialogMode.NOTE_BACKUP    -> {
                            val backup = dialogProps.value.second as Backup

                            StringInputDialogUI(
                                titleText = stringResource(id = R.string.edit_note),
                                initValue = backup.note,
                                openDialogCustom = openDialog,
                            ) {
                                viewModel.rewriteBackup(backup, backup.copy(note = it))
                            }
                        }

                        DialogMode.ENFORCE_LIMIT  -> {
                            ActionsDialogUI(
                                titleText = pkg.packageLabel,
                                messageText = stringResource(
                                    id = R.string.enforce_backups_limit_description,
                                    pref_numBackupRevisions.value
                                ),
                                onDismiss = { openDialog.value = false },
                                primaryText = stringResource(id = R.string.dialogYes),
                                primaryAction = {
                                    viewModel.enforceBackupsLimit(obj as Package)
                                }
                            )
                        }

                        DialogMode.ADD_TAG        -> {
                            StringInputDialogUI(
                                titleText = stringResource(id = R.string.add_tag),
                                initValue = dialogProps.value.second as String,
                                openDialogCustom = openDialog,
                                suggestions = state.allTags - state.appExtras.customTags,
                            ) {
                                viewModel.setExtras(
                                    packageName,
                                    state.appExtras.copy(
                                        customTags = state.appExtras.customTags.plus(it)
                                    )
                                )
                            }
                        }

                        else                      -> {}
                    }
                }
            }
        }
    }
}
