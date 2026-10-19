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

import android.content.Context
import android.content.Intent
import com.machiav3lli.backup.EnabledFilter
import com.machiav3lli.backup.InstalledFilter
import com.machiav3lli.backup.LatestFilter
import com.machiav3lli.backup.LaunchableFilter
import com.machiav3lli.backup.MAIN_FILTER_SPECIAL
import com.machiav3lli.backup.MAIN_FILTER_SYSTEM
import com.machiav3lli.backup.MAIN_FILTER_USER
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.MODE_DATA
import com.machiav3lli.backup.MODE_DATA_DE
import com.machiav3lli.backup.MODE_DATA_EXT
import com.machiav3lli.backup.MODE_DATA_MEDIA
import com.machiav3lli.backup.MODE_DATA_OBB
import com.machiav3lli.backup.MODE_NONE
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.Sort
import com.machiav3lli.backup.UpdatedFilter
import com.machiav3lli.backup.data.dbs.entity.AppExtras
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.data.entity.SpecialFilter
import com.machiav3lli.backup.possibleMainFilters
import com.machiav3lli.backup.ui.pages.pref_oldBackups
import java.text.Collator
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

//TODO hg42 filters for activity and schedule should be as equal as possible
// on first glance, some things are done differently

//---------------------------------------- filters for schedule

fun filterPackages(
    packages: List<Package>,
    tagsMap: Map<String, Set<String>>,
    filter: Int,
    specialFilter: SpecialFilter,
    customList: Set<String> = emptySet(),
    blockList: Set<String>,
    tagsList: Set<String>,
): List<Package> {

    val startPackages = (
            if (tagsList.isNotEmpty()) packages.filter {
                tagsMap[it.packageName]?.any { tag -> tagsList.contains(tag) }
                    ?: false
            } else packages
            ).let {
            if (customList.isNotEmpty()) it.filter { pkg ->
                customList.contains(pkg.packageName)
            } else it
        }

    val predicate: (Package) -> Boolean = {
        (if (filter and MAIN_FILTER_SYSTEM == MAIN_FILTER_SYSTEM) it.isSystem and !it.isSpecial else false)
                || (if (filter and MAIN_FILTER_USER == MAIN_FILTER_USER) !it.isSystem else false)
                || (if (filter and MAIN_FILTER_SPECIAL == MAIN_FILTER_SPECIAL) it.isSpecial else false)
    }

    return startPackages
        .filterNot {
            blockList.contains(it.packageName)
        }
        .filter(predicate)
        .applySpecialFilter(specialFilter) // filter last, with fewer packages, e.g. old backups is expensive
        .sortedWith { m1: Package, m2: Package ->
            m1.packageLabel.compareTo(m2.packageLabel, ignoreCase = true)
        }
}


//---------------------------------------- filters for activity

fun Collection<Package>.applySearch(
    query: String,
    extras: Map<String, AppExtras>,
): List<Package> = filter { item ->
    query.isEmpty() || (
            (extras[item.packageName]?.customTags ?: emptySet()).plus(
                listOfNotNull(
                    item.packageName,
                    item.packageLabel,
                    extras[item.packageName]?.note
                )
            )
                .any { it.contains(query, ignoreCase = true) }
            ) || item.backupList.any { it.note.contains(query, ignoreCase = true) }
}

/**
 * Filters a list of backups by matching their note against the search query.
 * Falls back to the full list when no backup's note matches
 * (e.g. when the app was found by its name instead of a note).
 */
fun List<Backup>.applyNoteSearch(query: String): List<Backup> =
    if (query.isEmpty()) this
    else filter { it.note.contains(query, ignoreCase = true) }.ifEmpty { this }

fun Collection<Package>.applyFilter(
    filter: SortFilterModel,
    tagsMap: Map<String, Set<String>>,
): List<Package> {
    val predicate: (Package) -> Boolean = {
        (if (filter.mainFilter and MAIN_FILTER_SYSTEM == MAIN_FILTER_SYSTEM) it.isSystem && !it.isSpecial else false)
                || (if (filter.mainFilter and MAIN_FILTER_USER == MAIN_FILTER_USER) !it.isSystem else false)
                || (if (filter.mainFilter and MAIN_FILTER_SPECIAL == MAIN_FILTER_SPECIAL) it.isSpecial else false)
    }
    return filter(predicate)
        .applyBackupFilter(filter.backupFilter)
        .applySpecialFilter(filter.specialFilter)
        .applySort(filter.sort, filter.sortAsc)
        .applyTagsFilter(filter.tags, tagsMap)
}

