/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2025  Antonios Hazim
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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.StrictMode
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.work.WorkManager
import com.charleskorn.kaml.Yaml
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.machiav3lli.backup.data.dbs.databaseModule
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.data.plugins.Plugin
import com.machiav3lli.backup.data.preferences.NeoPrefs.Companion.prefsModule
import com.machiav3lli.backup.data.preferences.pref_catchUncaughtException
import com.machiav3lli.backup.data.preferences.pref_logToSystemLogcat
import com.machiav3lli.backup.data.preferences.pref_maxLogLines
import com.machiav3lli.backup.data.preferences.pref_uncaughtExceptionsJumpToPreferences
import com.machiav3lli.backup.data.preferences.traceBusy
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.data.preferences.traceSection
import com.machiav3lli.backup.data.preferences.traceSerialize
import com.machiav3lli.backup.data.repository.PackageRepository
import com.machiav3lli.backup.manager.handler.AssetHandler
import com.machiav3lli.backup.manager.handler.ExportsHandler
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.PGPHandler
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.WorkHandler
import com.machiav3lli.backup.manager.services.PackageUnInstalledReceiver
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.activities.viewModelsModule
import com.machiav3lli.backup.ui.pages.pref_busyHitTime
import com.machiav3lli.backup.ui.pages.pref_cancelOnStart
import com.machiav3lli.backup.ui.pages.pref_prettyJson
import com.machiav3lli.backup.ui.pages.pref_useYamlPreferences
import com.machiav3lli.backup.ui.pages.pref_useYamlProperties
import com.machiav3lli.backup.ui.pages.pref_useYamlSchedules
import com.machiav3lli.backup.utils.FileUtils.BackupLocationInAccessibleException
import com.machiav3lli.backup.utils.ISO_DATE_TIME_FORMAT_MS
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.TraceUtils.beginNanoTimer
import com.machiav3lli.backup.utils.TraceUtils.classAndId
import com.machiav3lli.backup.utils.TraceUtils.endNanoTimer
import com.machiav3lli.backup.utils.TraceUtils.methodName
import com.machiav3lli.backup.utils.backupDirConfigured
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.isDynamicTheme
import com.machiav3lli.backup.utils.restartApp
import com.machiav3lli.backup.utils.scheduleAlarmsOnce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

val RESCUE_NAV get() = "rescue"

class NeoApp : Application(), KoinStartup {

    val work: WorkHandler by inject()
    val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    @KoinExperimentalAPI
    override fun onKoinStartup() = koinConfiguration {
        androidLogger()
        androidContext(this@NeoApp)
        modules(
            handlersModule,
            databaseModule,
            prefsModule,
            viewModelsModule,
        )
    }

    override fun onCreate() {
        if (Android.minSDK(Build.VERSION_CODES.S)) {
            /*StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )*/
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectUnsafeIntentLaunch()
                    .build()
            )
        }

        // do this early, context will be used immediately
        refNB = WeakReference(this)

        Timber.w("======================================== app ${classAndId(this)} PID=${Process.myPid()}")

        super.onCreate()

        if (pref_catchUncaughtException.value) {
            Thread.setDefaultUncaughtExceptionHandler { _, e ->
                try {
                    try {
                        //Timber.i("\n\n" + "=".repeat(60))
                        LogsHandler.unexpectedException(e)
                        //LogsHandler.logErrors("uncaught: ${e.message}")
                    } catch (_: Throwable) {
                        // ignore
                    }
                    if (pref_uncaughtExceptionsJumpToPreferences.value) {
                        context.restartApp(RESCUE_NAV)
                    }
                    object : Thread() {
                        override fun run() {
                            Looper.prepare()
                            Looper.loop()
                        }
                    }.start()
                } catch (_: Throwable) {
                    // ignore
                } finally {
                    activity?.finishAffinity()
                    exitProcess(3)
                }
            }
        }

        DynamicColors.applyToActivitiesIfAvailable(
            this,
            DynamicColorsOptions.Builder()
                .setPrecondition { _, _ -> isDynamicTheme }
                .build()
        )

        Plugin.ensureScanned()  // before ShellHandler, because plugins are used there
        initShellHandler()

