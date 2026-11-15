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
package com.machiav3lli.backup.viewmodels

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.machiav3lli.backup.data.repository.AppExtrasRepository
import com.machiav3lli.backup.data.repository.BlocklistRepository

abstract class BatchVM(
    blocklistRepository: BlocklistRepository,
    appExtrasRepository: AppExtrasRepository
) : MainVM(blocklistRepository, appExtrasRepository) {
    val apkBackupCheckedList = SnapshotStateMap<String, Int>()
    val dataBackupCheckedList = SnapshotStateMap<String, Int>()
}
