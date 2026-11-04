package com.machiav3lli.backup.ui.compose.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.data.entity.StringPref
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.suCommand
import com.machiav3lli.backup.ui.compose.mix

@Composable
fun SuCommandPreference(
    modifier: Modifier = Modifier,
    pref: StringPref,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
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
            LaunchedEffect(true) {
                pref.onChanged?.invoke(pref)
            }
            if (pref.value != suCommand) {
                Text(
                    "=> $suCommand",
                    color = Color.Cyan.mix(MaterialTheme.colorScheme.onSurface),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            TextInput(
                pref.value,
                modifier = Modifier.fillMaxWidth(),
            ) {
                pref.value = it
            }
        },
    )
}