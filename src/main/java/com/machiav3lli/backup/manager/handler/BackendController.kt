/*
 * OAndBackupX: open-source apps backup and restore app.
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
package com.machiav3lli.backup.manager.handler

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.machiav3lli.backup.BACKUP_INSTANCE_PROPERTIES_INDIR
import com.machiav3lli.backup.BACKUP_INSTANCE_REGEX_PATTERN
import com.machiav3lli.backup.BACKUP_PACKAGE_FOLDER_REGEX_PATTERN
import com.machiav3lli.backup.BACKUP_SPECIAL_FILE_REGEX_PATTERN
import com.machiav3lli.backup.BACKUP_SPECIAL_FOLDER_REGEX_PATTERN
import com.machiav3lli.backup.DamagedOp
import com.machiav3lli.backup.ERROR_PREFIX
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.PROP_NAME
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.AppInfo
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.dbs.entity.SpecialInfo
import com.machiav3lli.backup.data.entity.Package.Companion.invalidateCacheForPackage
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.preferences.traceBackupsScan
import com.machiav3lli.backup.data.preferences.traceBackupsScanAll
import com.machiav3lli.backup.data.preferences.traceTiming
import com.machiav3lli.backup.data.repository.PackageRepository
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import com.machiav3lli.backup.manager.handler.ShellCommands.Companion.currentProfile
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.ui.pages.pref_backupSuspendApps
import com.machiav3lli.backup.ui.pages.pref_createInvalidBackups
import com.machiav3lli.backup.ui.pages.pref_lookForEmptyBackups
import com.machiav3lli.backup.utils.SystemUtils.numCores
import com.machiav3lli.backup.utils.TraceUtils
import com.machiav3lli.backup.utils.TraceUtils.beginNanoTimer
import com.machiav3lli.backup.utils.TraceUtils.endNanoTimer
import com.machiav3lli.backup.utils.TraceUtils.formatBackups
import com.machiav3lli.backup.utils.TraceUtils.logNanoTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.get
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

val regexBackupInstance = Regex(BACKUP_INSTANCE_REGEX_PATTERN)
val regexPackageFolder = Regex(BACKUP_PACKAGE_FOLDER_REGEX_PATTERN)
val regexSpecialFolder = Regex(BACKUP_SPECIAL_FOLDER_REGEX_PATTERN)
val regexSpecialFile = Regex(BACKUP_SPECIAL_FILE_REGEX_PATTERN)
private const val SCANS_BATCH_SIZE = 50

val maxThreads = AtomicInteger(0)
val usedThreadsByName = mutableMapOf<String, AtomicInteger>()
fun clearThreadStats() {
    synchronized(usedThreadsByName) {
        usedThreadsByName.clear()
    }
}

fun checkThreadStats() {
    val nThreads = Thread.activeCount()
    maxThreads.getAndUpdate {
        if (it < nThreads)
            nThreads
        else
            it
    }
    synchronized(usedThreadsByName) {
        usedThreadsByName.getOrPut(Thread.currentThread().name) { AtomicInteger(0) }
            .getAndIncrement()
    }
}

val scanPool = when (1) {

    // force hang for recursive scanning
    0    -> Executors.newFixedThreadPool(1).asCoroutineDispatcher()

    // may hang for recursive scanning because threads are limited
    0    -> Executors.newFixedThreadPool(numCores).asCoroutineDispatcher()

    // unlimited threads!
    0    -> Executors.newCachedThreadPool().asCoroutineDispatcher()

    // creates many threads (~65)
    else -> Dispatchers.IO

    // TODO hg42 it's still not 100% clear, if queue based scanning prevents hanging
}

suspend fun scanBackups(
    directory: StorageFile,
    packageName: String = "",
    backupRoot: StorageFile,
    level: Int = 0,
    forceTrace: Boolean = false,
    damagedOp: DamagedOp? = null,
    onValidBackup: suspend (StorageFile) -> Unit,
    onInvalidBackup: suspend (StorageFile, StorageFile?, String?, String?) -> Unit,
) {
    val files = ConcurrentLinkedDeque<StorageFile>()
    val suspicious = AtomicInteger(0)

    // cache for directory listings
    val directoryCache = ConcurrentHashMap<String, List<StorageFile>>()

    if (level == 0 && packageName.isEmpty() && traceTiming.pref.value) {
        checkThreadStats()
        traceTiming { "threads max: ${maxThreads.get()} (before)" }
    }

    fun formatBackupFile(file: StorageFile) =
        file.path?.removePrefix(backupRoot.path ?: "")?.removePrefix("/") ?: ""

    fun traceBackupsScanPackage(lazyText: () -> String) {
        if (forceTrace) {
            if (packageName.isEmpty())
                TraceUtils.traceImpl("[BackupsScanAll] ${lazyText()}")
            else
                TraceUtils.traceImpl("[BackupsScan] ${lazyText()}")
        } else {
            if (packageName.isEmpty())
                traceBackupsScanAll(lazyText)
            else
                traceBackupsScan(lazyText)
        }
    }

    fun logSuspicious(file: StorageFile, reason: String) {
        formatBackupFile(file)
            .replace(ERROR_PREFIX, "")
            .let { relPath ->
                val message = "? $relPath ($reason)"
                NeoApp.addInfoLogText(message)
                traceBackupsScanAll { message }
            }
    }

    fun renameDamagedToERROR(file: StorageFile, reason: String) {
        when (damagedOp) {
            DamagedOp.RENAME,
            DamagedOp.CLEANUP
                 ->
                runCatching {
                    NeoApp.hitBusy()
                    val newName = "$ERROR_PREFIX${file.name}"
                    file.renameTo(newName)
                    suspicious.getAndIncrement()
                    logSuspicious(file, reason)
                }

            null -> {
                suspicious.getAndIncrement()
                logSuspicious(file, reason)
            }

            else -> {}
        }
    }

    fun onErrorPrefix(file: StorageFile) {
        runCatching {
            file.name?.let { name ->
                if (name.startsWith(ERROR_PREFIX)) {
                    NeoApp.hitBusy()
                    when (damagedOp) {
                        DamagedOp.UNDO -> {
                            val newName = name.removePrefix(ERROR_PREFIX)
                            if (file.renameTo(newName)) {
                                suspicious.getAndIncrement()
                                logSuspicious(file, "undo")
                            }
                        }

                        DamagedOp.DELETE,
                        DamagedOp.CLEANUP,
                                       -> {
                            if (file.deleteRecursive()) {
                                suspicious.getAndIncrement()
                                logSuspicious(file, "delete")
                            }
                        }

                        else           -> {}
                    }
                }
            }
        }
    }

    suspend fun handleInvalidProps(
        dir: StorageFile,
        file: StorageFile? = null,
        packageName: String? = null,
    ) {
        if (damagedOp != null)
            renameDamagedToERROR(dir, "no-props")
        else
            onInvalidBackup(dir, file, null, "no-props")
    }

    suspend fun handleEmptyBackup(
        dir: StorageFile,
        file: StorageFile? = null,
        packageName: String? = null,
    ) {
        if (damagedOp != null)
            renameDamagedToERROR(dir, "empty-backup")
        else
            onInvalidBackup(dir, file, null, "empty-backup")
    }

    suspend fun handleProps(
        file: StorageFile,
        path: String?,
        name: String?,
        onPropsFile: suspend (StorageFile) -> Unit,
        renamer: (suspend () -> Unit)? = null,
    ) {
        NeoApp.hitBusy()

        try {
            if (file.name == BACKUP_INSTANCE_PROPERTIES_INDIR
                ||
                (file.name?.removeSuffix(
                    ".$PROP_NAME"
                )?.let {
                    file.parent?.findFile(
                        it
                    )
                } != null)
            ) {
                beginNanoTimer("scanBackups.${if (packageName.isEmpty()) "" else "package."}onPropsFile")
                onPropsFile(file)
                endNanoTimer("scanBackups.${if (packageName.isEmpty()) "" else "package."}onPropsFile")
            } else
                renameDamagedToERROR(file, "no-dir")
        } catch (_: Throwable) {
            if (renamer != null)
                renamer()
            else {
                name?.let {
                    if (!it.contains(regexSpecialFile))
                        renameDamagedToERROR(file, "damaged")
                }
            }
        }
    }

    fun getDirectoryListing(file: StorageFile): List<StorageFile> {
        val path = file.path ?: return emptyList()

        return directoryCache.getOrPut(path) {
            beginNanoTimer("scanBackups.${if (packageName.isEmpty()) "" else "package."}listFiles")
            val result = file.listFiles()
            endNanoTimer("scanBackups.${if (packageName.isEmpty()) "" else "package."}listFiles")
            result
        }
    }

    suspend fun handleDirectory(
        file: StorageFile,
        collector: FlowCollector<StorageFile>? = null,
    ): Boolean {
        NeoApp.hitBusy()

        var list = getDirectoryListing(file)

        if (list.isEmpty())
            return false

        // all files for undo or del, otherwise filter
        if (damagedOp in listOf(DamagedOp.RENAME, DamagedOp.CLEANUP, null)) {
            // queue at front of queue (depth first)
            // filter out dir matching dir.properties
            val props = list.mapNotNull { it.name }.filter { it.endsWith(".$PROP_NAME") }
            val propDirs = props.map { it.removeSuffix(".$PROP_NAME") }

            if (damagedOp == DamagedOp.RENAME || damagedOp == DamagedOp.CLEANUP || pref_lookForEmptyBackups.value) {
                list.filter { it.name in propDirs }.forEach { dir ->
                    runCatching {   // in case it's not a directory etc.
                        if (getDirectoryListing(dir).isEmpty())
                            handleEmptyBackup(dir = dir)
                    }
                }
            }

            list = list.filterNot { it.name in propDirs }
        }

        // if matching directories are filtered out, we can sort normally,
        // so we sort reverted and push to front one by one
        list.sortedByDescending { it.name }.forEach {
            files.offerFirst(it)
        }

        return true
    }

    fun indent(level: Int, mark: String) = ":::${"|:::".repeat(level)}i"
    fun traceLine(mark: String, level: Int, file: StorageFile, text: String) =
        "${indent(level, mark)}${formatBackupFile(file)} ${text}"

    suspend fun scanBackupInstance(
        file: StorageFile,
        path: String,
        name: String,
        level: Int,
        onValidBackup: suspend (StorageFile) -> Unit,
    ) {
        traceBackupsScanPackage { traceLine("i", level, file, "instance") }

        if (file.isPropertyFile &&                                  // instance props
            !name.contains(regexSpecialFile)
        ) {
            val props = file
            traceBackupsScanPackage {
                traceLine(
                    ">",
                    level,
                    props,
                    "++++++++++++++++++++ props ok"
                )
            }

            handleProps(props, path, name, onValidBackup)

        } else {

            if (!name.contains(regexSpecialFolder) &&
                file.isDirectory                                    // instance dir
            ) {

                val dir = file
                // directories are filtered out for existing properties,
                // so if it's an instance directory it's solo -> error
                if (name.contains(regexPackageFolder)) {
                    // in case of flatStructure there could be a backup.properties inside
                    // TODO hg42 change so that this can be seen by the directory name
                    try {
                        dir.findFile(BACKUP_INSTANCE_PROPERTIES_INDIR)  // indir props
                            ?.let { props ->

                                traceBackupsScanPackage {
                                    traceLine(
                                        ">",
                                        level,
                                        props,
                                        "++++++++++++++++++++ props indir ok"
                                    )
                                }

                                handleProps(props, props.path, props.name, onValidBackup) {
                                    runCatching {
                                        dir.name?.let { name ->
                                            if (!name.contains(
                                                    regexSpecialFolder
                                                )
                                            ) {
                                                handleInvalidProps(
                                                    dir = dir, file = props,
                                                    packageName = if (packageName.isEmpty()) null else packageName
                                                )
                                            }
                                        }
                                    }
                                }
                            } ?: run {

                            handleInvalidProps(dir = dir)
                        }
                    } catch (_: Throwable) {
                        handleInvalidProps(dir = dir)
                    }

                } else {

                    handleInvalidProps(dir = dir)

                }
            }
        }
    }

    suspend fun processFile(
        file: StorageFile,
    ) {
        checkThreadStats()

        val name = file.name ?: ""
        val path = file.path ?: ""
        if (forceTrace)
            traceBackupsScanPackage { traceLine(">", level, file, "++++++++++++++++++++ file") }

        if (damagedOp in listOf(DamagedOp.UNDO, DamagedOp.DELETE)) {
            // undo for each file
            onErrorPrefix(file)
            // scan all files
            if (file.isDirectory)
                handleDirectory(file)
            // do nothing else
            return
        }

        if (name.contains(regexPackageFolder) ||                    // package folder
            name.contains(regexBackupInstance)                      // or backup instance
        ) {
            if (forceTrace)
                traceBackupsScanPackage {
                    traceLine(
                        "B",
                        level,
                        file,
                        "++++++++++++++++++++ backup"
                    )
                }

            if (path.contains(packageName)) {                           // package matches, empty matches all

                if (name.contains(regexBackupInstance)) {                   // instance ...

                    scanBackupInstance(file, path, name, level, onValidBackup)

                } else {                                                    // no instance

                    if (file.isPropertyFile &&
                        !name.contains(regexSpecialFile)                        // non-instance props (wtf is that? probably a saved file)
                    ) {
                        traceBackupsScanPackage {
                            traceLine(
                                ">",
                                level,
                                file,
                                "++++++++++++++++++++ non-instance props ok (a renamed backup?)"
                            )
                        }

                        handleProps(file, path, name, onValidBackup)

                    } else {
                        if (file.isDirectory) {                                 // non-instance-directory
                            val dir = file
                            traceBackupsScanPackage {
                                traceLine(
                                    "/",
                                    level,
                                    file,
                                    "++++++++++++++++++++ //////////////////// dir ok"
                                )
                            }

                            if (handleDirectory(dir).not()) {
                                // renameDamagedToERROR(dir, "empty-folder")
                            }
                        }
                    }
                }
            }

            // else
            //                                                          // single scan and non matching package -> ignored

        } else {                                                    // no package and no instance

            if (!name.contains(regexSpecialFolder) &&
                file.isDirectory                                        // no package or instance folder
            ) {
                val dir = file
                if (forceTrace)
                    traceBackupsScanPackage {
                        traceLine(
                            "F",
                            level,
                            file,
                            "/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\ folder ok"
                        )
                    }

                if (handleDirectory(dir).not()) {
                    // renameDamagedToERROR(dir, "empty-folder")
                }

            }
        }

        if (damagedOp == DamagedOp.CLEANUP) {
            // undo for each file
            onErrorPrefix(file)
            // scan all files
            if (file.isDirectory)
                handleDirectory(file)
            // do nothing else
            return
        }
    }

    handleDirectory(directory)    // top level directory

    var total = 0
    while (files.isNotEmpty()) {
        if (packageName.isEmpty())
            traceBackupsScanPackage { "queue filled with ${files.size}" }

        // reduce context switching by processing in batches
        val batch = mutableListOf<StorageFile>()
        repeat(minOf(SCANS_BATCH_SIZE, files.size)) {
            files.poll()?.let { batch.add(it) }
        }

        coroutineScope {
            batch.forEach { file ->
                launch(scanPool) {
                    processFile(file)
                }
            }
        }

        total += batch.size

        if (packageName.isEmpty())
            traceBackupsScanPackage { "queue processed batch of ${batch.size}" }
    }

    if (packageName.isEmpty()) {
        traceBackupsScanPackage { "queue total ----> $total" }
        if (suspicious.get() > 0)
            NeoApp.addInfoLogText(
                "${
                    when (damagedOp) {
                        DamagedOp.RENAME  -> "renamed"
                        DamagedOp.DELETE  -> "deleted"
                        DamagedOp.CLEANUP -> "cleaned-up"
                        DamagedOp.UNDO    -> "undo"
                        else              -> "suspicious"
                    }
                }: ${suspicious.get()}"
            )
    }
    directoryCache.clear()
}

fun findBackupsAll(forceTrace: Boolean = false): Map<String, Set<Backup>> {
    NeoApp.beginBusy("findBackups")
    try {
        clearThreadStats()

        val backupsMap = scanBackupsInternal(
            packageName = "",
            damagedOp = null,
            forceTrace = forceTrace,
        )

        traceBackupsScan { "*** --------------------> findBackups: packages: ${backupsMap.keys.size} backups: ${backupsMap.values.flatten().size}" }

        return backupsMap
    } finally {
        val time = NeoApp.endBusy("findBackups")
        NeoApp.addInfoLogText("findBackups: ${"%.3f".format(time / 1E9)} sec")

        if (traceTiming.pref.value) {
            logNanoTiming("scanBackups.", "scanBackups")
            traceTiming { "threads max: ${maxThreads.get()}" }
            val threads =
                synchronized(usedThreadsByName) { usedThreadsByName }.toMap()
            traceTiming { "threads used: (${threads.size})${threads.values}" }
        }
    }
}

fun Context.findBackupsPackage(
    packageName: String,
    forceTrace: Boolean = false,
): Map<String, Set<Backup>> {
    val backupsMap = scanBackupsInternal(
        packageName = packageName,
        damagedOp = null,
        forceTrace = forceTrace,
    )

    if (NeoApp.startup)
        traceBackupsScan {
            "<$packageName> single scan (DURING STARTUP!!!) ${
                formatBackups(
                    backupsMap[packageName] ?: listOf()
                )
            }"
        }
    else
        traceBackupsScan { "<$packageName> single scan ${formatBackups(backupsMap[packageName] ?: listOf())}" }

    return backupsMap
}

fun Context.findBackupsAndClean(
    damagedOp: DamagedOp,
    forceTrace: Boolean = false,
): Map<String, Set<Backup>> =
    runCatching {
        invalidateCacheForPackage()
        scanBackupsInternal(
            packageName = "",
            damagedOp = damagedOp,
            forceTrace = forceTrace,
        )
    }.onFailure {
        logException(it, backTrace = true)
    }.getOrDefault(emptyMap())

private fun scanBackupsInternal(
    packageName: String,
    damagedOp: DamagedOp?,
    forceTrace: Boolean,
): Map<String, Set<Backup>> {
    val backupsMap = ConcurrentHashMap<String, MutableSet<Backup>>()
    val backupRoot = NeoApp.backupRoot ?: return emptyMap()

    runBlocking {
        scanBackups(
            directory = backupRoot,
            packageName = packageName,
            backupRoot = backupRoot,
            damagedOp = damagedOp,
            forceTrace = forceTrace,
            onValidBackup = { props ->
                Backup.createFrom(props)
                    ?.let { backup ->
                        backupsMap.getOrPut(backup.packageName) { mutableSetOf() }
                            .add(backup)
                    }
            },
            onInvalidBackup = { dir: StorageFile, props: StorageFile?, pkg: String?, why: String? ->
                if (pref_createInvalidBackups.value) {
                    Backup.createInvalidFrom(dir, props, pkg, why)
                        ?.let { backup ->
                            backupsMap.getOrPut(backup.packageName) { mutableSetOf() }
                                .add(backup)
                        }
                }
            }
        )
    }

    return backupsMap
}

suspend fun Context.updateAppTables(
    backupsMap: Map<String, Set<Backup>>,
    installedPackageInfos: List<PackageInfo>,
    specialInfos: List<SpecialInfo>,
) {
    val packagesRepo = get<PackageRepository>(PackageRepository::class.java)

    try {
        NeoApp.beginBusy("updateAppTables")

        val installedNames = installedPackageInfos.map { it.packageName }.toSet()

        try {
            beginNanoTimer("unsuspend")

            if (!NeoApp.appsSuspendedChecked && pref_backupSuspendApps.value) {
                installedNames.filter { packageName ->
                    0 != (NeoApp.activity?.packageManager
                        ?.getPackageInfo(packageName, 0)
                        ?.applicationInfo
                        ?.flags
                        ?: 0) and ApplicationInfo.FLAG_SUSPENDED
                }.apply {
                    val profileId = currentProfile
                    NeoApp.main?.whileShowingSnackBar(getString(R.string.supended_apps_cleanup)) {
                        // cleanup suspended package if lock file found
                        this.forEach { packageName ->
                            runAsRoot("pm unsuspend --user $profileId $packageName")
                        }
                        NeoApp.appsSuspendedChecked = true
                    }
                }
            }
        } catch (e: Throwable) {
            logException(e, backTrace = true)
        } finally {
            endNanoTimer("unsuspend")
        }

        val specialNames = specialInfos.map { it.packageName }.toSet()

        val uninstalledPackagesWithBackup =
            try {
                beginNanoTimer("uninstalledPackagesWithBackup")

                (backupsMap.keys - installedNames - specialNames)
                    .mapNotNull {
                        backupsMap[it]?.maxByOrNull { it.backupDate }?.toAppInfo()
                    }
            } catch (e: Throwable) {
                logException(e, backTrace = true)
                emptyList()
            } finally {
                endNanoTimer("uninstalledPackagesWithBackup")
            }

        val appInfoList =
            try {
                beginNanoTimer("appInfoList")

                installedPackageInfos
                    .map { AppInfo(this, it) }
                    .union(uninstalledPackagesWithBackup)
            } catch (e: Throwable) {
                logException(e, backTrace = true)
                emptyList()
            } finally {
                endNanoTimer("appInfoList")
            }

        try {
            beginNanoTimer("dbUpdate")

            packagesRepo.replaceAppInfos(*appInfoList.toTypedArray())
        } catch (e: Throwable) {
            logException(e, backTrace = true)
        } finally {
            endNanoTimer("dbUpdate")
        }

    } catch (e: Throwable) {
        logException(e, backTrace = true)
    } finally {
        val time = NeoApp.endBusy("updateAppTables")
        NeoApp.addInfoLogText("updateAppTables: ${"%.3f".format(time / 1E9)} sec")
    }
}
