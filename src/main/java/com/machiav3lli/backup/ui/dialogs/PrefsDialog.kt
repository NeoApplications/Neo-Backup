package com.machiav3lli.backup.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.EnumPref
import com.machiav3lli.backup.data.entity.ListPref
import com.machiav3lli.backup.data.entity.StringPref
import com.machiav3lli.backup.data.preferences.PrefEnum
import com.machiav3lli.backup.data.preferences.PrefList
import com.machiav3lli.backup.ui.compose.blockShadow
import com.machiav3lli.backup.ui.compose.component.DialogNegativeButton
import com.machiav3lli.backup.ui.compose.component.DialogPositiveButton
import com.machiav3lli.backup.ui.compose.component.SelectableRow
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.Eye
import com.machiav3lli.backup.ui.compose.icons.phosphor.EyeSlash
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@Composable
fun EnumPrefDialogUI(
    pref: EnumPref,
    openDialogCustom: MutableState<Boolean>,
    onChanged: (() -> Unit) = {},
) {
    val context = LocalContext.current
    var selected by remember { mutableIntStateOf(pref.value) }
    val entryPairs = pref.entries.toList()

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = stringResource(pref.titleId), style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(1f, false)
                    .blockShadow()
            ) {
                items(items = entryPairs) {
                    val isSelected by remember {
                        derivedStateOf { selected == it.first }
                    }
                    SelectableRow(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        title = stringResource(id = it.second),
                        selectedState = isSelected
                    ) {
                        selected = it.first
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogNegativeButton(text = stringResource(id = R.string.dialogCancel)) {
                    openDialogCustom.value = false
                }
                DialogPositiveButton(text = stringResource(id = R.string.dialogSave)) {
                    if (pref.value != selected) {
                        pref.value = selected
                        onChanged()
                    }
                    openDialogCustom.value = false
                }
            }
        }
    }
}

@Composable
fun ListPrefDialogUI(
    pref: ListPref,
    openDialogCustom: MutableState<Boolean>,
    onChanged: (() -> Unit) = {},
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(pref.value) }
    val entryPairs = pref.entries.toList()

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = stringResource(pref.titleId), style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(1f, false)
                    .blockShadow()
            ) {
                items(items = entryPairs) {
                    val isSelected by remember {
                        derivedStateOf { selected == it.first }
                    }
                    SelectableRow(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        title = it.second,
                        selectedState = isSelected
                    ) {
                        selected = it.first
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogNegativeButton(text = stringResource(id = R.string.dialogCancel)) {
                    openDialogCustom.value = false
                }
                DialogPositiveButton(text = stringResource(id = R.string.dialogSave)) {
                    if (pref.value != selected) {
                        pref.value = selected
                        onChanged()
                    }
                    openDialogCustom.value = false
                }
            }
        }
    }
}

val RE_jumpChars = Regex("""[\t]""")
val RE_finishChars = Regex("""[\n]""")

