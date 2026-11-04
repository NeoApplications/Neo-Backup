/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.MODE_DATA
import com.machiav3lli.backup.MODE_DATA_DE
import com.machiav3lli.backup.MODE_DATA_EXT
import com.machiav3lli.backup.MODE_DATA_MEDIA
import com.machiav3lli.backup.MODE_DATA_OBB
import com.machiav3lli.backup.MODE_UNSET
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.Package
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

@Composable
fun RestoreDialogUI(
    appPackage: Package,
    backup: Backup,
    openDialogCustom: MutableState<Boolean>,
    onAction: (mode: Int) -> Unit,
) {
    val modePairs = buildMap {
        if (backup.hasApk)
            set(MODE_APK, stringResource(id = R.string.radio_apk))
        if (backup.hasAppData)
            set(MODE_DATA, stringResource(id = R.string.radio_data))
        if (backup.hasDevicesProtectedData)
            set(MODE_DATA_DE, stringResource(id = R.string.radio_deviceprotecteddata))
        if (backup.hasExternalData)
            set(MODE_DATA_EXT, stringResource(id = R.string.radio_externaldata))
        if (backup.hasObbData)
            set(MODE_DATA_OBB, stringResource(id = R.string.radio_obbdata))
        if (backup.hasMediaData)
            set(MODE_DATA_MEDIA, stringResource(id = R.string.radio_mediadata))
    }

    MultiSelectionDialogUI(
        titleText = appPackage.packageLabel,
        entryMap = modePairs.toPersistentMap(),
        selectedItems = modePairs.keys.toPersistentList(),
        openDialogCustom = openDialogCustom,
    ) {
        onAction(it.fold(MODE_UNSET) { acc, s -> acc xor s }) // TODO Add backup & action type?
    }
}