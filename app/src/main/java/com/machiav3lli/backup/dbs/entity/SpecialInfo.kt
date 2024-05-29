package com.machiav3lli.backup.dbs.entity

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Entity
import com.machiav3lli.backup.R
import com.machiav3lli.backup.handler.ShellCommands
import com.machiav3lli.backup.utils.FileUtils.BackupLocationInAccessibleException
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class is used to describe special backup files that use a hardcoded list of file paths
 */
@Entity
open class SpecialInfo : PackageInfo {

    var specialFiles: Array<String> = arrayOf()

    constructor(
        packageName: String,
        label: String = "",
        versionName: String? = "",
        versionCode: Int = 0,
        specialFiles: Array<String> = arrayOf(),
        icon: Int = -1,
    ) : super(packageName, label, versionName, versionCode, 0, null, arrayOf(), true, icon) {
        this.specialFiles = specialFiles
    }

    constructor(
        packageName: String,
        packageLabel: String,
        versionName: String?,
        versionCode: Int,
        profileId: Int,
        sourceDir: String?,
        splitSourceDirs: Array<String> = arrayOf(),
        isSystem: Boolean,
        icon: Int = -1,
    ) : super(
        packageName,
        packageLabel,
        versionName,
        versionCode,
        profileId,
        sourceDir,
        splitSourceDirs,
        isSystem,
        icon
    )

    override val isSpecial: Boolean
        get() = true

