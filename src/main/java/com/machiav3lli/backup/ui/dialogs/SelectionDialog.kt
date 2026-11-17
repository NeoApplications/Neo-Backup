package com.machiav3lli.backup.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.blockShadow
import com.machiav3lli.backup.ui.compose.component.DialogNegativeButton
import com.machiav3lli.backup.ui.compose.component.DialogPositiveButton
import com.machiav3lli.backup.ui.compose.component.MultiSelectionListItem
import com.machiav3lli.backup.ui.compose.component.WideSearchField
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList

@Composable
fun <T> MultiSelectionDialogUI(
    titleText: String,
    entryMap: PersistentMap<T, String>,
    selectedItems: PersistentList<T>,
    openDialogCustom: MutableState<Boolean>,
    withSearchBar: Boolean = false,
    extraContent: @Composable ColumnScope.() -> Unit = {},
    onSave: (List<T>) -> Unit,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(selectedItems) }
    val query = retain { mutableStateOf("") }
    val entryPairs = entryMap.toList()

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
            Text(text = titleText, style = MaterialTheme.typography.titleLarge)
            if (withSearchBar) {
                WideSearchField(
                    query = query.value,
                    onQueryChanged = {
                        query.value = it
                    },
                    onCleanQuery = {
                        query.value = ""
                    }
                )
            }
            LazyColumn(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(1f, false)
                    .blockShadow()
            ) {
                items(items = entryPairs.filter {
                    it.second.contains(query.value, true)
                }) { (key, label) ->
                    val isSelected = retain(selected, key) {
                        mutableStateOf(selected.contains(key))
                    }

                    MultiSelectionListItem(
                        text = label,
                        isChecked = isSelected.value,
                    ) {
                        selected = (if (it) selected.plus(key)
                        else selected.minus(key)).toPersistentList()
                    }
                }
            }

            extraContent()

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogNegativeButton(text = stringResource(id = R.string.dialogCancel)) {
                    openDialogCustom.value = false
                }
                DialogPositiveButton(text = stringResource(id = R.string.dialogOK)) {
                    onSave(selected)
                    openDialogCustom.value = false
                }
            }
        }
    }
}