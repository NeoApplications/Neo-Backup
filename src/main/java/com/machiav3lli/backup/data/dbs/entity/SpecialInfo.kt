package com.machiav3lli.backup.data.dbs.entity

import android.content.Context
import android.content.pm.PackageManager
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.plugins.SpecialFilesPlugin
import com.machiav3lli.backup.manager.handler.ShellCommands
import com.machiav3lli.backup.utils.FileUtils.BackupLocationInAccessibleException
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

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

        private var threadCount: AtomicInteger = AtomicInteger(0)
        private var locked = false

        fun clearCache() {
            synchronized(specialInfos) {
                specialInfos.clear()
            }
        }

        /**
         * Returns the list of special (virtual) packages
         *
         * @param context Context object
         * @return a list of of virtual packages
         * @throws BackupLocationInAccessibleException   when the backup location cannot be read for any reason
         * @throws StorageLocationNotConfiguredException when the backup location is not set in the configuration
         */
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
                    val specPrefix = "$ "

                    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                        specialInfos
                            .add(
                                SpecialInfo(
                                    "special.smsmms.json",
                                    specPrefix + context.getString(R.string.spec_smsmmsjson),
                                    "",
                                    0,
                                    arrayOf(
                                        "${context.cacheDir.absolutePath}/special.smsmms.json.json"
                                    ),
                                    R.drawable.ic_sms
                                )
                            )
                        specialInfos
                            .add(
                                SpecialInfo(
                                    "special.calllogs.json",
                                    specPrefix + context.getString(R.string.spec_calllogsjson),
                                    "",
                                    0,
                                    arrayOf(
                                        "${context.cacheDir.absolutePath}/special.calllogs.json.json"
                                    ),
                                    R.drawable.ic_call_logs
                                )
                            )
                    }

                    specialInfos += SpecialFilesPlugin.specialInfos(ShellCommands.currentProfile.toString())
                }
                locked = false
            }
            return specialInfos
        }
    }
}