    companion object {
        private val specialInfos: MutableList<SpecialInfo> = mutableListOf()

        /**
         * Returns the list of special (virtual) packages
         *
         * @param context Context object
         * @return a list of of virtual packages
         * @throws BackupLocationInAccessibleException   when the backup location cannot be read for any reason
         * @throws StorageLocationNotConfiguredException when the backup location is not set in the configuration
         */
        private var threadCount: AtomicInteger = AtomicInteger(0)
        private var locked = false

        fun clearCache() {
            synchronized(specialInfos) {
                specialInfos.clear()
            }
        }

        @Throws(
            BackupLocationInAccessibleException::class,
            StorageLocationNotConfiguredException::class
        )
        fun getSpecialInfos(context: Context): List<SpecialInfo> {
            // Careful: It is possible to specify whole directories, but there are two rules:
            // 1. Directories must end with a slash e.g. "/data/system/netstats/"
            // 2. The name of the directory must be unique:
            //      when "/data/system/netstats/" is added, "/my/netstats/" may not be added
            //      As result the backup procedure would put the contents of both directories into
            //      the same directory in the archive and the restore would do the same but in reverse.
            // Documentation note: This could be outdated, make sure the logic in BackupSpecialAction and
            // RestoreSpecialAction hasn't changed!
            if (locked) {
                synchronized(threadCount) {
                    threadCount.incrementAndGet()
                    Timber.d("################################################################### specialInfos locked, threads: ${threadCount.toInt()}")
                }
            }
            synchronized(specialInfos) { // if n calls run in parallel we may have n duplicates
                // because there is some time between asking for the size and the first add
                locked = true
                if (specialInfos.size == 0) {
                    // caching this prevents recreating AppInfo-objects all the time and at wrong times
                    val userId = ShellCommands.currentUser
                    val miscDir = "/data/misc"
                    val systemDir = "/data/system"
                    val userDir = "$systemDir/users/$userId"
                    val systemCeDir = "/data/system_ce/$userId"
                    val systemDeDirGlobal = "/data/system_de"
                    val vendorDeDir = "/data/vendor_de/$userId"
                    val specPrefix = "$ "

                    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                        specialInfos
                            .add(
                                SpecialInfo(
                                    "special.smsmms.json",
                                    specPrefix + context.getString(R.string.spec_smsmmsjson),
                                    Build.VERSION.RELEASE,
                                    Build.VERSION.SDK_INT, arrayOf(
                                        "${context.cacheDir.absolutePath}/special.smsmms.json.json"
                                    ), R.drawable.ic_sms
                                )
                            )
                        specialInfos
                            .add(
                                SpecialInfo(
                                    "special.calllogs.json",
                                    specPrefix + context.getString(R.string.spec_calllogsjson),
                                    Build.VERSION.RELEASE,
                                    Build.VERSION.SDK_INT, arrayOf(
                                        "${context.cacheDir.absolutePath}/special.calllogs.json.json"
                                    ), R.drawable.ic_call_logs
                                )
                            )
                    }
                    specialInfos
                        .add(
                            SpecialInfo(
                                "special.accounts",
                                specPrefix + context.getString(R.string.spec_accounts),
                                Build.VERSION.RELEASE,
                                Build.VERSION.SDK_INT, arrayOf(
                                    "$systemCeDir/accounts_ce.db"
                                ), R.drawable.ic_accounts
                            )
                        )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val brightnessStatsBaseDir = if (Build.VERSION.SDK_INT <
                            Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            systemDeDirGlobal
                        } else {
                            systemDir
                        }
                        specialInfos
                            .add(
                                SpecialInfo(
                                    "special.adaptive.brightness",
                                    specPrefix + context.getString(R.string.spec_adaptiveBrightness),
                                    Build.VERSION.RELEASE,
                                    Build.VERSION.SDK_INT, arrayOf(
                                        // Note: Restoring these files very likely only works if
                                        // the device is abruptly rebooted after restoring them.
                                        // Also for a full backup of the adaptive brightness data,
                                        // the Device Health Service's app data needs to be backed
                                        // up, too.
                                        "$brightnessStatsBaseDir/ambient_brightness_stats.xml",
                                        "$brightnessStatsBaseDir/brightness_events.xml",
                                        "$systemDir/display-manager-state.xml"
                                    ),
                                    R.drawable.ic_adaptive_brightness
                                )
                            )
                    }
                    specialInfos
                        .add(
                            SpecialInfo(
                                "special.bluetooth",
                                specPrefix + context.getString(R.string.spec_bluetooth),
                                Build.VERSION.RELEASE,
                                Build.VERSION.SDK_INT, arrayOf(
                                    "$miscDir/bluedroid/bt_config.conf"
                                ), R.drawable.ic_bluetooth
                            )
                        )
                    specialInfos
                        .add(
                            SpecialInfo(
                                "special.data.usage.policy",
                                specPrefix + context.getString(R.string.spec_data),
                                Build.VERSION.RELEASE,
                                Build.VERSION.SDK_INT, arrayOf(
                                    "$systemDir/netpolicy.xml",
                                    "$systemDir/netstats/"
                                ), R.drawable.ic_privacy
                            )
                        )
                    specialInfos
                        .add(
                            SpecialInfo(
                                "special.fingerprint",
                                specPrefix + context.getString(R.string.spec_fingerprint),
                                Build.VERSION.RELEASE,
                                Build.VERSION.SDK_INT, arrayOf(
                                    "$userDir/settings_fingerprint.xml",
                                    "$vendorDeDir/fpdata/"
                                ), R.drawable.ic_fingerprint
                            )
                        )
                    specialInfos
                        .add(
                            SpecialInfo(
                                "special.wallpaper",
                                specPrefix + context.getString(R.string.spec_wallpaper),
                                Build.VERSION.RELEASE,
                                Build.VERSION.SDK_INT, arrayOf(
                                    "$userDir/wallpaper",
                                    "$userDir/wallpaper_info.xml"
                                ), R.drawable.ic_wallpaper
                            )
                        )
                    // Location of the WifiConfigStore had been moved with Android R
                    val wifiConfigLocation = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        "$miscDir/wifi/WifiConfigStore.xml"
                    } else {
                        "$miscDir/apexdata/com.android.wifi/WifiConfigStore.xml"
                    }
                    specialInfos
                        .add(
                            SpecialInfo(
                                "special.wifi.access.points",
                                specPrefix + context.getString(R.string.spec_wifiAccessPoints),
                                Build.VERSION.RELEASE,
                                Build.VERSION.SDK_INT, arrayOf(
                                    wifiConfigLocation
                                ), R.drawable.ic_wifi
                            )
                        )
                }
                locked = false
            }
            return specialInfos
        }
    }
}
