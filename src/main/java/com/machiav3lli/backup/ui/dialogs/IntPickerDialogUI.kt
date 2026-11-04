/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2023  Antonios Hazim
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.component.DialogNegativeButton
import com.machiav3lli.backup.ui.compose.component.DialogPositiveButton
import kotlinx.collections.immutable.PersistentList
import kotlin.math.roundToInt

@Composable
fun IntPickerDialogUI(
    value: Int,
    defaultValue: Int,
    entries: PersistentList<Int>,
    openDialogCustom: MutableState<Boolean>,
    onSave: (Int) -> Unit,
) {
    val context = LocalContext.current
    var currentValue by remember {
        mutableIntStateOf(value)
    }
    val sliderState = remember {
        SliderState(
            value = (entries.indexOf(value).takeUnless { it == -1 }
                ?: entries.indexOf(defaultValue)).toFloat(),
            trackRange = 0f..entries.lastIndex.toFloat(),
            steps = entries.size - 2
        )
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.sched_interval),
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                Slider(
                    state = sliderState,
                    modifier = Modifier
                        .weight(1f, false)
                        .requiredHeight(24.dp),
                    onValueChange = {
                        sliderState.value = it
                    },
                    onValueChangeFinished = {
                        currentValue = entries[sliderState.value.roundToInt()]
                    }
                )
                Spacer(modifier = Modifier.requiredWidth(8.dp))
                Text(
                    text = currentValue.toString(),
                    modifier = Modifier.widthIn(min = 48.dp)
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
                    onSave(entries[sliderState.value.roundToInt()])
                    openDialogCustom.value = false
                }
            }
        }
    }
}