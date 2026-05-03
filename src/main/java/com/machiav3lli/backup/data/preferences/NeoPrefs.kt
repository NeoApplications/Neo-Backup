package com.machiav3lli.backup.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.machiav3lli.backup.BACKUP_FILTER_DEFAULT
import com.machiav3lli.backup.EnabledFilter
import com.machiav3lli.backup.InstalledFilter
import com.machiav3lli.backup.LatestFilter
import com.machiav3lli.backup.LaunchableFilter
import com.machiav3lli.backup.MAIN_FILTER_USER
import com.machiav3lli.backup.Sort
import com.machiav3lli.backup.UpdatedFilter
import com.machiav3lli.backup.batchModesSequence
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.possibleMainFilters
import com.machiav3lli.backup.utils.extensions.combine
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

class NeoPrefs private constructor(val context: Context) : KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    val sortHome = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.SORT_HOME,
        defaultValue = Sort.LABEL.ordinal,
        entries = Sort.entries.map { it.ordinal },
    )

    val sortBackup = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.SORT_BACKUP,
        defaultValue = Sort.LABEL.ordinal,
        entries = Sort.entries.map { it.ordinal },
    )

    val sortRestore = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.SORT_RESTORE,
        defaultValue = Sort.LABEL.ordinal,
        entries = Sort.entries.map { it.ordinal },
    )

    val sortAscHome = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = PrefKey.SORT_ASC_HOME,
        defaultValue = true,
    )

    val sortAscBackup = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = PrefKey.SORT_ASC_BACKUP,
        defaultValue = true,
    )

    val sortAscRestore = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = PrefKey.SORT_ASC_RESTORE,
        defaultValue = true,
    )

    val mainFilterHome = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.MAIN_FILTER_HOME,
        defaultValue = MAIN_FILTER_USER,
        entries = possibleMainFilters, // not really, but shouldn't have an effect
    )

    val mainFilterBackup = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.MAIN_FILTER_BACKUP,
        defaultValue = MAIN_FILTER_USER,
        entries = possibleMainFilters, // not really, but shouldn't have an effect
    )

    val mainFilterRestore = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.MAIN_FILTER_RESTORE,
        defaultValue = MAIN_FILTER_USER,
        entries = possibleMainFilters, // not really, but shouldn't have an effect
    )

    val backupFilterHome = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.BACKUP_FILTER_HOME,
        defaultValue = BACKUP_FILTER_DEFAULT,
        entries = batchModesSequence, // not really, but shouldn't have an effect
    )

    val backupFilterBackup = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.BACKUP_FILTER_BACKUP,
        defaultValue = BACKUP_FILTER_DEFAULT,
        entries = batchModesSequence, // not really, but shouldn't have an effect
    )

    val backupFilterRestore = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.BACKUP_FILTER_RESTORE,
        defaultValue = BACKUP_FILTER_DEFAULT,
        entries = batchModesSequence, // not really, but shouldn't have an effect
    )

    val installedFilterHome = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.INSTALLED_FILTER_HOME,
        defaultValue = InstalledFilter.ALL.ordinal,
        entries = InstalledFilter.entries.map { it.ordinal },
    )

    val installedFilterBackup = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.INSTALLED_FILTER_BACKUP,
        defaultValue = InstalledFilter.ALL.ordinal,
        entries = InstalledFilter.entries.map { it.ordinal },
    )

    val installedFilterRestore = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.INSTALLED_FILTER_RESTORE,
        defaultValue = InstalledFilter.ALL.ordinal,
        entries = InstalledFilter.entries.map { it.ordinal },
    )

    val launchableFilterHome = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.LAUNCHABLE_FILTER_HOME,
        defaultValue = LaunchableFilter.ALL.ordinal,
        entries = LaunchableFilter.entries.map { it.ordinal },
    )

    val launchableFilterBackup = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.LAUNCHABLE_FILTER_BACKUP,
        defaultValue = LaunchableFilter.ALL.ordinal,
        entries = LaunchableFilter.entries.map { it.ordinal },
    )

    val launchableFilterRestore = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.LAUNCHABLE_FILTER_RESTORE,
        defaultValue = LaunchableFilter.ALL.ordinal,
        entries = LaunchableFilter.entries.map { it.ordinal },
    )

    val updatedFilterHome = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.UPDATED_FILTER_HOME,
        defaultValue = UpdatedFilter.ALL.ordinal,
        entries = UpdatedFilter.entries.map { it.ordinal },
    )

    val updatedFilterBackup = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.UPDATED_FILTER_BACKUP,
        defaultValue = UpdatedFilter.ALL.ordinal,
        entries = UpdatedFilter.entries.map { it.ordinal },
    )

    val updatedFilterRestore = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.UPDATED_FILTER_RESTORE,
        defaultValue = UpdatedFilter.ALL.ordinal,
        entries = UpdatedFilter.entries.map { it.ordinal },
    )

    val latestFilterHome = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.LATEST_FILTER_HOME,
        defaultValue = LatestFilter.ALL.ordinal,
        entries = LatestFilter.entries.map { it.ordinal },
    )

    val latestFilterBackup = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.LATEST_FILTER_BACKUP,
        defaultValue = LatestFilter.ALL.ordinal,
        entries = LatestFilter.entries.map { it.ordinal },
    )

    val latestFilterRestore = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.LATEST_FILTER_RESTORE,
        defaultValue = LatestFilter.ALL.ordinal,
        entries = LatestFilter.entries.map { it.ordinal },
    )

    val enabledFilterHome = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.ENABLED_FILTER_HOME,
        defaultValue = EnabledFilter.ALL.ordinal,
        entries = EnabledFilter.entries.map { it.ordinal },
    )

    val enabledFilterBackup = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.ENABLED_FILTER_BACKUP,
        defaultValue = EnabledFilter.ALL.ordinal,
        entries = EnabledFilter.entries.map { it.ordinal },
    )

    val enabledFilterRestore = PrefInt(
        dataStore = dataStore,
        dataStoreKey = PrefKey.ENABLED_FILTER_RESTORE,
        defaultValue = EnabledFilter.ALL.ordinal,
        entries = EnabledFilter.entries.map { it.ordinal },
    )

    val tagsFilterHome = PrefStringSet(
        dataStore = dataStore,
        dataStoreKey = PrefKey.TAGS_FILTER_HOME,
        defaultValue = emptySet(),
    )

    val tagsFilterBackup = PrefStringSet(
        dataStore = dataStore,
        dataStoreKey = PrefKey.TAGS_FILTER_BACKUP,
        defaultValue = emptySet(),
    )

    val tagsFilterRestore = PrefStringSet(
        dataStore = dataStore,
        dataStoreKey = PrefKey.TAGS_FILTER_RESTORE,
        defaultValue = emptySet(),
    )

    fun homeSortFilterFlow(): Flow<SortFilterModel> = combine(
        sortHome.flow(),
        sortAscHome.flow(),
        mainFilterHome.flow(),
        backupFilterHome.flow(),
        installedFilterHome.flow(),
        launchableFilterHome.flow(),
        updatedFilterHome.flow(),
        latestFilterHome.flow(),
        enabledFilterHome.flow(),
        tagsFilterHome.flow(),
    ) { sort, sortAsc, main, backup, installed, launchable, updated, latest, enabled, tags ->
        SortFilterModel(
            sort = sort,
            sortAsc = sortAsc,
            mainFilter = main,
            backupFilter = backup,
            installedFilter = installed,
            launchableFilter = launchable,
            updatedFilter = updated,
            latestFilter = latest,
            enabledFilter = enabled,
            tags = tags,
        )
    }

    fun backupSortFilterFlow(): Flow<SortFilterModel> = combine(
        sortBackup.flow(),
        sortAscBackup.flow(),
        mainFilterBackup.flow(),
        backupFilterBackup.flow(),
        installedFilterBackup.flow(),
        launchableFilterBackup.flow(),
        updatedFilterBackup.flow(),
        latestFilterBackup.flow(),
        enabledFilterBackup.flow(),
        tagsFilterBackup.flow(),
    ) { sort, sortAsc, main, backup, installed, launchable, updated, latest, enabled, tags ->
        SortFilterModel(
            sort = sort,
            sortAsc = sortAsc,
            mainFilter = main,
            backupFilter = backup,
            installedFilter = installed,
            launchableFilter = launchable,
            updatedFilter = updated,
            latestFilter = latest,
            enabledFilter = enabled,
            tags = tags,
        )
    }

    fun restoreSortFilterFlow(): Flow<SortFilterModel> = combine(
        sortRestore.flow(),
        sortAscRestore.flow(),
        mainFilterRestore.flow(),
        backupFilterRestore.flow(),
        installedFilterRestore.flow(),
        launchableFilterRestore.flow(),
        updatedFilterRestore.flow(),
        latestFilterRestore.flow(),
        enabledFilterRestore.flow(),
        tagsFilterRestore.flow(),
    ) { sort, sortAsc, main, backup, installed, launchable, updated, latest, enabled, tags ->
        SortFilterModel(
            sort = sort,
            sortAsc = sortAsc,
            mainFilter = main,
            backupFilter = backup,
            installedFilter = installed,
            launchableFilter = launchable,
            updatedFilter = updated,
            latestFilter = latest,
            enabledFilter = enabled,
            tags = tags,
        )
    }

    companion object {
        val prefsModule = module {
            singleOf(::NeoPrefs)
            singleOf(::provideDataStore)
        }

        private fun provideDataStore(context: Context): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = {
                    context.preferencesDataStoreFile("neo_backup")
                },
            )
        }
    }
}