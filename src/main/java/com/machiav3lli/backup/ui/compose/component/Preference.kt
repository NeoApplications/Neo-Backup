package com.machiav3lli.backup.ui.compose.component

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machiav3lli.backup.ICON_SIZE_MEDIUM
import com.machiav3lli.backup.ICON_SIZE_SMALL
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.BooleanPref
import com.machiav3lli.backup.data.entity.EnumPref
import com.machiav3lli.backup.data.entity.IntPref
import com.machiav3lli.backup.data.entity.ListPref
import com.machiav3lli.backup.data.entity.PasswordPref
import com.machiav3lli.backup.data.entity.Pref
import com.machiav3lli.backup.data.entity.StringEditPref
import com.machiav3lli.backup.data.entity.StringPref
import com.machiav3lli.backup.data.preferences.PrefBoolean
import com.machiav3lli.backup.data.preferences.PrefDelegate
import com.machiav3lli.backup.data.preferences.PrefEditString
import com.machiav3lli.backup.data.preferences.PrefEnum
import com.machiav3lli.backup.data.preferences.PrefInt
import com.machiav3lli.backup.data.preferences.PrefList
import com.machiav3lli.backup.data.preferences.PrefString
import com.machiav3lli.backup.data.preferences.PrefStringSet
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.FolderNotch
import com.machiav3lli.backup.ui.compose.icons.phosphor.Hash
import com.machiav3lli.backup.ui.compose.ifThen
import com.machiav3lli.backup.utils.backupFolderExists
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

const val UNDEFINED_VALUE = "---"

@Composable
fun PrefIcon(
    icon: ImageVector?,
    titleId: Int,
    tint: Color? = null,
) {
    val title = if (titleId < 0) null else stringResource(id = titleId)
    //traceCompose { "PrefIcon: $title ${icon?.name} $tint" }

    if (icon != null)
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(ICON_SIZE_MEDIUM),   //TODO BUTTON_ICON_SIZE?
            tint = tint ?: MaterialTheme.colorScheme.onSurface
        )
    else
        Spacer(modifier = Modifier.requiredWidth(ICON_SIZE_MEDIUM + 4.dp))
}

@Composable
fun BasePreference(
    modifier: Modifier = Modifier,
    pref: Pref,
    dirty: Boolean = pref.dirty.value,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    valueShown: String? = null,
    index: Int = 0,
    groupSize: Int = 1,
    endWidget: (@Composable (isEnabled: Boolean) -> Unit)? = null,
    bottomWidget: (@Composable (isEnabled: Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
) {
    val isEnabled by remember(pref.enableIf?.invoke() ?: true) {
        mutableStateOf(pref.enableIf?.invoke() ?: true)
    }

    val base = index.toFloat() / groupSize
    val rank = (index + 1f) / groupSize

    val surfaceColor by animateColorAsState(
        if (dirty) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainer,
        label = "surfaceColor"
    )

    LaunchedEffect(dirty) {
        delay(500)
        traceDebug { "pref: $dirty ${pref.key} -> ${pref.icon?.name} ${pref.iconTint} (basepref launch)" }
        pref.dirty.value = false
    }

    //traceCompose { "BasePreference: $dirty ${pref.key} -> ${pref.icon?.name} ${pref.iconTint}" }

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = if (base == 0f) MaterialTheme.shapes.large.topStart
                    else MaterialTheme.shapes.extraSmall.topStart,
                    topEnd = if (base == 0f) MaterialTheme.shapes.large.topEnd
                    else MaterialTheme.shapes.extraSmall.topEnd,
                    bottomStart = if (rank == 1f) MaterialTheme.shapes.large.bottomStart
                    else MaterialTheme.shapes.extraSmall.bottomStart,
                    bottomEnd = if (rank == 1f) MaterialTheme.shapes.large.bottomEnd
                    else MaterialTheme.shapes.extraSmall.bottomEnd
                )
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick ?: {}
            ),
        colors = ListItemDefaults.colors(
            containerColor = surfaceColor,
        ),
        leadingContent = { PrefIcon(pref.icon, pref.titleId, pref.iconTint?.invoke(pref)) },
        headlineContent = {
            Text(
                text = if (titleId != -1) stringResource(id = titleId) else pref.key,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp
            )
        },
        supportingContent = {
            Column(
                modifier = Modifier
                    .ifThen(!isEnabled) {
                        alpha(0.3f)
                    }
            ) {
                if (summaryId != -1) {
                    val summaryText = stringResource(id = summaryId)
                    Text(
                        text = summaryText,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                summary?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                valueShown?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                    )
                }
                bottomWidget?.let { widget ->
                    Spacer(modifier = Modifier.requiredWidth(8.dp))
                    widget(isEnabled)
                }
            }
        },
        trailingContent = if (endWidget != null) {
            { endWidget(isEnabled) }
        } else null,
    )
}

