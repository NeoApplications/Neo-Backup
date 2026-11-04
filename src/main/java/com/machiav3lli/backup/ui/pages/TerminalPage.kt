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

import android.content.Context
import android.os.Environment
import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machiav3lli.backup.ICON_SIZE_SMALL
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.entity.Log
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.share
import com.machiav3lli.backup.manager.handler.ShellCommands
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.needFreshShell
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRootPipeOutCollectErr
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.suCommand
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.suInfo
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.sysInfo
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.utilBox
import com.machiav3lli.backup.manager.handler.ShellHandler.FileInfo.Companion.utilBoxInfo
import com.machiav3lli.backup.manager.handler.findBackups
import com.machiav3lli.backup.manager.handler.maxThreads
import com.machiav3lli.backup.manager.handler.usedThreadsByName
import com.machiav3lli.backup.ui.compose.blockBorderBottom
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.SimpleButton
import com.machiav3lli.backup.ui.compose.component.SmallButton
import com.machiav3lli.backup.ui.compose.component.TopBar
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowDown
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowUDownLeft
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowUp
import com.machiav3lli.backup.ui.compose.icons.phosphor.Equals
import com.machiav3lli.backup.ui.compose.icons.phosphor.GearSix
import com.machiav3lli.backup.ui.compose.icons.phosphor.MagnifyingGlass
import com.machiav3lli.backup.ui.compose.icons.phosphor.Play
import com.machiav3lli.backup.ui.compose.icons.phosphor.ShareNetwork
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import com.machiav3lli.backup.ui.compose.ifThen
import com.machiav3lli.backup.ui.compose.isAtBottom
import com.machiav3lli.backup.ui.compose.isAtTop
import com.machiav3lli.backup.utils.BACKUP_DATE_TIME_FORMATTER
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.SystemUtils.applicationIssuer
import com.machiav3lli.backup.utils.SystemUtils.getAndroidFolder
import com.machiav3lli.backup.utils.TraceUtils.listNanoTiming
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


//var terminalShell = shellDefaultBuilder().build()

fun shell(command: String, silent: Boolean = false): List<String> {
    val lines = mutableListOf<String>()
    var result: Shell.Result? = null
    try {
        //val env = "EPKG=\"${OABX.lastErrorPackage}\" ECMD=\"${OABX.lastErrorCommand}\""
        //result = runAsRoot("$env $command")
        result = runAsRoot(command, throwFail = false)
        if (!silent)
            lines += listOf(
                "--- # $command${if (!result.isSuccess) "  => ${result.code}" else "  => ok"}"
            )
    } catch (e: Throwable) {
        lines += listOfNotNull(
            "--- # $command -> ERROR",
            e::class.simpleName,
            e.message,
            e.cause?.message
        )
    }
    if (result != null) {
        lines += result.err.map { "? $it" }
        lines += result.out
    }
    return lines
}

fun appInfo(): List<String> {
    return listOf(
        "------ application",
        "package   = ${SystemUtils.packageName}",
        "version   = ${SystemUtils.versionName} : ${SystemUtils.versionCode}",
        NeoApp.context.applicationIssuer.let { "signed by = $it" },
    )
}

fun baseInfo(): List<String> {
    return appInfo() +
            listOf("------ system") +
            sysInfo() +
            listOf("------ superuser") +
            suInfo() +
            listOf("------ shell utility box") + utilBoxInfo()
}

fun extendedInfo() =
    baseInfo() +
            shell("getenforce") +
            shell("su --help") +
            shell("${utilBox.name} --version") +
            shell("${utilBox.name} --help")

fun logInt() =
    listOf("------ last internal log messages") +
            NeoApp.lastLogMessages

const val maxLogcat = "-t 100000"

fun logApp() =
    listOf("--- logcat app") +
            shell("logcat -d $maxLogcat --pid=${Process.myPid()} | grep -v SHELLOUT:")

fun logRel() =
    listOf("--- logcat related") +
            shell("logcat -d $maxLogcat | grep -v SHELLOUT: | grep -E '(machiav3lli.backup|NeoBackup>)'")

fun logSys() =
    listOf("--- logcat system") +
            shell("logcat -d $maxLogcat | grep -v SHELLOUT:")

