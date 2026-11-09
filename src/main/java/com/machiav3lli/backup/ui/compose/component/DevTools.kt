package com.machiav3lli.backup.ui.compose.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.machiav3lli.backup.DamagedOp
import com.machiav3lli.backup.ERROR_PREFIX
import com.machiav3lli.backup.ICON_SIZE_SMALL
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.PREFS_BACKUP_FILE
import com.machiav3lli.backup.data.entity.LaunchPref
import com.machiav3lli.backup.data.entity.Pref
import com.machiav3lli.backup.data.entity.Pref.Companion.preferencesFromSerialized
import com.machiav3lli.backup.data.entity.Pref.Companion.preferencesToSerialized
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.entity.UndeterminedStorageFile
import com.machiav3lli.backup.data.plugins.Plugin
import com.machiav3lli.backup.data.plugins.SpecialFilesPlugin
import com.machiav3lli.backup.data.plugins.TextPlugin
import com.machiav3lli.backup.data.preferences.pref_autoLogAfterSchedule
import com.machiav3lli.backup.data.preferences.pref_autoLogExceptions
import com.machiav3lli.backup.data.preferences.pref_autoLogSuspicious
import com.machiav3lli.backup.data.preferences.pref_catchUncaughtException
import com.machiav3lli.backup.data.preferences.pref_logToSystemLogcat
import com.machiav3lli.backup.data.preferences.pref_maxLogLines
import com.machiav3lli.backup.data.preferences.pref_trace
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import com.machiav3lli.backup.manager.handler.findBackupsAndClean
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowUUpLeft
import com.machiav3lli.backup.ui.compose.icons.phosphor.MagnifyingGlass
import com.machiav3lli.backup.ui.compose.icons.phosphor.Pencil
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import com.machiav3lli.backup.ui.pages.DevPrefGroups
import com.machiav3lli.backup.ui.pages.Logs
import com.machiav3lli.backup.ui.pages.Terminal
import com.machiav3lli.backup.ui.pages.TerminalText
import com.machiav3lli.backup.ui.pages.logRel
import com.machiav3lli.backup.ui.pages.supportInfoLogShare
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.recreateActivities
import com.machiav3lli.backup.utils.restartApp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File

// TODO break in parts

@Composable
fun SimpleButton(
    text: String,
    modifier: Modifier = Modifier,
    important: Boolean = false,
    action: () -> Unit,
) {
    val color = if (important) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val textColor = if (important) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    SmallFloatingActionButton(
        modifier = Modifier
            .padding(2.dp, 0.dp)
            .wrapContentWidth()
            .wrapContentHeight()
            .then(modifier),
        containerColor = color,
        onClick = action
    ) {
        Text(
            modifier = Modifier
                .padding(8.dp, 0.dp),
            text = text,
            color = textColor
        )
    }
}

@Composable
fun SmallButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    action: () -> Unit,
) {
    RoundButton(
        icon = icon,
        modifier = modifier,
        onClick = action,
        tint = tint ?: MaterialTheme.colorScheme.primary
    )
}

