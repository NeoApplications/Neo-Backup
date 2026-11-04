package com.machiav3lli.backup.ui.compose.component

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.MODE_ALL
import com.machiav3lli.backup.MODE_UNSET
import com.machiav3lli.backup.MenuAction
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.batchModes
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.entity.IntPref
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.preferences.traceContextMenu
import com.machiav3lli.backup.data.repository.SelectionsRepository
import com.machiav3lli.backup.manager.handler.BackupRestoreHelper
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.unexpectedException
import com.machiav3lli.backup.manager.handler.ShellCommands
import com.machiav3lli.backup.manager.handler.ShellCommands.Companion.currentProfile
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArchiveTray
import com.machiav3lli.backup.ui.compose.icons.phosphor.Check
import com.machiav3lli.backup.ui.compose.icons.phosphor.Play
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import com.machiav3lli.backup.ui.pages.pref_fixNavBarOverlap
import com.machiav3lli.backup.utils.SystemUtils.numCores
import com.machiav3lli.backup.utils.SystemUtils.runParallel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.compose.koinInject
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

const val logEachN = 1000L

val yesNo = listOf(
    "yes" to "no",
    "really!" to "oh no!",
    "yeah" to "forget it"
)

@Composable
fun Confirmation(
    expanded: MutableState<Boolean>,
    text: String = "Are you sure?",
    onAction: () -> Unit = {},
) {
    val (yes, no) = yesNo.random()
    DropdownMenuItem(
        leadingIcon = { Icon(Phosphor.Check, null, tint = Color.Green) },
        text = { Text(yes) },
        onClick = {
            expanded.value = false
            onAction()
        }
    )
    DropdownMenuItem(
        leadingIcon = { Icon(Phosphor.X, null, tint = Color.Red) },
        text = { Text(no) },
        onClick = {
            expanded.value = false
        }
    )
}

@Composable
fun DataPartsSelector(
    result: MutableIntState,
) {
    //TODO hg42 add sizes

    batchModes.forEach { (mode, name) ->
        DropdownMenuItem(
            leadingIcon = {
                if ((result.intValue and mode) != 0)
                    Icon(Phosphor.Check, null, tint = Color.Green)
                else
                    Icon(Phosphor.X, null, tint = Color.Red)
            },
            text = { Text(name) },
            onClick = {
                result.intValue = result.intValue xor mode
            }
        )
    }
}

val persist_batchMode = IntPref(
    key = "persist.batchMode",
    entries = (MODE_UNSET..MODE_ALL).toList(),
    defaultValue = MODE_ALL
)

@Composable
fun SelectDataParts(
    expanded: MutableState<Boolean>,
    onAction: (mode: Int) -> Unit = {},
) {
    val mode = remember { mutableIntStateOf(persist_batchMode.value) }

    DataPartsSelector(mode)

    HorizontalDivider() //--------------------------------------------------------------------------

    DropdownMenuItem(
        leadingIcon = { Icon(Phosphor.Play, null) },
        text = { Text("doit!") },
        onClick = {
            expanded.value = false
            persist_batchMode.value = mode.intValue
            onAction(mode.intValue)
        }
    )
}

@Composable
fun TextInputMenuItem(
    text: String = "",
    placeholder: String = "",
    trailingIcon: ImageVector? = null,
    onAction: (String) -> Unit = {},
) {
    val input = remember { mutableStateOf(text) }
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(textFieldFocusRequester) {
        delay(100.milliseconds)
        textFieldFocusRequester.requestFocus()
    }

    fun submit() {
        focusManager.clearFocus()
        onAction(input.value)
    }

    DropdownMenuItem(
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .testTag("input")
                    .focusRequester(textFieldFocusRequester),
                value = input.value,
                placeholder = { Text(text = placeholder, color = Color.Gray) },
                singleLine = true,
                trailingIcon = {
                    trailingIcon?.let { icon ->
                        IconButton(onClick = { submit() }) {
                            Icon(icon, null)
                        }
                    }
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        submit()
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false
                ),
                onValueChange = {
                    if (it.contains("\n")) {
                        input.value = it.replace("\n", "")
                        submit()
                    } else
                        input.value = it
                }
            )
        },
        onClick = {}
    )
}

