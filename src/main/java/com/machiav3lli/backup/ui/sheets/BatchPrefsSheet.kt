package com.machiav3lli.backup.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.ui.compose.component.NeoPrefsGroup
import com.machiav3lli.backup.ui.pages.pref_backupCache
import com.machiav3lli.backup.ui.pages.pref_backupDeviceProtectedData
import com.machiav3lli.backup.ui.pages.pref_backupExternalData
import com.machiav3lli.backup.ui.pages.pref_backupMediaData
import com.machiav3lli.backup.ui.pages.pref_backupNoBackupData
import com.machiav3lli.backup.ui.pages.pref_backupObbData
import com.machiav3lli.backup.ui.pages.pref_restoreCache
import com.machiav3lli.backup.ui.pages.pref_restoreDeviceProtectedData
import com.machiav3lli.backup.ui.pages.pref_restoreExternalData
import com.machiav3lli.backup.ui.pages.pref_restoreMediaData
import com.machiav3lli.backup.ui.pages.pref_restoreNoBackupData
import com.machiav3lli.backup.ui.pages.pref_restoreObbData
import kotlinx.collections.immutable.persistentListOf

@Composable
fun BatchPrefsSheet(backupBoolean: Boolean) {
    val prefs = if (backupBoolean) {
        persistentListOf(
            pref_backupDeviceProtectedData,
            pref_backupExternalData,
            pref_backupObbData,
            pref_backupMediaData,
            pref_backupNoBackupData,
            pref_backupCache,
        )
    } else {
        persistentListOf(
            pref_restoreDeviceProtectedData,
            pref_restoreExternalData,
            pref_restoreObbData,
            pref_restoreMediaData,
            pref_restoreNoBackupData,
            pref_restoreCache,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            NeoPrefsGroup(prefs = prefs)
        }
    }
}
