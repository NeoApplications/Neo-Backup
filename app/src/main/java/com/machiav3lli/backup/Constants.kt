/*
 * OAndBackupX: open-source apps backup and restore app.
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

import java.time.format.DateTimeFormatter

const val TAG_BASE = "OAndBackupX"
const val PREFS_SHARED_PRIVATE = "com.machiav3lli.backup"

const val SCHEDULES_DB_NAME = "schedules.db"
const val BLACKLIST_DB_NAME = "blacklists.db"

const val BLACKLIST_ARGS_ID = "blacklistId"
const val BLACKLIST_ARGS_PACKAGES = "blacklistedPackages"
const val PREFS_SORT_FILTER = "sortFilter"
const val PREFS_FIRST_LAUNCH = "firstLaunch"
const val PREFS_IGNORE_BATTERY_OPTIMIZATION = "ignoreBatteryOptimization"
const val PREFS_SKIPPEDENCRYPTION = "skippedEncryptionCounter"

const val PREFS_THEME = "themes"
const val PREFS_LANGUAGES = "languages"
const val PREFS_LANGUAGES_DEFAULT = "system"
const val PREFS_PATH_BACKUP_DIRECTORY = "pathBackupFolder"
const val PREFS_BIOMETRICLOCK = "biometricLock"
const val PREFS_OLDBACKUPS = "oldBackups"
const val PREFS_REMEMBERFILTERING = "rememberFiltering"
const val PREFS_ENCRYPTION = "encryption"
const val PREFS_PASSWORD = "password"
const val PREFS_PASSWORD_CONFIRMATION = "passwordConfirmation"
const val PREFS_ENABLESPECIALBACKUPS = "enableSpecialBackups"
const val PREFS_SALT = "salt"
const val PREFS_EXCLUDECACHE = "excludeCache"
const val PREFS_EXTERNALDATA = "backupExternalData"
const val PREFS_OBBDATA = "backupObbData"
const val PREFS_DEVICEPROTECTEDDATA = "backupDeviceProtectedData"
const val PREFS_NUM_BACKUP_REVISIONS = "numBackupRevisions"
const val PREFS_HOUSEKEEPING_MOMENT = "housekeepingMoment"
const val PREFS_DISABLEVERIFICATION = "disableVerification"
const val PREFS_RESTOREWITHALLPERMISSIONS = "giveAllPermissions"
const val PREFS_ALLOWDOWNGRADE = "allowDowngrade"
const val PREFS_KILLBEFOREACTION = "killBeforeAction"
const val PREFS_BATCH_DELETE = "batchDelete"
const val PREFS_COPYSELF = "copySelfApk"
const val PREFS_LOGVIEWER = "logViewer"

const val MODE_UNSET = 0
const val MODE_APK = 1
const val MODE_DATA = 2
const val MODE_BOTH = 3

const val MAIN_SORT_LABEL = '0'
const val MAIN_SORT_PACKAGENAME = '1'
const val MAIN_SORT_DATASIZE = '2'

const val MAIN_FILTER_ALL = '0'
const val MAIN_FILTER_SYSTEM = '1'
const val MAIN_FILTER_USER = '2'
const val MAIN_FILTER_SPECIAL = '3'
const val MAIN_FILTER_LAUNCHABLE = '4'

const val MAIN_BACKUPFILTER_ALL = '0'
const val MAIN_BACKUPFILTER_BOTH = '1'
const val MAIN_BACKUPFILTER_APK = '2'
const val MAIN_BACKUPFILTER_DATA = '3'
const val MAIN_BACKUPFILTER_NONE = '4'

const val MAIN_SPECIALFILTER_ALL = '0'
const val MAIN_SPECIALFILTER_NEW_UPDATED = '1'
const val MAIN_SPECIALFILTER_NOTINSTALLED = '2'
const val MAIN_SPECIALFILTER_OLD = '3'
const val MAIN_SPECIALFILTER_SPLIT = '4'
const val MAIN_SPECIALFILTER_INSTALLED = '5'

const val SCHED_FILTER_ALL = 0
const val SCHED_FILTER_USER = 1
const val SCHED_FILTER_SYSTEM = 2
const val SCHED_FILTER_NEW_UPDATED = 3
const val SCHED_FILTER_LAUNCHABLE = 4

const val BUNDLE_USERS = "users"
const val NEED_REFRESH = "needRefresh"

const val HELP_CHANGELOG = "https://github.com/machiav3lli/oandbackupx/blob/master/CHANGELOG.md"
const val HELP_TELEGRAM = "https://t.me/OAndBackupX"
const val HELP_ELEMENT = "https://matrix.to/#/!PiXJUneYCnkWAjekqX:matrix.org?via=matrix.org&via=chat.astafu.de&via=zerc.net"
const val HELP_LICENSE = "https://github.com/machiav3lli/oandbackupx/blob/master/LICENSE.md"
const val HELP_ISSUES = "https://github.com/machiav3lli/oandbackupx/blob/master/ISSUES.md"
const val HELP_FAQ = "https://github.com/machiav3lli/oandbackupx/blob/master/FAQ.md"

val BACKUP_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

fun classAddress(address: String): String {
    return PREFS_SHARED_PRIVATE + address
}

fun classTag(tag: String): String {
    return TAG_BASE + tag
}

fun exodusUrl(app: String): String {
    return "https://reports.exodus-privacy.eu.org/reports/$app/latest"
}

enum class HousekeepingMoment(val value: String) {
    BEFORE("before"), AFTER("after");

    companion object {
        fun fromString(value: String): HousekeepingMoment {
            for (enumValue in values()) {
                if (enumValue.value == value) {
                    return enumValue
                }
            }
            throw IllegalArgumentException("No constant with value '$value'")
        }
    }
}
