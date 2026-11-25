package com.machiav3lli.backup.data.preferences

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.machiav3lli.backup.BACKUP_DIRECTORY_INTENT
import com.machiav3lli.backup.BACKUP_FILTER_DEFAULT
import com.machiav3lli.backup.COMPRESSION_TYPES
import com.machiav3lli.backup.ENCRYPTION
import com.machiav3lli.backup.EnabledFilter
import com.machiav3lli.backup.InstalledFilter
import com.machiav3lli.backup.LatestFilter
import com.machiav3lli.backup.LaunchableFilter
import com.machiav3lli.backup.MAIN_FILTER_USER
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.NeoApp.Companion.mainSerializer
import com.machiav3lli.backup.PREFS_LANGUAGES_SYSTEM
import com.machiav3lli.backup.R
import com.machiav3lli.backup.Sort
import com.machiav3lli.backup.UpdatedFilter
import com.machiav3lli.backup.batchModesSequence
import com.machiav3lli.backup.data.entity.NeoTheme
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.encryptionModes
import com.machiav3lli.backup.manager.handler.PGPHandler
import com.machiav3lli.backup.possibleMainFilters
import com.machiav3lli.backup.ui.compose.component.StringEditPreference
import com.machiav3lli.backup.ui.compose.component.StringPreference
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.AndroidLogo
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowsClockwise
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowsOutLineVertical
import com.machiav3lli.backup.ui.compose.icons.phosphor.AsteriskSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.Broom
import com.machiav3lli.backup.ui.compose.icons.phosphor.Bug
import com.machiav3lli.backup.ui.compose.icons.phosphor.CalendarX
import com.machiav3lli.backup.ui.compose.icons.phosphor.CircleWavyWarning
import com.machiav3lli.backup.ui.compose.icons.phosphor.Clock
import com.machiav3lli.backup.ui.compose.icons.phosphor.ClockCounterClockwise
import com.machiav3lli.backup.ui.compose.icons.phosphor.FileZip
import com.machiav3lli.backup.ui.compose.icons.phosphor.FingerprintSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.FloppyDisk
import com.machiav3lli.backup.ui.compose.icons.phosphor.FolderNotch
import com.machiav3lli.backup.ui.compose.icons.phosphor.GameController
import com.machiav3lli.backup.ui.compose.icons.phosphor.Hash
import com.machiav3lli.backup.ui.compose.icons.phosphor.Key
import com.machiav3lli.backup.ui.compose.icons.phosphor.List
import com.machiav3lli.backup.ui.compose.icons.phosphor.ListNumbers
import com.machiav3lli.backup.ui.compose.icons.phosphor.Lock
import com.machiav3lli.backup.ui.compose.icons.phosphor.Password
import com.machiav3lli.backup.ui.compose.icons.phosphor.PlayCircle
import com.machiav3lli.backup.ui.compose.icons.phosphor.Prohibit
import com.machiav3lli.backup.ui.compose.icons.phosphor.ProhibitInset
import com.machiav3lli.backup.ui.compose.icons.phosphor.ShieldCheckered
import com.machiav3lli.backup.ui.compose.icons.phosphor.ShieldStar
import com.machiav3lli.backup.ui.compose.icons.phosphor.Swatches
import com.machiav3lli.backup.ui.compose.icons.phosphor.TagSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.TextAa
import com.machiav3lli.backup.ui.compose.icons.phosphor.Textbox
import com.machiav3lli.backup.ui.compose.icons.phosphor.Translate
import com.machiav3lli.backup.ui.compose.icons.phosphor.TrashSimple
import com.machiav3lli.backup.ui.navigation.NavRoute
import com.machiav3lli.backup.ui.pages.pref_restartAppOnLanguageChange
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.backupDirConfigured
import com.machiav3lli.backup.utils.backupFolderExists
import com.machiav3lli.backup.utils.defaultFromJson
import com.machiav3lli.backup.utils.defaultToJson
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.extensions.combine
import com.machiav3lli.backup.utils.getLanguageList
import com.machiav3lli.backup.utils.recreateActivities
import com.machiav3lli.backup.utils.restartApp
import com.machiav3lli.backup.utils.scheduleAlarmsOnce
import com.machiav3lli.backup.utils.setBackupDir
import com.machiav3lli.backup.utils.setCustomTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.koin.compose.koinInject
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
        defaultValue = PREFS_LANGUAGES_SYSTEM,
        onChanged = { _ ->
            if (pref_restartAppOnLanguageChange.value)
                context.restartApp()
            else
                recreateActivities()
        },
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.LANGUAGES,
    )

    val neoTheme = PrefTheme(
        titleId = R.string.prefs_theme,
        icon = Phosphor.Swatches,
        defaultValue = if (Android.minSDK(31)) NeoTheme.DynamicSystem
        else NeoTheme.SystemBlack,
        onChanged = { MainScope().launch { onThemeChanged() } },
        dataStore = dataStore,
        dataStoreKey = UserPrefKey.NEO_THEME,
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
                setBackupDir(p.value.toUri())
            }
        },
        UI = { pref, _, index, groupSize ->
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

    // ------------------- ADVANCED PREFS ------------------------------

    val enableSpecialBackups = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = AdvancedPrefKey.ENABLE_SPECIAL_BACKUPS,
        defaultValue = false,
        titleId = R.string.prefs_enablespecial,
        summaryId = R.string.prefs_enablespecial_summary,
        icon = Phosphor.AsteriskSimple,
        onChanged = { NeoApp.main?.enableSpecials((it as PrefBoolean).value) },
    )

    val disableVerification = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = AdvancedPrefKey.DISABLE_VERIFICATION,
        defaultValue = true,
        titleId = R.string.prefs_disableverification,
        summaryId = R.string.prefs_disableverification_summary,
        icon = Phosphor.AndroidLogo,
    )

    val giveAllPermissions = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = AdvancedPrefKey.GIVE_ALL_PERMISSIONS,
        defaultValue = false,
        titleId = R.string.prefs_restoreallpermissions,
        summaryId = R.string.prefs_restoreallpermissions_summary,
        icon = Phosphor.ShieldStar,
    )

    val allowDowngrade = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = AdvancedPrefKey.ALLOW_DOWNGRADE,
        defaultValue = false,
        titleId = R.string.prefs_allowdowngrade,
        summaryId = R.string.prefs_allowdowngrade_summary,
        icon = Phosphor.ClockCounterClockwise,
    )

    val useExactAlarm = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = AdvancedPrefKey.USE_EXACT_ALARM,
        defaultValue = true,
        titleId = R.string.prefs_useexactalarm_title,
        summaryId = R.string.prefs_useexactalarm_summary,
        icon = Phosphor.Clock,
        onChanged = { scheduleAlarmsOnce(NeoApp.context) },
    )

    // ------------------- SERVICE PREFS ------------------------------

    val encryptionMode = PrefEnum(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.ENCRYPTION_MODE,
        defaultValue = ENCRYPTION.NONE.ordinal,
        titleId = R.string.prefs_encryption,
        icon = Phosphor.Key,
        entries = encryptionModes,
    )

    val encryption = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.ENC,
        titleId = R.string.prefs_encryption,
        summaryId = R.string.prefs_encryption_summary,
        icon = Phosphor.Key,
        onClick = { NeoApp.main?.moveTo(NavRoute.Encryption) },
    )

    val password = PrefString(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.ENCRYPTION_PASSWORD,
        defaultValue = "",
        titleId = R.string.prefs_password,
        summaryId = R.string.prefs_password_summary,
        icon = Phosphor.Password,
        iconTint = { pref ->
            val p = pref as PrefString
            if (p.value.isNotEmpty()) Color.Green else Color.Gray
        },
    )

    val pgpKey = PrefString(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.PGP_KEY,
        defaultValue = "",
        titleId = R.string.prefs_pgp_key,
        summaryId = R.string.prefs_pgp_key_summary,
        icon = Phosphor.Key,
        UI = { pref, _, index, groupSize ->
            val scope = rememberCoroutineScope()
            val pgpManager: PGPHandler = koinInject<PGPHandler>()

            val p = pref as PrefString
            val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    uri?.let {
                        scope.launch {
                            pgpManager.loadKeyFromUri(it).fold(
                                onSuccess = {
                                    pgpManager.isKeyLoaded()
                                },
                                onFailure = { _ -> }
                            )
                        }
                    }
                }

            StringPreference(
                pref = p,
                index = index,
                groupSize = groupSize,
                onClick = {
                    launcher.launch(arrayOf("*/*"))
                },
            )
        },
    )

    val pgpPasscode = PrefString(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.PGP_PASSCODE,
        defaultValue = "",
        titleId = R.string.prefs_pgp_passcode,
        summaryId = R.string.prefs_pgp_passcode_summary,
        icon = Phosphor.Password,
    )

    val restorePermissions = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.RESTORE_PERMISSIONS,
        defaultValue = true,
        titleId = R.string.prefs_restorepermissions,
        summaryId = R.string.prefs_restorepermissions_summary,
        icon = Phosphor.ShieldStar,
    )

    val numBackupRevisions = PrefInt(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.NUM_BACKUP_REVISIONS,
        defaultValue = 2,
        titleId = R.string.prefs_numBackupRevisions,
        summaryId = R.string.prefs_numBackupRevisions_summary,
        icon = Phosphor.Hash,
        entries = (0..9) + (10..20 step 2) + (50..200 step 50),
    )

    val enableStorageCheck = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.ENABLE_STORAGE_CHECK,
        defaultValue = true,
        titleId = R.string.prefs_enablestoragecheck,
        summaryId = R.string.prefs_enablestoragecheck_summary,
        icon = Phosphor.FloppyDisk,
    )

    val compressionType = PrefList(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.COMPRESSION_TYPE,
        defaultValue = "zst",
        titleId = R.string.prefs_compression_type,
        summaryId = R.string.prefs_compression_type_summary,
        icon = Phosphor.FileZip,
        entries = COMPRESSION_TYPES,
    )

    val compressionLevel = PrefInt(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.COMPRESSION_LEVEL,
        defaultValue = 2,
        titleId = R.string.prefs_compression_level,
        summaryId = R.string.prefs_compression_level_summary,
        icon = Phosphor.FileZip,
        entries = (0..9).toList(),
    )

    val enableSessionInstaller = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.ENABLE_SESSION_INSTALLER,
        defaultValue = true,
        titleId = R.string.prefs_sessionIinstaller,
        summaryId = R.string.prefs_sessionIinstaller_summary,
        icon = Phosphor.TagSimple,
    )

    val installationPackage = PrefString(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.INSTALLATION_PACKAGE,
        defaultValue = SystemUtils.packageName,
        titleId = R.string.prefs_installerpackagename,
        icon = Phosphor.Textbox,
    )

    val backupDeviceProtectedData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.BACKUP_DEVICE_PROTECTED_DATA,
        defaultValue = true,
        titleId = R.string.prefs_deviceprotecteddata,
        summaryId = R.string.prefs_deviceprotecteddata_summary,
        icon = Phosphor.ShieldCheckered,
    )

    val backupExternalData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.BACKUP_EXTERNAL_DATA,
        defaultValue = true,
        titleId = R.string.prefs_externaldata,
        summaryId = R.string.prefs_externaldata_summary,
        icon = Phosphor.FloppyDisk,
    )

    val backupObbData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.BACKUP_OBB_DATA,
        defaultValue = true,
        titleId = R.string.prefs_obbdata,
        summaryId = R.string.prefs_obbdata_summary,
        icon = Phosphor.GameController,
    )

    val backupMediaData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.BACKUP_MEDIA_DATA,
        defaultValue = true,
        titleId = R.string.prefs_mediadata,
        summaryId = R.string.prefs_mediadata_summary,
        icon = Phosphor.PlayCircle,
    )

    val backupNoBackupData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.BACKUP_NO_BACKUP_DATA,
        defaultValue = false,
        titleId = R.string.prefs_nobackupdata,
        summaryId = R.string.prefs_nobackupdata_summary,
        icon = Phosphor.ProhibitInset,
        onChanged = { NeoApp.assets.updateExcludeFiles() },
    )

    val backupCache = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.BACKUP_CACHE,
        defaultValue = false,
        titleId = R.string.prefs_backupcache,
        summaryId = R.string.prefs_backupcache_summary,
        icon = Phosphor.Prohibit,
    )

    val restoreDeviceProtectedData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.RESTORE_DEVICE_PROTECTED_DATA,
        defaultValue = true,
        titleId = R.string.prefs_deviceprotecteddata_rst,
        summaryId = R.string.prefs_deviceprotecteddata_rst_summary,
        icon = Phosphor.ShieldCheckered,
    )

    val restoreExternalData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.RESTORE_EXTERNAL_DATA,
        defaultValue = true,
        titleId = R.string.prefs_externaldata_rst,
        summaryId = R.string.prefs_externaldata_rst_summary,
        icon = Phosphor.FloppyDisk,
    )

    val restoreObbData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.RESTORE_OBB_DATA,
        defaultValue = true,
        titleId = R.string.prefs_obbdata_rst,
        summaryId = R.string.prefs_obbdata_rst_summary,
        icon = Phosphor.GameController,
    )

    val restoreMediaData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.RESTORE_MEDIA_DATA,
        defaultValue = true,
        titleId = R.string.prefs_mediadata_rst,
        summaryId = R.string.prefs_mediadata_rst_summary,
        icon = Phosphor.PlayCircle,
    )

    val restoreNoBackupData = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.RESTORE_NO_BACKUP_DATA,
        defaultValue = false,
        titleId = R.string.prefs_nobackupdata_rst,
        summaryId = R.string.prefs_nobackupdata_rst_summary,
        icon = Phosphor.ProhibitInset,
        onChanged = { NeoApp.assets.updateExcludeFiles() },
    )

    val restoreCache = PrefBoolean(
        dataStore = dataStore,
        dataStoreKey = ServicePrefKey.RESTORE_CACHE,
        defaultValue = false,
        titleId = R.string.prefs_restorecache,
        summaryId = R.string.prefs_restorecache_summary,
        icon = Phosphor.Prohibit,
    )

    // ------------------- TOOL PREFS ------------------------------

    val batchDelete = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.BATCH_DELETE,
        titleId = R.string.prefs_batchdelete,
        summaryId = R.string.prefs_batchdelete_summary,
        icon = Phosphor.TrashSimple,
    )

    val cleanupBackupDir = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.CLEANUP_BACKUP_DIR,
        titleId = R.string.prefs_cleanup_backupdir,
        summaryId = R.string.prefs_cleanup_backupdir_summary,
        icon = Phosphor.Broom,
    )

    val enforceBackupsLimit = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.ENFORCE_BACKUPS_LIMIT,
        titleId = R.string.enforce_backups_limit,
        summaryId = R.string.enforce_backups_limit_summary,
        icon = Phosphor.Hash,
    )

    val copySelfApk = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.COPY_SELF_APK,
        titleId = R.string.prefs_copyselfapk,
        summaryId = R.string.prefs_copyselfapk_summary,
        icon = Phosphor.AndroidLogo,
    )

    val schedulesExportImport = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.SCHEDULES_EXPORT_IMPORT,
        titleId = R.string.prefs_schedulesexportimport,
        summaryId = R.string.prefs_schedulesexportimport_summary,
        icon = Phosphor.CalendarX,
    )

    val saveAppsList = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.SAVE_APPS_LIST,
        titleId = R.string.prefs_saveappslist,
        summaryId = R.string.prefs_saveappslist_summary,
        icon = Phosphor.ListNumbers,
    )

    val logViewer = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.LOG_VIEWER,
        titleId = R.string.prefs_logviewer,
        icon = Phosphor.Bug,
    )

    val terminal = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.TERMINAL,
        titleId = R.string.prefs_tools_terminal,
        icon = Phosphor.Bug,
    )

    val exportSettings = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.EXPORT_SETTINGS,
        titleId = R.string.prefs_exportsettings,
        summaryId = R.string.prefs_exportsettings_summary,
        icon = Phosphor.FloppyDisk,
    )

    val importSettings = PrefLaunch(
        dataStore = dataStore,
        dataStoreKey = ToolPrefKey.IMPORT_SETTINGS,
        titleId = R.string.prefs_importsettings,
        summaryId = R.string.prefs_importsettings_summary,
        icon = Phosphor.ArrowsClockwise,
    )

    private val unexportableKeys: Set<String> = setOf(
        ServicePrefKey.ENCRYPTION_PASSWORD.name,
        ServicePrefKey.PGP_KEY.name,
        ServicePrefKey.PGP_PASSCODE.name,
        ServicePrefKey.ENCRYPTION_MODE.name,
        ServicePrefKey.ENC.name,
    )

    private val exportableSettings: List<PrefSetting<*>> = listOf(
        // Sort / filter prefs
        PrefSetting(
            sortHome.key,
            { sortHome.value },
            { sortHome.set(it) }),
        PrefSetting(
            sortBackup.key,
            { sortBackup.value },
            { sortBackup.set(it) }),
        PrefSetting(
            sortRestore.key,
            { sortRestore.value },
            { sortRestore.set(it) }),
        PrefSetting(
            sortAscHome.key,
            { sortAscHome.value },
            { sortAscHome.set(it) }),
        PrefSetting(
            sortAscBackup.key,
            { sortAscBackup.value },
            { sortAscBackup.set(it) }),
        PrefSetting(
            sortAscRestore.key,
            { sortAscRestore.value },
            { sortAscRestore.set(it) }),
        PrefSetting(
            mainFilterHome.key,
            { mainFilterHome.value },
            { mainFilterHome.set(it) }),
        PrefSetting(
            mainFilterBackup.key,
            { mainFilterBackup.value },
            { mainFilterBackup.set(it) }),
        PrefSetting(
            mainFilterRestore.key,
            { mainFilterRestore.value },
            { mainFilterRestore.set(it) }),
        PrefSetting(
            backupFilterHome.key,
            { backupFilterHome.value },
            { backupFilterHome.set(it) }),
        PrefSetting(
            backupFilterBackup.key,
            { backupFilterBackup.value },
            { backupFilterBackup.set(it) }),
        PrefSetting(
            backupFilterRestore.key,
            { backupFilterRestore.value },
            { backupFilterRestore.set(it) }),
        PrefSetting(
            installedFilterHome.key,
            { installedFilterHome.value },
            { installedFilterHome.set(it) }),
        PrefSetting(
            installedFilterBackup.key,
            { installedFilterBackup.value },
            { installedFilterBackup.set(it) }),
        PrefSetting(
            installedFilterRestore.key,
            { installedFilterRestore.value },
            { installedFilterRestore.set(it) }),
        PrefSetting(
            launchableFilterHome.key,
            { launchableFilterHome.value },
            { launchableFilterHome.set(it) }),
        PrefSetting(
            launchableFilterBackup.key,
            { launchableFilterBackup.value },
            { launchableFilterBackup.set(it) }),
        PrefSetting(
            launchableFilterRestore.key,
            { launchableFilterRestore.value },
            { launchableFilterRestore.set(it) }),
        PrefSetting(
            updatedFilterHome.key,
            { updatedFilterHome.value },
            { updatedFilterHome.set(it) }),
        PrefSetting(
            updatedFilterBackup.key,
            { updatedFilterBackup.value },
            { updatedFilterBackup.set(it) }),
        PrefSetting(
            updatedFilterRestore.key,
            { updatedFilterRestore.value },
            { updatedFilterRestore.set(it) }),
        PrefSetting(
            latestFilterHome.key,
            { latestFilterHome.value },
            { latestFilterHome.set(it) }),
        PrefSetting(
            latestFilterBackup.key,
            { latestFilterBackup.value },
            { latestFilterBackup.set(it) }),
        PrefSetting(
            latestFilterRestore.key,
            { latestFilterRestore.value },
            { latestFilterRestore.set(it) }),
        PrefSetting(
            enabledFilterHome.key,
            { enabledFilterHome.value },
            { enabledFilterHome.set(it) }),
        PrefSetting(
            enabledFilterBackup.key,
            { enabledFilterBackup.value },
            { enabledFilterBackup.set(it) }),
        PrefSetting(
            enabledFilterRestore.key,
            { enabledFilterRestore.value },
            { enabledFilterRestore.set(it) }),
        // User prefs
        PrefSetting(
            languages.key,
            { languages.value },
            { languages.set(it) }),
        PrefSetting(
            neoTheme.key,
            { neoTheme.value },
            { neoTheme.set(it) },
            toJson = { value: NeoTheme? ->
                if (value == null) JsonNull
                else JsonPrimitive(Json.encodeToString(value))
            },
            fromJson = { element ->
                if (element is JsonPrimitive) {
                    Json.decodeFromString<NeoTheme>(element.content)
                } else null
            },
        ),
        PrefSetting(
            deviceLock.key,
            { deviceLock.value },
            { deviceLock.set(it) }),
        PrefSetting(
            biometricLock.key,
            { biometricLock.value },
            { biometricLock.set(it) }),
        PrefSetting(
            multilineInfoChips.key,
            { multilineInfoChips.value },
            { multilineInfoChips.set(it) }),
        PrefSetting(
            singularBackupRestore.key,
            { singularBackupRestore.value },
            { singularBackupRestore.set(it) }),
        PrefSetting(
            newAndUpdatedNotification.key,
            { newAndUpdatedNotification.value },
            { newAndUpdatedNotification.set(it) }),
        PrefSetting(
            squeezeNavText.key,
            { squeezeNavText.value },
            { squeezeNavText.set(it) }),
        PrefSetting(
            altNavBarItem.key,
            { altNavBarItem.value },
            { altNavBarItem.set(it) }),
        PrefSetting(
            altBackupDate.key,
            { altBackupDate.value },
            { altBackupDate.set(it) }),
        PrefSetting(
            altBlockLayout.key,
            { altBlockLayout.value },
            { altBlockLayout.set(it) }),
        PrefSetting(
            oldBackups.key,
            { oldBackups.value },
            { oldBackups.set(it) }),
        // Advanced prefs
        PrefSetting(
            enableSpecialBackups.key,
            { enableSpecialBackups.value },
            { enableSpecialBackups.set(it) }),
        PrefSetting(
            disableVerification.key,
            { disableVerification.value },
            { disableVerification.set(it) }),
        PrefSetting(
            giveAllPermissions.key,
            { giveAllPermissions.value },
            { giveAllPermissions.set(it) }),
        PrefSetting(
            allowDowngrade.key,
            { allowDowngrade.value },
            { allowDowngrade.set(it) }),
        PrefSetting(
            useExactAlarm.key,
            { useExactAlarm.value },
            { useExactAlarm.set(it) }),
        // Service prefs (excluding encrypted/password)
        PrefSetting(
            restorePermissions.key,
            { restorePermissions.value },
            { restorePermissions.set(it) }),
        PrefSetting(
            numBackupRevisions.key,
            { numBackupRevisions.value },
            { numBackupRevisions.set(it) }),
        PrefSetting(
            enableStorageCheck.key,
            { enableStorageCheck.value },
            { enableStorageCheck.set(it) }),
        PrefSetting(
            compressionType.key,
            { compressionType.value },
            { compressionType.set(it) }),
        PrefSetting(
            compressionLevel.key,
            { compressionLevel.value },
            { compressionLevel.set(it) }),
        PrefSetting(
            enableSessionInstaller.key,
            { enableSessionInstaller.value },
            { enableSessionInstaller.set(it) }),
        PrefSetting(
            installationPackage.key,
            { installationPackage.value },
            { installationPackage.set(it) }),
        PrefSetting(
            backupDeviceProtectedData.key,
            { backupDeviceProtectedData.value },
            { backupDeviceProtectedData.set(it) }),
        PrefSetting(
            backupExternalData.key,
            { backupExternalData.value },
            { backupExternalData.set(it) }),
        PrefSetting(
            backupObbData.key,
            { backupObbData.value },
            { backupObbData.set(it) }),
        PrefSetting(
            backupMediaData.key,
            { backupMediaData.value },
            { backupMediaData.set(it) }),
        PrefSetting(
            backupNoBackupData.key,
            { backupNoBackupData.value },
            { backupNoBackupData.set(it) }),
        PrefSetting(
            backupCache.key,
            { backupCache.value },
            { backupCache.set(it) }),
        PrefSetting(
            restoreDeviceProtectedData.key,
            { restoreDeviceProtectedData.value },
            { restoreDeviceProtectedData.set(it) }),
        PrefSetting(
            restoreExternalData.key,
            { restoreExternalData.value },
            { restoreExternalData.set(it) }),
        PrefSetting(
            restoreObbData.key,
            { restoreObbData.value },
            { restoreObbData.set(it) }),
        PrefSetting(
            restoreMediaData.key,
            { restoreMediaData.value },
            { restoreMediaData.set(it) }),
        PrefSetting(
            restoreNoBackupData.key,
            { restoreNoBackupData.value },
            { restoreNoBackupData.set(it) }),
        PrefSetting(
            restoreCache.key,
            { restoreCache.value },
            { restoreCache.set(it) }),
    )

    fun exportToJson(): String {
        val obj = buildJsonObject {
            for (setting in exportableSettings) {
                if (setting.key in unexportableKeys) continue
                val value = setting.getErased()
                if (value is Unit) continue
                put(setting.key, setting.toJsonErased(value))
            }
        }
        return mainSerializer.encodeToString(JsonElement.serializer(), obj)
    }

    suspend fun importFromJson(json: String) {
        val element = Json.decodeFromString(JsonElement.serializer(), json)
        val obj = element as? JsonObject ?: return
        for ((key, jsonElement) in obj) {
            exportableSettings.find { it.key == key }?.let { setting ->
                setting.fromJsonErased(jsonElement)?.let { value ->
                    setting.setErased(value)
                }
            }
        }
    }

    private fun onThemeChanged() {
        context.setCustomTheme()
        //recreateActivities()
    }

    fun observeDependency(key: String) = key.takeIf { it.isNotBlank() }?.let {
        dataStore.data.map { preferences ->
            preferences
        }
    } ?: flowOf(emptyPreferences())

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

        private data class PrefSetting<T>(
            val key: String,
            val get: () -> T?,
            val set: suspend (T) -> Unit,
            val toJson: (T?) -> JsonElement = ::defaultToJson,
            val fromJson: (JsonElement) -> T? = ::defaultFromJson,
        ) {
            fun getErased(): Any? = get()
            suspend fun setErased(value: Any?) = set(value as T)
            fun toJsonErased(value: Any?): JsonElement = toJson(value as T)
            fun fromJsonErased(element: JsonElement): Any? = fromJson(element)
        }
    }
}