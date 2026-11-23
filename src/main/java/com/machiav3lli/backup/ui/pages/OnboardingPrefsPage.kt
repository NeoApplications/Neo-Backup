package com.machiav3lli.backup.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.ColoringState
import com.machiav3lli.backup.data.preferences.PrefDelegate
import com.machiav3lli.backup.data.preferences.PrefEnum
import com.machiav3lli.backup.data.preferences.PrefList
import com.machiav3lli.backup.data.preferences.PrefTheme
import com.machiav3lli.backup.ui.compose.blockBorder
import com.machiav3lli.backup.ui.compose.component.ActionButton
import com.machiav3lli.backup.ui.compose.component.NeoPrefsGroup
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowRight
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.DSEnumPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.DSListPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.ThemePrefDialogUI
import kotlinx.collections.immutable.persistentListOf

@Composable
fun OnboardingPrefsPage(
    onNext: () -> Unit,
) {
    val openDialog = remember { mutableStateOf(false) }
    var dialogsPref by remember { mutableStateOf<PrefDelegate<out Any>?>(null) }

    Column {
        LazyColumn(
            modifier = Modifier
                .blockBorder()
                .weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            oboardingPrefGroups { pref ->
                dialogsPref = pref
                openDialog.value = true
            }
        }
        ActionButton(
            text = stringResource(id = R.string.dialog_start),
            icon = Phosphor.ArrowRight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            coloring = ColoringState.Positive,
            onClick = onNext,
        )
    }

    if (openDialog.value) {
        val currentPref = dialogsPref
        BaseDialog(onDismiss = { openDialog.value = false }) {
            when (currentPref) {
                is PrefList  -> DSListPrefDialogUI(
                    pref = currentPref,
                    openDialogCustom = openDialog,
                )

                is PrefEnum  -> DSEnumPrefDialogUI(
                    pref = currentPref,
                    openDialogCustom = openDialog,
                )

                is PrefTheme -> ThemePrefDialogUI(
                    pref = currentPref,
                    openDialogCustom = openDialog,
                )

                else         -> {}
            }
        }
    }
}

private fun LazyListScope.oboardingPrefGroups(onPrefDialog: (PrefDelegate<out Any>) -> Unit) {
    val userPrefs = persistentListOf(
        pref_languages,
        pref_neoTheme,
        pref_pathBackupFolder,
        pref_deviceLock,
        pref_biometricLock,
    )
    val servicePrefs = persistentListOf(
        pref_encryption,
        pref_numBackupRevisions,
        pref_restorePermissions,
        pref_compressionType,
        pref_compressionLevel,
    )
    val advancedPrefs = persistentListOf(
        pref_enableSpecialBackups,
        pref_giveAllPermissions,
    )

    item {
        NeoPrefsGroup(
            prefs = userPrefs,
            heading = stringResource(id = R.string.prefs_user_short),
            onPrefDialog = onPrefDialog
        )
    }
    item {
        NeoPrefsGroup(
            prefs = servicePrefs,
            heading = stringResource(id = R.string.prefs_service_short),
            onPrefDialog = onPrefDialog
        )
    }
    item {
        NeoPrefsGroup(
            prefs = advancedPrefs,
            heading = stringResource(id = R.string.prefs_advanced_short),
            onPrefDialog = onPrefDialog
        )
    }
}
