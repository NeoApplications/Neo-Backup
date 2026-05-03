package com.machiav3lli.backup.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object PrefKey {
    // SortFilter.home
    val SORT_ASC_HOME = booleanPreferencesKey("sortAsc_home")
    val SORT_HOME = intPreferencesKey("sort_home")
    val MAIN_FILTER_HOME = intPreferencesKey("main_filter_home")
    val BACKUP_FILTER_HOME = intPreferencesKey("backup_filter_home")
    val INSTALLED_FILTER_HOME = intPreferencesKey("installed_filter_home")
    val LAUNCHABLE_FILTER_HOME = intPreferencesKey("launchable_filter_home")
    val UPDATED_FILTER_HOME = intPreferencesKey("updated_filter_home")
    val LATEST_FILTER_HOME = intPreferencesKey("latest_filter_home")
    val ENABLED_FILTER_HOME = intPreferencesKey("enabled_filter_home")
    val TAGS_FILTER_HOME = stringSetPreferencesKey("tags_filter_home")

    // SortFilter.backup
    val SORT_ASC_BACKUP = booleanPreferencesKey("sortAsc_backup")
    val SORT_BACKUP = intPreferencesKey("sort_backup")
    val MAIN_FILTER_BACKUP = intPreferencesKey("main_filter_backup")
    val BACKUP_FILTER_BACKUP = intPreferencesKey("backup_filter_backup")
    val INSTALLED_FILTER_BACKUP = intPreferencesKey("installed_filter_backup")
    val LAUNCHABLE_FILTER_BACKUP = intPreferencesKey("launchable_filter_backup")
    val UPDATED_FILTER_BACKUP = intPreferencesKey("updated_filter_backup")
    val LATEST_FILTER_BACKUP = intPreferencesKey("latest_filter_backup")
    val ENABLED_FILTER_BACKUP = intPreferencesKey("enabled_filter_backup")
    val TAGS_FILTER_BACKUP = stringSetPreferencesKey("tags_filter_backup")

    // SortFilter.restore
    val SORT_ASC_RESTORE = booleanPreferencesKey("sortAsc_restore")
    val SORT_RESTORE = intPreferencesKey("sort_restore")
    val MAIN_FILTER_RESTORE = intPreferencesKey("main_filter_restore")
    val BACKUP_FILTER_RESTORE = intPreferencesKey("backup_filter_restore")
    val INSTALLED_FILTER_RESTORE = intPreferencesKey("installed_filter_restore")
    val LAUNCHABLE_FILTER_RESTORE = intPreferencesKey("launchable_filter_restore")
    val UPDATED_FILTER_RESTORE = intPreferencesKey("updated_filter_restore")
    val LATEST_FILTER_RESTORE = intPreferencesKey("latest_filter_restore")
    val ENABLED_FILTER_RESTORE = intPreferencesKey("enabled_filter_restore")
    val TAGS_FILTER_RESTORE = stringSetPreferencesKey("tags_filter_restore")
}

object UserPrefKey {
    val LANGUAGES = stringPreferencesKey("user.languages")
    val APP_THEME = intPreferencesKey("user.appThemeNeo")
    val APP_ACCENT_COLOR = intPreferencesKey(".appAccentColor")
    val APP_SECONDARY_COLOR = intPreferencesKey(".appSecondaryColor")
    val PATH_BACKUP_FOLDER = stringPreferencesKey("user.pathBackupFolder")
    val DEVICE_LOCK = booleanPreferencesKey("user.deviceLock")
    val BIOMETRIC_LOCK = booleanPreferencesKey("user.biometricLock")
    val MULTILINE_INFO_CHIPS = booleanPreferencesKey("user.multilineInfoChips")
    val SINGULAR_BACKUP_RESTORE = booleanPreferencesKey("user.singularBackupRestore")
    val NEW_AND_UPDATED_NOTIFICATION = booleanPreferencesKey("user.newAndUppdatedNotification")
    val SQUEEZE_NAV_TEXT = booleanPreferencesKey("user.squeezeNavText")
    val ALT_NAV_BAR_ITEM = booleanPreferencesKey("user.altNavBarItem")
    val ALT_BACKUP_DATE = booleanPreferencesKey("user.altBackupDate")
    val ALT_BLOCK_LAYOUT = booleanPreferencesKey("user.altBlockLayout")
    val OLD_BACKUPS = intPreferencesKey("user.oldBackups")
}