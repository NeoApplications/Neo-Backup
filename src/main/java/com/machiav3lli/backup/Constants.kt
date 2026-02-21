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
package com.machiav3lli.backup

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.data.dbs.entity.PackageInfo
import com.machiav3lli.backup.data.entity.ChipItem
import com.machiav3lli.backup.data.entity.Legend
import com.machiav3lli.backup.data.entity.Link
import com.machiav3lli.backup.utils.extensions.Android
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

const val PREFS_SHARED_PRIVATE = "com.machiav3lli.backup"
const val STATEFLOW_SUBSCRIBE_BUFFER = 60_000L // 1 minute buffer

const val NOTIFICATION_CHANNEL_ACTIONWORK = "AppActionWork"
const val NOTIFICATION_CHANNEL_HANDLER = "NotificationHandler"
const val NOTIFICATION_CHANNEL_SCHEDULE = "ScheduleWork"
const val NOTIFICATION_CHANNEL_REFRESH = "RefreshBackups"

const val ADMIN_PREFIX = "!-"

val COMPRESSION_TYPES = mapOf(
    "gz" to "Gzip Compression",    // TODO translation?
    "zst" to "Zstd Compression",    // TODO translation?
    "no" to "No Compression"      // TODO translation?
)

const val SELECTIONS_FOLDER_NAME_BASE = "SELECTIONS"
const val SELECTIONS_FOLDER_NAME = "$ADMIN_PREFIX$SELECTIONS_FOLDER_NAME_BASE"

const val EXPORTS_FOLDER_NAME_BASE = "EXPORTS"
const val EXPORTS_FOLDER_NAME = "$ADMIN_PREFIX$EXPORTS_FOLDER_NAME_BASE"
const val EXPORTS_FOLDER_NAME_ALT = EXPORTS_FOLDER_NAME_BASE

const val LOGS_FOLDER_NAME_BASE =
    "LOGS"  // TODO deprecated, only for excluding from backup scanning

const val ERROR_PREFIX = "${ADMIN_PREFIX}ERROR."

const val PREFS_BACKUP_FILE = "${ADMIN_PREFIX}app.preferences"

const val PROP_NAME = "properties"
const val LOG_INSTANCE = "%s.log.txt"

enum class MenuAction { PUT, GET, DEL }

// optional millisec to include old format
const val BACKUP_INSTANCE_REGEX_PATTERN = """\d\d\d\d-\d\d-\d\d-\d\d-\d\d-\d\d(-\d\d\d)?-user_\d+"""

fun backupInstanceDir(packageInfo: PackageInfo, dateTimeStr: String) =
    "$dateTimeStr-user_${packageInfo.profileId}"

fun backupInstanceDirFlat(packageInfo: PackageInfo, dateTimeStr: String) =
    "${packageInfo.packageName}@$dateTimeStr-user_${packageInfo.profileId}"

fun backupInstanceProps(packageInfo: PackageInfo, dateTimeStr: String) =
    "${backupInstanceDir(packageInfo, dateTimeStr)}.$PROP_NAME"

fun backupInstancePropsFlat(packageInfo: PackageInfo, dateTimeStr: String) =
    "${backupInstanceDirFlat(packageInfo, dateTimeStr)}.$PROP_NAME"

const val BACKUP_INSTANCE_PROPERTIES_INDIR = "backup.$PROP_NAME"
const val BACKUP_PACKAGE_FOLDER_REGEX_PATTERN = """\w+(\.\w+)+"""
const val BACKUP_SPECIAL_FILE_REGEX_PATTERN = """(^\.|^$ADMIN_PREFIX|^$ERROR_PREFIX)"""
const val BACKUP_SPECIAL_FOLDER_REGEX_PATTERN =
    """(^\.|^$ADMIN_PREFIX|$EXPORTS_FOLDER_NAME_BASE|$LOGS_FOLDER_NAME_BASE|$SELECTIONS_FOLDER_NAME_BASE)"""
const val EXPORTS_INSTANCE = "%s.scheds"

const val MIME_TYPE_FILE = "application/octet-stream"
const val MIME_TYPE_DIR = DocumentsContract.Document.MIME_TYPE_DIR

const val MAIN_DB_NAME = "main.db"
const val PACKAGES_LIST_GLOBAL_ID = -1L

const val ACTION_CANCEL = "cancel"
const val ACTION_RUN_SCHEDULE = "schedule"
const val ACTION_CANCEL_SCHEDULE = "cancel_schedule"
const val ACTION_RE_SCHEDULE = "reschedule"
const val ACTION_CRASH = "crash"

