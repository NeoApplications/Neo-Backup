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
import com.machiav3lli.backup.data.dbs.entity.ExtrasTags
import com.machiav3lli.backup.data.entity.MainState
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.data.repository.AppExtrasRepository
import com.machiav3lli.backup.data.repository.BlocklistRepository
import com.machiav3lli.backup.utils.TraceUtils.trace
import com.machiav3lli.backup.utils.applyFilter
import com.machiav3lli.backup.utils.extensions.NeoViewModel
import com.machiav3lli.backup.utils.getStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
abstract class MainVM(
    private val blocklistRepository: BlocklistRepository,
    appExtrasRepository: AppExtrasRepository,
) : NeoViewModel() {
    abstract val state: StateFlow<MainState>
    abstract val sortFilterModel: Flow<SortFilterModel>

    protected val extras = appExtrasRepository.getAllFlow()
        .mapLatest { it.associateBy { extra -> extra.packageName } }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
            emptyMap()
        )

    val tagsMap = appExtrasRepository.getTagsMapFlow()
        .mapLatest { it.associate { tag -> Pair(tag.packageName, tag.customTags) } }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
            emptyMap()
        )

    val allTags = appExtrasRepository.getTagsMapFlow()
        .mapLatest { it.flatMap(ExtrasTags::customTags).toSet() }
        .trace { "*** allTags <<- ${it.size}" }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
            emptySet()
        )

    val stats = flow {
        emitAll(
            combine(state, sortFilterModel, tagsMap) { state, sortFilterModel, tags ->
                state.packages
                    .filterNot { it.packageName in state.blocklist }
                    .applyFilter(sortFilterModel, tags)
                    .let { getStats(it) }
            }
        )
    }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
            getStats(emptyList())
        )

    fun updateBlocklist(packages: Set<String>) {
        viewModelScope.launch {
            blocklistRepository.updateGlobalBlocklist(packages)
        }
    }

    /**
     * to-be-implemented in respective VM
     */
    open fun setSortFilter(value: SortFilterModel) {}
}
