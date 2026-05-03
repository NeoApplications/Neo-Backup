package com.machiav3lli.backup.ui.pages


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.data.preferences.NeoPrefs
import com.machiav3lli.backup.data.preferences.PrefBoolean
import com.machiav3lli.backup.data.preferences.PrefDelegate
import com.machiav3lli.backup.data.preferences.PrefEditString
import com.machiav3lli.backup.data.preferences.PrefEnum
import com.machiav3lli.backup.data.preferences.PrefInt
import com.machiav3lli.backup.data.preferences.PrefList
import com.machiav3lli.backup.ui.compose.component.NeoPrefsGroup
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.DSEnumPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.DSListPrefDialogUI
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.koin.java.KoinJavaComponent.get

@Composable
fun UserPrefsPage() {
    val context = LocalContext.current
    val openDialog = remember { mutableStateOf(false) }
    var dialogsPref by remember { mutableStateOf<PrefDelegate<out Any>?>(null) }

    val prefs: PersistentList<PrefDelegate<out Any>> = persistentListOf(
        pref_languages,
        pref_appTheme,
        //pref_appAccentColor,
        //pref_appSecondaryColor,
        pref_pathBackupFolder,
        pref_deviceLock,
        pref_biometricLock,
        pref_multilineInfoChips,
        pref_singularBackupRestore,
        pref_newAndUpdatedNotification,
        pref_squeezeNavText,
        pref_altNavBarItem,
        pref_altBackupDate,
        pref_altBlockLayout,
        pref_oldBackups,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            NeoPrefsGroup(prefs = prefs) { pref ->
                dialogsPref = pref
                openDialog.value = true
            }
        }
    }

    if (openDialog.value) {
        val currentPref = dialogsPref
        BaseDialog(onDismiss = { openDialog.value = false }) {
            when (currentPref) {
                //pref_languages,
                is PrefList -> DSListPrefDialogUI(
                    pref = currentPref,
                    openDialogCustom = openDialog,
                )

                //pref_appTheme,
                //pref_appAccentColor,
                //pref_appSecondaryColor,
                is PrefEnum -> DSEnumPrefDialogUI(
                    pref = currentPref,
                    openDialogCustom = openDialog,
                )

                else        -> {}
            }
        }
    }
}

// Access NeoPrefs via Koin
private val neoPrefs: NeoPrefs by lazy { get(NeoPrefs::class.java) }

// User preferences (DataStore)
val pref_languages: PrefList get() = neoPrefs.languages
val pref_appTheme: PrefEnum get() = neoPrefs.appTheme
val pref_appAccentColor: PrefEnum get() = neoPrefs.appAccentColor
val pref_appSecondaryColor: PrefEnum get() = neoPrefs.appSecondaryColor
val pref_pathBackupFolder: PrefEditString get() = neoPrefs.pathBackupFolder
val pref_deviceLock: PrefBoolean get() = neoPrefs.deviceLock
val pref_biometricLock: PrefBoolean get() = neoPrefs.biometricLock
val pref_multilineInfoChips: PrefBoolean get() = neoPrefs.multilineInfoChips
val pref_singularBackupRestore: PrefBoolean get() = neoPrefs.singularBackupRestore
val pref_newAndUpdatedNotification: PrefBoolean get() = neoPrefs.newAndUpdatedNotification
val pref_squeezeNavText: PrefBoolean get() = neoPrefs.squeezeNavText
val pref_altNavBarItem: PrefBoolean get() = neoPrefs.altNavBarItem
val pref_altBackupDate: PrefBoolean get() = neoPrefs.altBackupDate
val pref_altBlockLayout: PrefBoolean get() = neoPrefs.altBlockLayout
val pref_oldBackups: PrefInt get() = neoPrefs.oldBackups