@Composable
fun BasePreference(
    modifier: Modifier = Modifier,
    pref: PrefDelegate<out Any>,
    dirty: Boolean = pref.dirty.value,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    valueShown: String? = null,
    index: Int = 0,
    groupSize: Int = 1,
    endWidget: (@Composable (isEnabled: Boolean) -> Unit)? = null,
    bottomWidget: (@Composable (isEnabled: Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
) {
    val isEnabled by remember(pref.enableIf?.invoke() ?: true) {
        mutableStateOf(pref.enableIf?.invoke() ?: true)
    }

    val base = index.toFloat() / groupSize
    val rank = (index + 1f) / groupSize

    val surfaceColor by animateColorAsState(
        if (dirty) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainer,
        label = "surfaceColor"
    )

    LaunchedEffect(dirty) {
        delay(500)
        traceDebug { "pref: $dirty ${pref.key} -> ${pref.icon?.name} ${pref.iconTint} (basepref launch)" }
        pref.dirty.value = false
    }

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = if (base == 0f) MaterialTheme.shapes.large.topStart
                    else MaterialTheme.shapes.extraSmall.topStart,
                    topEnd = if (base == 0f) MaterialTheme.shapes.large.topEnd
                    else MaterialTheme.shapes.extraSmall.topEnd,
                    bottomStart = if (rank == 1f) MaterialTheme.shapes.large.bottomStart
                    else MaterialTheme.shapes.extraSmall.bottomStart,
                    bottomEnd = if (rank == 1f) MaterialTheme.shapes.large.bottomEnd
                    else MaterialTheme.shapes.extraSmall.bottomEnd
                )
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick ?: {}
            ),
        colors = ListItemDefaults.colors(
            containerColor = surfaceColor,
        ),
        leadingContent = { PrefIcon(pref.icon, pref.titleId, pref.iconTint?.invoke(pref)) },
        headlineContent = {
            Text(
                text = if (titleId != -1) stringResource(id = titleId) else pref.key,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp
            )
        },
        supportingContent = {
            Column(
                modifier = Modifier
                    .ifThen(!isEnabled) {
                        alpha(0.3f)
                    }
            ) {
                if (summaryId != -1) {
                    val summaryText = stringResource(id = summaryId)
                    Text(
                        text = summaryText,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                summary?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                valueShown?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                    )
                }
                bottomWidget?.let { widget ->
                    Spacer(modifier = Modifier.requiredWidth(8.dp))
                    widget(isEnabled)
                }
            }
        },
        trailingContent = if (endWidget != null) {
            { endWidget(isEnabled) }
        } else null,
    )
}

@Composable
fun LaunchPreference(
    modifier: Modifier = Modifier,
    pref: Pref,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
) {
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = summary,
        index = index,
        groupSize = groupSize,
        onClick = onClick,
    )
}

@Composable
fun StringPreference(
    modifier: Modifier = Modifier,
    pref: StringPref,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        valueShown = pref.value,
        index = index,
        groupSize = groupSize,
        onClick = onClick,
    )
}

@Composable
fun StringPreference(
    pref: PrefString,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val value by pref.state
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        valueShown = value,
        index = index,
        groupSize = groupSize,
        onClick = onClick,
    )
}

