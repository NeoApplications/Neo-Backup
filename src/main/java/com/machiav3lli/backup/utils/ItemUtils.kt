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
package com.machiav3lli.backup.utils

import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEach
import com.machiav3lli.backup.CHIP_SIZE_APP
import com.machiav3lli.backup.CHIP_SIZE_CACHE
import com.machiav3lli.backup.CHIP_SIZE_DATA
import com.machiav3lli.backup.CHIP_SPLIT
import com.machiav3lli.backup.CHIP_TYPE
import com.machiav3lli.backup.CHIP_VERSION
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.AppExtras
import com.machiav3lli.backup.data.entity.InfoChipItem
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.AsteriskSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.CircleWavyWarning
import com.machiav3lli.backup.ui.compose.icons.phosphor.Spinner
import com.machiav3lli.backup.ui.compose.icons.phosphor.User
import com.machiav3lli.backup.ui.compose.theme.ColorDisabled
import com.machiav3lli.backup.ui.compose.theme.ColorNotInstalled
import com.machiav3lli.backup.ui.compose.theme.ColorSpecial
import com.machiav3lli.backup.ui.compose.theme.ColorSystem
import com.machiav3lli.backup.ui.compose.theme.ColorUpdated
import com.machiav3lli.backup.ui.compose.theme.ColorUser
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

data class AppStats(
    val nApps: Int,
    val nBackups: Int,
    val nUpdated: Int,
    val szApps: Long,
    val szData: Long,
)

fun getStats(appsList: List<Package>): AppStats {   //TODO hg42 we actually want the separate parts, need to find an api
    var nBackups = 0
    var nUpdated = 0
    var szApps = 0L
    var szData = 0L
    appsList.fastForEach {
        if (it.hasBackups) {
            nBackups += it.numberOfBackups
            if (it.isUpdated) nUpdated += 1
        }
        szApps += it.appBytes
        szData += it.dataBytes
    }
    return AppStats(appsList.size, nBackups, nUpdated, szApps, szData)
}

fun PackageManager.getInstalledPackageInfosWithPermissions() =
    getInstalledPackages(0).mapNotNull {
        try {
            getPackageInfo(it.packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: Throwable) {
            LogsHandler.unexpectedException(e)
            null
        }
    }

fun List<AppExtras>.get(packageName: String) =
    find { it.packageName == packageName } ?: AppExtras(packageName)

@Composable
fun Package.infoChips(): PersistentList<InfoChipItem> = listOfNotNull(
    InfoChipItem(
        flag = CHIP_TYPE,
        text = stringResource(
            when {
                isSpecial -> R.string.apptype_special
                isSystem  -> R.string.apptype_system
                else      -> R.string.apptype_user
            }
        ),
        icon = when {
            isSpecial -> Phosphor.AsteriskSimple
            isSystem  -> Phosphor.Spinner
            else      -> Phosphor.User
        },
        color = when {
            !isInstalled -> ColorNotInstalled
            isDisabled   -> ColorDisabled
            isSpecial    -> ColorSpecial
            isSystem     -> ColorSystem
            else         -> ColorUser
        }
    ),
    InfoChipItem(
        flag = CHIP_VERSION,
        text = versionName ?: versionCode.toString(),
        icon = if (this.isUpdated) Phosphor.CircleWavyWarning else null,
        color = if (this.isUpdated) ColorUpdated else null,
    ),
    InfoChipItem(
        flag = CHIP_SIZE_APP,
        text = stringResource(id = R.string.app_size) + " " + Formatter.formatFileSize(
            LocalContext.current,
            storageStats?.appBytes ?: 0
        ),
    ),
    InfoChipItem(
        flag = CHIP_SIZE_DATA,
        text = stringResource(id = R.string.data_size) + " " + Formatter.formatFileSize(
            LocalContext.current,
            storageStats?.dataBytes ?: 0
        ),
    ),
    InfoChipItem(
        flag = CHIP_SIZE_CACHE,
        text = stringResource(id = R.string.cache_size) + " " + Formatter.formatFileSize(
            LocalContext.current,
            storageStats?.cacheBytes ?: 0
        ),
    ),
    if (this.apkSplits.isNotEmpty()) InfoChipItem(
        flag = CHIP_SPLIT,
        text = stringResource(id = R.string.split_apks),
    ) else null
).toPersistentList()