private fun Collection<Package>.applyBackupFilter(backupFilter: Int): List<Package> {
    val predicate: (Package) -> Boolean = {
        (if (backupFilter and MODE_NONE == MODE_NONE) !it.hasBackups or !(it.hasApk or it.hasData)
        else false)
                || (if (backupFilter and MODE_APK == MODE_APK) it.hasApk else false)
                || (if (backupFilter and MODE_DATA == MODE_DATA) it.hasAppData else false)
                || (if (backupFilter and MODE_DATA_DE == MODE_DATA_DE) it.hasDevicesProtectedData else false)
                || (if (backupFilter and MODE_DATA_EXT == MODE_DATA_EXT) it.hasExternalData else false)
                || (if (backupFilter and MODE_DATA_OBB == MODE_DATA_OBB) it.hasObbData else false)
                || (if (backupFilter and MODE_DATA_MEDIA == MODE_DATA_MEDIA) it.hasMediaData else false)
    }
    return filter(predicate)
}

private fun List<Package>.applySpecialFilter(
    specialFilter: SpecialFilter,
): List<Package> {
    val context = NeoApp.context
    val predicate: (Package) -> Boolean
    var launchableAppsList = listOf<String>()
    if (specialFilter.launchableFilter != LaunchableFilter.ALL.ordinal) {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        launchableAppsList = context.packageManager.queryIntentActivities(mainIntent, 0)
            .map { it.activityInfo.packageName }
    }
    val days = pref_oldBackups.value
    val installedPredicate = when (specialFilter.installedFilter) {
        InstalledFilter.INSTALLED.ordinal -> Package::isInstalled
        InstalledFilter.NOT.ordinal       -> { appInfo: Package -> !appInfo.isInstalled }
        else                              -> { _: Package -> true }
    }
    val launchablePredicate = when (specialFilter.launchableFilter) {
        LaunchableFilter.LAUNCHABLE.ordinal -> { appInfo: Package ->
            launchableAppsList.contains(appInfo.packageName)
        }

        LaunchableFilter.NOT.ordinal        -> { appInfo: Package ->
            !launchableAppsList.contains(appInfo.packageName)
        }

        else                                -> { _: Package -> true }
    }
    val updatedPredicate = when (specialFilter.updatedFilter) {
        UpdatedFilter.UPDATED.ordinal -> Package::isUpdated
        UpdatedFilter.NEW.ordinal     -> Package::isNew
        UpdatedFilter.NOT.ordinal     -> { appInfo: Package -> !appInfo.isUpdated }
        else                          -> { _: Package -> true }
    }
    val enabledPredicate = when (specialFilter.enabledFilter) {
        EnabledFilter.ENABLED.ordinal  -> { appInfo: Package -> !appInfo.isDisabled }
        EnabledFilter.DISABLED.ordinal -> Package::isDisabled
        else                           -> { _: Package -> true }
    }
    val latestPredicate = when (specialFilter.latestFilter) {
        LatestFilter.OLD.ordinal -> { appInfo: Package ->
            when {
                appInfo.hasBackups -> {
                    val lastBackup = appInfo.latestBackup?.backupDate
                    val diff = ChronoUnit.DAYS.between(lastBackup, LocalDateTime.now())
                    diff >= days
                }

                else               -> false
            }
        }

        LatestFilter.NEW.ordinal -> { appInfo: Package ->
            when {
                appInfo.hasBackups -> {
                    val lastBackup = appInfo.latestBackup?.backupDate
                    val diff = ChronoUnit.DAYS.between(lastBackup, LocalDateTime.now())
                    diff < days
                }

                else               -> true
            }
        }

        else                     -> { _: Package -> true }
    }
    predicate = { pkg: Package ->
        installedPredicate(pkg) and
                launchablePredicate(pkg) and
                updatedPredicate(pkg) and
                enabledPredicate(pkg) and
                latestPredicate(pkg)
    }
    return filter(predicate)
}

