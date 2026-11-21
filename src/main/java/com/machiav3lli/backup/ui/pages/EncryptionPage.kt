package com.machiav3lli.backup.ui.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
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
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.ColoringState
import com.machiav3lli.backup.data.preferences.PrefDelegate
import com.machiav3lli.backup.data.preferences.PrefEnum
import com.machiav3lli.backup.data.preferences.PrefString
import com.machiav3lli.backup.data.preferences.ServicePrefKey
import com.machiav3lli.backup.manager.handler.PGPHandler
import com.machiav3lli.backup.ui.compose.blockBorder
import com.machiav3lli.backup.ui.compose.component.ActionButton
import com.machiav3lli.backup.ui.compose.component.ExpandableBlock
import com.machiav3lli.backup.ui.compose.component.NeoPrefsGroup
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.TopBar
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.GearSix
import com.machiav3lli.backup.ui.compose.icons.phosphor.Key
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.DSEnumPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.DSStringPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.KeyDialogUI
import com.machiav3lli.backup.ui.navigation.NavItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun EncryptionPage(pgpManager: PGPHandler = koinInject(), navigateUp: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = persistentListOf(
        pref_encryption_mode,
        pref_password,
        pref_pgpKey,
        pref_pgpPasscode,
    )

    val openDialog = remember { mutableStateOf(false) }
    val openCreateDialog = remember { mutableStateOf(false) }
    var dialogsPref by remember { mutableStateOf<PrefDelegate<out Any>?>(null) }
    var keyProtector by remember { mutableStateOf(Pair("", "")) }

    val createKeyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pgp-keys")
    ) { outputUri ->
        outputUri?.let { destination ->
            scope.launch {
                pgpManager.generateNewKey(keyProtector.first, keyProtector.second, destination)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                title = stringResource(id = NavItem.Encryption.title),
                navigationAction = {
                    RoundButton(
                        icon = Phosphor.GearSix,
                        description = stringResource(id = android.R.string.cancel),
                        onClick = navigateUp,
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .blockBorder()
                .fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                NeoPrefsGroup(
                    prefs = prefs,
                    onPrefDialog = { pref ->
                        dialogsPref = pref
                        openDialog.value = true
                    },
                )
            }
            item {
                ExpandableBlock(
                    heading = stringResource(R.string.prefs_pgp_key_management),
                ) {
                    ActionButton(
                        text = stringResource(R.string.create_pgp_key),
                        icon = Phosphor.Key,
                        modifier = Modifier.weight(1f),
                        coloring = ColoringState.Positive,
                    ) {
                        openCreateDialog.value = true
                        openDialog.value = true
                    }
                }
            }
        }

        if (openDialog.value) BaseDialog(onDismiss = { openDialog.value = false }) {
            when {
                openCreateDialog.value    -> KeyDialogUI(
                    titleId = R.string.create_pgp_key,
                    onDismiss = {
                        openDialog.value = false
                        openCreateDialog.value = false
                    },
                    onAction = { userId, passcode ->
                        keyProtector = Pair(userId, passcode)
                        createKeyLauncher.launch("neo_key.pgp")
                    }
                )

                dialogsPref is PrefEnum   -> DSEnumPrefDialogUI(
                    pref = dialogsPref as PrefEnum,
                    openDialogCustom = openDialog,
                )

                dialogsPref is PrefString -> {
                    val pref = dialogsPref as PrefString
                    val isPrivate = pref.key == ServicePrefKey.ENCRYPTION_PASSWORD.name ||
                            pref.key == ServicePrefKey.PGP_PASSCODE.name
                    DSStringPrefDialogUI(
                        pref = pref,
                        isPrivate = isPrivate,
                        confirm = isPrivate,
                        openDialogCustom = openDialog
                    )
                }

                else                      -> {}
            }
        }
    }
}