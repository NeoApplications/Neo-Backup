package com.machiav3lli.backup.data.preferences

import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.BooleanPref
import com.machiav3lli.backup.data.entity.IntPref
import com.machiav3lli.backup.utils.TraceUtils

//---------------------------------------- developer settings - logging

val pref_maxLogLines = IntPref(
    key = "dev-log.maxLogLines",
    summary = "maximum lines for internal logging",
    entries = ((10..90 step 10) +
            (100..450 step 50) +
            (500..1500 step 500) +
            (2000..5000 step 1000) +
            (5000..20000 step 5000)
            ).toList(),
    defaultValue = 2000
)

val pref_maxLogCount = IntPref(
    key = "dev-log.maxLogCount",
    summary = "maximum count of log files (= entries on log page)",
    entries = ((1..9 step 1) + (10..100 step 10)).toList(),
    defaultValue = 20
)

val pref_catchUncaughtException = BooleanPref(
    key = "dev-log.catchUncaughtException",
    summaryId = R.string.prefs_catchuncaughtexception_summary,
    defaultValue = false
)

val pref_uncaughtExceptionsJumpToPreferences = BooleanPref(
    key = "dev-log.uncaughtExceptionsJumpToPreferences",
    summary = "in case of unexpected crashes jump to preferences (prevent loops if a preference causes this, and allows to change it, back button leaves the app)",
    defaultValue = false,
    enableIf = { pref_catchUncaughtException.value }
)

val pref_logToSystemLogcat = BooleanPref(
    key = "dev-log.logToSystemLogcat",
    summary = "log to Android logcat, otherwise only internal (internal doesn't help if the app is restarted or if you are catching logs externally, e.g. via Scoop)",
    defaultValue = true
)

val pref_autoLogExceptions = BooleanPref(
    key = "dev-log.autoLogExceptions",
    summary = "create a log for each unexpected exception (may disturb the timing of other operations, meant to detect catched but not expected exceptions, developers are probably intersted in these)",
    defaultValue = false
)

val pref_autoLogSuspicious = BooleanPref(
    key = "dev-log.autoLogSuspicious",
    summary = "create a log for some suspicious but partly expected situations, e.g. detection of duplicate schedules (don't use it regularly)",
    defaultValue = false
)

val pref_autoLogAfterSchedule = BooleanPref(
    key = "dev-log.autoLogAfterSchedule",
    summary = "create a log after each schedule execution",
    defaultValue = false
)

val pref_autoLogUnInstallBroadcast = BooleanPref(
    key = "dev-log.autoLogUnInstallBroadcast",
    summary = "create a log when a package is installed or uninstalled",
    defaultValue = false
)

//---------------------------------------- developer settings - tracing

val pref_trace = BooleanPref(
    key = "dev-trace.trace",
    summary = "global switch for all traceXXX options",
    defaultValue = !NeoApp.isRelease,
)

val traceSection = TraceUtils.TracePrefBold(
    name = "Section",
    summary = "trace important sections (backup, schedule, etc.)",
    default = true
)

val tracePlugin = TraceUtils.TracePref(
    name = "Plugin",
    summary = "trace plugins",
    default = true
)

val traceSchedule = TraceUtils.TracePrefBold(
    name = "Schedule",
    summary = "trace schedules",
    default = true
)

val tracePrefs = TraceUtils.TracePref(
    name = "Prefs",
    summary = "trace preferences",
    default = true
)

val traceFlows = TraceUtils.TracePrefBold(
    name = "Flows",
    summary = "trace Kotlin Flows (reactive data streams)",
    default = true
)

val traceBusy = TraceUtils.TracePrefBold(
    name = "Busy",
    default = true,
    summary = "trace beginBusy/endBusy (busy indicator)"
)

val traceTiming = TraceUtils.TracePrefBold(
    name = "Timing",
    default = true,
    summary = "show code segment timers"
)

val traceContextMenu = TraceUtils.TracePref(
    name = "ContextMenu",
    summary = "trace context menu actions and events",
    default = true
)

val traceCompose = TraceUtils.TracePref(
    name = "Compose",
    summary = "trace recomposition of UI elements",
    default = true
)

val traceDebug = TraceUtils.TracePref(
    name = "Debug",
    summary = "trace for debugging purposes (for devs)",
    default = false
)

val traceWIP = TraceUtils.TracePrefExtreme(
    name = "WIP",
    summary = "trace for debugging purposes (for devs)",
    default = false
)

val traceAccess = TraceUtils.TracePref(
    name = "Access",
    summary = "trace access",
    default = false
)

val traceBackups = TraceUtils.TracePref(
    name = "Backups",
    summary = "trace backups",
    default = true
)

val traceBackupsScan = TraceUtils.TracePref(
    name = "BackupsScan",
    summary = "trace scanning of backup directory for properties files (for scanning with package name)",
    default = false
)

val traceBackupsScanAll = TraceUtils.TracePref(
    name = "BackupsScanAll",
    summary = "trace scanning of backup directory for properties files (for complete scan)",
    default = false
)

val traceSerialize = TraceUtils.TracePref(
    name = "Serialize",
    summary = "trace json/yaml/... conversions",
    default = false
)