enum class DialogMode {
    NONE,
    BACKUP,
    RESTORE,
    DELETE,
    DELETE_ALL,
    CLEAN_CACHE,
    FORCE_KILL,
    ENABLE_DISABLE,
    UNINSTALL,
    ADD_TAG,
    NOTE,
    ENFORCE_LIMIT,
    NOTE_BACKUP,
    BLOCKLIST,
    CUSTOMLIST,
    TIME_PICKER,
    INTERVAL_SETTER,
    SCHEDULE_NAME,
    SCHEDULE_RUN,
    NO_SAF,
    PERMISSION_USAGE_STATS,
    PERMISSION_SMS_MMS,
    PERMISSION_CALL_LOGS,
    PERMISSION_CONTACTS,
    PERMISSION_BATTERY_OPTIMIZATION,
    TOOL_DELETE_BACKUP_UNINSTALLED,
    TOOL_CLEANUP_BACKUP_DIR,
    TOOL_SAVE_APPS_LIST,
}

const val PREFS_LANGUAGES_SYSTEM = "system"
const val EXTRA_PACKAGE_NAME = "packageName"
const val EXTRA_BACKUP_BOOLEAN = "backupBoolean"
const val EXTRA_SCHEDULE_ID = "scheduleId"
const val EXTRA_PERIODIC = "periodic"
const val EXTRA_NAME = "name"
const val EXTRA_STATS = "stats"

const val FIELD_BACKUP_DATE = "backupDate"
const val FIELD_BLOCKLIST_ID = "blocklistId"
const val FIELD_BLOCK_LIST = "blockList"
const val FIELD_CUSTOM_LIST = "customList"
const val FIELD_ID = "id"
const val FIELD_IS_SYSTEM = "isSystem"
const val FIELD_NAME = "name"
const val FIELD_PACKAGE_NAME = "packageName"
const val FIELD_CUSTOM_TAGS = "customTags"

enum class THEME {
    LIGHT,
    DARK,
    BLACK,
    LIGHT_MEDIUM,
    DARK_MEDIUM,
    BLACK_MEDIUM,
    LIGHT_HIGH,
    DARK_HIGH,
    BLACK_HIGH,
    SYSTEM,
    SYSTEM_BLACK,
    DYNAMIC,
    DYNAMIC_LIGHT,
    DYNAMIC_DARK,
    DYNAMIC_BLACK,
}

val themeItems = mutableMapOf(
    THEME.LIGHT.ordinal to R.string.prefs_theme_light,
    THEME.DARK.ordinal to R.string.prefs_theme_dark,
    THEME.BLACK.ordinal to R.string.prefs_theme_black,
    THEME.LIGHT_MEDIUM.ordinal to R.string.light_medium_contrast,
    THEME.DARK_MEDIUM.ordinal to R.string.dark_medium_contrast,
    THEME.BLACK_MEDIUM.ordinal to R.string.black_medium_contrast,
    THEME.LIGHT_HIGH.ordinal to R.string.light_high_contrast,
    THEME.DARK_HIGH.ordinal to R.string.dark_high_contrast,
    THEME.BLACK_HIGH.ordinal to R.string.black_high_contrast,
).apply {
    if (Android.minSDK(Build.VERSION_CODES.Q)) {
        set(THEME.SYSTEM.ordinal, R.string.prefs_theme_system)
        set(THEME.SYSTEM_BLACK.ordinal, R.string.prefs_theme_system_black)
    }
    if (Android.minSDK(Build.VERSION_CODES.S)) {
        set(THEME.DYNAMIC.ordinal, R.string.prefs_theme_dynamic)
        set(THEME.DYNAMIC_LIGHT.ordinal, R.string.prefs_theme_dynamic_light)
        set(THEME.DYNAMIC_DARK.ordinal, R.string.prefs_theme_dynamic_dark)
        set(THEME.DYNAMIC_BLACK.ordinal, R.string.prefs_theme_dynamic_black)
    }
}

enum class ENCRYPTION {
    NONE,
    PASSWORD,
    PGP,
}

val encryptionModes = mutableMapOf(
    ENCRYPTION.NONE.ordinal to R.string.prefs_encryption_none,
    ENCRYPTION.PASSWORD.ordinal to R.string.prefs_encryption_password,
    ENCRYPTION.PGP.ordinal to R.string.prefs_encryption_pgp,
)

