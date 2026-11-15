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

import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.STATEFLOW_SUBSCRIBE_BUFFER
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.entity.MainState
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.data.preferences.NeoPrefs
import com.machiav3lli.backup.data.repository.AppExtrasRepository
import com.machiav3lli.backup.data.repository.BlocklistRepository
import com.machiav3lli.backup.data.repository.PackageRepository
import com.machiav3lli.backup.data.repository.ScheduleRepository
import com.machiav3lli.backup.ui.pages.pref_newAndUpdatedNotification
import com.machiav3lli.backup.utils.applyFilter
import com.machiav3lli.backup.utils.applySearch
import com.machiav3lli.backup.utils.extensions.combine
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeVM(
    packageRepository: PackageRepository,
    blocklistRepository: BlocklistRepository,
    appExtrasRepository: AppExtrasRepository,
    private val scheduleRepository: ScheduleRepository,
    private val prefs: NeoPrefs,
) : MainVM(blocklistRepository, appExtrasRepository) {
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - FLOWS
    private val searchQuery = MutableStateFlow("")
    private val selection = MutableStateFlow(emptySet<String>())

    val schedules = scheduleRepository.getAllFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
            emptyList()
        )

    override val sortFilterModel = prefs.homeSortFilterFlow()

    override val state: StateFlow<MainState> = combine(
        packageRepository.getPackagesFlow(),
        blocklistRepository.getBlocklist(),
        sortFilterModel,
        extras,
        searchQuery,
        selection,
    ) { packages, blocklist, sortFilter, extras, search, selection ->
        val (filteredPackages, updatedPackages) = packages
            .filterNot { it.packageName in blocklist }
            .let {
                Pair(
                    it.applySearch(search, extras)
                        .applyFilter(
                            sortFilter,
                            extras.mapValues { (_, value) -> value.customTags }
                        ),
                    it.filter { item ->
                        item.isUpdated || (pref_newAndUpdatedNotification.value && item.isNew)
                    }
                )
            }

        MainState(
            packages = packages.toPersistentList(),
            filteredPackages = filteredPackages.toPersistentList(),
            updatedPackages = updatedPackages.toPersistentList(),
            blocklist = blocklist.toPersistentSet(),
            searchQuery = search,
            selection = selection.toPersistentSet(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
        MainState()
    )
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - FLOWS end

    fun setSearchQuery(value: String) {
        viewModelScope.launch { searchQuery.update { value } }
    }

    override fun setSortFilter(value: SortFilterModel) {
        viewModelScope.launch {
            prefs.sortHome.set(value.sort)
            prefs.sortAscHome.set(value.sortAsc)
            prefs.mainFilterHome.set(value.mainFilter)
            prefs.backupFilterHome.set(value.backupFilter)
            prefs.installedFilterHome.set(value.installedFilter)
            prefs.launchableFilterHome.set(value.launchableFilter)
            prefs.updatedFilterHome.set(value.updatedFilter)
            prefs.latestFilterHome.set(value.latestFilter)
            prefs.enabledFilterHome.set(value.enabledFilter)
            prefs.tagsFilterHome.set(value.tags)
        }
    }

    fun toggleSelection(packageName: String) {
        viewModelScope.launch {
            selection.update { current ->
                if (current.contains(packageName)) {
                    current - packageName
                } else {
                    current + packageName
                }
            }
        }
    }

    fun updateSchedule(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.updateSchedule(schedule, false)
        }
    }
}