@Preview
@Composable
fun StringPreferencePreview() {
    val pref_pathBackupFolder = StringPref(
        key = "user.pathBackupFolder",
        titleId = R.string.prefs_pathbackupfolder,
        icon = Phosphor.FolderNotch,
        iconTint = { if (backupFolderExists()) Color.Green else Color.Red },
        defaultValue = "path/to/backup/folder",
    )

    StringPreference(
        pref = pref_pathBackupFolder,
    )
}

@Composable
fun StringEditPreference(
    pref: StringEditPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        bottomWidget = {
            TextInput(
                pref.value,
                modifier = Modifier.fillMaxWidth(),
                onClick = onClick,
            ) {
                pref.value = it
            }
        },
        onClick = onClick,
    )
}

@Composable
fun StringEditPreference(
    pref: PrefEditString,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val value by pref.state
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        bottomWidget = {
            TextInput(
                value,
                modifier = Modifier.fillMaxWidth(),
                onClick = onClick,
            ) {
                kotlinx.coroutines.runBlocking { pref.set(it) }
            }
        },
        onClick = onClick,
    )
}

@Preview
@Composable
fun StringEditPreferencePreview() {
    val pref_suCommand = StringEditPref(
        key = "user.suCommand",
        icon = Phosphor.Hash,
        iconTint = {
            val pref = it as StringEditPref
            if (pref.value == "test") Color.Green
            else if (pref.value == "test2") Color.Red
            else Color.Gray
        },
        defaultValue = "su --mount-master",
    )

    val pref = pref_suCommand

    Column {
        Column {
            Row {
                ActionButton(text = "test") {
                    pref.value = "test"
                }
                ActionButton(text = "test2") {
                    pref.value = "test2"
                }
            }
            StringEditPreference(
                pref = pref
            )
        }

        val pref_pathBackupFolder = StringEditPref(
            key = "user.pathBackupFolder",
            titleId = R.string.prefs_pathbackupfolder,
            icon = Phosphor.FolderNotch,
            iconTint = { if (backupFolderExists()) Color.Green else Color.Red },
            defaultValue = "path/to/backup/folder",
        )

        StringEditPreference(
            pref = pref_pathBackupFolder,
        )
    }
}

@Composable
fun PasswordPreference(
    pref: PasswordPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val valueShown = if (pref.value.isNotEmpty()) "*********" else UNDEFINED_VALUE
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        valueShown = valueShown,
        onClick = onClick,
    )
}

@Composable
fun EnumPreference(
    pref: EnumPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val valueShown = pref.entries[pref.value]?.let { stringResource(id = it) } ?: UNDEFINED_VALUE
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        valueShown = valueShown,
        onClick = onClick,
    )
}

@Composable
fun EnumPreference(
    pref: PrefEnum,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val value by pref.state
    val valueShown = pref.entries[value]?.let { stringResource(id = it) } ?: UNDEFINED_VALUE
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        valueShown = valueShown,
        onClick = onClick,
    )
}

@Composable
fun ListPreference(
    pref: ListPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: () -> Unit = {},
) {
    val valueShown = pref.entries[pref.value] ?: UNDEFINED_VALUE
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        valueShown = valueShown,
        onClick = onClick,
    )
}

@Composable
fun ListPreference(
    pref: PrefList,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: () -> Unit = {},
) {
    val value by pref.state
    val valueShown = pref.entries[value] ?: UNDEFINED_VALUE
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        valueShown = valueShown,
        onClick = onClick,
    )
}

@Composable
fun SwitchPreference(
    pref: BooleanPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    var checked by remember(pref.value) { mutableStateOf(pref.value) }  //TODO hg42 remove remember ???
    val interactionSource = remember { MutableInteractionSource() }
    val check = { value: Boolean ->
        pref.value = value
        checked = value
    }

    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        onClick = {
            onCheckedChange(!checked)
            check(!checked)
        },
        interactionSource = interactionSource,
        endWidget = { isEnabled ->
            Switch(
                modifier = Modifier
                    .height(ICON_SIZE_SMALL),
                checked = checked,
                interactionSource = interactionSource,
                onCheckedChange = {
                    onCheckedChange(it)
                    check(it)
                },
                enabled = isEnabled,
            )
        },
    )
}

