package com.machiav3lli.backup.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowUUpLeft
import com.machiav3lli.backup.ui.compose.icons.phosphor.MagnifyingGlass
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import com.machiav3lli.backup.ui.compose.ifThenElse
import com.machiav3lli.backup.ui.compose.vertical
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.pages.pref_showInfoLogBar
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Float.max

@Composable
fun ProgressIndicator() {
    val busy by remember(NeoApp.busy.value) { NeoApp.busy }
    val progress by remember(
        NeoApp.progress.value.first,
        NeoApp.progress.value.second
    ) { NeoApp.progress }

    if (progress.first) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            color = MaterialTheme.colorScheme.primary,
            progress = { max(0.02f, progress.second) }
        )
    } else if (busy) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun GlobalIndicators() {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ProgressIndicator()
    }
}

@Composable
fun TitleOrInfoLog(
    title: String,
    showInfo: Boolean,
    tempShowInfo: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val infoLogText = NeoApp.getInfoLogText(n = 5, fill = "")
    val scroll = rememberScrollState(0)
    val scope = rememberCoroutineScope()
    val rotation by animateFloatAsState(
        targetValue = if (showInfo) -90f else 0f,
        label = "rotation"
    )

    LaunchedEffect(infoLogText) {
        tempShowInfo.value = true
        scope.launch {
            scroll.scrollTo(scroll.maxValue)
            delay(5000)
            tempShowInfo.value = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        Text(
            text = title,
            style = if (showInfo) MaterialTheme.typography.labelMedium
            else MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .ifThenElse(
                    boolean = showInfo,
                    modifier = { vertical() },
                    elseModifier = { fillMaxWidth() }
                )
                .graphicsLayer { rotationZ = rotation }
                .wrapContentHeight()
        )

        if (showInfo) Text(
            text = infoLogText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraSmall
                )
                .padding(horizontal = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    navigationAction: @Composable (() -> Unit) = {},
    actions: @Composable (RowScope.() -> Unit) = {},
) {
    val showDevTools = remember { mutableStateOf(false) }
    val tempShowInfo = remember { mutableStateOf(false) }
    val showInfo =
        !showDevTools.value && (NeoApp.showInfoLog || tempShowInfo.value) && pref_showInfoLogBar.value

    Box { // overlay TopBar and indicators
        Row(
            modifier = Modifier
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .height(72.dp)
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            navigationAction()
            title.takeIf { it.isNotBlank() }?.let { title ->
                TitleOrInfoLog(
                    title = title,
                    showInfo = showInfo,
                    tempShowInfo = tempShowInfo,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = {
                                if (pref_showInfoLogBar.value) {
                                    NeoApp.showInfoLog = !NeoApp.showInfoLog
                                }
                                if (!NeoApp.showInfoLog)
                                    tempShowInfo.value = false
                            },
                            onLongClick = {
                                showDevTools.value = true
                            }
                        )
                )
                if (showDevTools.value) {
                    BaseDialog(onDismiss = { showDevTools.value = false }) {
                        DevTools(expanded = showDevTools)
                    }
                }
            }
            actions()
        }

        // must be second item to overlay first
        GlobalIndicators()

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    title: String,
    expanded: MutableState<Boolean>,
    query: String,
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable() (RowScope.() -> Unit) = {},
) {
    val showDevTools = remember { mutableStateOf(false) }
    val tempShowInfo = remember { mutableStateOf(false) }
    val showInfo =
        !showDevTools.value && (NeoApp.showInfoLog || tempShowInfo.value) && pref_showInfoLogBar.value
    val (isExpanded, onExpanded) = remember { expanded }
    val enterPositive = expandHorizontally(expandFrom = Alignment.End)
    val exitPositive = shrinkHorizontally(shrinkTowards = Alignment.End)
    val enterNegative = expandHorizontally(expandFrom = Alignment.Start)
    val exitNegative = shrinkHorizontally(shrinkTowards = Alignment.Start)

    Box {
        Row(
            modifier = modifier
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .height(72.dp)
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            title.takeIf { it.isNotBlank() }?.let { title ->
                AnimatedVisibility(
                    modifier = Modifier.weight(1f),
                    visible = !isExpanded,
                    enter = enterNegative,
                    exit = exitNegative,
                ) {
                    TitleOrInfoLog(
                        title = title,
                        showInfo = showInfo,
                        tempShowInfo = tempShowInfo,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    if (pref_showInfoLogBar.value) {
                                        NeoApp.showInfoLog = !NeoApp.showInfoLog
                                    }
                                    if (!NeoApp.showInfoLog)
                                        tempShowInfo.value = false
                                },
                                onLongClick = {
                                    showDevTools.value = true
                                }
                            )
                    )
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = enterPositive,
                    exit = exitPositive,
                ) {
                    WideSearchField(
                        query = query,
                        showCloseButton = true,
                        onClose = {
                            onClose()
                            onExpanded(false)
                        },
                        onQueryChanged = onQueryChanged,
                        onCleanQuery = {
                            onQueryChanged("")
                        }
                    )
                }
                if (showDevTools.value) {
                    BaseDialog(onDismiss = { showDevTools.value = false }) {
                        DevTools(expanded = showDevTools)
                    }
                }
            }
            AnimatedVisibility(
                visible = !isExpanded,
                enter = enterPositive,
                exit = exitPositive,
            ) {
                Row { actions() }
            }
        }

        // must be second item to overlay first
        GlobalIndicators()

    }
}

@Composable
fun WideSearchField(
    query: String,
    modifier: Modifier = Modifier,
    label: String = stringResource(id = R.string.search),
    focusOnCompose: Boolean = true,
    showCloseButton: Boolean = false,
    onClose: () -> Unit = {},
    onCleanQuery: () -> Unit,
    onQueryChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }
    LaunchedEffect(textFieldFocusRequester) { if (focusOnCompose) textFieldFocusRequester.requestFocus() }

    var textFieldValue by remember {
        mutableStateOf(query)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onQueryChanged(it)
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(textFieldFocusRequester),
            shape = MaterialTheme.shapes.extraLarge,
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Phosphor.MagnifyingGlass,
                    contentDescription = stringResource(id = R.string.search),
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = textFieldValue.isNotEmpty(),
                    enter = expandHorizontally(expandFrom = Alignment.Start),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
                ) {
                    IconButton(onClick = {
                        textFieldValue = ""
                        onCleanQuery()
                    }) {
                        Icon(
                            imageVector = Phosphor.ArrowUUpLeft,
                            contentDescription = stringResource(id = R.string.clear_text)
                        )
                    }
                }
            },
            singleLine = true,
            label = { Text(text = label) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        if (showCloseButton) IconButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {
                textFieldValue = ""
                onCleanQuery()
                onClose()
            }) {
            Icon(
                imageVector = Phosphor.X,
                contentDescription = stringResource(id = R.string.dialogCancel)
            )
        }
    }
}