        val result = registerReceiver(
            PackageUnInstalledReceiver(),
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
        )
        Timber.d("registerReceiver: PackageUnInstalledReceiver = $result")

        if (pref_cancelOnStart.value)
            work.cancel()
        work.prune()

        MainScope().launch {
            addInfoLogText("--> click title to keep infobox open")
            addInfoLogText("--> long press title for dev tools")
            com.machiav3lli.backup.utils.BackupShareUtils.cleanupShareCache(this@NeoApp)
        }
    }

    override fun onTerminate() {
        work.release()
        refNB = WeakReference(null)
        super.onTerminate()
        applicationScope.cancel()
    }

    companion object {

        @ExperimentalSerializationApi
        val serMod = SerializersModule {
            //contextual(Boolean.serializer())
            //contextual(Int.serializer())
            //contextual(String.serializer())
            //polymorphic(Any::class) {
            //    subclass(Boolean.serializer())
            //    subclass(Int.serializer())
            //    subclass(String.serializer())
            //}
            //polymorphic(Any::class) {
            //    subclass(Boolean::class)
            //    subclass(Int::class)
            //    subclass(String::class)
            //}
            //polymorphic(Any::class) {
            //    subclass(Boolean::class, Boolean.serializer())
            //    subclass(Int::class, Int.serializer())
            //    subclass(String::class, String.serializer())
            //}
            //polymorphic(Any::class, Boolean::class, Boolean.serializer())
            //polymorphic(Any::class, Int::class, Int.serializer())
            //polymorphic(Any::class, String::class, String.serializer())
        }

        // create alternatives here and switch when used to allow dynamic prefs
        @OptIn(ExperimentalSerializationApi::class)
        val JsonDefault = Json {
            serializersModule = serMod
        }

        @OptIn(ExperimentalSerializationApi::class)
        val JsonPretty = Json {
            serializersModule = serMod
            prettyPrint = true
        }

        @OptIn(ExperimentalSerializationApi::class)
        val YamlDefault = Yaml(serMod)

        private val propsSerializerDef: Pair<String, StringFormat>
            get() =
                when {
                    pref_useYamlProperties.value -> "yaml" to YamlDefault
                    pref_prettyJson.value        -> "json" to JsonPretty
                    else                         -> "json" to JsonDefault
                }
        val propsSerializer: StringFormat get() = propsSerializerDef.second
        private val propsSerializerSuffix: String get() = propsSerializerDef.first

        private val prefsSerializerDef: Pair<String, StringFormat>
            get() =
                when {
                    pref_useYamlPreferences.value -> "yaml" to YamlDefault
                    else                          -> "json" to JsonPretty
                }
        private val prefsSerializer: StringFormat get() = prefsSerializerDef.second
        private val prefsSerializerSuffix: String get() = prefsSerializerDef.first

        private val schedSerializerDef: Pair<String, StringFormat>
            get() =
                when {
                    pref_useYamlSchedules.value -> "yaml" to YamlDefault
                    else                        -> "json" to JsonPretty
                }
        val schedSerializer: StringFormat get() = schedSerializerDef.second
        private val schedSerializerSuffix: String get() = schedSerializerDef.first

        inline fun <reified T> toSerialized(serializer: StringFormat, value: T) =
            serializer.encodeToString(value)

        inline fun <reified T> fromSerialized(serialized: String): T {
            traceSerialize { "serialized: <-- $serialized" }
            val props: T = try {
                JsonDefault.decodeFromString(serialized)
            } catch (_: Throwable) {
                YamlDefault.decodeFromString(serialized)
            }
            traceSerialize { "    object: --> $props" }
            return props
        }

        val lastLogMessages = ConcurrentLinkedQueue<String>()
        fun addLogMessage(message: String) {
            val maxLogLines = try {
                pref_maxLogLines.value
            } catch (_: Throwable) {
                2000
            }
            lastLogMessages.add(message)
            val size = lastLogMessages.size
            val nDelete = size - maxLogLines
            if (nDelete > 0)
                repeat(nDelete) {
                    lastLogMessages.remove()
                }
        }

        var logsDirectory: StorageFile? = null
            get() {
                if (field == null) {
                    field = context.getExternalFilesDir(null)
                        ?.let { StorageFile(it).ensureDirectory("logs") }
                        ?: context.filesDir.let { StorageFile(it).ensureDirectory("logs") }
                }
                return field
            }

        var lastErrorPackage = ""
        var lastErrorCommands = ConcurrentLinkedQueue<String>()
        fun addErrorCommand(command: String) {
            val maxErrorCommands = 10
            lastErrorCommands.add(command)
            val size = lastErrorCommands.size
            val nDelete = size - maxErrorCommands
            if (nDelete > 0)
                repeat(nDelete) {
                    lastErrorCommands.remove()
                }
        }

        private var logSections = mutableMapOf<String, Int>()
            .withDefault { 0 }     //TODO hg42 use AtomicInteger? but map is synchronized anyways

        init {

            Timber.plant(object : Timber.DebugTree() {

                override fun log(
                    priority: Int, tag: String?, message: String, t: Throwable?,
                ) {
                    val logToSystemLogcat = try {
                        pref_logToSystemLogcat.value
                    } catch (_: Throwable) {
                        true
                    }
                    if (logToSystemLogcat)
                        super.log(priority, "$tag", message, t)

                    val prio =
                        when (priority) {
                            Log.VERBOSE -> "V"
                            Log.ASSERT  -> "A"
                            Log.DEBUG   -> "D"
                            Log.ERROR   -> "E"
                            Log.INFO    -> "I"
                            Log.WARN    -> "W"
                            else        -> "?"
                        }
                    val now = SystemUtils.now
                    val date = ISO_DATE_TIME_FORMAT_MS.format(now)
                    try {
                        addLogMessage("$date $prio $tag : $message")
                    } catch (e: Throwable) {
                        // ignore
                        runCatching {
                            lastLogMessages.clear()
                            addLogMessage("$date E LOG : while adding or limiting log lines")
                            addLogMessage(
                                "$date E LOG : ${
                                    LogsHandler.message(
                                        e,
                                        backTrace = true
                                    )
                                }"
                            )
                        }
                    }
                }

                override fun createStackElementTag(element: StackTraceElement): String {
                    var tag = "${
                        super.createStackElementTag(element)
                    }:${
                        element.lineNumber
                    }::${
                        element.methodName
                    }"
                    if (tag.contains("TraceUtils"))
                        tag = ""
                    return "NeoBackup>$tag"
                }
            })
        }

        var startup = true
        const val startupMsg = "******************** startup" // ensure it's the same for begin/end

        // app should always be created
        var refNB: WeakReference<NeoApp> = WeakReference(null)
        val NB: NeoApp get() = refNB.get()!!

        val context: Context get() = NB.applicationContext

        private var assetsRef: WeakReference<AssetHandler> = WeakReference(null)
        val assets: AssetHandler
            get() {
                if (assetsRef.get() == null)
                    assetsRef = WeakReference(AssetHandler(context))
                return assetsRef.get()!!
            }

        // activity might be null
        private var activityRefs = mutableListOf<WeakReference<Activity>>()
        private var activityRef: WeakReference<Activity> = WeakReference(null)
        val activity: Activity?
            get() {
                return activityRef.get()
            }

        fun addActivity(activity: Activity) {
            activityRef = WeakReference(activity)
            synchronized(activityRefs) {
                traceDebug { "activities.add: ${classAndId(activity)}" }
                // remove activities of the same class
                //activityRef.get()?.localClassName.let { localClassName ->
                //    activityRefs.removeIf { it.get()?.localClassName == localClassName }
                //}
                activityRefs.add(activityRef)
                activityRefs.removeIf { it.get() == null }
                traceDebug { "activities(add): ${activityRefs.map { classAndId(it.get()) }}" }
            }
        }

        fun resumeActivity(activity: Activity) {
            activityRef = WeakReference(activity)
            synchronized(activityRefs) {
                traceDebug { "activities.res: ${classAndId(activity)}" }
                activityRefs.removeIf { it.get() == activity }
                activityRefs.add(activityRef)
                activityRefs.removeIf { it.get() == null }
                traceDebug { "activities(res): ${activityRefs.map { classAndId(it.get()) }}" }
            }

            scheduleAlarmsOnce(context)        // if any activity is started
        }

        fun removeActivity(activity: Activity) {
            synchronized(activityRefs) {
                traceDebug { "activities.remove: ${classAndId(activity)}" }
                //activityRefs.removeIf { it.get()?.localClassName == activity.localClassName }
                activityRefs.removeIf { it.get() == activity }
                activityRef = WeakReference(null)
                activityRefs.removeIf { it.get() == null }
                traceDebug { "activities(remove): ${activityRefs.map { classAndId(it.get()) }}" }
            }
        }

        val activities: List<Activity>
            get() {
                synchronized(activityRefs) {
                    return activityRefs.mapNotNull { it.get() }
                }
            }

        // main might be null
        var mainRef: WeakReference<NeoActivity> = WeakReference(null)
        var main: NeoActivity?
            get() {
                return mainRef.get()
            }
            set(mainActivity) {
                mainRef = WeakReference(mainActivity)
            }
        var mainSaved: WeakReference<NeoActivity> =
            WeakReference(null)    // just to see if activity changed

        var appsSuspendedChecked = false

        var shellHandler: ShellHandler? = null
            private set

        fun initShellHandler(): ShellHandler? {
            return try {
                shellHandler = ShellHandler()
                shellHandler
            } catch (e: Throwable) {
                null
            }
        }

        val isRelease get() = SystemUtils.packageName.endsWith(".backup")
        val isDebug get() = SystemUtils.packageName.contains("debug")
        val isNeo get() = SystemUtils.packageName.contains("neo")
        val isHg42 get() = SystemUtils.packageName.contains("hg42")

        //------------------------------------------------------------------------------------------ backupRoot

        var backupRoot: StorageFile? = null
            get() {
                if (field == null) {
                    val storagePath = backupDirConfigured
                    if (storagePath.isEmpty()) {
                        Timber.e("backup storage location not configured")
                        throw StorageLocationNotConfiguredException()
                    }
                    val storageDir = StorageFile.fromUri(storagePath)
                    if (!storageDir.exists()) { //TODO hg42 for now only existing directories allowed
                        Timber.e("backup storage location not accessible: $storagePath")
                        throw BackupLocationInAccessibleException("Cannot access the root location '$storagePath'")
                    }
                    Timber.e("backup storage location found at ${storageDir.path}")
                    field = storageDir
                }
                return field
            }

        //------------------------------------------------------------------------------------------ infoText

        var infoLogLines = mutableStateListOf<String>()

        const val nInfoLogLines = 100
        var showInfoLog by mutableStateOf(false)

        fun clearInfoLogText() {
            synchronized(infoLogLines) {
                infoLogLines = mutableStateListOf()
            }
        }

        fun addInfoLogText(value: String) {
            synchronized(infoLogLines) {
                infoLogLines.add(value)
                if (infoLogLines.size > nInfoLogLines)
                    infoLogLines.drop(1)
            }
        }

        fun getInfoLogText(n: Int = nInfoLogLines, fill: String? = null): String {
            synchronized(infoLogLines) {
                val lines = infoLogLines.takeLast(n).toMutableList()
                if (fill != null)
                    while (lines.size < n)
                        lines.add(fill)
                return lines.joinToString("\n")
            }
        }

        //------------------------------------------------------------------------------------------ wakelock

        // if any background work is to be done
        private var theWakeLock: PowerManager.WakeLock? = null
        private var wakeLockNested = AtomicInteger(0)
        private const val WAKELOCK_TAG = "NeoBackup:Application"

        // count the nesting levels
        // might be difficult sometimes, because
        // the lock must be transferred from one object/function to another
        // e.g. from the receiver to the service
        fun wakelock(aquire: Boolean) {
            if (aquire) {
                traceDebug { "%%%%% $WAKELOCK_TAG wakelock aquire (before: $wakeLockNested)" }
                if (wakeLockNested.accumulateAndGet(+1, Int::plus) == 1) {
                    val pm: PowerManager = get(PowerManager::class.java)
                    theWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
                    theWakeLock?.acquire(60 * 60 * 1000L)
                    traceDebug { "%%%%% $WAKELOCK_TAG wakelock ACQUIRED" }
                }
            } else {
                traceDebug { "%%%%% $WAKELOCK_TAG wakelock release (before: $wakeLockNested)" }
                if (wakeLockNested.accumulateAndGet(-1, Int::plus) == 0) {
                    traceDebug { "%%%%% $WAKELOCK_TAG wakelock RELEASING" }
                    theWakeLock?.release()
                }
            }
        }

        //------------------------------------------------------------------------------------------ progress

        val progress = mutableStateOf(Pair(false, 0f))

        fun setProgress(now: Int = 0, max: Int = 0) {
            if (max <= 0)
                progress.value = Pair(false, 0f)
            else
                progress.value = Pair(true, 1f * now / max)
        }

        //------------------------------------------------------------------------------------------ section

        fun beginLogSection(section: String) {
            var count: Int
            synchronized(logSections) {
                count = logSections.getValue(section)
                logSections[section] = count + 1
                //if (count == 0 && xxx)  logMessages.clear()           //TODO hg42
            }
            traceSection { """*** ${"|---".repeat(count)}\ $section""" }
            beginNanoTimer("section.$section")
        }

        fun endLogSection(section: String) {    //TODO hg42 timer!
            val time = endNanoTimer("section.$section")
            var count: Int
            synchronized(logSections) {
                count = logSections.getValue(section)
                logSections[section] = count - 1
            }
            traceSection { "*** ${"|---".repeat(count - 1)}/ $section ${"%.3f".format(time / 1E9)} sec" }
            //if (count == 0 && xxx)  ->Log                             //TODO hg42
        }

        //------------------------------------------------------------------------------------------ busy

        var busyCountDownAtomic = AtomicInteger(0)
        var busyLevelAtomic = AtomicInteger(0)
        val busyTick = 250
        var busy = mutableStateOf(false)
        var busyLevel = mutableIntStateOf(0)
        var busyCountDown = mutableIntStateOf(0)

        init {
            CoroutineScope(Dispatchers.IO).launch {
                while (true) {
                    delay(busyTick.toLong())
                    busyCountDownAtomic.getAndUpdate {
                        if (it > 0) {
                            val next = it - 1
                            busyCountDown.intValue = next
                            busyLevel.intValue = busyLevelAtomic.get()
                            if (next == 0)
                                busy.value = false
                            else if (!busy.value)
                                busy.value = true
                            next
                        } else
                            it
                    }
                }
            }

            //TODO hg42 beginBusy(startupMsg)
            hitBusy(120000) // startup
        }

        fun hitBusy(time: Int = pref_busyHitTime.value) {
            busyCountDownAtomic.set(
                time / busyTick
            )
        }

        fun beginBusy(name: String? = null) {
            traceBusy {
                val label = name ?: methodName(1)
                """*** \ busy $label"""
            }
            busyLevelAtomic.incrementAndGet()
            hitBusy(60000)
            beginNanoTimer("busy.$name")
        }

        fun endBusy(name: String? = null): Long {
            val time = endNanoTimer("busy.$name")
            busyLevelAtomic.decrementAndGet()
            if (busyLevelAtomic.get() == 0) {
                busyCountDownAtomic.set(1)
            }
            traceBusy {
                val label = name ?: methodName(1)
                "*** / busy $label ${"%.3f".format(time / 1E9)} sec"
            }
            return time
        }

        //------------------------------------------------------------------------------------------ runningSchedules

        // TODO remove after checking (unique workers shouldn't have duplicates anyway)
        val runningSchedules = mutableMapOf<Long, Boolean>()

        //------------------------------------------------------------------------------------------ backups

        fun putBackups(packageName: String, backups: Set<Backup>) {
            runBlocking(Dispatchers.IO) {
                get<PackageRepository>(PackageRepository::class.java).apply {
                    updatePackageBackups(packageName, backups)
                }
            }
        }

        fun getBackups(packageName: String): Set<Backup> {
            get<PackageRepository>(PackageRepository::class.java).apply {
                return if (startup) emptySet()
                else getBackups(packageName)
            }
        }
    }
}

val handlersModule = module {
    single { WorkManager.getInstance(get()) }
    single { WorkHandler(get(), get()) }
    single { ExportsHandler(get()) }
    single { get<Context>().getSystemService(Context.POWER_SERVICE) as PowerManager }
    singleOf(::PGPHandler)
}