@Composable
fun StringPrefDialogUI(
    pref: StringPref,
    isPrivate: Boolean = false,
    confirm: Boolean = false,
    openDialogCustom: MutableState<Boolean>,
    onChanged: (() -> Unit) = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val mainFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }
    var focusField by remember { mutableStateOf("main") }

    var savedValue by remember { mutableStateOf(TextFieldValue(if (isPrivate) "" else pref.value)) }
    var savedValueConfirm by remember { mutableStateOf(TextFieldValue("")) }
    var isEdited by remember { mutableStateOf(false) }
    var notMatching by remember { mutableStateOf(false) }

    val textColor by animateColorAsState(
        targetValue = if (isPrivate) {
            if (savedValue != savedValueConfirm)
                Color.Red
            else
                Color.Green
        } else
            MaterialTheme.colorScheme.onSurface, label = "textColor"
    )

    fun submit() {
        focusManager.clearFocus()
        if (confirm && (savedValue != savedValueConfirm)) {
            notMatching = true
            focusField = "confirm"
        } else {
            if (pref.value != savedValue.text) {
                pref.value = savedValue.text
                onChanged()
            }
            openDialogCustom.value = false
        }
    }

    LaunchedEffect(focusField) {
        delay(100)
        when (focusField) {
            "main"    -> {
                mainFocusRequester.requestFocus()
                val end = savedValue.text.length
                savedValue = TextFieldValue(savedValue.text, selection = TextRange(end, end))
            }

            "confirm" -> {
                if (confirm) {
                    confirmFocusRequester.requestFocus()
                    val end = savedValueConfirm.text.length
                    savedValueConfirm =
                        TextFieldValue(savedValueConfirm.text, selection = TextRange(end, end))
                }
            }
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        // from https://stackoverflow.com/questions/65304229/toggle-password-field-jetpack-compose
        var isPasswordVisible by remember { mutableStateOf(!isPrivate) }  // rememberSavable?

        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = stringResource(pref.titleId), style = MaterialTheme.typography.titleLarge)
            TextField(
                modifier = Modifier
                    .shadow(1.dp, MaterialTheme.shapes.medium)
                    .fillMaxWidth()
                    .focusRequester(mainFocusRequester),
                value = savedValue,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                onValueChange = {
                    isEdited = true
                    if (it.text.contains(RE_finishChars)) {
                        if (confirm)
                            focusField = "confirm"
                        else
                            submit()
                    } else if (it.text.contains(RE_jumpChars)) {
                        if (confirm)
                            focusField = "confirm"
                    } else
                        savedValue = it         // only save when no control char
                },
                visualTransformation = if (isPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = if (isPrivate) KeyboardType.Password else KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (confirm)
                        focusField = "confirm"
                    else
                        submit()
                }),
                trailingIcon = {
                    Row {
                        if (isPrivate)
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Phosphor.Eye else Phosphor.EyeSlash,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        IconButton(onClick = { savedValue = TextFieldValue("") }) {
                            Icon(
                                imageVector = Phosphor.X,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
            if (confirm) {
                TextField(
                    modifier = Modifier
                        .shadow(1.dp, MaterialTheme.shapes.medium)
                        .fillMaxWidth()
                        .focusRequester(confirmFocusRequester),
                    value = savedValueConfirm,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    onValueChange = {
                        isEdited = true
                        if (it.text.contains(RE_finishChars))
                            submit()
                        else if (it.text.contains(RE_jumpChars))
                            focusField = "main"
                        else
                            savedValueConfirm = it      // only save when no control char
                    },
                    visualTransformation = if (isPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        submit()
                    }),
                    trailingIcon = {
                        Row {
                            if (isPrivate)
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Phosphor.Eye else Phosphor.EyeSlash,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            IconButton(onClick = { savedValueConfirm = TextFieldValue("") }) {
                                Icon(
                                    imageVector = Phosphor.X,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    },
                )
            }
            AnimatedVisibility(visible = notMatching) {
                Text(
                    //TODO could also be used with other than passwords (confirm != isPrivate)
                    text = stringResource(id = R.string.prefs_password_match_false),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogNegativeButton(text = stringResource(id = R.string.dialogCancel)) {
                    openDialogCustom.value = false
                }
                DialogPositiveButton(text = stringResource(id = R.string.dialogSave)) {
                    submit()
                }
            }
        }
    }
}

@Composable
fun DSEnumPrefDialogUI(
    pref: PrefEnum,
    openDialogCustom: MutableState<Boolean>,
    onChanged: (() -> Unit) = {},
) {
    val currentValue by pref.state
    var selected by remember(currentValue) { mutableIntStateOf(currentValue) }
    val entryPairs = pref.entries.toList()

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = stringResource(pref.titleId), style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(1f, false)
                    .blockShadow()
            ) {
                items(items = entryPairs) {
                    SelectableRow(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        title = stringResource(id = it.second),
                        selectedState = selected == it.first
                    ) {
                        selected = it.first
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogNegativeButton(text = stringResource(id = R.string.dialogCancel)) {
                    openDialogCustom.value = false
                }
                DialogPositiveButton(text = stringResource(id = R.string.dialogSave)) {
                    runBlocking {
                        if (pref.value != selected) {
                            pref.set(selected)
                            onChanged()
                        }
                    }
                    openDialogCustom.value = false
                }
            }
        }
    }
}

@Composable
fun DSListPrefDialogUI(
    pref: PrefList,
    openDialogCustom: MutableState<Boolean>,
    onChanged: (() -> Unit) = {},
) {
    val currentValue by pref.state
    var selected by remember(currentValue) { mutableStateOf(currentValue) }
    val entryPairs = pref.entries.toList()

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = stringResource(pref.titleId), style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(1f, false)
                    .blockShadow()
            ) {
                items(items = entryPairs) {
                    SelectableRow(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        title = it.second,
                        selectedState = selected == it.first
                    ) {
                        selected = it.first
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogNegativeButton(text = stringResource(id = R.string.dialogCancel)) {
                    openDialogCustom.value = false
                }
                DialogPositiveButton(text = stringResource(id = R.string.dialogSave)) {
                    runBlocking {
                        if (pref.value != selected) {
                            pref.set(selected)
                            onChanged()
                        }
                    }
                    openDialogCustom.value = false
                }
            }
        }
    }
}
