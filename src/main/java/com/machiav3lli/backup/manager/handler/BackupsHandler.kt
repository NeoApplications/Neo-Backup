/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2026  Antonios Hazim
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
package com.machiav3lli.backup.manager.handler

import android.app.Application
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.dbs.entity.SpecialInfo
import com.machiav3lli.backup.data.entity.Package.Companion.invalidateCacheForPackage
import com.machiav3lli.backup.data.repository.PackageRepository
import com.machiav3lli.backup.ui.pages.pref_earlyEmptyBackups
import com.machiav3lli.backup.utils.FileUtils.BackupLocationInAccessibleException
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.getInstalledPackageInfosWithPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BackupsHandler(
    private val appContext: Application,
    private val packagesRepo: PackageRepository,
) {
    private val mutex = Mutex()

    suspend fun refreshAll(): BackupIndexResult = withContext(Dispatchers.IO) {
        mutex.withLock { refreshAllLocked() }
    }

    suspend fun refreshPackage(packageName: String): BackupIndexResult {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!packagesRepo.isInitialized) refreshAllLocked()
                else refreshPackageLocked(packageName)
            }
        }
    }

    suspend fun ensureLoaded(): BackupIndexResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (packagesRepo.isInitialized) BackupIndexResult.Success(packagesRepo.getBackupsList().size)
            else refreshAllLocked()
        }
    }

    private suspend fun refreshAllLocked(): BackupIndexResult {
        return try {
            invalidate()
            val installedPackageInfos =
                appContext.packageManager.getInstalledPackageInfosWithPermissions()
            val specialInfos = SpecialInfo.getSpecialInfos(appContext)

            // preset installed packages with empty backups lists
            // this prevents scanning them again when a package needs it's backups later
            // doing it here also avoids setting all packages to empty lists when findbackups fails
            // so there is a chance that scanning for backups of a single package will work later
            if (pref_earlyEmptyBackups.value) {
                val installedNames =
                    installedPackageInfos.map { it.packageName } + specialInfos.map { it.packageName }
                packagesRepo.deleteBackupsOf(installedNames)
            }

            val backupsMap = findBackupsAll()
            packagesRepo.replaceAllBackups(backupsMap)
            appContext.updateAppTables(backupsMap, installedPackageInfos, specialInfos)
            BackupIndexResult.Success(backupsMap.values.sumOf { it.size })
        } catch (e: BackupLocationInAccessibleException) {
            LogsHandler.logException(e, backTrace = true)
            BackupIndexResult.LocationInaccessible(e.message)
        } catch (e: StorageLocationNotConfiguredException) {
            LogsHandler.logException(e, backTrace = true)
            BackupIndexResult.LocationInaccessible(e.message)
        } catch (e: Throwable) {
            LogsHandler.logException(e, backTrace = true)
            BackupIndexResult.Failure(e)
        }
    }

    private fun refreshPackageLocked(packageName: String): BackupIndexResult {
        return try {
            invalidate(packageName)
            val backupsMap = appContext.findBackupsPackage(packageName)
            packagesRepo.updatePackageBackups(packageName, backupsMap[packageName] ?: emptySet())
            BackupIndexResult.Success(backupsMap.values.sumOf { it.size })
        } catch (e: BackupLocationInAccessibleException) {
            LogsHandler.logException(e, backTrace = true)
            BackupIndexResult.LocationInaccessible(e.message)
        } catch (e: StorageLocationNotConfiguredException) {
            LogsHandler.logException(e, backTrace = true)
            BackupIndexResult.LocationInaccessible(e.message)
        } catch (e: Throwable) {
            LogsHandler.logException(e, backTrace = true)
            BackupIndexResult.Failure(e)
        }
    }

    private fun invalidate(packageName: String = "") {
        invalidateCacheForPackage(packageName)
        if (packageName.isEmpty())
            NeoApp.backupRoot = null
    }
}

sealed interface BackupIndexResult {
    data class Success(val backupCount: Int) : BackupIndexResult
    data class LocationInaccessible(val message: String?) : BackupIndexResult
    data class Failure(val throwable: Throwable) : BackupIndexResult
}
