package com.machiav3lli.backup.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.component.DialogNegativeButton
import com.machiav3lli.backup.ui.compose.component.DialogPositiveButton
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.Eye
import com.machiav3lli.backup.ui.compose.icons.phosphor.EyeSlash
import com.machiav3lli.backup.ui.compose.icons.phosphor.X

@Composable
fun KeyDialogUI(
    @StringRes titleId: Int,
    onDismiss: () -> Unit,
    onAction: ((String, String) -> Unit),
) {
    val focusManager = LocalFocusManager.current
    val userIdFR = remember { FocusRequester() }
    val passcodeFR = remember { FocusRequester() }

    var userId by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }

    val submit: () -> Unit = {
        if (userId.isNotBlank()) {
            focusManager.clearFocus()
            onAction(userId, passcode)
            onDismiss()
        } else userIdFR.requestFocus()
    }

    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        var isPasscodeVisible by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(titleId),
                style = MaterialTheme.typography.titleLarge
            )
            TextField(
                modifier = Modifier
                    .shadow(1.dp, MaterialTheme.shapes.medium)
                    .fillMaxWidth()
                    .focusRequester(userIdFR),
                value = userId,
                label = { Text(text = stringResource(R.string.user_id)) },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                onValueChange = { userId = it },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                ),
                isError = userId.isEmpty(),
                keyboardActions = KeyboardActions(onDone = { passcodeFR.requestFocus() }),
                trailingIcon = {
                    Row {
                        IconButton(
                            shapes = IconButtonDefaults.shapes(),
                            onClick = { userId = "" },
                        ) {
                            Icon(
                                imageVector = Phosphor.X,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
            TextField(
                modifier = Modifier
                    .shadow(1.dp, MaterialTheme.shapes.medium)
                    .fillMaxWidth()
                    .focusRequester(passcodeFR),
                value = passcode,
                label = { Text(text = stringResource(R.string.passcode)) },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                onValueChange = {
                    passcode = it
                },
                visualTransformation = if (isPasscodeVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    Row {
                        IconButton(
                            shapes = IconButtonDefaults.shapes(),
                            onClick = { isPasscodeVisible = !isPasscodeVisible },
                        ) {
                            Icon(
                                imageVector = if (isPasscodeVisible) Phosphor.Eye else Phosphor.EyeSlash,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(
                            shapes = IconButtonDefaults.shapes(),
                            onClick = { passcode = "" },
                        ) {
                            Icon(
                                imageVector = Phosphor.X,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogNegativeButton(
                    text = stringResource(id = R.string.dialogCancel),
                    onClick = onDismiss
                )
                DialogPositiveButton(
                    text = stringResource(id = R.string.dialogSave),
                    onClick = submit
                )
            }
        }
    }
}