@Composable
fun CheckboxPreference(
    pref: BooleanPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    var checked by remember(pref.value) { mutableStateOf(pref.value) }  //TODO hg42 remove remember ???
    val check = { value: Boolean ->
        pref.value = value
        checked = value
    }

    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        onClick = {
            onCheckedChange(!checked)
            check(!checked)
        },
        endWidget = { isEnabled ->
            Checkbox(
                modifier = Modifier
                    .height(ICON_SIZE_SMALL),
                checked = checked,
                onCheckedChange = {
                    onCheckedChange(it)
                    check(it)
                },
                enabled = isEnabled,
            )
        },
    )
}

@Composable
fun BooleanPreference(
    pref: BooleanPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    SwitchPreference(
        pref = pref,
        modifier = modifier,
        dirty = dirty,
        index = index,
        groupSize = groupSize,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
fun BooleanPreference(
    pref: PrefBoolean,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    val checked by pref.state
    val check = { value: Boolean ->
        kotlinx.coroutines.runBlocking { pref.set(value) }
        onCheckedChange(value)
    }

    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        onClick = {
            onCheckedChange(!checked)
            check(!checked)
        },
        endWidget = { isEnabled ->
            Switch(
                modifier = Modifier.height(ICON_SIZE_SMALL),
                checked = checked,
                onCheckedChange = {
                    onCheckedChange(it)
                    check(it)
                },
                enabled = isEnabled,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBarPreference(
    pref: IntPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onValueChange: (Int) -> Unit = {},
) {
    val sliderState = remember {
        SliderState(
            value = pref.entries.indexOf(pref.value).toFloat(),
            valueRange = 0f..pref.entries.lastIndex.toFloat(),
            steps = pref.entries.size - 2
        )
    }
    sliderState.onValueChangeFinished = {
        pref.value = pref.entries[sliderState.value.roundToInt()]
        onValueChange(sliderState.value.roundToInt())
    }

    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        bottomWidget = { isEnabled ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Slider(
                    state = sliderState,
                    modifier = Modifier
                        .requiredHeight(24.dp)
                        .weight(1f),
                    enabled = isEnabled
                )
                Text(
                    text = pref.entries[sliderState.value.roundToInt()].toString(),
                    modifier = Modifier.widthIn(min = 48.dp)
                )
            }
        },
    )
}

@Composable
fun IntPreference(
    pref: IntPref,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onValueChange: (Int) -> Unit = {},
) {
    SeekBarPreference(
        pref = pref,
        modifier = modifier,
        dirty = dirty,
        index = index,
        groupSize = groupSize,
        onValueChange = onValueChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntPreference(
    pref: PrefInt,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onValueChange: (Int) -> Unit = {},
) {
    val value by pref.state
    val sliderState = remember {
        SliderState(
            value = pref.entries.indexOf(value).toFloat(),
            valueRange = 0f..pref.entries.lastIndex.toFloat(),
            steps = pref.entries.size - 2
        )
    }
    sliderState.onValueChangeFinished = {
        val newValue = pref.entries[sliderState.value.roundToInt()]
        kotlinx.coroutines.runBlocking { pref.set(newValue) }
        onValueChange(sliderState.value.roundToInt())
    }

    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        bottomWidget = { isEnabled ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Slider(
                    state = sliderState,
                    modifier = Modifier
                        .requiredHeight(24.dp)
                        .weight(1f),
                    enabled = isEnabled
                )
                Text(
                    text = pref.entries[sliderState.value.roundToInt()].toString(),
                    modifier = Modifier.widthIn(min = 48.dp)
                )
            }
        },
    )
}

@Composable
fun StringSetPreference(
    pref: PrefStringSet,
    modifier: Modifier = Modifier,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val value by pref.state
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        valueShown = value.joinToString(", "),
        index = index,
        groupSize = groupSize,
        onClick = onClick,
    )
}
