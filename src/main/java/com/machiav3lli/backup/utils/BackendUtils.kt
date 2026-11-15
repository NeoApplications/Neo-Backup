package com.machiav3lli.backup.utils

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process
import com.machiav3lli.backup.MAIN_FILTER_SYSTEM
import com.machiav3lli.backup.MAIN_FILTER_USER
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.dbs.entity.AppInfo
import com.machiav3lli.backup.data.dbs.entity.SpecialInfo
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.manager.actions.BaseAppAction.Companion.ignoredPackages
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import kotlin.system.measureTimeMillis

// TODO respect special filter
fun Context.getPackageInfoList(filter: Int): List<PackageInfo> =
    packageManager.getInstalledPackageInfosWithPermissions()
        .filter { packageInfo: PackageInfo ->
            val isSystem =
                (packageInfo.applicationInfo?.flags ?: 0) and
                        ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
            val isIgnored = packageInfo.packageName.matches(ignoredPackages)
            if (isIgnored)
                Timber.i("ignored package: ${packageInfo.packageName}")
            (filter and MAIN_FILTER_SYSTEM == MAIN_FILTER_SYSTEM && isSystem && !isIgnored)
                    || (filter and MAIN_FILTER_USER == MAIN_FILTER_USER && !isSystem && !isIgnored)
        }
        .toList()

fun Context.getInstalledPackageList(): MutableList<Package> { // only used in ScheduledActionTask
    var packageList: MutableList<Package> = mutableListOf()

    try {
        NeoApp.beginBusy("getInstalledPackageList")

        val time = measureTimeMillis {

            val pm = packageManager
            val includeSpecial = specialBackupsEnabled
            val packageInfoList = pm.getInstalledPackageInfosWithPermissions()
            packageList = packageInfoList
                .filterNot { it.packageName.matches(ignoredPackages) }
                .mapNotNull {
                    try {
                        Package(this, it)
                    } catch (e: AssertionError) {
                        Timber.e("Could not create Package for ${it}: $e")
                        null
                    }
                }
                .toMutableList()

            // Special Backups must added before the uninstalled packages, because otherwise it would
            // discover the backup directory and run in a special case where the directory is empty.
            // This would mean, that no package info is available – neither from backup.properties
            // nor from PackageManager.
            if (includeSpecial) {
                SpecialInfo.getSpecialInfos(this).forEach {
                    packageList.add(Package(it))
                }
            }
        }

        NeoApp.addInfoLogText(
            "getPackageList: ${(time / 1000 + 0.5).toInt()} sec"
        )
    } catch (e: Throwable) {
        logException(e, backTrace = true)
    } finally {
        NeoApp.endBusy("getInstalledPackageList")
    }

    return packageList
}

fun List<Package>.toAppInfoList(): List<AppInfo> =
    filterNot { it.isSpecial }.map { it.packageInfo as AppInfo }

fun List<AppInfo>.toPackageList(
    context: Context,
    blockList: List<String> = listOf(),
): MutableList<Package> {
    var packageList: MutableList<Package> = mutableListOf()

    try {
        NeoApp.beginBusy("toPackageList")

        val includeSpecial = specialBackupsEnabled

        packageList =
            this.filterNot {
                it.packageName.matches(ignoredPackages)
            }
                .mapNotNull {
                    val pkg = try {
                        Package(context, it)
                    } catch (e: AssertionError) {
                        Timber.e("Could not create Package for ${it}: $e")
                        null
                    }
                    //pkg?.updateBackupList(backupsMap[pkg.packageName].orEmpty())
                    pkg
                }
                .toMutableList()

        // Special Backups must added before the uninstalled packages, because otherwise it would
        // discover the backup directory and run in a special case where no the directory is empty.
        // This would mean, that no package info is available – neither from backup.properties
        // nor from PackageManager.
        // TODO show special packages directly without restarting NB
        //val specialList = mutableListOf<String>()
        if (includeSpecial) {
            SpecialInfo.getSpecialInfos(context).forEach {
                if (!blockList.contains(it.packageName)) {
                    packageList.add(Package(it))
                }
            }
        }

    } catch (e: Throwable) {
        LogsHandler.unexpectedException(e)
    } finally {
        NeoApp.endBusy("toPackageList")
    }

    return packageList
}

@Throws(PackageManager.NameNotFoundException::class)
fun Context.getPackageStorageStats(
    packageName: String,
    storageUuid: UUID = packageManager.getApplicationInfo(packageName, 0).storageUuid,
): StorageStats? {
    val storageStatsManager =
        getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    return try {
        storageStatsManager.queryStatsForPackage(
            storageUuid,
            packageName,
            Process.myUserHandle()
        )
    } catch (e: IOException) {
        Timber.e("Could not retrieve storage stats of $packageName: $e")
        null
    } catch (e: Throwable) {
        LogsHandler.unexpectedException(e, packageName)
        null
    }
}

fun Context.getSpecial(packageName: String) =
    SpecialInfo.getSpecialInfos(this)
        .find { it.packageName == packageName }
        ?.let { Package(it) }