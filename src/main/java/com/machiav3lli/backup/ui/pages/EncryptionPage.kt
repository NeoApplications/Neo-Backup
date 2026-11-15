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
import com.machiav3lli.backup.data.entity.EnumPref
import com.machiav3lli.backup.data.entity.PasswordPref
import com.machiav3lli.backup.data.entity.Pref
import com.machiav3lli.backup.manager.handler.PGPHandler
import com.machiav3lli.backup.ui.compose.blockBorder
import com.machiav3lli.backup.ui.compose.component.ActionButton
import com.machiav3lli.backup.ui.compose.component.ExpandableBlock
import com.machiav3lli.backup.ui.compose.component.PrefsGroup
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.TopBar
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.GearSix
import com.machiav3lli.backup.ui.compose.icons.phosphor.Key
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.EnumPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.KeyDialogUI
import com.machiav3lli.backup.ui.dialogs.StringPrefDialogUI
import com.machiav3lli.backup.ui.navigation.NavItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun EncryptionPage(pgpManager: PGPHandler = koinInject(), navigateUp: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = Pref.prefGroups["encryption"]?.toPersistentList() ?: persistentListOf()

    val openDialog = remember { mutableStateOf(false) }
    val openCreateDialog = remember { mutableStateOf(false) }
    var dialogsPref by remember { mutableStateOf<Pref?>(null) }
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
                PrefsGroup(prefs = prefs) { pref ->
                    dialogsPref = pref
                    openDialog.value = true
                }
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
                openCreateDialog.value -> KeyDialogUI(
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

                //pref_password
                dialogsPref is PasswordPref -> StringPrefDialogUI(
                    pref = dialogsPref as PasswordPref,
                    isPrivate = true,
                    confirm = true,
                    openDialogCustom = openDialog
                )

                //pref_encryption_mode,
                dialogsPref is EnumPref -> EnumPrefDialogUI(
                    pref = dialogsPref as EnumPref,
                    openDialogCustom = openDialog,
                )
            }
        }
    }
}