fun dumpPrefs() =
    listOf("------ preferences") +
            publicPreferences(persist = true).map {
                "${it.group}.${it.key} = $it"
            }

fun dumpEnv() =
    listOf("------ environment") +
            shell("set")

fun dumpAlarms() =
    listOf("------ alarms") +
            shell("dumpsys alarm | sed -n '/Alarm.*machiav3lli[.]backup/,/PendingIntent/{p}'")

fun dumpTiming() =
    listOf("------ timing") +
            listNanoTiming()

fun Context.dumpDbSchedule() =
    listOf("------ schedule db") +
            shell("sqlite3 ${getDatabasePath("main.db")} \"SELECT * FROM schedule ORDER BY id ASC\"")

fun Context.dumpDbAppInfo() =
    listOf("------ app info db") +
            shell("sqlite3 ${getDatabasePath("main.db")} \"SELECT * FROM appinfo ORDER BY packageName ASC\"")

fun accessTest1(title: String, directory: String, comment: String) =
    listOf("--- $title") +
            shell("echo \"$directory/*\"", silent = true) +
            shell("echo \"$(ls $directory/ | wc -l) $comment\"", silent = true) +
            shell("ls -dAlZ $directory", silent = true) +
            run {
                val stringStream = ByteArrayOutputStream()
                runAsRootPipeOutCollectErr(
                    stringStream,
                    "echo \"$(ls $directory/ | wc -l) $comment - not using libsu\""
                )
                runAsRootPipeOutCollectErr(stringStream, "ls -dAlZ $directory")
                stringStream.toString().lines().filterNot { it.isEmpty() }
            }

fun accessTest() =
    listOf("------ access") +
            shell("readlink /proc/1/ns/mnt") +
            shell("readlink /proc/self/ns/mnt") +
            listOf(
                "--- when not using libsu (for streaming in backup/restore)",
                "uses: echo command | $suCommand"
            ) +
            accessTest1(
                "system app",
                "\$ANDROID_ASSETS", "packages (system app)"
            ) +
            accessTest1(
                "user app",
                "\$ANDROID_DATA/app", "packages (user app)"
            ) +
            accessTest1(
                "data",
                "\$ANDROID_DATA/user/${ShellCommands.currentProfile}", "packages (data)"
            ) +
            accessTest1(
                "dedata",
                "\$ANDROID_DATA/user_de/${ShellCommands.currentProfile}", "packages (dedata)"
            ) +
            accessTest1(
                "external",
                getAndroidFolder("data")?.path ?: "\$EXTERNAL_STORAGE/Android/data",
                "packages (external)"
            ) +
            accessTest1(
                "obb",
                getAndroidFolder("obb")?.path ?: "\$EXTERNAL_STORAGE/Android/obb",
                "packages (obb)"
            ) +
            accessTest1(
                "media",
                getAndroidFolder("media")?.path ?: "\$EXTERNAL_STORAGE/Android/media",
                "packages (media)"
            ) +
            accessTest1(
                "misc",
                "\$ANDROID_DATA/misc", "misc data"
            )


fun threadsInfo(): List<String> {
    val threads =
        synchronized(usedThreadsByName) { usedThreadsByName }.toMap()
    return listOf(
        "------ threads",
        "max: ${maxThreads.get()}",
        "used: (${threads.size})${threads.values}",
    )
}

fun lastErrorPkg(): List<String> {
    val pkg = NeoApp.lastErrorPackage
    return if (pkg.isNotEmpty()) {
        listOf("------ last error package: $pkg") +
                shell("ls -l \$ANDROID_DATA/user/0/$pkg") +
                shell("ls -l \$ANDROID_DATA/user_de/0/$pkg") +
                shell("ls -l \$EXTERNAL_STORAGE/Android/*/$pkg")
    } else {
        listOf("------ ? no last error package")
    }
}

fun lastErrorCommand(): List<String> {
    val cmds = NeoApp.lastErrorCommands
    return if (cmds.isNotEmpty()) {
        listOf("------ last error command") + cmds
    } else {
        listOf("------ ? no last error command")
    }
}

