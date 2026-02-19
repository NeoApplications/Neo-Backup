package com.machiav3lli.backup.ui.pages

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.BooleanPref
import com.machiav3lli.backup.data.entity.IntPref
import com.machiav3lli.backup.data.entity.LaunchPref
import com.machiav3lli.backup.data.entity.Pref
import com.machiav3lli.backup.data.entity.PrefUI
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.entity.StringPref
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.findSuCommand
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.isLikeRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.suCommand
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.validateSuCommand
import com.machiav3lli.backup.ui.compose.component.BasePreference
import com.machiav3lli.backup.ui.compose.component.PrefsExpandableGroupHeader
import com.machiav3lli.backup.ui.compose.component.PrefsGroup
import com.machiav3lli.backup.ui.compose.component.PrefsGroupCollapsed
import com.machiav3lli.backup.ui.compose.component.TextInput
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.AndroidLogo
import com.machiav3lli.backup.ui.compose.icons.phosphor.AsteriskSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.Clock
import com.machiav3lli.backup.ui.compose.icons.phosphor.ClockCounterClockwise
import com.machiav3lli.backup.ui.compose.icons.phosphor.Hash
import com.machiav3lli.backup.ui.compose.icons.phosphor.ShieldStar
import com.machiav3lli.backup.ui.compose.icons.phosphor.Warning
import com.machiav3lli.backup.ui.compose.mix
import com.machiav3lli.backup.utils.SystemUtils.numCores
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.scheduleAlarmsOnce
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun DevPrefGroups() {
    val devUserOptions = Pref.prefGroups["dev-adv"]?.toPersistentList() ?: persistentListOf()
    val devFileOptions = Pref.prefGroups["dev-file"]?.toPersistentList() ?: persistentListOf()
    val devLogOptions = Pref.prefGroups["dev-log"]?.toPersistentList() ?: persistentListOf()
    val devTraceOptions = Pref.prefGroups["dev-trace"]?.toPersistentList() ?: persistentListOf()
    val devHackOptions = Pref.prefGroups["dev-hack"]?.toPersistentList() ?: persistentListOf()
    val devAltOptions = Pref.prefGroups["dev-alt"]?.toPersistentList() ?: persistentListOf()
    val devNewOptions = Pref.prefGroups["dev-new"]?.toPersistentList() ?: persistentListOf()
    val devFakeOptions = Pref.prefGroups["dev-fake"]?.toPersistentList() ?: persistentListOf()

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PrefsGroupCollapsed(prefs = devUserOptions, heading = "advanced users (those who know)")
        PrefsGroupCollapsed(
            prefs = devAltOptions,
            heading = "alternatives (to compare two variants)"
        )
        PrefsGroupCollapsed(prefs = devLogOptions, heading = "logging")
        PrefsGroupCollapsed(prefs = devTraceOptions, heading = "tracing")
        PrefsGroupCollapsed(prefs = devFileOptions, heading = "file handling")
        PrefsGroupCollapsed(prefs = devHackOptions, heading = "workarounds (hacks)")
        PrefsGroupCollapsed(prefs = devFakeOptions, heading = "faking (for testing)")
        PrefsGroupCollapsed(prefs = devNewOptions, heading = "new experimental (for devs)")
    }
}

@Composable
fun AdvancedPrefsPage() {
    val context = LocalContext.current
    val (expanded, expand) = remember { mutableStateOf(false) }

    val prefs = Pref.prefGroups["adv"]?.toPersistentList() ?: persistentListOf()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PrefsGroup(prefs = prefs) { pref ->
                // TODO do things
            }
        }
        item {
            PrefsExpandableGroupHeader(
                titleId = R.string.prefs_dev_settings,
                summaryId = R.string.prefs_dev_settings_summary,
                icon = Phosphor.Warning
            ) {
                expand(!expanded)
            }
        }
        item {
            //Box {  // hg42: use Box as workaround for weird animation behavior  //TODO hg42 seems to be fixed now? //TODO wech
            AnimatedVisibility(
                visible = expanded,
                //enter = EnterTransition.None,
                //exit = ExitTransition.None
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DevPrefGroups()
            }
            //}
        }
    }
}


val debug = if (NeoApp.isDebug)
    "dev"
else
    "debug"

val hg42 = if (NeoApp.isHg42)
    "dev"