@Composable
fun Selections(
    action: MenuAction,
    selection: PersistentSet<String> = persistentSetOf(),
    blocklist: PersistentSet<String> = persistentSetOf(),
    schedules: PersistentList<Schedule> = persistentListOf(),
    onUpdateBlocklist: (Set<String>) -> Unit = {},
    onUpdateSchedule: (Schedule) -> Unit = {},
    onAction: (Set<String>) -> Unit = {},
) {
    val selectionsRepo: SelectionsRepository = koinInject()
    val scope = rememberCoroutineScope()
    val savedSelections by produceState(initialValue = emptyList()) {
        value = selectionsRepo.getSelections()
    }

    // Selections
    if (savedSelections == null)
        DropdownMenuItem(
            text = { Text("--- no selections dir ---") },
            onClick = {}
        )
    else if (savedSelections?.isEmpty() == true)
        DropdownMenuItem(
            text = { Text("--- no saved selections ---") },
            onClick = {}
        )
    else {
        DropdownMenuItem(
            enabled = false, onClick = {},
            text = { Text("--- selections saved ---") }
        )
        savedSelections?.forEach { savedSelection ->
            DropdownMenuItem(
                text = { Text("  ${savedSelection.name}") },
                onClick = {
                    scope.launch {
                        when (action) {
                            MenuAction.GET -> {
                                val newSelection = selectionsRepo.loadSelection(savedSelection.name)
                                onAction(newSelection)
                            }

                            MenuAction.PUT -> {
                                selectionsRepo.saveSelection(savedSelection.name, selection)
                                onAction(selection)
                            }

                            MenuAction.DEL -> {
                                selectionsRepo.deleteSelection(savedSelection.name)
                                onAction(selection)
                            }
                        }
                    }
                }
            )
        }
    }

    if (action != MenuAction.DEL) {

        // Schedules
        if (schedules.isEmpty())
            DropdownMenuItem(
                text = { Text("--- no schedules ---") },
                onClick = {}
            )
        else {
            DropdownMenuItem(
                enabled = false, onClick = {},
                text = { Text("--- schedules include ---") }
            )
            schedules.forEach { schedule ->
                schedule.name.let { name ->
                    DropdownMenuItem(
                        text = { Text("  $name") },
                        onClick = {
                            when (action) {
                                MenuAction.GET -> {
                                    val newSelection = schedule.customList
                                    onAction(newSelection)
                                }

                                MenuAction.PUT -> {
                                    scope.launch {
                                        onUpdateSchedule(
                                            schedule.copy(customList = selection.toSet())
                                        )
                                    }.invokeOnCompletion {
                                        onAction(selection)
                                    }
                                }
                            }
                        }
                    )
                }
            }
            DropdownMenuItem(
                enabled = false, onClick = {},
                text = { Text("--- schedules exclude ---") }
            )
            schedules.forEach { schedule ->
                schedule.name.let { name ->
                    DropdownMenuItem(
                        text = { Text("  $name") },
                        onClick = {
                            when (action) {
                                MenuAction.GET -> {
                                    val newSelection = schedule.blockList
                                    onAction(newSelection)
                                }

                                MenuAction.PUT -> {
                                    scope.launch {
                                        onUpdateSchedule(
                                            schedule.copy(blockList = selection.toSet())
                                        )
                                    }.invokeOnCompletion {
                                        onAction(selection)
                                    }
                                }

                                else           -> {}
                            }
                        }
                    )
                }
            }
        }

        // Block List
        DropdownMenuItem(
            enabled = false, onClick = {},
            text = { Text("--- global ---") }
        )
        DropdownMenuItem(
            text = { Text("  blocklist") },
            onClick = {
                when (action) {
                    MenuAction.GET -> {
                        val newSelection = blocklist
                        onAction(newSelection)
                    }

                    MenuAction.PUT -> {
                        onUpdateBlocklist(selection.toSet())
                        onAction(selection)
                    }
                }
            }
        )
    }
}

@Composable
fun SelectionGetMenu(
    blocklist: PersistentSet<String>,
    schedules: PersistentList<Schedule>,
    onAction: (Set<String>) -> Unit = {},
) {
    Selections(
        action = MenuAction.GET,
        blocklist = blocklist,
        schedules = schedules,
        onAction = onAction,
    )
}

