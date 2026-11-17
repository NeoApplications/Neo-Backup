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
package com.machiav3lli.backup.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.MODE_DATA
import com.machiav3lli.backup.MODE_DATA_DE
import com.machiav3lli.backup.MODE_DATA_EXT
import com.machiav3lli.backup.MODE_DATA_MEDIA
import com.machiav3lli.backup.MODE_DATA_OBB
import com.machiav3lli.backup.MODE_UNSET
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.ui.compose.component.CheckChip
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.Eye
import com.machiav3lli.backup.ui.compose.icons.phosphor.EyeSlash
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import com.machiav3lli.backup.utils.isPGPModeEnabled
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

@Composable
fun RestoreDialogUI(
    appPackage: Package,
    backup: Backup,
    openDialogCustom: MutableState<Boolean>,
    onAction: (mode: Int, password: String?) -> Unit,
) {
    var useOwnPassword by remember { mutableStateOf(false) }
    val passwordState = rememberTextFieldState()
    val modePairs = buildMap {
        if (backup.hasApk)
            set(MODE_APK, stringResource(id = R.string.radio_apk))
        if (backup.hasAppData)
            set(MODE_DATA, stringResource(id = R.string.radio_data))
        if (backup.hasDevicesProtectedData)
            set(MODE_DATA_DE, stringResource(id = R.string.radio_deviceprotecteddata))
        if (backup.hasExternalData)
            set(MODE_DATA_EXT, stringResource(id = R.string.radio_externaldata))
        if (backup.hasObbData)
            set(MODE_DATA_OBB, stringResource(id = R.string.radio_obbdata))
        if (backup.hasMediaData)
            set(MODE_DATA_MEDIA, stringResource(id = R.string.radio_mediadata))
    }

    MultiSelectionDialogUI(
        titleText = appPackage.packageLabel,
        entryMap = modePairs.toPersistentMap(),
        selectedItems = modePairs.keys.toPersistentList(),
        openDialogCustom = openDialogCustom,
        extraContent = {
            if (backup.isEncrypted && !isPGPModeEnabled()) RestorePasswordField(
                passwordState = passwordState,
                expanded = useOwnPassword,
            ) { checked ->
                useOwnPassword = checked
                if (!useOwnPassword) passwordState.clearText()
            }
        },
    ) { selected ->
        onAction(
            selected.fold(MODE_UNSET) { acc, s -> acc xor s },
            passwordState.text.toString()
                .takeIf { backup.isEncrypted && useOwnPassword && it.isNotBlank() },
        )
    }
}

@Composable
private fun RestorePasswordField(
    passwordState: TextFieldState,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val rejectControlChars = remember {
        InputTransformation {
            if (RE_finishChars.containsMatchIn(asCharSequence()) ||
                RE_jumpChars.containsMatchIn(asCharSequence())
            ) {
                revertAllChanges()
            }
        }
    }

    LaunchedEffect(expanded) {
        if (!expanded)
            isPasswordVisible = false
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CheckChip(
            modifier = Modifier.fillMaxWidth(),
            checked = expanded,
            textId = R.string.restore_password_override,
            checkedTextId = R.string.restore_password_override,
            onCheckedChange = onToggle,
        )
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedSecureTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    state = passwordState,
                    colors = OutlinedTextFieldDefaults.colors(),
                    shape = MaterialTheme.shapes.medium,
                    textObfuscationMode = if (isPasswordVisible) TextObfuscationMode.Visible
                    else TextObfuscationMode.Hidden,
                    inputTransformation = rejectControlChars,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = if (isPasswordVisible) KeyboardType.PasswordVisible
                        else KeyboardType.Password,
                    ),
                    supportingText = {
                        Text(stringResource(id = R.string.restore_password_override_summary))
                    },
                    trailingIcon = {
                        Row {
                            RoundButton(
                                icon = if (isPasswordVisible) Phosphor.Eye
                                else Phosphor.EyeSlash,
                                description = stringResource(id = R.string.prefs_password)
                            ) {
                                isPasswordVisible = !isPasswordVisible
                            }
                            RoundButton(
                                icon = Phosphor.X,
                                description = stringResource(id = R.string.dialogUndo)
                            ) {
                                passwordState.clearText()
                            }
                        }
                    },
                )
            }
        }
    }
}