fun onErrorInfo(): List<String> {
    try {
        val logs = logInt() + logApp()
        return listOf("=== onError log", "") +
                baseInfo() +
                dumpPrefs() +
                dumpEnv() +
                lastErrorPkg() +
                lastErrorCommand() +
                logs
    } finally {
    }
}

fun textLog(lines: List<String>): StorageFile? {
    return LogsHandler.writeToLogFile(lines.joinToString("\n"))
}

fun textLogShare(lines: List<String>, temporary: Boolean = false) {
    if (lines.isNotEmpty()) {

        val text = lines.joinToString("\n")
        if (!temporary) {
            runCatching {
                LogsHandler.writeToLogFile(text)?.let { file ->
                    share(Log.fromFile(file), asFile = true)
                }
                return
            }
            // in error cases fall through to saving as a temporary file
        }

        val now = LocalDateTime.now()
        val fileName = "${BACKUP_DATE_TIME_FORMATTER.format(now)}.log.txt"

        runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .let { dir ->
                    File(dir, fileName).let { file ->
                        file.writeText(text)
                        SystemUtils.share(
                            StorageFile(file),
                            asFile = true
                        )
                    }
                }
        }.onFailure {
            //TODO hg42 we could try even more here
            logException(it)
        }
    }
}

fun supportInfo(title: String = ""): List<String> {
    try {
        val logs = logInt() + logRel()
        return listOf("=== ${title.ifEmpty { "support log" }}", "") +
                extendedInfo() +
                dumpPrefs() +
                dumpEnv() +
                dumpAlarms() +
                dumpTiming() +
                accessTest() +
                threadsInfo() +
                lastErrorPkg() +
                lastErrorCommand() +
                logs
    } finally {
    }
}

fun supportLog(title: String = "") {
    textLog(supportInfo(title))
}

fun supportInfoLogShare() {
    textLogShare(supportInfo())
}