@Composable
fun TextInput(
    text: TextFieldValue,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailingIcon: (@Composable () -> Unit)? = null,
    focusInitially: Boolean = false,
    submitEachChange: Boolean = false,
    onUnfocusedClick: (() -> Unit)? = null,
    onSubmit: (TextFieldValue) -> Unit = {},
) {
    val input = remember(text) { mutableStateOf(text) }
    var editing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    fun submit(final: Boolean = true) {
        onSubmit(input.value)
        if (final) {
            focusManager.clearFocus()
        }
    }

    val clickTextToEdit = (onUnfocusedClick == null || editing)

    // with onUnfocusClick set,
    //   clicking the field executes the action
    //   clicking the icon sets the focus
    //   the unfocused field is disabled so it needs two steps
    //     first setting editing to recompose it enabled
    //     then requesting the focus
    if (onUnfocusedClick != null)
        LaunchedEffect(editing) {
            if (editing)
                focusRequester.requestFocus()
        }

    if (focusInitially)
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

    OutlinedTextField(
        modifier = modifier
            .testTag("input")
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                editing = focusState.isFocused
            },
        value = input.value,
        enabled = clickTextToEdit,
        placeholder = { Text(text = placeholder, color = Color.Gray) },
        singleLine = false,
        maxLines = 5,
        colors = TextFieldDefaults.colors().copy(
            unfocusedIndicatorColor = TextFieldDefaults.colors().unfocusedIndicatorColor.copy(alpha = 0.2f),
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            disabledTextColor = MaterialTheme.colorScheme.primary,
            disabledTrailingIconColor = TextFieldDefaults.colors().unfocusedTrailingIconColor
        ),
        trailingIcon = trailingIcon ?: {
            val spacing = 13.dp
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier
                    .padding(
                        start = spacing,
                        end = spacing
                    )
            ) {
                if (editing) {
                    Icon(
                        imageVector = Phosphor.X,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                input.value = TextFieldValue("")
                            }
                    )
                    if (!submitEachChange)
                        Icon(
                            imageVector = Phosphor.ArrowUUpLeft,
                            contentDescription = null,
                            modifier = Modifier
                                .clickable {
                                    input.value = text
                                    focusManager.clearFocus()
                                }
                        )
                    //Icon(
                    //    imageVector = Phosphor.Check,
                    //    contentDescription = null,
                    //    modifier = Modifier
                    //        .clickable {
                    //            submit()
                    //        }
                    //)
                } else {
                    Icon(
                        modifier = Modifier
                            .clickable {
                                editing = true
                            },
                        imageVector = Phosphor.Pencil,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        keyboardActions = KeyboardActions(
            onDone = {
                submit()
            }
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            autoCorrect = false
        ),
        onValueChange = {
            input.value = it
            if (submitEachChange)
                submit(false)
        }
    )
}

@Composable
fun TextInput(
    text: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailingIcon: (@Composable () -> Unit)? = null,
    focusInitially: Boolean = false,
    submitEachChange: Boolean = false,
    onClick: (() -> Unit)? = null,
    onSubmit: (String) -> Unit = {},
) {
    var textFieldValue by remember(text) { mutableStateOf(TextFieldValue(text)) }

    TextInput(
        text = textFieldValue,
        modifier = modifier,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        focusInitially = focusInitially,
        submitEachChange = submitEachChange,
        onUnfocusedClick = onClick,
    ) {
        textFieldValue = it
        onSubmit(it.text)
    }
}

@Preview
@Composable
fun TextInputPreview() {

    var textValue by remember { mutableStateOf(TextFieldValue("text value")) }
    var text by remember { mutableStateOf("text") }
    var longtext by remember { mutableStateOf("long text which is too long for the space and causes overflow which may push the icon out and other misbehaviours") }

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        TextInput(text = textValue) { textValue = it }
        TextInput(text = textValue) { textValue = it }
        TextInput(text = text) { text = it }
        TextInput(text = text) { text = it }
        TextInput(text = longtext, focusInitially = true) { longtext = it }
    }
}


var devToolsTab = mutableStateOf("")
val devToolsSearch = mutableStateOf(TextFieldValue(""))

val devToolsTabs = listOf<Pair<String, @Composable () -> Any>>(
    "SUPPORT" to { DevSupportTab() },
    "logs" to { DevLogsTab() },
    "log" to { DevLogTab() },
    "infolog" to { DevInfoLogTab() },
    "tools" to { DevToolsTab() },
    "term" to { DevTerminalTab() },
    "devsett" to { DevSettingsTab() },
    "plugins" to { DevPluginsTab() },
) + if (!NeoApp.isRelease) listOf<Pair<String, @Composable () -> Any>>(
    //"refreshScreen" to { OABX.context.recreateActivities(); devToolsTab.value = "" },
    //"invBackupLoc" to { FileUtils.invalidateBackupLocation() ; devToolsTab.value = "" },
    //"updateAppTables" to { OABX.context.updateAppTables() ; devToolsTab.value = "" },
    //"findBackups" to { OABX.context.findBackups() ; devToolsTab.value = "" },
) else emptyList()