else
    "hg42"


// TODO move some of the dev prefs to advanced/service
//---------------------------------------- developer settings - advanced users

@Composable
fun SuCommandPreference(
    modifier: Modifier = Modifier,
    pref: StringPref,
    dirty: Boolean = pref.dirty.value,
    index: Int = 0,
    groupSize: Int = 1,
) {
    //traceCompose { "SuCommandPreference: $pref" }
    BasePreference(
        modifier = modifier,
        pref = pref,
        dirty = dirty,
        titleId = pref.titleId,
        summaryId = pref.summaryId,
        summary = pref.summary,
        index = index,
        groupSize = groupSize,
        bottomWidget = {
            LaunchedEffect(true) {
                pref.onChanged?.invoke(pref)
            }
            if (pref.value != suCommand) {
                Text(
                    "=> $suCommand",
                    color = Color.Cyan.mix(MaterialTheme.colorScheme.onSurface),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            TextInput(
                pref.value,
                modifier = Modifier.fillMaxWidth(),
            ) {
                pref.value = it
            }
        },
    )
}

class SuCommandPref(
    key: String,
    private: Boolean = true,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    enableIf: (() -> Boolean)? = null,
    onChanged: ((Pref) -> Unit)? = null,
) : StringPref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        SuCommandPreference(pref = pref as SuCommandPref, index = index, groupSize = groupSize)
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
)

val suCommand_summary
    get() = """
        the command used to elevate the shell to a 'root' shell (in our sense),
        the whole command must be a shell, reading commands from stdin and executing them,
        there are also builtin fallback commands
        """.trimIndent().replace("\n", " ").trim()

val suCommand_default = "su -c 'nsenter --mount=/proc/1/ns/mnt sh'"

val pref_suCommand = SuCommandPref(
    key = "dev-adv.suCommand",
    //TODO hg42 pref description is not shown currently for StringPrefs, because a hack uses it to show the value
    summary = suCommand_summary,
    icon = Phosphor.Hash,
    iconTint = {
        val pref = it as SuCommandPref
        if (isLikeRoot == true) {
            if (pref.value == suCommand)
                Color.Green
            else
                Color.Green.copy(alpha = 0.5f)      //TODO hg42 because here is not @Ccomposable
        } else {
            Color.Red
        }
    },
    defaultValue = "",
) {
    val pref = it as SuCommandPref
    var test = pref.value
    if (test == "")
        test = suCommand_default
    if (test != suCommand) {
        if (!validateSuCommand(test)) {
            findSuCommand()
            traceDebug { "findSuCommand: suCommand = $suCommand" }
            if (pref.value == "")
                pref.value = suCommand
        }
    }
    pref.summary = suCommand_summary
    traceDebug { "summary: ${pref.summary}" }
    traceDebug { "pref: ${pref.dirty} ${pref.key} -> ${pref.icon?.name} ${pref.iconTint} (launch)" }
    pref.dirty.value = true
}

val pref_libsuUseRootShell = BooleanPref(
    key = "dev-adv.libsuUseRootShell",
    summary = """
        start libsu shell as 'su' instead of 'sh' before suCommand elevates it
        (as a paranoid fallback, in case 'sh' might not allow elevating for an unknown reason)
        [needs restart]
        """.trimIndent().replace("\n", " ").trim(),
    defaultValue = true
)

val pref_libsuTimeout = IntPref(
    key = "dev-adv.libsuTimeout",
    summary = "[seconds] timeout for libsu commands (does not affect the tar commands)",
    entries = ((10..90 step 10) + (100..300 step 50)).toList(),
    defaultValue = 60
)

val pref_maxJobs = IntPref(
    key = "dev-adv.maxJobs",
    summary = "maximum number of jobs run concurrently (0 = default = numCores)[needs restart]",
    entries = (0..1 * numCores).toList(),
    defaultValue = 0
)

val pref_menuButtonAlwaysVisible = BooleanPref(
    key = "dev-adv.menuButtonAlwaysVisible",
    summary = "also show context menu button when selection is empty",
    defaultValue = false
)

val pref_busyFadeTime = IntPref(
    key = "dev-adv.busyFadeTime",
    summary = "time to fade busy color (ms)",
    entries = (0..5000 step 250).toList(),
    defaultValue = 2000
)

val pref_cancelOnStart = BooleanPref(
    key = "dev-adv.cancelOnStart",
    summaryId = R.string.prefs_cancelonstart_summary,
    defaultValue = false
)

val pref_showInfoLogBar = BooleanPref(
    key = "dev-adv.showInfoLogBar",
    summaryId = R.string.prefs_showinfologbar_summary,
    defaultValue = false
)

val pref_useAlarmClock = BooleanPref(
    key = "dev-adv.useAlarmClock",
    summaryId = R.string.prefs_usealarmclock_summary,
    defaultValue = false
)

val pref_backupPauseApps = BooleanPref(
    key = "dev-adv.backupPauseApps",
    summary = """
        pause apps during backups to avoid inconsistencies caused
        by ongoing file changes or other conflicts (doesn't seem to have big benefits)
        """.trimIndent().replace("\n", " ").trim(),
    defaultValue = false
)

val pref_backupSuspendApps = BooleanPref(
    key = "dev-adv.backupSuspendApps",
    summary = "additionally use pm suspend command to pause apps (unfortunately not very useful, some disadvantages)",
    defaultValue = false,
    enableIf = { pref_backupPauseApps.value }
)

val pref_restoreKillApps = BooleanPref(
    key = "dev-adv.restoreKillApps",
    summary = "kill apps before restores",
    defaultValue = true
)

val pref_strictHardLinks = BooleanPref(
    key = "dev-adv.strictHardLinks",
    summaryId = R.string.prefs_stricthardlinks_summary,
    defaultValue = false
)

val pref_shareAsFile = BooleanPref(
    key = "dev-adv.shareAsFile",
    summary = "share logs as file, otherwise as text",
    defaultValue = true
)

val pref_maxRetriesPerPackage = IntPref(
    key = "dev-adv.maxRetriesPerPackage",
    summaryId = R.string.prefs_maxretriesperpackage_summary,
    entries = (0..10).toList(),
    defaultValue = 1
)

val pref_backupTarCmd = BooleanPref(
    key = "dev-adv.backupTarCmd",
    summaryId = R.string.prefs_backuptarcmd_summary,
    defaultValue = true
)

val pref_restoreTarCmd = BooleanPref(
    key = "dev-adv.restoreTarCmd",
    summaryId = R.string.prefs_restoretarcmd_summary,
    defaultValue = true
)


//---------------------------------------- developer settings - file handling

val pref_shadowRootFile = BooleanPref(
    key = "dev-file.shadowRootFile",
    summaryId = R.string.prefs_shadowrootfile_summary,
    defaultValue = false,
) {
    StorageFile.invalidateCache()
    pref_pathBackupFolder.value = pref_pathBackupFolder.value
}

val pref_cacheUris = BooleanPref(
    key = "dev-file.cacheUris",
    summaryId = R.string.prefs_cacheuris_summary,
    defaultValue = true
)

val pref_cacheFileLists = BooleanPref(
    key = "dev-file.cacheFileLists",
    summaryId = R.string.prefs_cachefilelists_summary,
    defaultValue = true
)


//---------------------------------------- developer settings - implementation alternatives

val pref_paranoidBackupLists = BooleanPref(
    key = "dev-alt.paranoidBackupLists",
    summary = "verify file system after adding or deleting backups (slower, especially remote)",
    defaultValue = false
)

val pref_paranoidHousekeeping = BooleanPref(
    key = "dev-alt.paranoidHousekeeping",
    summary = "verify file system before housekeeping (slower, especially remote)",
    defaultValue = false
)

val pref_ignoreLockedInHousekeeping = BooleanPref(
    key = "dev-alt.ignoreLockedInHousekeeping",
    summary = "keep the configured number of unlocked backups, instead of also counting locked backups",
    defaultValue = false
)

val pref_restartAppOnLanguageChange = BooleanPref(
    key = "dev-alt.restartAppOnLanguageChange",
    summary = "create human readable yaml format for preferences",
    defaultValue = false
)

val pref_useYamlPreferences = BooleanPref(
    key = "$debug-alt.useYamlPreferences",
    summary = "create human readable yaml format for preferences",
    defaultValue = false
)

val pref_useYamlSchedules = BooleanPref(
    key = "$debug-alt.useYamlSchedules",
    summary = "create human readable yaml format for schedules",
    defaultValue = false
)

val pref_useYamlProperties = BooleanPref(
    key = "$debug-alt.useYamlProperties",
    summary = "create human readable yaml format for backup properties",
    defaultValue = false
)

val pref_prettyJson = BooleanPref(  //TODO hg42 to be removed
    key = "dev-alt.prettyJson",
    summary = "create human readable json files. Note they should be compatible in both directions",
    defaultValue = true
)

val pref_busyTurnTime = IntPref(
    key = "dev-alt.busyTurnTime",
    summary = "time the animated busy bars need for one rotation (ms)",
    entries = (500..50000 step 500).toList(),
    defaultValue = 50000
)

val pref_versionOpacity = IntPref(
    key = "dev-alt.versionOpacity",
    summary = "opacity of version [percent]",
    entries = ((0..9 step 1) + (10..100 step 5)).toList(),
    defaultValue = if (NeoApp.isRelease) 1 else 75
    // invisible but can be seen with image processing
)

val pref_busyHitTime = IntPref(
    key = "dev-alt.busyHitTime",
    summary = "time being busy after hitting the watchdog (ms)",
    entries = (NeoApp.busyTick..4000 step NeoApp.busyTick).toList(),
    defaultValue = 2000
)

val pref_createInvalidBackups = BooleanPref(
    key = "dev-alt.createInvalidBackups",
    summary = "create dummy 'INVALID' backups when detecting errors (for now, do not try to delete them!!!)",
    defaultValue = false
)

val pref_lookForEmptyBackups = BooleanPref(
    key = "dev-alt.lookForEmptyBackups",
    summary = "scan for empty backups (slower refresh, especially remote)",
    defaultValue = false
)

val pref_earlyEmptyBackups = BooleanPref(   //TODO hg42 to be removed
    key = "dev-alt.earlyEmptyBackups",
    summary = "empty backup lists for installed packages early, to prevent single scanning",
    defaultValue = true
)

val pref_flatStructure = BooleanPref(
    key = "dev-alt.flatStructure",
    summary = "use a flat directory structure",
    defaultValue = false
)

val pref_propertiesInDir = BooleanPref(
    key = "$debug-alt.propertiesInDir",         //TODO hg42 currently not working in scanner (hmm, I think it works now)
    summary = "store the properties inside the backup directory",
    defaultValue = false
)

val pref_restoreAvoidTemporaryCopy = BooleanPref(
    key = "dev-alt.restoreAvoidTemporaryCopy",
    summaryId = R.string.prefs_restoreavoidtempcopy_summary,
    defaultValue = false
)

val pref_useWorkManagerForSingleManualJob = BooleanPref(
    key = "dev-alt.useWorkManagerForSingleManualJob",
    summary = "also queue single manual jobs from app sheet (note they are added at the end of the queue for now)",
    defaultValue = false
)

val pref_useForegroundInService = BooleanPref(
    key = "dev-alt.useForegroundInService",
    summary = "use foreground notification in service",
    defaultValue = true
)

val pref_useForegroundInJob = BooleanPref(
    key = "dev-alt.useForegroundInJob",
    summary = "sue foreground notification in each job (per package)",
    defaultValue = false
)

val pref_useExpedited = BooleanPref(
    key = "dev-alt.useExpedited",
    summaryId = R.string.prefs_useexpedited_summary,
    defaultValue = true
)


//---------------------------------------- developer settings - workarounds

val pref_fixNavBarOverlap = IntPref(
    key = "dev-hack.fixNavBarOverlap",
    summary = "fix UI overlapping system navbars [in 'dp', usually needs something like 42]",
    entries = (0..64).toList(),
    defaultValue = if (Android.minSDK(Build.VERSION_CODES.R)) 0 else 42
)

val pref_delayBeforeRefreshAppInfo = IntPref(
    key = "dev-hack.delayBeforeRefreshAppInfo",
    summaryId = R.string.prefs_delaybeforerefreshappinfo_summary,
    entries = (0..30).toList(),
    defaultValue = 0
)

val pref_refreshAppInfoTimeout = IntPref(
    key = "dev-hack.refreshAppInfoTimeout",
    summaryId = R.string.prefs_refreshappinfotimeout_summary,
    entries = ((0..9 step 1) + (10..120 step 10)).toList(),
    defaultValue = 30
)


//---------------------------------------- developer settings - faking

val pref_killThisApp = LaunchPref(
    key = "dev-fake.killThisApp",
    summary = """
        terminate app, service and process, but leave the schedules(=alarms) intact
        (in contrast to force-close, where alarms are removed from the system)
        """.trimIndent().replace("\n", " ").trim(),
) {
    NeoApp.activity?.let { ActivityCompat.finishAffinity(it) }
    System.exit(0)
}

val pref_fakeScheduleDups = IntPref(
    key = "dev-fake.fakeScheduleDups",
    summary = "count of additional equal schedules to run at once, 0 = do not fake [for testing only]",
    entries = (0..9).toList(),
    defaultValue = 0
)

val pref_fakeBackupSeconds = IntPref(
    key = "dev-fake.fakeBackupSeconds",
    summary = "[seconds] time for faked backups, 0 = do not fake [for testing only]",
    entries = ((0..9 step 1) + (10..55 step 5) + (60..1200 step 60)).toList(),
    defaultValue = 0
)

val pref_fakeScheduleMin = IntPref(
    key = "dev-fake.fakeScheduleMin",
    summary = "[minutes] =1: day->hour, hour->minute, minutes->seconds  >1: run enabled schedules every x min [for testing only]",
    entries = (listOf(0, 1) + (3..9 step 1) + (10..60 step 5)).toList(),
    defaultValue = 0
)

val pref_forceCrash = LaunchPref(
    key = "dev-fake.forceCrash",
    summary = "crash the app [for testing only]"
) {
    throw Exception("forceCrash")
}


//---------------------------------------- advanced preferences

val pref_enableSpecialBackups = BooleanPref(
    key = "adv.enableSpecialBackups",
    titleId = R.string.prefs_enablespecial,
    summaryId = R.string.prefs_enablespecial_summary,
    icon = Phosphor.AsteriskSimple,
    defaultValue = false,
    onChanged = { NeoApp.main?.enableSpecials((it as BooleanPref).value) }
)

val pref_disableVerification = BooleanPref(
    key = "adv.disableVerification",
    titleId = R.string.prefs_disableverification,
    summaryId = R.string.prefs_disableverification_summary,
    icon = Phosphor.AndroidLogo,
    defaultValue = true
)

val pref_giveAllPermissions = BooleanPref(
    key = "adv.giveAllPermissions",
    titleId = R.string.prefs_restoreallpermissions,
    summaryId = R.string.prefs_restoreallpermissions_summary,
    icon = Phosphor.ShieldStar,
    defaultValue = false
)

val pref_allowDowngrade = BooleanPref(
    key = "adv.allowDowngrade",
    titleId = R.string.prefs_allowdowngrade,
    summaryId = R.string.prefs_allowdowngrade_summary,
    icon = Phosphor.ClockCounterClockwise,
    defaultValue = false
)

val pref_useExactAlarm = BooleanPref(
    key = "adv.useExactAlarm",
    titleId = R.string.prefs_useexactalarm_title,
    summaryId = R.string.prefs_useexactalarm_summary,
    icon = Phosphor.Clock,
    defaultValue = true,
    onChanged = { scheduleAlarmsOnce(NeoApp.context) }
)

//---------------------------------------- values that should persist for internal purposes (no UI)

val persist_firstLaunch = BooleanPref(
    key = "persist.firstLaunch",
    defaultValue = false
)

val persist_beenWelcomed = BooleanPref(
    key = "persist.beenWelcomed",
    defaultValue = false
)

val persist_ignoreBatteryOptimization = BooleanPref(
    key = "persist.ignoreBatteryOptimization",
    defaultValue = false
)

val persist_salt = StringPref(
    key = "persist.salt",
    defaultValue = ""
)

val persist_skippedEncryptionCounter = IntPref(
    key = "persist.skippedEncryptionCounter",
    entries = (0..100).toList(),
    defaultValue = 0
)

//----------------------------------------

fun publicPreferences(persist: Boolean = false) =
    Pref.prefGroups.flatMap {
        val (group, prefs) = it
        prefs.mapNotNull { pref ->
            if (pref.private ||
                pref is LaunchPref ||
                pref.group == "kill" ||
                (persist && pref.group == "persist")
            )
                null
            else
                pref
        }
    }
