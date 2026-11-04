package com.machiav3lli.backup.ui.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.COMPRESSION_TYPES
import com.machiav3lli.backup.ENCRYPTION
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.BooleanPref
import com.machiav3lli.backup.data.entity.EnumPref
import com.machiav3lli.backup.data.entity.IntPref
import com.machiav3lli.backup.data.entity.KeyPref
import com.machiav3lli.backup.data.entity.LaunchPref
import com.machiav3lli.backup.data.entity.ListPref
import com.machiav3lli.backup.data.entity.PasswordPref
import com.machiav3lli.backup.data.entity.Pref
import com.machiav3lli.backup.data.entity.StringPref
import com.machiav3lli.backup.encryptionModes
import com.machiav3lli.backup.manager.handler.PGPHandler
import com.machiav3lli.backup.ui.compose.component.PrefsGroup
import com.machiav3lli.backup.ui.compose.component.StringPreference
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.FileZip
import com.machiav3lli.backup.ui.compose.icons.phosphor.FloppyDisk
import com.machiav3lli.backup.ui.compose.icons.phosphor.Hash
import com.machiav3lli.backup.ui.compose.icons.phosphor.Key
import com.machiav3lli.backup.ui.compose.icons.phosphor.Password
import com.machiav3lli.backup.ui.compose.icons.phosphor.ShieldStar
import com.machiav3lli.backup.ui.compose.icons.phosphor.TagSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.Textbox
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.EnumPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.ListPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.StringPrefDialogUI
import com.machiav3lli.backup.ui.navigation.NavRoute
import com.machiav3lli.backup.utils.SystemUtils
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

@Composable
fun ServicePrefsPage() {
    val openDialog = remember { mutableStateOf(false) }
    var dialogsPref by remember { mutableStateOf<Pref?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ServicePrefGroups { pref ->
            dialogsPref = pref
            openDialog.value = true
        }
    }

    if (openDialog.value) {
        BaseDialog(onDismiss = { openDialog.value = false }) {
            when (dialogsPref) {
                is ListPref -> ListPrefDialogUI(
                    pref = dialogsPref as ListPref,
                    openDialogCustom = openDialog,
                )

                is EnumPref -> EnumPrefDialogUI(
                    pref = dialogsPref as EnumPref,
                    openDialogCustom = openDialog
                )

                is PasswordPref -> StringPrefDialogUI(
                    pref = dialogsPref as PasswordPref,
                    isPrivate = true,
                    confirm = true,
                    openDialogCustom = openDialog
                )

                is StringPref -> StringPrefDialogUI(
                    pref = dialogsPref as StringPref,
                    openDialogCustom = openDialog
                )
            }
        }
    }
}

fun LazyListScope.ServicePrefGroups(onPrefDialog: (Pref) -> Unit) {
    val generalServicePrefs = Pref.prefGroups["srv"]?.toPersistentList() ?: persistentListOf()
    val backupServicePrefs = Pref.prefGroups["srv-bkp"]?.toPersistentList() ?: persistentListOf()
    val restoreServicePrefs = Pref.prefGroups["srv-rst"]?.toPersistentList() ?: persistentListOf()

    item {
        PrefsGroup(
            prefs = generalServicePrefs,
            onPrefDialog = onPrefDialog
        )
    }
    item {
        PrefsGroup(
            prefs = backupServicePrefs,
            heading = stringResource(id = R.string.backup),
            onPrefDialog = onPrefDialog
        )
    }
    item {
        PrefsGroup(
            prefs = restoreServicePrefs,
            heading = stringResource(id = R.string.restore),
            onPrefDialog = onPrefDialog
        )
    }
}

val pref_encryption = LaunchPref(
    key = "srv.enc",
    titleId = R.string.prefs_encryption,
    summaryId = R.string.prefs_encryption_summary,
    icon = Phosphor.Key,
    onClick = { NeoApp.main?.moveTo(NavRoute.Encryption) }
)

val pref_encryption_mode = EnumPref(
    key = "encryption.mode",
    titleId = R.string.prefs_encryption,
    icon = Phosphor.Key,
    entries = encryptionModes,
    defaultValue = ENCRYPTION.NONE.ordinal,
)

