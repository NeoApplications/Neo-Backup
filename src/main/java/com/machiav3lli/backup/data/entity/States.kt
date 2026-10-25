package com.machiav3lli.backup.data.entity

import androidx.compose.runtime.Immutable
import com.machiav3lli.backup.data.dbs.entity.AppExtras
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.dbs.entity.Schedule
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class MainState(
    val packages: PersistentList<Package> = persistentListOf(),
    val filteredPackages: PersistentList<Package> = persistentListOf(),
    val updatedPackages: PersistentList<Package> = persistentListOf(),
    val blocklist: PersistentSet<String> = persistentSetOf(),
    val searchQuery: String = "",
    val sortFilter: SortFilterModel = SortFilterModel(),
    val selection: PersistentSet<String> = persistentSetOf(),
)

@Immutable
data class SchedulerState(
    val enabledSchedules: PersistentList<Schedule> = persistentListOf(),
    val disabledSchedules: PersistentList<Schedule> = persistentListOf(),
    val blocklist: PersistentSet<String> = persistentSetOf(),
    val tagsMap: PersistentMap<String, Set<String>> = persistentMapOf(),
    val tagsList: PersistentSet<String> = persistentSetOf(),
)

@Immutable
data class ScheduleState(
    val schedule: Schedule? = null,
    val blockList: PersistentSet<String> = persistentSetOf(),
    val customList: PersistentSet<String> = persistentSetOf(),
    val globalBlockList: PersistentSet<String> = persistentSetOf(),
    val tagsMap: PersistentMap<String, Set<String>> = persistentMapOf(),
    val tagsList: PersistentSet<String> = persistentSetOf(),
)

@Immutable
data class AppState(
    val pkg: Package? = null,
    val backups: List<Backup> = emptyList(),
    val appExtras: AppExtras = AppExtras(),
    val allTags: PersistentSet<String> = persistentSetOf(),
    val suggestionTags: PersistentSet<String> = persistentSetOf(),
)