@Composable
fun SelectionPutMenu(
    selection: PersistentSet<String>,
    blocklist: PersistentSet<String>,
    schedules: PersistentList<Schedule>,
    onUpdateBlocklist: (Set<String>) -> Unit,
    onUpdateSchedule: (Schedule) -> Unit,
    onAction: () -> Unit = {},
) {
    val selectionsRepo: SelectionsRepository = koinInject()
    val name = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    NeoApp.backupRoot?.let { _ ->
        TextInputMenuItem(
            text = name.value,
            placeholder = "new selection name",
            trailingIcon = Phosphor.ArchiveTray,
        ) {
            name.value = it
            scope.launch {
                selectionsRepo.saveSelection(name.value, selection)
                onAction()
            }
        }
    }

    Selections(
        action = MenuAction.PUT,
        selection = selection,
        blocklist = blocklist,
        schedules = schedules,
        onUpdateBlocklist = onUpdateBlocklist,
        onUpdateSchedule = onUpdateSchedule,
        onAction = { onAction() }
    )
}

@Composable
fun SelectionRemoveMenu(
    onAction: () -> Unit = {},
) {
    Selections(
        action = MenuAction.DEL,
        onAction = { onAction() }
    )
}

fun openSubMenu(
    subMenu: MutableState<(@Composable () -> Unit)?>,
    content: @Composable () -> Unit,
) {
    subMenu.value = {
        DropdownMenu(
            expanded = true,
            offset = DpOffset(100.dp, (-1000).dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            onDismissRequest = { subMenu.value = null }
        ) {
            if (pref_fixNavBarOverlap.value > 0) {
                Column(
                    modifier = Modifier
                        .padding(bottom = pref_fixNavBarOverlap.value.dp)
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

fun closeSubMenu(
    subMenu: MutableState<(@Composable () -> Unit)?>,
) {
    subMenu.value = null
}

fun List<Package>.withBackups() = filter { it.hasBackups }
fun List<Package>.installed() = filter { it.isInstalled }

// menu actions should continue even if the ui is left
val menuScope = MainScope()
val menuPool = Executors.newFixedThreadPool(numCores).asCoroutineDispatcher()
// Dispatchers.Default  unclear and can do anything in the future
// Dispatchers.IO       creates many threads (~65)

fun launchPackagesAction(
    action: String,
    todo: suspend () -> Unit,
) {
    CoroutineScope(menuPool).launch {
        val name = "menu.$action"
        try {
            NeoApp.beginBusy(name)
            todo()
        } catch (e: Throwable) {
            unexpectedException(e)
        } finally {
            val time = NeoApp.endBusy(name)
            NeoApp.addInfoLogText("$name: ${"%.3f".format(time / 1E9)} sec")
        }
    }
}

suspend fun forEachPackage(
    packages: List<Package>,
    action: String,
    selection: Set<String>,
    toggleSelection: (String) -> Unit,
    select: Boolean? = true,
    parallel: Boolean = true,
    todo: suspend (p: Package) -> Unit = {},
) {
    fun job(p: Package) = suspend {
        if (select == true && selection.contains(p.packageName))
            toggleSelection(p.packageName)
        traceContextMenu { "$action ${p.packageName}" }
        todo(p)
    }

    if (parallel) {
        runParallel(packages, scope = menuScope, pool = menuPool) {
            job(it)
            select?.let { selected ->
                if (selected != selection.contains(it.packageName))
                    toggleSelection(it.packageName)
            }
        }
    } else {
        packages.forEach {
            job(it)
            yield()
            select?.let { selected ->
                if (selected != selection.contains(it.packageName))
                    toggleSelection(it.packageName)
            }
        }
    }
}

fun launchEachPackage(
    // TODO revamp
    packages: List<Package>,
    action: String,
    selection: Set<String>,
    toggleSelection: (String) -> Unit,
    select: Boolean? = true,
    parallel: Boolean = true,
    todo: suspend (p: Package) -> Unit = {},
) {
    launchPackagesAction(action) {
        forEachPackage(
            packages = packages,
            action = action,
            selection = selection,
            toggleSelection = toggleSelection,
            select = select,
            parallel = parallel,
            todo = todo
        )
    }
}

fun NeoActivity.launchBackup(packages: List<Package>, mode: Int) {
    val selectedAndInstalled = packages.installed()
    startBatchAction(
        true,
        selectedAndInstalled.map { it.packageName },
        selectedAndInstalled.map { mode }
    )
}

fun NeoActivity.launchRestore(packages: List<Package>, mode: Int) {
    val packagesWithBackups = packages.withBackups()
    startBatchAction(
        false,
        packagesWithBackups.map { it.packageName },
        packagesWithBackups.map { mode }
    )
}

fun launchEnable(
    packages: List<Package>,
    selection: Set<String>,
    toggleSelection: (String) -> Unit,
) {
    launchEachPackage(packages, "enable", selection, toggleSelection, parallel = false) {
        val users = listOf(currentProfile.toString())
        //runAsRoot("pm enable ${it.packageName}")
        ShellCommands.enableDisable(users, it.packageName, true)
        Package.invalidateCacheForPackage(it.packageName)
    }
}

fun launchDisable(
    packages: List<Package>,
    selection: Set<String>,
    toggleSelection: (String) -> Unit,
) {
    launchEachPackage(packages, "disable", selection, toggleSelection, parallel = false) {
        val users = listOf(currentProfile.toString())
        //runAsRoot("pm disable ${it.packageName}")
        ShellCommands.enableDisable(users, it.packageName, false)
        Package.invalidateCacheForPackage(it.packageName)
    }
}

fun launchUninstall(
    packages: List<Package>,
    selection: Set<String>,
    toggleSelection: (String) -> Unit,
) {
    launchEachPackage(packages, "uninstall", selection, toggleSelection, parallel = false) {
        val users = listOf(currentProfile.toString())
        //runAsRoot("pm uninstall ${it.packageName}")
        ShellCommands.uninstall(users, it.packageName, it.apkPath, it.dataPath, it.isSystem)
        Package.invalidateCacheForPackage(it.packageName)
    }
}

fun launchDeleteBackups(
    packages: List<Package>,
    selection: Set<String>,
    toggleSelection: (String) -> Unit,
) {
    launchEachPackage(packages.withBackups(), "delete backups", selection, toggleSelection) {
        it.deleteAllBackups()
        Package.invalidateCacheForPackage(it.packageName)
    }
}

fun launchLimitBackups(
    packages: List<Package>,
    selection: Set<String>,
    toggleSelection: (String) -> Unit,
) {
    launchEachPackage(packages.withBackups(), "limit backups", selection, toggleSelection) {
        BackupRestoreHelper.housekeepingPackageBackups(it)
        Package.invalidateCacheForPackage(it.packageName)
    }
}

@Composable
fun MainPackageContextMenu(
    expanded: MutableState<Boolean>,
    packageItem: Package?,
    productsList: PersistentList<Package>,
    selection: PersistentSet<String>,
    blocklist: PersistentSet<String>,
    schedules: PersistentList<Schedule>,
    toggleSelection: (String) -> Unit,
    onUpdateBlocklist: (Set<String>) -> Unit,
    onUpdateSchedule: (Schedule) -> Unit,
    openSheet: (Package) -> Unit = {},
) {
    val visible = productsList
    val activity = LocalActivity.current as NeoActivity

    fun List<Package>.selected() = filter { selection.contains(it.packageName) }

    val selectedVisible by remember { mutableStateOf(visible.selected()) }   // freeze selection

    val subMenu = remember {                                    //TODO hg42 var/by ???
        mutableStateOf<(@Composable () -> Unit)?>(null)
    }
    subMenu.value?.let { it() }

    if (!expanded.value)
        closeSubMenu(subMenu)

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    DropdownMenu(
        expanded = expanded.value,
        //offset = DpOffset(20.dp, 0.dp),
        offset = with(LocalDensity.current) {
            DpOffset(
                offsetX.roundToInt().toDp(),
                offsetY.roundToInt().toDp()
            )
        },
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        onDismissRequest = { expanded.value = false }
    ) {

        if (!NeoApp.isRelease) {
            val number = remember { mutableIntStateOf(0) }
            DropdownMenuItem(
                text = { Text("test = ${number.intValue}") },
                onClick = {
                    openSubMenu(subMenu) {
                        TextInputMenuItem(
                            text = number.intValue.toString(),
                            onAction = {
                                number.intValue = it.toInt()
                                closeSubMenu(subMenu)
                                //expanded.value = false
                            }
                        )
                    }
                }
            )
        }

        packageItem?.let {

            DropdownMenuItem(
                enabled = false, onClick = {},
                text = { Text(packageItem.packageName) }
            )

            DropdownMenuItem(
                text = { Text("Open App Sheet") },
                onClick = {
                    expanded.value = false
                    openSheet(packageItem)
                }
            )

            HorizontalDivider() //------------------------------------------------------------------
        }

        DropdownMenuItem(
            enabled = false, onClick = {},
            text = { Text("selection:") }
        )

        DropdownMenuItem(
            text = { Text("Select Visible") },
            onClick = {
                expanded.value = false
                (visible.map { it.packageName }.toSet() - selection).forEach {
                    toggleSelection(it)
                }
            }
        )

        DropdownMenuItem(
            text = { Text("Deselect Visible") },
            onClick = {
                expanded.value = false
                visible.map { it.packageName }.toSet()
                    .intersect(selection)
                    .forEach {
                        toggleSelection(it)
                    }
            }
        )

        DropdownMenuItem(
            text = { Text("Deselect Not Visible") },
            onClick = {
                expanded.value = false
                (selection - visible.map { it.packageName }.toSet()).forEach {
                    toggleSelection(it)
                }
            }
        )

        DropdownMenuItem(
            text = { Text("Deselect All") },
            onClick = {
                expanded.value = false
                selection.forEach(toggleSelection)
            }
        )

        DropdownMenuItem(
            text = { Text("Get...") },
            onClick = {
                openSubMenu(subMenu) {
                    SelectionGetMenu(
                        blocklist = blocklist,
                        schedules = schedules,
                        onAction = { selectionLoaded ->
                            expanded.value = false
                            selection.forEach(toggleSelection)
                            selectionLoaded.forEach { toggleSelection(it) }
                        }
                    )
                }
            }
        )

        DropdownMenuItem(
            text = { Text("Put...") },
            onClick = {
                openSubMenu(subMenu) {
                    SelectionPutMenu(
                        selection = selection,
                        blocklist = blocklist,
                        schedules = schedules,
                        onUpdateBlocklist = onUpdateBlocklist,
                        onUpdateSchedule = onUpdateSchedule,
                    ) {
                        expanded.value = false
                        //launchSelect(selectedVisible)
                    }
                }
            }
        )

        DropdownMenuItem(
            text = { Text("Remove...") },
            onClick = {
                openSubMenu(subMenu) {
                    SelectionRemoveMenu {
                        expanded.value = false
                        //launchSelect(selectedVisible)
                    }
                }
            }
        )

        if (selection.isNotEmpty()) {

            HorizontalDivider() //------------------------------------------------------------------

            DropdownMenuItem(
                enabled = false, onClick = {},
                text = { Text("${selectedVisible.count()} selected and visible items:") }
            )

            DropdownMenuItem(
                text = { Text("Backup...") },
                onClick = {
                    openSubMenu(subMenu) {
                        SelectDataParts(expanded) { mode ->
                            expanded.value = false
                            activity.launchBackup(selectedVisible, mode)
                        }
                    }
                }
            )

            DropdownMenuItem(
                text = { Text("Restore...") },
                onClick = {
                    openSubMenu(subMenu) {
                        SelectDataParts(expanded) { mode ->
                            expanded.value = false
                            activity.launchRestore(selectedVisible, mode)
                        }
                    }
                }
            )

            HorizontalDivider() //------------------------------------------------------------------

            DropdownMenuItem(
                text = { Text("Enable") },
                onClick = {
                    expanded.value = false
                    launchEnable(selectedVisible, selection, toggleSelection)
                }
            )

            DropdownMenuItem(
                text = { Text("Disable...") },
                onClick = {
                    openSubMenu(subMenu) {
                        Confirmation(expanded) {
                            launchDisable(selectedVisible, selection, toggleSelection)
                        }
                    }
                }
            )

            DropdownMenuItem(
                text = { Text("Uninstall...") },
                onClick = {
                    openSubMenu(subMenu) {
                        Confirmation(expanded) {
                            launchUninstall(selectedVisible, selection, toggleSelection)
                        }
                    }
                }
            )

            HorizontalDivider() //------------------------------------------------------------------

            DropdownMenuItem(
                text = { Text("Delete All Backups...") },
                onClick = {
                    openSubMenu(subMenu) {
                        Confirmation(expanded) {
                            launchDeleteBackups(selectedVisible, selection, toggleSelection)
                        }
                    }
                }
            )

            DropdownMenuItem(
                text = { Text("Limit Backups...") },
                onClick = {
                    openSubMenu(subMenu) {
                        Confirmation(expanded) {
                            launchLimitBackups(selectedVisible, selection, toggleSelection)
                        }
                    }
                }
            )
        }
    }
}