@Composable
fun DevInfoLogTab() {

    TerminalText(NeoApp.infoLogLines)
}

@Composable
fun DevLogsTab() {

    Logs()
}

@Composable
fun DevLogTab() {

    val lines = remember { mutableStateOf<List<String>>(listOf()) }

    LaunchedEffect(true) {
        launch {
            lines.value = NeoApp.lastLogMessages.toList()
            if (lines.value.isEmpty())
                lines.value = logRel()
        }
    }

    TerminalText(lines.value)
}

@Composable
fun DevSettingsTab() {

    var search by devToolsSearch
    val scroll = rememberScrollState(0)

    Column {
        TextInput(
            text = search,
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            placeholder = "search (and='+' or=',')",
            trailingIcon = {
                if (search.text.isEmpty())
                    Icon(
                        imageVector = Phosphor.MagnifyingGlass,
                        contentDescription = "search",
                        //tint = tint,
                        modifier = Modifier.size(ICON_SIZE_SMALL)
                    )
                else
                    Icon(
                        imageVector = Phosphor.X,
                        contentDescription = "clear",
                        //tint = tint,
                        modifier = Modifier
                            .size(ICON_SIZE_SMALL)
                            .clickable {
                                search =            // keep on it's own line for easier breakpoints
                                    TextFieldValue("")
                            }
                    )
            },
            submitEachChange = true,
            onSubmit = {
                search = it                        // keep in it's own line for easier breakpoints
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(scroll)
                .weight(1f)
        ) {
            if (search.text.isEmpty())
                DevPrefGroups()
            else {
                val alternates = search.text.split(',').map {
                    it.split("+")
                        .filter { key -> key.length >= 2 }
                }.filter { it.isNotEmpty() }
                PrefsGroup(
                    prefs =
                        Pref.prefGroups.values.flatten()
                            .filter { pref ->
                                pref.group !in listOf("persist", "kill") &&
                                        alternates.any { alt ->
                                            alt.all { key ->
                                                pref.key.contains(key, ignoreCase = true)
                                            }
                                        }
                            }.toPersistentList()
                )
            }
        }
    }
}

@Composable
fun DevDialog(
    onDismiss: () -> Unit,
    dialogUI: @Composable (() -> Unit),
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            dialogUI()
        }
    }
}