private fun List<Package>.applySort(sort: Int, sortAsc: Boolean): List<Package> =
    if (!sortAsc) {
        when (sort) {
            Sort.PACKAGENAME.ordinal
                 -> sortedWith(
                compareBy(Collator.getInstance().reversed()) { it.packageName.lowercase() }
            )

            Sort.APP_SIZE.ordinal
                 -> sortedByDescending { it.appBytes }

            Sort.DATA_SIZE.ordinal
                 -> sortedByDescending { it.dataBytes }

            Sort.APPDATA_SIZE.ordinal
                 -> sortedByDescending { it.appBytes + it.dataBytes }

            Sort.BACKUP_SIZE.ordinal
                 -> sortedByDescending { it.backupBytes }

            Sort.BACKUP_DATE.ordinal
                 -> sortedWith(compareBy<Package> { it.latestBackup?.backupDate }.thenBy { it.packageLabel })

            else -> sortedWith(
                compareBy(Collator.getInstance().reversed()) { it.packageLabel.lowercase() }
            )
        }
    } else {
        when (sort) {
            Sort.PACKAGENAME.ordinal
                 -> sortedWith(compareBy(Collator.getInstance()) { it.packageName.lowercase() })

            Sort.APP_SIZE.ordinal
                 -> sortedBy { it.appBytes }

            Sort.DATA_SIZE.ordinal
                 -> sortedBy { it.dataBytes }

            Sort.APPDATA_SIZE.ordinal
                 -> sortedBy { it.appBytes + it.dataBytes }

            Sort.BACKUP_SIZE.ordinal
                 -> sortedBy { it.backupBytes }

            Sort.BACKUP_DATE.ordinal
                 -> sortedWith(compareByDescending<Package> { it.latestBackup?.backupDate }.thenBy { it.packageLabel })

            else -> sortedWith(
                compareBy(Collator.getInstance()) { it.packageLabel.lowercase() }
            )
        }
    }

fun List<Package>.applyTagsFilter(
    tags: Set<String>,
    tagsMap: Map<String, Set<String>>
): List<Package> = filter {
    tags.isEmpty() || (tagsMap[it.packageName] ?: emptySet()).any { tag -> tag in tags }
}

fun filterToString(context: Context, filter: Int): String = possibleMainFilters
    .filter { it and filter == it }
    .let { activeFilters ->
        if (activeFilters.containsAll(possibleMainFilters)) context.getString(R.string.radio_all)
        else activeFilters.joinToString(", ") {
            context.getString(
                when (it) {
                    MAIN_FILTER_USER    -> R.string.radio_user
                    MAIN_FILTER_SPECIAL -> R.string.radio_special
                    else                -> R.string.radio_system // MAIN_FILTER_SYSTEM
                }
            )
        }
    }

fun specialFilterToString(context: Context, specialFilter: SpecialFilter) = listOfNotNull(
    when (specialFilter.launchableFilter) {
        LaunchableFilter.LAUNCHABLE.ordinal -> context.getString(R.string.radio_launchable)
        LaunchableFilter.NOT.ordinal        -> context.getString(R.string.radio_notlaunchable)
        else                                -> null
    },
    when (specialFilter.enabledFilter) {
        EnabledFilter.ENABLED.ordinal  -> context.getString(R.string.show_enabled_apps)
        EnabledFilter.DISABLED.ordinal -> context.getString(R.string.showDisabled)
        else                           -> null
    },
    when (specialFilter.latestFilter) {
        LatestFilter.NEW.ordinal -> context.getString(R.string.show_new_backups)
        LatestFilter.OLD.ordinal -> context.getString(R.string.showOldBackups)
        else                     -> null
    },
    when (specialFilter.updatedFilter) {
        UpdatedFilter.UPDATED.ordinal -> context.getString(R.string.show_updated_apps)
        UpdatedFilter.NEW.ordinal     -> context.getString(R.string.show_new_apps)
        UpdatedFilter.NOT.ordinal     -> context.getString(R.string.show_old_apps)
        else                          -> null
    },
).joinToString(", ")