@Preview
@Composable
fun ProgressPreview() {
    var count by remember { mutableIntStateOf(0) }

    val maxCount = 4

    SideEffect {
        if (count >= 0)
            NeoApp.setProgress(count, maxCount)
        else if (count == -2)
            NeoApp.setProgress()
        else
            NeoApp.hitBusy(2000)
    }

    NeoApp.clearInfoLogText()
    repeat(10) { NeoApp.addInfoLogText("line $it") }
    NeoApp.setProgress(count, maxCount)

    LaunchedEffect(true) {
        MainScope().launch {
            while (count < maxCount) {
                NeoApp.beginBusy()
                NeoApp.addInfoLogText("count is $count")
                delay(1000)
                count = (count + 1) % (maxCount + 2)
                NeoApp.endBusy()
                if (count > maxCount)
                    NeoApp.setProgress()
                NeoApp.addInfoLogText("count is $count")
                delay(1000)
            }
        }
    }

    TopBar(
        title = if (count >= 0)
            "count $count"
        else if (count == -2)
            "off"
        else
            "busy",
    ) {
        Button(
            onClick = {
                count = (count + 3) % (maxCount + 3) - 2
            }
        ) {
            Text("$count")
        }
    }
}

@Preview
@Composable
fun VerticalPreview() {
    Row(
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            modifier = Modifier
                .vertical()
                .graphicsLayer { rotationZ = -90f },
            fontWeight = FontWeight.Bold,
            text = "vertical text"
        )
        Text(text = "horizontal")
    }
}