@Composable
fun MenuSelector(
    selectedOption: String,
    options: MutableSet<String>,
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedOption)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PluginEditor(plugin: Plugin? = null, onSubmit: (plugin: Plugin?) -> Unit) {

    var name by remember { mutableStateOf(TextFieldValue(plugin?.name ?: "")) }
    var editPlugin by remember { mutableStateOf(plugin) }
    var selectedType by remember {
        mutableStateOf(
            editPlugin?.let { Plugin.typeFor(it) } ?: Plugin.DEFAULT_TYPE
        )
    }
    val where = Plugin.displayPath(editPlugin?.file?.path ?: "")

    fun submit() {
        if ((plugin != null)
            && (plugin.name == (editPlugin?.name ?: ""))
            && (plugin::class != editPlugin?.let { it::class })
            && (!plugin.isBuiltin)
        ) {
            plugin.delete()
        }
        editPlugin?.ensureEditable()
        editPlugin?.save()
        onSubmit(editPlugin)
    }

    fun cancel() {
        onSubmit(null)
    }

    fun delete() {
        if (editPlugin?.isBuiltin == false)
            editPlugin?.delete()
        onSubmit(null)
    }

    fun share() {
        editPlugin?.file
            ?.also { SystemUtils.share(StorageFile(it)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            MenuSelector(selectedType, Plugin.pluginTypes.keys) {
                selectedType = it
            }
            Spacer(modifier = Modifier.weight(1f))
            if (editPlugin == null) {
                SimpleButton("Create") {
                    if (Plugin.userDir != null) {
                        val file = Plugin.fileFor(
                            dir = Plugin.userDir!!,
                            name = name.text,
                            type = selectedType
                        )!!
                        editPlugin = Plugin.createFrom(file = file)
                    }
                    editPlugin!!.save()
                }
            } else {
                SimpleButton(if (editPlugin?.isBuiltin != false) "Save Copy" else "Save") {
                    if (editPlugin != null && Plugin.userDir != null) {
                        try {
                            val file = Plugin.fileFor(
                                dir = Plugin.userDir!!,
                                name = name.text,
                                type = selectedType
                            )!!
                            if (Plugin.typeFor(editPlugin) != selectedType) {
                                val text = if (editPlugin is TextPlugin)
                                    (editPlugin as TextPlugin).text
                                else
                                    null
                                editPlugin = Plugin.createFrom(file = file)
                                text?.let { (editPlugin as TextPlugin).text = it }
                            }
                            editPlugin!!.file = file
                        } catch (_: Throwable) {
                        }
                    }
                    submit()
                }
            }
            SimpleButton("Share") {
                share()
            }
            if (editPlugin?.isBuiltin == false)
                SimpleButton("Delete") {
                    delete()
                }
            SimpleButton("Cancel") {
                cancel()
            }
        }
        TextInput(name, placeholder = "Name") {
            name = it
        }
        Text(
            text = where,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        editPlugin?.Editor()
    }
}


@Composable
fun PluginItem(
    plugin: Plugin,
    onChange: (plugin: Plugin) -> Unit,
    onEdit: (plugin: Plugin) -> Unit,
) {
    val path = Plugin.displayPath(plugin.file.path)
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (plugin.isBuiltin)
                MaterialTheme.colorScheme.surfaceContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        modifier = Modifier.clickable {
            onEdit(plugin)
        }
    ) {
        Row {
            Checkbox(checked = plugin.enabled, onCheckedChange = {
                plugin.enable(it)
                onChange(plugin)
            })
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .weight(1f)
            ) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PluginsPage() {

    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val pluginList = remember { mutableStateListOf<Plugin>() }
    //val pluginList by remember { derivedStateOf { getAll<Plugin>() } }

    fun reload() {
        pluginList.clear()
        Plugin.scan()
        pluginList.addAll(Plugin.getAll())
    }

    LaunchedEffect(true) {
        reload()
    }

    fun edit(plugin: Plugin?) {
        dialog = {
            DevDialog(onDismiss = { dialog = null }) {
                PluginEditor(plugin) {
                    dialog = null
                    reload()
                }
            }
        }
    }

    Column {
        Row {
            Spacer(modifier = Modifier.weight(1f))

            SimpleButton("New") {
                edit(null)
            }

            SimpleButton("Reload") {
                reload()
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val scope = this
            Plugin.pluginTypes.keys.forEach { type ->
                val typePlugins =
                    pluginList.filter { Plugin.typeFor(it) == type }.sortedBy { it.name }
                if (typePlugins.isNotEmpty()) {
                    scope.apply {
                        stickyHeader {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceDim
                                ),
                            ) {
                                Text(
                                    type,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }
                        items(typePlugins, key = { it.name }) { plugin ->
                            PluginItem(
                                plugin,
                                onChange = { reload() },
                                onEdit = { edit(plugin) },
                            )
                        }
                    }
                }
            }
        }
    }

    dialog?.let { it() }
}

@SuppressLint("SdCardPath")
@Preview
@Composable
fun PluginsPagePreview() {
    Plugin.setPlugins(
        listOf(
            "/data/user/0/com.machiav3lli.backup.hg42/files/plugin/test_app1.special_files",
            "/data/user/0/com.machiav3lli.backup.hg42/files/plugin/test_app2.special_files",
            "/storage/emulated/Android/data/com.machiav3lli.backup.hg42/files/plugin/test_ext.special_files",
        ).mapIndexed { index, path ->
            "test_files$index" to SpecialFilesPlugin(File(path))
        }.toMap()
    )

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        PluginsPage()
    }
}

@Composable
fun DevPluginsTab() {

    PluginsPage()
}


val pref_renameDamagedToERROR = LaunchPref(
    key = "dev-tool.renameDamagedToERROR",
    summary = "rename damaged backups from xxx to ${ERROR_PREFIX}xxx (e.g. damaged properties file, properties without directory, directory without properties).\nHint: search recursively for ${ERROR_PREFIX} in a capable file manager"
) {
    MainScope().launch(Dispatchers.IO) {
        NeoApp.beginBusy("renameDamagedToERROR")
        NeoApp.context.findBackupsAndClean(DamagedOp.RENAME)
        devToolsTab.value = "infolog"
        NeoApp.endBusy("renameDamagedToERROR")
    }
}

val pref_undoDamagedToERROR = LaunchPref(
    key = "dev-tool.undoDamagedToERROR",
    summary = "rename all ${ERROR_PREFIX}xxx back to xxx"
) {
    MainScope().launch(Dispatchers.IO) {
        NeoApp.beginBusy("undoDamagedToERROR")
        NeoApp.context.findBackupsAndClean(DamagedOp.UNDO)
        devToolsTab.value = "infolog"
        NeoApp.endBusy("undoDamagedToERROR")
    }
}

val pref_deleteERROR = LaunchPref(
    key = "dev-tool.deleteERROR",
    summary = "delete all ${ERROR_PREFIX}xxx"
) {
    MainScope().launch(Dispatchers.IO) {
        NeoApp.beginBusy("deleteERROR")
        NeoApp.context.findBackupsAndClean(DamagedOp.DELETE)
        devToolsTab.value = "infolog"
        NeoApp.endBusy("deleteERROR")
    }
}

val pref_savePreferences = LaunchPref(
    key = "dev-tool.savePreferences",
    summary = "save preferences to $PREFS_BACKUP_FILE, note, that the PASSWORD is NOT INCLUDED"
) {
    MainScope().launch(Dispatchers.IO) {
        val serialized = preferencesToSerialized()
        if (serialized.isNotEmpty()) {
            runCatching {
                NeoApp.backupRoot?.let { backupRoot ->
                    UndeterminedStorageFile(backupRoot, PREFS_BACKUP_FILE).let {
                        it.writeText(serialized)?.let {
                            NeoApp.addInfoLogText("saved ${it.name}")
                        }
                    }
                }
            }
        }
    }
}

val pref_loadPreferences = LaunchPref(
    key = "dev-tool.loadPreferences",
    summary = "load preferences from $PREFS_BACKUP_FILE, note, that the PASSWORD is NOT INCLUDED, please set it manually"
) {
    MainScope().launch(Dispatchers.IO) {
        runCatching {
            NeoApp.backupRoot?.let { backupRoot ->
                backupRoot.findFile(PREFS_BACKUP_FILE)?.let {
                    val serialized = it.readText()
                    preferencesFromSerialized(serialized)
                    NeoApp.addInfoLogText("loaded ${it.name}")
                    recreateActivities()
                }
            }
        }
    }
}

fun openFileManager(folder: StorageFile) {
    folder.uri?.let { uri ->
        MainScope().launch(Dispatchers.Default) {
            try {
                traceDebug { "uri = $uri" }
                when (1) {
                    0 -> {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_VIEW
                                flags = FLAG_ACTIVITY_NEW_TASK or
                                        FLAG_ACTIVITY_MULTIPLE_TASK or
                                        FLAG_ACTIVITY_LAUNCH_ADJACENT
                                setData(uri)
                                //setDataAndType(uri, "*/*")
                                //putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                                addCategory(Intent.CATEGORY_BROWSABLE)
                            }
                        NeoApp.activity?.startActivity(intent)
                    }

                    0 -> {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_GET_CONTENT
                                flags = FLAG_ACTIVITY_NEW_TASK or
                                        FLAG_ACTIVITY_MULTIPLE_TASK or
                                        FLAG_ACTIVITY_LAUNCH_ADJACENT
                                setData(uri)
                                //setDataAndType(uri, "*/*")
                                //putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                                addCategory(Intent.CATEGORY_BROWSABLE)
                            }
                        val chooser = Intent.createChooser(intent, "Browse")
                        NeoApp.activity?.startActivity(chooser)
                    }

                    0 -> {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_GET_CONTENT
                                flags = FLAG_ACTIVITY_NEW_TASK or
                                        FLAG_ACTIVITY_MULTIPLE_TASK or
                                        FLAG_ACTIVITY_LAUNCH_ADJACENT
                                setDataAndType(uri, "*/*")
                                //putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                        val chooser = Intent.createChooser(intent, "Browse")
                        NeoApp.activity?.startActivity(chooser)
                    }

                    0 -> {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_OPEN_DOCUMENT_TREE
                                //flags =
                                //    FLAG_ACTIVITY_NEW_TASK or
                                //            FLAG_ACTIVITY_MULTIPLE_TASK or
                                //            FLAG_ACTIVITY_LAUNCH_ADJACENT
                                setData(uri)
                                //setDataAndType(uri, "*/*")
                                //setDataAndType(uri, "resource/folder")
                                //setDataAndType(uri, "vnd.android.document/directory")
                                //setDataAndType(uri, EXTRA_MIME_TYPES)
                                //addCategory(CATEGORY_APP_FILES)
                            }
                        NeoApp.activity?.startActivity(intent)
                    }

                    1 -> {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_VIEW
                                flags =
                                    FLAG_ACTIVITY_NEW_TASK or
                                            FLAG_ACTIVITY_MULTIPLE_TASK or
                                            FLAG_ACTIVITY_LAUNCH_ADJACENT
                                //setData(uri)
                                //setDataAndType(uri, "*/*")
                                //setDataAndType(uri, "resource/folder")
                                setDataAndType(uri, "vnd.android.document/directory")
                                //putExtra(EXTRA_MIME_TYPES, arrayOf(
                                //    "vnd.android.document/directory",
                                //    "resource/folder",
                                //))
                                //addCategory(CATEGORY_APP_FILES)
                                //addCategory(Intent.CATEGORY_BROWSABLE)
                            }
                        NeoApp.context.startActivity(intent)
                    }

                    else -> {}
                }
                traceDebug { "ok" }
            } catch (e: Throwable) {
                logException(e, backTrace = true)
            }
        }
    }
}