@Composable
fun TerminalText(
    text: List<String>,
    modifier: Modifier = Modifier,
    limitLines: Int = 0,
    scrollOnAdd: Boolean = true,
) {

    val hscroll = rememberScrollState()
    val listState = rememberLazyListState()
    var wrap by retain { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var autoScroll by remember { mutableStateOf(scrollOnAdd) }

    val fontLineFactor = 1.4     //TODO hg42 factor 1.4 is empiric, how is it correctly?
    val searchFontFactor = 1.4
    val fontSize = 10.sp
    val lineHeightSp = fontSize * fontLineFactor
    val lineSpacing = 1.dp
    val lineHeight = with(LocalDensity.current) { lineHeightSp.toDp() }
    val totalLineHeight = lineHeight + lineSpacing

    var search by remember { mutableStateOf("") }

    if (scrollOnAdd && autoScroll) {
        LaunchedEffect(text.size) {
            listState.scrollToItem(index = text.size)
            autoScroll = true
        }
    }

    autoScroll = listState.isAtBottom()

    val lines = if (search.isNotEmpty())
        text.filter { it.contains(search, ignoreCase = true) } + (1..4).map { "" }
    else
        text

    Box(
        modifier = modifier
            .ifThen(limitLines == 0) { Modifier.fillMaxHeight() }
            .fillMaxWidth()
            .background(color = Color.Transparent),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .ifThen(limitLines == 0) { fillMaxHeight() }
                //.ifThen(!wrap) { horizontalScroll(hscroll) }
                .padding(0.dp)
                .background(color = Color(0.2f, 0.2f, 0.3f, alpha = 0.9f))
        ) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .ifThen(limitLines == 0) { fillMaxHeight() }
                        .ifThen(limitLines > 0) {
                            heightIn(
                                0.dp,
                                totalLineHeight * limitLines + lineSpacing
                            )
                        }
                        .fillMaxWidth()
                        .ifThen(!wrap) { horizontalScroll(hscroll) }
                        .padding(8.dp, 0.dp, 0.dp, 0.dp),
                    verticalArrangement = Arrangement.spacedBy(lineSpacing),
                    state = listState
                ) {
                    items(lines) {
                        val color =
                            when {
                                it.contains("error", ignoreCase = true) -> Color(1f, 0f, 0f)
                                it.contains("warning", ignoreCase = true) -> Color(1f, 0.5f, 0f)
                                it.contains("***") -> Color(0f, 1f, 1f)
                                it.startsWith("===") -> Color(1f, 1f, 0f)
                                it.startsWith("---") -> Color(
                                    0.8f,
                                    0.8f,
                                    0f
                                )

                                else -> Color.White
                            }
                        Text(
                            if (it == "") " " else it,     //TODO workaround for solved Bug_UI_SelectableContainerCrashOnEmptyText, remove if material3+compose versions will definitely not decreased anymore
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize,
                            lineHeight = lineHeightSp,
                            color = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            //val focusManager = LocalFocusManager.current

            TextField(
                modifier = Modifier
                    .weight(1f),
                value = search,
                singleLine = true,
                //placeholder = { Text(text = "search", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                    focusedTrailingIconColor = MaterialTheme.colorScheme.primary, //if (search.length > 0) Color.Transparent else overlayColor
                ),
                textStyle = TextStyle(
                    fontSize = fontSize * searchFontFactor,
                    lineHeight = lineHeightSp * searchFontFactor
                ),
                trailingIcon = {
                    if (search.isEmpty())
                        Icon(
                            imageVector = Phosphor.MagnifyingGlass,
                            contentDescription = "search",
                            modifier = Modifier.size(ICON_SIZE_SMALL)
                            //tint = tint,
                            //contentDescription = description
                        )
                    else
                        Icon(
                            imageVector = Phosphor.X,
                            contentDescription = "search",
                            modifier = Modifier
                                .size(ICON_SIZE_SMALL)
                                .clickable { search = "" }
                            //tint = tint,
                            //contentDescription = description,
                        )
                },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false
                    //imeAction = ImeAction.Done
                ),
                //keyboardActions = KeyboardActions(
                //    onDone = {
                //        todo
                //        search = ""
                //    }
                //),
                onValueChange = {
                    search = it
                }
            )
            SmallButton(icon = Phosphor.ShareNetwork) {
                textLogShare(lines)
            }
            SmallButton(icon = if (wrap) Phosphor.ArrowUDownLeft else Phosphor.Equals) {
                wrap = !wrap
            }
            // TODO move nav actions above the bar
            SmallButton(
                icon = Phosphor.ArrowUp,
                tint = if (listState.isAtTop()) Color.Transparent else null
            ) {
                scope.launch { listState.scrollToItem(0) }
            }
            SmallButton(
                icon = Phosphor.ArrowDown,
                tint = if (listState.isAtBottom()) Color.Transparent else null
            ) {
                autoScroll = true
                scope.launch { listState.scrollToItem(text.size) }
            }
        }
    }
}

@Composable
fun TerminalPage(
    title: String? = null,
    navigateUp: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (title != null)
                TopBar(
                    title = title,
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

        Terminal(modifier = Modifier.padding(paddingValues))
    }
}