val BUTTON_SIZE_MEDIUM = 48.dp
val ICON_SIZE_SMALL = 24.dp // Default
val ICON_SIZE_MEDIUM = 32.dp
val ICON_SIZE_LARGE = 48.dp

val accentColorItems = mapOf(
    0 to R.string.prefs_accent_0,
    1 to R.string.prefs_accent_1,
    2 to R.string.prefs_accent_2,
    3 to R.string.prefs_accent_3,
    4 to R.string.prefs_accent_4,
    5 to R.string.prefs_accent_5,
    6 to R.string.prefs_accent_6,
    7 to R.string.prefs_accent_7,
    8 to R.string.prefs_accent_8
)

val secondaryColorItems = mapOf(
    0 to R.string.prefs_secondary_0,
    1 to R.string.prefs_secondary_1,
    2 to R.string.prefs_secondary_2,
    3 to R.string.prefs_secondary_3,
    4 to R.string.prefs_secondary_4,
    5 to R.string.prefs_secondary_5,
    6 to R.string.prefs_secondary_6,
    7 to R.string.prefs_secondary_7,
    8 to R.string.prefs_secondary_8
)

const val ALT_MODE_UNSET = 0
const val ALT_MODE_APK = 1
const val ALT_MODE_DATA = 2
const val ALT_MODE_BOTH = 3

const val MODE_UNSET = 0b0000000

const val MODE_APK = 0b0010000
const val MODE_DATA = 0b0001000
const val MODE_DATA_DE = 0b0000100
const val MODE_DATA_EXT = 0b0000010
const val MODE_DATA_OBB = 0b0000001
const val MODE_DATA_MEDIA = 0b1000000

const val MODE_NONE = 0b0100000     //TODO name? it's not a mode! BACKUP_FILTER_NONE?

val batchModes =
    mapOf(
        MODE_APK to "apk",
        MODE_DATA to "data",
        MODE_DATA_DE to "device-protected",
        MODE_DATA_EXT to "external",
        MODE_DATA_OBB to "obb",
        MODE_DATA_MEDIA to "media"
    )
val batchOperations = mapOf(
    MODE_APK to "a",
    MODE_DATA to "=d",
    MODE_DATA_DE to "==p",
    MODE_DATA_EXT to "===x",
    MODE_DATA_OBB to "====o",
    MODE_DATA_MEDIA to "=====m",
)
val batchModesSequence =
    listOf(MODE_APK, MODE_DATA, MODE_DATA_DE, MODE_DATA_EXT, MODE_DATA_OBB, MODE_DATA_MEDIA)

val MODE_ALL = batchModesSequence.reduce { a, b -> a.or(b) }
val BACKUP_FILTER_DEFAULT = MODE_ALL or MODE_NONE

val scheduleBackupModeChipItems = persistentListOf(
    ChipItem.Apk,
    ChipItem.Data,
    ChipItem.DeData,
    ChipItem.ExtData,
    ChipItem.ObbData,
    ChipItem.MediaData
)

val mainBackupModeChipItems: PersistentList<ChipItem> = persistentListOf(
    ChipItem.None,
    ChipItem.Apk,
    ChipItem.Data,
    ChipItem.DeData,
    ChipItem.ExtData,
    ChipItem.ObbData,
    ChipItem.MediaData
)

enum class Sort { LABEL, PACKAGENAME, APP_SIZE, DATA_SIZE, APPDATA_SIZE, BACKUP_SIZE, BACKUP_DATE }

val sortChipItems = persistentListOf(
    ChipItem.Label,
    ChipItem.PackageName,
    ChipItem.AppSize,
    ChipItem.DataSize,
    ChipItem.AppDataSize,
    ChipItem.BackupSize,
    ChipItem.BackupDate
)

const val MAIN_FILTER_UNSET = 0b000
const val MAIN_FILTER_SYSTEM = 0b100
const val MAIN_FILTER_USER = 0b010
const val MAIN_FILTER_SPECIAL = 0b001
const val MAIN_FILTER_DEFAULT = MAIN_FILTER_SYSTEM or MAIN_FILTER_USER or MAIN_FILTER_SPECIAL
const val MAIN_FILTER_DEFAULT_WITHOUT_SPECIAL = MAIN_FILTER_SYSTEM or MAIN_FILTER_USER
val possibleMainFilters = listOf(MAIN_FILTER_SYSTEM, MAIN_FILTER_USER, MAIN_FILTER_SPECIAL)