val pref_openBackupDir = LaunchPref(
    key = "dev-tool.openBackupDir",
    summary = "open backup directory in associated app"
) {
    NeoApp.backupRoot?.let { openFileManager(it) }
}

@Composable
fun DevTerminalTab() {

    Terminal()
}

@Composable
fun DevToolsTab() {

    val scroll = rememberScrollState(0)

    val prefs = Pref.prefGroups["dev-tool"]?.toPersistentList() ?: persistentListOf()

    Column(
        modifier = Modifier
            .verticalScroll(scroll)
    ) {
        PrefsGroup(prefs = prefs) { pref ->
        }
    }
}

val pref_prepareSupport = LaunchPref(
    key = "dev-support.prepareSupport",
    summary = "prepare settings for usual support purposes"
) {
    Pref.prefGroups["dev-trace"]?.forEach {
        Pref.setPrefFlag(it.key, it.defaultValue as Boolean)
    }
    pref_trace.value = true
    traceDebug.pref.value = true
    pref_maxLogLines.value = 20_000
    pref_logToSystemLogcat.value = true
    pref_catchUncaughtException.value = true
    pref_autoLogExceptions.value = true
    pref_autoLogSuspicious.value = true
    pref_autoLogAfterSchedule.value = true
}

val pref_shareSupportLog = LaunchPref(
    key = "dev-support.shareSupportLog",
    summary = "create and share a support log"
) {
    MainScope().launch {
        supportInfoLogShare()
    }
}