@Composable
fun Terminal(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val output = remember { mutableStateListOf<String>() }
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val padding = 4.dp

    fun launch(todo: () -> Unit) {
        scope.launch(Dispatchers.Default) {
            todo()
        }
    }

    fun produce(produceLines: () -> List<String>) {
        launch {
            val hittingBusy = CoroutineScope(Dispatchers.Default)
            hittingBusy.launch {
                while (true) {
                    NeoApp.hitBusy(50)
                    delay(50.milliseconds)
                }
            }

            runCatching {
                focusManager.clearFocus()
            }

            val lines = produceLines()

            output.addAll(lines)

            hittingBusy.cancel()
        }
    }

    fun run(command: String) {
        produce { shell(command) }
    }

    DisposableEffect(Unit) {
        onDispose {
            needFreshShell()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            //.focusRequester(shellFocusRequester),
            value = command,
            singleLine = false,
            placeholder = { Text(text = "shell command", color = Color.Gray) },
            trailingIcon = {
                Row {
                    if (command.isNotEmpty())
                        RoundButton(icon = Phosphor.X) {
                            command = ""
                        }
                    RoundButton(icon = Phosphor.Play) {
                        command.removeSuffix("\n")
                        run(command)
                        command = ""
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    command.removeSuffix("\n")
                    run(command)
                    command = ""
                }
            ),
            onValueChange = {
                //if (it.endsWith("\n")) {
                //    run(command)
                //    command = ""
                //} else
                command = it
            }
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SimpleButton(
                "SUPPORT",
                important = true
            ) { launch { supportInfoLogShare() } }
            SimpleButton(
                "share",
                important = true
            ) { launch { textLogShare(output) } }
            SimpleButton("clear", important = true) { output.clear() }
            SimpleButton("log/int") { produce { logInt() } }
            SimpleButton("log/app") { produce { logApp() } }
            SimpleButton("log/rel") { produce { logRel() } }
            SimpleButton("log/all") { produce { logSys() } }
            SimpleButton("info") { produce { extendedInfo() } }
            SimpleButton("prefs") { produce { dumpPrefs() } }
            SimpleButton("env") { produce { dumpEnv() } }
            SimpleButton("alarms") { produce { dumpAlarms() } }
            SimpleButton("timing") { produce { dumpTiming() } }
            SimpleButton("threads") { produce { threadsInfo() } }
            SimpleButton("access") { produce { accessTest() } }
            SimpleButton("dbpkg") { produce { context.dumpDbAppInfo() } }
            SimpleButton("dbsch") { produce { context.dumpDbSchedule() } }
            SimpleButton("errInfo") { produce { lastErrorPkg() + lastErrorCommand() } }
            SimpleButton("err->cmd") {
                command =
                    if (NeoApp.lastErrorCommands.isNotEmpty())
                        NeoApp.lastErrorCommands.first()
                    else
                        "no error command"
            }
            SimpleButton("findBackups") { scope.launch { context.findBackups(forceTrace = true) } }
        }
        Box(
            modifier = Modifier
                .background(color = Color.Transparent)
                .padding(4.dp)
                .blockBorderBottom()
                .weight(1f)
        ) {
            TerminalText(
                modifier = Modifier.fillMaxWidth(),
                text = output,
                limitLines = 0,
                scrollOnAdd = true
            )
        }
    }
}

@Preview
@Composable
fun PreviewTerminal() {
    Box(
        modifier = Modifier
        //.height(500.dp)
        //.width(500.dp)
    ) {
        TerminalPage { }
    }
}

@Preview
@Composable
fun PreviewTerminalText() {

    val text = remember {
        mutableStateListOf(
            "0123456789.0123456789.0123456789.0123456789.0123456789.0123456789.0123456789.0123456789.",
            //"bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb bbbb.",
            //"cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc cccc.",
            //"dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd dddd.",
            //"eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee eeee.",
            "=== yyy",
            "--- xxx",
            "*** zzz",
            "this is an error",
            "this is an ERROR",
            "this is a warning",
            "this is a WARNING",
            *((8..100).map { "line $it" }.toTypedArray())
        )
    }

    LaunchedEffect(true) {
        launch {
            delay(3.seconds)
            text.add("some added text")
        }
    }

    Box(
        modifier = Modifier
            //.height(500.dp)
            //.width(500.dp)
            .padding(0.dp)
            .background(color = Color(0.2f, 0.2f, 0.3f))
    ) {
        TerminalText(text, limitLines = 20, scrollOnAdd = false)
    }
}

@Preview
@Composable
fun PreviewTestTextWidth() {

    val listState = rememberLazyListState()
    val lineSpacing = 8.dp
    val hscroll = rememberScrollState()
    val wrap = false

    Box(
        modifier = Modifier
            .height(500.dp)
            .fillMaxWidth()
            .background(color = Color.Blue)
            .padding(8.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                //.ifThen(!wrap) { horizontalScroll(hscroll) }
                .background(color = Color.Gray)
        ) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        //.fillMaxWidth()
                        .padding(8.dp)
                        .ifThen(!wrap) { horizontalScroll(hscroll) }
                        .background(color = Color.Yellow),
                    verticalArrangement = Arrangement.spacedBy(lineSpacing),
                    state = listState
                ) {
                    items(100) {
                        Text(
                            text = "line $it ###################################################",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(4.dp)
                                .border(1.dp, Color.Black)
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