val mainFilterChipItems = persistentListOf(ChipItem.System, ChipItem.User, ChipItem.Special)
val mainFilterChipItemsSansSpecial = persistentListOf(ChipItem.System, ChipItem.User)

enum class LaunchableFilter { ALL, LAUNCHABLE, NOT }

val launchableFilterChipItems = persistentListOf(
    ChipItem.All,
    ChipItem.Launchable,
    ChipItem.NotLaunchable,
)

enum class InstalledFilter { ALL, INSTALLED, NOT }

val installedFilterChipItems = persistentListOf(
    ChipItem.All,
    ChipItem.Installed,
    ChipItem.NotInstalled,
)

enum class UpdatedFilter { ALL, UPDATED, NEW, NOT }

val updatedFilterChipItems = persistentListOf(
    ChipItem.All,
    ChipItem.UpdatedApps,
    ChipItem.NewApps,
    ChipItem.OldApps,
)

enum class LatestFilter { ALL, OLD, NEW }

val latestFilterChipItems = persistentListOf(
    ChipItem.All,
    ChipItem.OldBackups,
    ChipItem.NewBackups,
)

enum class EnabledFilter { ALL, ENABLED, DISABLED }

val enabledFilterChipItems = persistentListOf(
    ChipItem.All,
    ChipItem.Enabled,
    ChipItem.Disabled,
)

val IGNORED_PERMISSIONS = listOfNotNull(
    Manifest.permission.ACCESS_WIFI_STATE,
    Manifest.permission.ACCESS_NETWORK_STATE,
    Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
    if (Android.minSDK(Build.VERSION_CODES.P)) Manifest.permission.FOREGROUND_SERVICE else null,
    Manifest.permission.INSTALL_SHORTCUT,
    Manifest.permission.INTERNET,
    if (Android.minSDK(Build.VERSION_CODES.R)) Manifest.permission.QUERY_ALL_PACKAGES else null,
    Manifest.permission.REQUEST_DELETE_PACKAGES,
    Manifest.permission.RECEIVE_BOOT_COMPLETED,
    Manifest.permission.READ_SYNC_SETTINGS,
    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    Manifest.permission.USE_FINGERPRINT,
    Manifest.permission.WAKE_LOCK,
)

const val BUNDLE_USERS = "users"

const val CHIP_TYPE = 0
const val CHIP_VERSION = 1
const val CHIP_SIZE_APP = 2
const val CHIP_SIZE_DATA = 3
const val CHIP_SIZE_CACHE = 4
const val CHIP_SPLIT = 5

enum class DamagedOp {
    RENAME,
    UNDO,
    DELETE,
    CLEANUP,
}

const val HELP_CHANGELOG =
    "https://codeberg.org/NeoApplications/Neo-Backup/src/branch/main/CHANGELOG.md"
const val HELP_TELEGRAM = "https://t.me/neo_backup"
const val HELP_MATRIX = "https://matrix.to/#/#neo-backup:matrix.org"
const val HELP_LICENSE =
    "https://codeberg.org/NeoApplications/Neo-Backup/src/branch/main/LICENSE.md"
const val HELP_ISSUES = "https://github.com/NeoApplications/Neo-Backup/issues"
const val HELP_FAQ = "https://codeberg.org/NeoApplications/Neo-Backup/src/branch/main/FAQ.md"

const val LINK_KEEP_ANDROID_OPEN = "https://keepandroidopen.org"

val linksList =
    listOf(Link.Changelog, Link.Telegram, Link.Matrix, Link.FAQ, Link.Issues, Link.License)

val legendList = listOf(
    Legend.Exodus,
    Legend.Launch,
    Legend.Disable,
    Legend.Enable,
    Legend.Uninstall,
    Legend.Block,
    Legend.System,
    Legend.User,
    Legend.Special,
    Legend.APK,
    Legend.Data,
    Legend.DE_Data,
    Legend.External,
    Legend.OBB,
    Legend.Media,
    Legend.Updated,
)

val OPEN_DIRECTORY_INTENT = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    .addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
    .putExtra("android.content.extra.SHOW_ADVANCED", true)
    .putExtra("android.content.extra.FANCY", true)
    .putExtra("android.content.extra.SHOW_FILESIZE", true)
val BACKUP_DIRECTORY_INTENT = OPEN_DIRECTORY_INTENT

fun classAddress(address: String): String = PREFS_SHARED_PRIVATE + address

fun exodusUrl(app: String): String = "https://reports.exodus-privacy.eu.org/reports/$app/latest"