val pref_password = PasswordPref(
    key = "encryption.password",
    titleId = R.string.prefs_password,
    summaryId = R.string.prefs_password_summary,
    icon = Phosphor.Password,
    iconTint = {
        val pref = it as PasswordPref
        if (pref.value.isNotEmpty()) Color.Green else Color.Gray
    },
    enableIf = { pref_encryption_mode.value == ENCRYPTION.PASSWORD.ordinal },
    defaultValue = "",
)

val kill_password = PasswordPref(   // make sure password is never saved in non-encrypted prefs
    key = "kill.password",
    private = false,
    defaultValue = ""
)
val kill_password_set = run { kill_password.value = "" }

val pref_pgpKey = KeyPref(
    key = "encryption.pgpKey",
    titleId = R.string.prefs_pgp_key,
    summaryId = R.string.prefs_pgp_key_summary,
    icon = Phosphor.Key,
    enableIf = { pref_encryption_mode.value == ENCRYPTION.PGP.ordinal },
    UI = { it, _, index, groupSize ->
        val scope = rememberCoroutineScope()
        val pgpManager: PGPHandler = get(PGPHandler::class.java)

        val pref = it as KeyPref
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    scope.launch {
                        pgpManager.loadKeyFromUri(it).fold(
                            onSuccess = {
                                pgpManager.isKeyLoaded()
                            },
                            onFailure = { _ -> }
                        )
                    }
                }
            }

        StringPreference(
            pref = pref,
            index = index,
            groupSize = groupSize,
            onClick = {
                launcher.launch(arrayOf("*/*"))
            },
        )
    },
    defaultValue = "",
)

val pref_pgpPasscode = PasswordPref(
    key = "encryption.pgpPasscode",
    titleId = R.string.prefs_pgp_passcode,
    summaryId = R.string.prefs_pgp_passcode_summary,
    icon = Phosphor.Password,
    enableIf = { pref_encryption_mode.value == ENCRYPTION.PGP.ordinal },
    defaultValue = "",
)

val pref_restorePermissions = BooleanPref(
    key = "srv.restorePermissions",
    titleId = R.string.prefs_restorepermissions,
    summaryId = R.string.prefs_restorepermissions_summary,
    icon = Phosphor.ShieldStar,
    defaultValue = true
)

val pref_numBackupRevisions = IntPref(
    key = "srv.numBackupRevisions",
    titleId = R.string.prefs_numBackupRevisions,
    summaryId = R.string.prefs_numBackupRevisions_summary,
    icon = Phosphor.Hash,
    entries = ((0..9) + (10..20 step 2) + (50..200 step 50)).toList(),
    defaultValue = 2
)

val pref_enableStorageCheck = BooleanPref(
    key = "srv.enableStorageCheck",
    titleId = R.string.prefs_enablestoragecheck,
    summaryId = R.string.prefs_enablestoragecheck_summary,
    icon = Phosphor.FloppyDisk,
    defaultValue = true
)

val pref_compressionType = ListPref(
    key = "srv.compressionType",
    titleId = R.string.prefs_compression_type,
    summaryId = R.string.prefs_compression_type_summary,
    icon = Phosphor.FileZip,
    entries = COMPRESSION_TYPES,
    defaultValue = "zst"
)

val pref_compressionLevel = IntPref(
    key = "srv.compressionLevel",
    titleId = R.string.prefs_compression_level,
    summaryId = R.string.prefs_compression_level_summary,
    icon = Phosphor.FileZip,
    entries = (0..9).toList(),
    defaultValue = 2
)

val pref_enableSessionInstaller = BooleanPref(
    key = "srv.enableSessionInstaller",
    titleId = R.string.prefs_sessionIinstaller,
    summaryId = R.string.prefs_sessionIinstaller_summary,
    icon = Phosphor.TagSimple,
    defaultValue = true
)

val pref_installationPackage = StringPref(
    key = "srv.installationPackage",
    titleId = R.string.prefs_installerpackagename,
    icon = Phosphor.Textbox,
    defaultValue = SystemUtils.packageName
)
