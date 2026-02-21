package com.machiav3lli.backup.ui.compose.component

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.dbs.entity.PackageInfo
import com.machiav3lli.backup.data.entity.ColoringState
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.manager.handler.ShellCommands.Companion.currentProfile
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ClockCounterClockwise
import com.machiav3lli.backup.ui.compose.icons.phosphor.Lock
import com.machiav3lli.backup.ui.compose.icons.phosphor.LockOpen
import com.machiav3lli.backup.ui.compose.icons.phosphor.ShareNetwork
import com.machiav3lli.backup.ui.compose.icons.phosphor.TrashSimple
import com.machiav3lli.backup.ui.pages.pref_altBackupDate
import com.machiav3lli.backup.utils.BACKUP_DATE_TIME_SHOW_FORMATTER
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.getFormattedDate
import java.io.File
import java.time.LocalDateTime

@Composable
fun BackupItem_headlineContent(
    item: Backup,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f, false),
            text = "${item.versionName ?: ""} (${item.versionCode})",
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(visible = (item.cpuArch != Android.mainPlatform)) {
                Text(
                    text = " ${item.cpuArch} ",
                    color = Color.Red,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            BackupLabels(item = item)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupItem_supportingContent(
    item: Backup,
    onNote: ((Backup) -> Unit)? = null,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (pref_altBackupDate.value)
                    item.backupDate.format(BACKUP_DATE_TIME_SHOW_FORMATTER)
                else item.backupDate.getFormattedDate(true),
                modifier = Modifier.align(Alignment.Top),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
            val fileSizeText = if (item.backupVersionCode != 0)
                Formatter.formatFileSize(LocalContext.current, item.size)
            else ""
            Text(
                text = " - $fileSizeText",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoteTagItem(
                note = item.note,
                maxLines = 3,
                onNote = onNote?.let { { it(item) } },
            )
            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                Text(
                    text = if (item.backupVersionCode == 0)
                        "old"
                    else
                        "${item.backupVersionCode / 1000}.${item.backupVersionCode % 1000}",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
            if (item.isEncrypted) {
                Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("${item.cipherType}") } },
                        state = rememberTooltipState(),
                    ) {
                        Text(
                            text = "enc",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                }
            }
            val compressionText = if (item.isCompressed) {
                if (item.compressionType.isNullOrEmpty())
                    "gz"
                else
                    "${item.compressionType}"
            } else ""
            if (compressionText.isNotEmpty()) Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    text = compressionText,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
            val directoryTag = item.directoryTag
            if (directoryTag.isNotEmpty())
                Text(
                    text = " - $directoryTag",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            AnimatedVisibility(visible = (item.profileId != currentProfile)) {
                Row {
                    Text(
                        text = " 👤",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "${item.profileId}",
                        color = Color.Red,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun BackupItem(
    item: Backup,
    onRestore: (Backup) -> Unit = { },
    onDelete: (Backup) -> Unit = { },
    onNote: (Backup) -> Unit = { },
    rewriteBackup: (Backup, Backup) -> Unit = { _, _ -> },
) {
    var persistent by remember(item.persistent) {
        mutableStateOf(item.persistent)
    }
    val togglePersistent = {
        persistent = !persistent
        rewriteBackup(item, item.copy(persistent = persistent))
    }
    val lockColor by animateColorAsState(
        if (persistent) Color.Red else LocalContentColor.current,
        label = "lockColor",
    )

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        headlineContent = { BackupItem_headlineContent(item) },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BackupItem_supportingContent(item, onNote = onNote)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundButton(
                        icon = if (persistent) Phosphor.Lock
                        else Phosphor.LockOpen,
                        tint = lockColor,
                        onClick = togglePersistent
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilledRoundButton(
                            icon = Phosphor.TrashSimple,
                            description = stringResource(id = R.string.deleteBackup),
                            tint = MaterialTheme.colorScheme.tertiaryContainer,
                            onTint = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = { onDelete(item) }
                        )
                        if (!item.packageLabel.contains("INVALID"))
                            ActionButton(
                                text = stringResource(id = R.string.restore),
                                icon = Phosphor.ClockCounterClockwise,
                                coloring = ColoringState.Positive,
                                onClick = { onRestore(item) },
                            )
                    }
                }
            }
        },
    )
}

@Composable
fun RestoreBackupItem(
    item: Backup,
    index: Int,
    isApkChecked: Boolean = false,
    isDataChecked: Boolean = false,
    onApkClick: (String, Boolean, Int) -> Unit = { _: String, _: Boolean, _: Int -> },
    onDataClick: (String, Boolean, Int) -> Unit = { _: String, _: Boolean, _: Int -> },
) {
    var apkChecked by remember(isApkChecked) { mutableStateOf(isApkChecked) }
    var dataChecked by remember(isDataChecked) { mutableStateOf(isDataChecked) }
    val showApk by remember(item) { mutableStateOf(item.hasApk) }
    val showData by remember(item) { mutableStateOf(item.hasData) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = apkChecked,
                enabled = showApk,
                onCheckedChange = {
                    apkChecked = it
                    onApkClick(item.packageName, it, index)
                }
            )
            Checkbox(
                checked = dataChecked,
                enabled = showData,
                onCheckedChange = {
                    dataChecked = it
                    onDataClick(item.packageName, it, index)
                }
            )
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
                headlineContent = { BackupItem_headlineContent(item) },
                supportingContent = { BackupItem_supportingContent(item) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BackupPreview(item: (@Composable (item: Backup) -> Unit) = { BackupItem(it) }) {
    var note by remember { mutableStateOf("a very very very very very very very very long note text and even longer") }

    val backupRootLocal = File("/tmp", "backup")
    NeoApp.backupRoot = StorageFile(backupRootLocal)

    val backup = Backup(
        base = PackageInfo(
            packageName = "some.package.name",
            versionName = "1.2.3.4-some-long-version",
            versionCode = 1234,
        ),
        backupDate = LocalDateTime.now(),
        hasApk = true,
        hasAppData = true,
        hasDevicesProtectedData = true,
        hasExternalData = false,
        hasObbData = false,
        hasMediaData = false,
        compressionType = "zst",
        cipherType = "aes-256-cbc-etc",
        iv = null,
        cpuArch = "arm64",
        permissions = emptyList(),
        persistent = false,
        note = note,
        size = 123456789,
    )
    backup.dir = StorageFile(File(backupRootLocal, "some/subfolder"))

    val backup_without_note = backup.copy(note = "", versionName = "1.2.3.4")
    val backup_invalid = backup.copy(packageLabel = "? INVALID", note = "invalid")

    Column(
        modifier = Modifier
            .width(500.dp)
    ) {
        FlowRow {
            ActionButton("none") {
                note = ""
                backup.dir = NeoApp.backupRoot
            }
            ActionButton("short") {
                note = "note text"
                backup.dir = StorageFile(File(backupRootLocal, "subfolder"))
            }
            ActionButton("long") {
                note = "a very very very long note text"
                backup.dir = StorageFile(File(backupRootLocal, "some/subfolder/in/dir"))
            }
            ActionButton("extreme") {
                note = "a very very very very very very very very long note text and even longer"
                backup.dir = StorageFile(File(backupRootLocal, "some/subfolder/in/backup/dir"))
            }
            ActionButton("multiline") {
                note = "a very very very long note text\nmultiple\nlines"
                backup.dir = StorageFile(File(backupRootLocal, "some/subfolder/in/backup/dir"))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        item(backup)
        Spacer(modifier = Modifier.height(8.dp))
        item(backup_without_note)
        Spacer(modifier = Modifier.height(8.dp))
        item(backup_invalid)
    }
}

@Preview(showBackground = true)
@Composable
fun RestoreBackupPreview() {
    BackupPreview(item = { RestoreBackupItem(it, index = 0) })
}
