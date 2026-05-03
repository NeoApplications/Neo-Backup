package com.machiav3lli.backup.data.preferences

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.machiav3lli.backup.BACKUP_DIRECTORY_INTENT
import com.machiav3lli.backup.BACKUP_FILTER_DEFAULT
import com.machiav3lli.backup.EnabledFilter
import com.machiav3lli.backup.InstalledFilter
import com.machiav3lli.backup.LatestFilter
import com.machiav3lli.backup.LaunchableFilter
import com.machiav3lli.backup.MAIN_FILTER_USER
import com.machiav3lli.backup.R
import com.machiav3lli.backup.Sort
import com.machiav3lli.backup.THEME
import com.machiav3lli.backup.UpdatedFilter
import com.machiav3lli.backup.accentColorItems
import com.machiav3lli.backup.batchModesSequence
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.possibleMainFilters
import com.machiav3lli.backup.secondaryColorItems
import com.machiav3lli.backup.themeItems
import com.machiav3lli.backup.ui.compose.component.StringEditPreference
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowsOutLineVertical
import com.machiav3lli.backup.ui.compose.icons.phosphor.CalendarX
import com.machiav3lli.backup.ui.compose.icons.phosphor.CircleWavyWarning
import com.machiav3lli.backup.ui.compose.icons.phosphor.Clock
import com.machiav3lli.backup.ui.compose.icons.phosphor.EyedropperSample
import com.machiav3lli.backup.ui.compose.icons.phosphor.FingerprintSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.FolderNotch
import com.machiav3lli.backup.ui.compose.icons.phosphor.List
import com.machiav3lli.backup.ui.compose.icons.phosphor.Lock
import com.machiav3lli.backup.ui.compose.icons.phosphor.Swatches
import com.machiav3lli.backup.ui.compose.icons.phosphor.TagSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.TextAa
import com.machiav3lli.backup.ui.compose.icons.phosphor.Translate
import com.machiav3lli.backup.ui.pages.pref_restartAppOnLanguageChange
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.backupDirConfigured
import com.machiav3lli.backup.utils.backupFolderExists
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.extensions.combine
import com.machiav3lli.backup.utils.getLanguageList
import com.machiav3lli.backup.utils.recreateActivities
import com.machiav3lli.backup.utils.restartApp
import com.machiav3lli.backup.utils.setBackupDir
import com.machiav3lli.backup.utils.setCustomTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import timber.log.Timber

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

    // ------------------- USER PREFS ------------------------------
    val languages = PrefList(
        titleId = R.string.prefs_languages,
        icon = Phosphor.Translate,
        entries = context.getLanguageList(),
        defaultValue = com.machiav3lli.backup.PREFS_LANGUAGES_SYSTEM,
        onChanged = { pref ->
            if (pref_restartAppOnLanguageChange.value)
                context.restartApp()
            else
                recreateActivities()
        },
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.LANGUAGES,
    )

    val appTheme = PrefEnum(
        titleId = R.string.prefs_theme,
        icon = Phosphor.Swatches,
        entries = themeItems,
        defaultValue = if (Android.minSDK(31)) THEME.DYNAMIC.ordinal else THEME.SYSTEM.ordinal,
        onChanged = { onThemeChanged() },
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.APP_THEME,
    )

    val appAccentColor = PrefEnum(
        titleId = R.string.prefs_accent_color,
        icon = Phosphor.EyedropperSample,
        entries = accentColorItems,
        defaultValue = with(SystemUtils.packageName) {
            when {
                contains("hg42")  -> 8
                contains("debug") -> 4
                else              -> 0
            }
        },
        onChanged = { onThemeChanged() },
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.APP_ACCENT_COLOR,
    )

    val appSecondaryColor = PrefEnum(
        titleId = R.string.prefs_secondary_color,
        icon = Phosphor.EyedropperSample,
        entries = secondaryColorItems,
        defaultValue = with(SystemUtils.packageName) {
            when {
                contains(".rel")  -> 0
                contains("debug") -> 4
                else              -> 3
            }
        },
        onChanged = { onThemeChanged() },
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.APP_SECONDARY_COLOR,
    )

    val pathBackupFolder = PrefEditString(
        titleId = R.string.prefs_pathbackupfolder,
        icon = Phosphor.FolderNotch,
        iconTint = { pref ->
            val prefValue = pref.value as? String ?: ""
            val alpha =
                if (prefValue == runCatching { backupDirConfigured }.getOrNull()) 1f else 0.3f
            if (prefValue.isEmpty()) Color.Gray
            else if (backupFolderExists(prefValue)) Color.Green.copy(alpha = alpha)
            else Color.Red.copy(alpha = alpha)
        },
        defaultValue = "",
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.PATH_BACKUP_FOLDER,
        onChanged = { pref ->
            val p = pref as PrefEditString
            if (p.value.isNotEmpty()) {
                setBackupDir(Uri.parse(p.value))
            }
        },
        UI = { pref, onDialogUI, index, groupSize ->
            val context = LocalContext.current
            val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                        result.data?.let {
                            val uri = it.data ?: return@let
                            val oldDir = try {
                                backupDirConfigured
                            } catch (e: StorageLocationNotConfiguredException) {
                                ""
                            }
                            if (oldDir != uri.toString()) {
                                val flags =
                                    it.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                context.contentResolver.takePersistableUriPermission(uri, flags)
                                Timber.i("setting uri $uri")
                                setBackupDir(uri)
                            }
                        }
                    }
                }
            StringEditPreference(
                pref = pref as PrefEditString,
                index = index,
                groupSize = groupSize,
                onClick = {
                    launcher.launch(BACKUP_DIRECTORY_INTENT)
                }
            )
        },
    )

    val deviceLock = PrefBoolean(
        titleId = R.string.prefs_devicelock,
        summaryId = R.string.prefs_devicelock_summary,
        icon = Phosphor.Lock,
        defaultValue = false,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.DEVICE_LOCK,
    )

    val biometricLock = PrefBoolean(
        titleId = R.string.prefs_biometriclock,
        summaryId = R.string.prefs_biometriclock_summary,
        icon = Phosphor.FingerprintSimple,
        defaultValue = false,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.BIOMETRIC_LOCK,
    )

    val multilineInfoChips = PrefBoolean(
        titleId = R.string.prefs_multilineinfochips,
        summaryId = R.string.prefs_multilineinfochips_summary,
        icon = Phosphor.ArrowsOutLineVertical,
        defaultValue = false,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.MULTILINE_INFO_CHIPS,
    )

    val singularBackupRestore = PrefBoolean(
        titleId = R.string.prefs_singularbackuprestore,
        summaryId = R.string.prefs_singularbackuprestore_summary,
        icon = Phosphor.List,
        defaultValue = true,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.SINGULAR_BACKUP_RESTORE,
    )

    val newAndUpdatedNotification = PrefBoolean(
        titleId = R.string.prefs_newandupdatednotification,
        summaryId = R.string.prefs_newandupdatednotification_summary,
        icon = Phosphor.CircleWavyWarning,
        defaultValue = false,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.NEW_AND_UPDATED_NOTIFICATION,
    )

    val squeezeNavText = PrefBoolean(
        titleId = R.string.prefs_squeezenavtext,
        summaryId = R.string.prefs_squeezenavtext_summary,
        icon = Phosphor.TextAa,
        defaultValue = false,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.SQUEEZE_NAV_TEXT,
    )

    val altNavBarItem = PrefBoolean(
        titleId = R.string.prefs_altnavbaritem,
        summaryId = R.string.prefs_altnavbaritem_summary,
        icon = Phosphor.TagSimple,
        defaultValue = false,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.ALT_NAV_BAR_ITEM,
    )

    val altBackupDate = PrefBoolean(
        titleId = R.string.prefs_altbackupdate,
        summaryId = R.string.prefs_altbackupdate_summary,
        icon = Phosphor.CalendarX,
        defaultValue = false,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.ALT_BACKUP_DATE,
    )

    val altBlockLayout = PrefBoolean(
        titleId = R.string.prefs_altblocklayout,
        summaryId = R.string.prefs_altblocklayout_summary,
        icon = Phosphor.Swatches,
        defaultValue = false,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.ALT_BLOCK_LAYOUT,
    )

    val oldBackups = PrefInt(
        titleId = R.string.prefs_oldbackups,
        summaryId = R.string.prefs_oldbackups_summary,
        icon = Phosphor.Clock,
        entries = (1..30).toList(),
        defaultValue = 2,
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.OLD_BACKUPS,
    )

    private fun onThemeChanged() {
        context.setCustomTheme()
        //recreateActivities()
    }

    fun observeDependency(key: String) = key.takeIf { it.isNotBlank() }?.let {
        dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: false
        }.distinctUntilChanged()
    } ?: flowOf(true)

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