val pref_afterSupport = LaunchPref(
    key = "dev-support.afterSupport",
    summary = "set settings to normal"
) {
    Pref.prefGroups["dev-trace"]?.forEach {
        Pref.setPrefFlag(it.key, it.defaultValue as Boolean)
    }
    pref_trace.value = true
    pref_maxLogLines.apply { value = defaultValue as Int }
    pref_logToSystemLogcat.apply { value = defaultValue as Boolean }
    pref_catchUncaughtException.apply { value = defaultValue as Boolean }
    pref_autoLogExceptions.apply { value = defaultValue as Boolean }
    pref_autoLogSuspicious.apply { value = defaultValue as Boolean }
    pref_autoLogAfterSchedule.apply { value = defaultValue as Boolean }
}

@Composable
fun DevSupportTab() {
    val scroll = rememberScrollState(0)

    val prefs = Pref.prefGroups["dev-support"]?.toPersistentList() ?: persistentListOf()

    Column(
        modifier = Modifier
            .verticalScroll(scroll)
    ) {
        PrefsGroup(prefs = prefs) { pref ->
        }
    }
}

@Composable
fun DevTools(
    expanded: MutableState<Boolean>,
    goto: String? = null,
    search: String? = null,
) {
    var tab by devToolsTab
    val activity = LocalActivity.current as NeoActivity

    LaunchedEffect(true) {
        goto?.let {
            devToolsTab.value = goto
        }
        search?.let {
            devToolsSearch.value = TextFieldValue(search)
        }
        if (devToolsTab.value.isEmpty())
            devToolsTab.value = "devsett"
    }

    val tempShowInfo = remember { mutableStateOf(false) }
    val showInfo = NeoApp.showInfoLog || tempShowInfo.value

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = AbsoluteRoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    //.wrapContentSize()
                    .padding(8.dp, 4.dp, 8.dp, 0.dp)
                    .combinedClickable(
                        onClick = { expanded.value = false },
                        onLongClick = { tab = "" }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight()
                        .background(Color.Transparent)
                ) {
                    TitleOrInfoLog(
                        title = "DevTools",
                        showInfo = showInfo,
                        tempShowInfo = tempShowInfo,
                        modifier = Modifier
                            .wrapContentHeight()
                            .combinedClickable(
                                onClick = {
                                    NeoApp.showInfoLog = NeoApp.showInfoLog.not()
                                    if (NeoApp.showInfoLog.not())
                                        tempShowInfo.value = false
                                },
                                onLongClick = {
                                }
                            )
                    )
                }
                //Text(text = tab, modifier = Modifier)
                RefreshButton(hideIfNotBusy = true)
                SimpleButton(
                    "          close          "
                ) {
                    expanded.value = false
                    try {
                        if (activity != null)
                        ;
                    } catch (e: Throwable) {
                        activity.restartApp()
                    }
                }
            }

            @Composable
            fun TabButton(name: String) {
                SimpleButton(
                    text = name,
                    important = (tab == name),
                ) {
                    if (tab != name)
                        tab = name
                    else {
                        tab = ""
                        MainScope().launch {
                            yield()
                            tab = name
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        top = 0.dp,
                        end = 8.dp,
                        bottom = 4.dp
                    )
                    .combinedClickable(
                        onClick = { expanded.value = false },
                        onLongClick = { tab = "" }
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                devToolsTabs.forEach {
                    TabButton(it.first)
                }
            }

            devToolsTabs.find { it.first == tab }?.let {
                it.second()
            }
        }
    }
}

@SuppressLint("SdCardPath")
@Preview
@Composable
fun DevToolsPreview() {
    Plugin.setPlugins(
        listOf(
            "/data/user/0/com.machiav3lli.backup.hg42/files/plugin/test_app1.special_files",
            "/data/user/0/com.machiav3lli.backup.hg42/files/plugin/test_app2.special_files",
            "/storage/emulated/Android/data/com.machiav3lli.backup.hg42/files/plugin/test_ext.special_files",
        ).mapIndexed { index, path ->
            "test_files$index" to SpecialFilesPlugin(File(path))
        }.toMap()
    )

    val expanded = remember { mutableStateOf(true) }
    var count by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .width(500.dp)
            .height(1000.dp)
    ) {
        Row {
            SimpleButton(if (expanded.value) "close" else "open") {
                expanded.value = expanded.value.not()
            }
            SimpleButton("count") {
                count++
                NeoApp.addInfoLogText("line $count")
            }
            SimpleButton("busy") {
                NeoApp.hitBusy(5000)
            }
        }
        if (expanded.value)
            DevTools(expanded)
    }
}
