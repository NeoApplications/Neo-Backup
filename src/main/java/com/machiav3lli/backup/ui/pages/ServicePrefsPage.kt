package com.machiav3lli.backup.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.machiav3lli.backup.data.entity.PasswordPref
import com.machiav3lli.backup.data.preferences.NeoPrefs
import com.machiav3lli.backup.data.preferences.PrefBoolean
import com.machiav3lli.backup.data.preferences.PrefDelegate
import com.machiav3lli.backup.data.preferences.PrefEnum
import com.machiav3lli.backup.data.preferences.PrefInt
import com.machiav3lli.backup.data.preferences.PrefLaunch
import com.machiav3lli.backup.data.preferences.PrefList
import com.machiav3lli.backup.data.preferences.PrefString
import com.machiav3lli.backup.data.preferences.ServicePrefKey
import com.machiav3lli.backup.ui.compose.component.NeoPrefsGroup
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.DSEnumPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.DSListPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.DSStringPrefDialogUI
import kotlinx.collections.immutable.persistentListOf
import org.koin.java.KoinJavaComponent.get

@Composable
fun ServicePrefsPage() {
    val openDialog = remember { mutableStateOf(false) }
    var dialogsPref by remember { mutableStateOf<PrefDelegate<out Any>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        servicePrefGroups { pref ->
            dialogsPref = pref
            openDialog.value = true
        }
    }

    if (openDialog.value) {
        val currentPref = dialogsPref
        BaseDialog(onDismiss = { openDialog.value = false }) {
            when (currentPref) {
                is PrefList -> DSListPrefDialogUI(
                    pref = currentPref,
                    openDialogCustom = openDialog,
                )

                is PrefEnum -> DSEnumPrefDialogUI(
                    pref = currentPref,
                    openDialogCustom = openDialog
                )

                is PrefString -> {
                    val isPassword = currentPref.key == ServicePrefKey.ENCRYPTION_PASSWORD.name ||
                        currentPref.key == ServicePrefKey.PGP_PASSCODE.name
                    DSStringPrefDialogUI(
                        pref = currentPref,
                        isPrivate = isPassword,
                        confirm = isPassword,
                        openDialogCustom = openDialog
                    )
                }

                else         -> {}
            }
        }
    }
}

fun LazyListScope.servicePrefGroups(onPrefDialog: (PrefDelegate<out Any>) -> Unit) {
    val servicePrefs = persistentListOf(
        pref_encryption,
        pref_restorePermissions,
        pref_numBackupRevisions,
        pref_enableStorageCheck,
        pref_compressionType,
        pref_compressionLevel,
        pref_enableSessionInstaller,
        pref_installationPackage,
    )
    val backupServicePrefs = persistentListOf(
        pref_backupDeviceProtectedData,
        pref_backupExternalData,
        pref_backupObbData,
        pref_backupMediaData,
        pref_backupNoBackupData,
        pref_backupCache,
    )
    val restoreServicePrefs = persistentListOf(
        pref_restoreDeviceProtectedData,
        pref_restoreExternalData,
        pref_restoreObbData,
        pref_restoreMediaData,
        pref_restoreNoBackupData,
        pref_restoreCache,
    )

    item {
        NeoPrefsGroup(
            prefs = servicePrefs,
            onPrefDialog = onPrefDialog
        )
    }
    item {
        NeoPrefsGroup(
            prefs = backupServicePrefs,
            heading = stringResource(id = R.string.backup),
            onPrefDialog = onPrefDialog
        )
    }
    item {
        NeoPrefsGroup(
            prefs = restoreServicePrefs,
            heading = stringResource(id = R.string.restore),
            onPrefDialog = onPrefDialog
        )
    }
}

private val neoPrefs: NeoPrefs by lazy { get(NeoPrefs::class.java) }

val pref_encryption: PrefLaunch get() = neoPrefs.encryption
val pref_encryption_mode: PrefEnum get() = neoPrefs.encryptionMode
val pref_password: PrefString get() = neoPrefs.password
val pref_pgpKey: PrefString get() = neoPrefs.pgpKey
val pref_pgpPasscode: PrefString get() = neoPrefs.pgpPasscode
val pref_restorePermissions: PrefBoolean get() = neoPrefs.restorePermissions
val pref_numBackupRevisions: PrefInt get() = neoPrefs.numBackupRevisions
val pref_enableStorageCheck: PrefBoolean get() = neoPrefs.enableStorageCheck
val pref_compressionType: PrefList get() = neoPrefs.compressionType
val pref_compressionLevel: PrefInt get() = neoPrefs.compressionLevel
val pref_enableSessionInstaller: PrefBoolean get() = neoPrefs.enableSessionInstaller
val pref_installationPackage: PrefString get() = neoPrefs.installationPackage
val pref_backupDeviceProtectedData: PrefBoolean get() = neoPrefs.backupDeviceProtectedData
val pref_backupExternalData: PrefBoolean get() = neoPrefs.backupExternalData
val pref_backupObbData: PrefBoolean get() = neoPrefs.backupObbData
val pref_backupMediaData: PrefBoolean get() = neoPrefs.backupMediaData
val pref_backupNoBackupData: PrefBoolean get() = neoPrefs.backupNoBackupData
val pref_backupCache: PrefBoolean get() = neoPrefs.backupCache
val pref_restoreDeviceProtectedData: PrefBoolean get() = neoPrefs.restoreDeviceProtectedData
val pref_restoreExternalData: PrefBoolean get() = neoPrefs.restoreExternalData
val pref_restoreObbData: PrefBoolean get() = neoPrefs.restoreObbData
val pref_restoreMediaData: PrefBoolean get() = neoPrefs.restoreMediaData
val pref_restoreNoBackupData: PrefBoolean get() = neoPrefs.restoreNoBackupData
val pref_restoreCache: PrefBoolean get() = neoPrefs.restoreCache

val kill_password = PasswordPref(   // make sure password is never saved in non-encrypted prefs
    key = "kill.password",
    private = false,
    defaultValue = ""
)
val kill_password_set = run { kill_password.value = "" }
