package com.machiav3lli.backup.viewmodels

import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.STATEFLOW_SUBSCRIBE_BUFFER
import com.machiav3lli.backup.data.entity.MainState
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.data.preferences.NeoPrefs
import com.machiav3lli.backup.data.repository.AppExtrasRepository
import com.machiav3lli.backup.data.repository.BlocklistRepository
import com.machiav3lli.backup.data.repository.PackageRepository
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

class RestoreBatchVM(
    packageRepository: PackageRepository,
    blocklistRepository: BlocklistRepository,
    appExtrasRepository: AppExtrasRepository,
    private val prefs: NeoPrefs,
) : BatchVM(blocklistRepository, appExtrasRepository) {
    private val searchQuery = MutableStateFlow("")
    private val selection = MutableStateFlow(emptySet<String>())

    override val sortFilterModel = prefs.restoreSortFilterFlow()

    override val state: StateFlow<MainState> = combine(
        packageRepository.getPackagesFlow(),
        blocklistRepository.getBlocklist(),
        sortFilterModel,
        extras,
        searchQuery,
        selection,
    ) { packages, blocklist, sortFilter, extras, search, selection ->
        val filteredPackages = packages
            .filter { it.packageName !in blocklist && it.hasBackups }
            .applySearch(search, extras)
            .applyFilter(sortFilter, extras.mapValues { it.value.customTags })

        MainState(
            packages = packages.toPersistentList(),
            filteredPackages = filteredPackages.toPersistentList(),
            blocklist = blocklist.toPersistentSet(),
            searchQuery = search,
            selection = selection.toPersistentSet(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
        MainState()
    )

    fun setSearchQuery(value: String) {
        viewModelScope.launch { searchQuery.update { value } }
    }

    override fun setSortFilter(value: SortFilterModel) {
        viewModelScope.launch {
            prefs.sortRestore.set(value.sort)
            prefs.sortAscRestore.set(value.sortAsc)
            prefs.mainFilterRestore.set(value.mainFilter)
            prefs.backupFilterRestore.set(value.backupFilter)
            prefs.installedFilterRestore.set(value.installedFilter)
            prefs.launchableFilterRestore.set(value.launchableFilter)
            prefs.updatedFilterRestore.set(value.updatedFilter)
            prefs.latestFilterRestore.set(value.latestFilter)
            prefs.enabledFilterRestore.set(value.enabledFilter)
            prefs.tagsFilterRestore.set(value.tags)
        }
    }
}