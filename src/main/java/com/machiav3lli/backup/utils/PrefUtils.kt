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
package com.machiav3lli.backup.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import androidx.biometric.BiometricManager
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.machiav3lli.backup.ENCRYPTION
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.PREFS_LANGUAGES_SYSTEM
import com.machiav3lli.backup.PREFS_SHARED_PRIVATE
import com.machiav3lli.backup.R
import com.machiav3lli.backup.config.BuildConfig
import com.machiav3lli.backup.data.entity.NeoTheme
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import com.machiav3lli.backup.manager.tasks.RefreshBackupsWorker
import com.machiav3lli.backup.ui.pages.persist_salt
import com.machiav3lli.backup.ui.pages.pref_allowDowngrade
import com.machiav3lli.backup.ui.pages.pref_appAccentColor
import com.machiav3lli.backup.ui.pages.pref_appSecondaryColor
import com.machiav3lli.backup.ui.pages.pref_appTheme
import com.machiav3lli.backup.ui.pages.pref_backupDeviceProtectedData
import com.machiav3lli.backup.ui.pages.pref_backupExternalData
import com.machiav3lli.backup.ui.pages.pref_backupMediaData
import com.machiav3lli.backup.ui.pages.pref_backupObbData
import com.machiav3lli.backup.ui.pages.pref_biometricLock
import com.machiav3lli.backup.ui.pages.pref_compressionLevel
import com.machiav3lli.backup.ui.pages.pref_compressionType
import com.machiav3lli.backup.ui.pages.pref_deviceLock
import com.machiav3lli.backup.ui.pages.pref_disableVerification
import com.machiav3lli.backup.ui.pages.pref_enableSpecialBackups
import com.machiav3lli.backup.ui.pages.pref_encryption_mode
import com.machiav3lli.backup.ui.pages.pref_giveAllPermissions
import com.machiav3lli.backup.ui.pages.pref_languages
import com.machiav3lli.backup.ui.pages.pref_neoTheme
import com.machiav3lli.backup.ui.pages.pref_password
import com.machiav3lli.backup.ui.pages.pref_pathBackupFolder
import com.machiav3lli.backup.ui.pages.pref_pgpPasscode
import com.machiav3lli.backup.ui.pages.pref_restoreDeviceProtectedData
import com.machiav3lli.backup.ui.pages.pref_restoreExternalData
import com.machiav3lli.backup.ui.pages.pref_restoreMediaData
import com.machiav3lli.backup.ui.pages.pref_restoreObbData
import com.machiav3lli.backup.ui.pages.pref_shadowRootFile
import java.nio.charset.StandardCharsets
import java.util.Locale

const val READ_PERMISSION = 2
const val WRITE_PERMISSION = 3
const val SMS_PERMISSION = 4
const val CONTACTS_PERMISSION = 5
const val CALLLOGS_PERMISSION = 6

fun Context.getDefaultSharedPreferences(): SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(this)

fun Context.getPrivateSharedPrefs(): SharedPreferences {
    val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    return EncryptedSharedPreferences.create(
        this,
        PREFS_SHARED_PRIVATE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

fun getCryptoSalt(): ByteArray {
    val userSalt = persist_salt.value
    return if (userSalt.isNotEmpty()) {
        userSalt.toByteArray(StandardCharsets.UTF_8)
    } else FALLBACK_SALT
}

fun isEncryptionEnabled(): Boolean =
    pref_encryption_mode.value != ENCRYPTION.NONE.ordinal && getEncryptionPassword().isNotEmpty()

fun isPasswordEncryptionEnabled(): Boolean =
    pref_encryption_mode.value == ENCRYPTION.PASSWORD.ordinal && getEncryptionPassword().isNotEmpty()

fun isPGPEncryptionEnabled(): Boolean =
    pref_encryption_mode.value == ENCRYPTION.PGP.ordinal && getEncryptionPassword().isNotEmpty()

fun getEncryptionPassword(): String = when (pref_encryption_mode.value) {
    ENCRYPTION.PASSWORD.ordinal -> pref_password.value
    ENCRYPTION.PGP.ordinal      -> pref_pgpPasscode.value
    else                        -> ""
}

fun isCompressionEnabled(): Boolean =
    getCompressionType().isNotEmpty() && getCompressionLevel() > 0

fun getCompressionType(): String = pref_compressionType.value

fun getCompressionLevel() = pref_compressionLevel.value

fun isDeviceLockEnabled(): Boolean = pref_deviceLock.value

fun Context.isDeviceLockAvailable(): Boolean =
    (getSystemService(KeyguardManager::class.java) as KeyguardManager).isDeviceSecure

fun isBiometricLockEnabled(): Boolean = pref_biometricLock.value

fun Context.isBiometricLockAvailable(): Boolean =
    BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS


val backupDirConfigured: String
    get() {
        val location = pref_pathBackupFolder.value
        if (location.isEmpty())
            throw StorageLocationNotConfiguredException()
        return location
    }

fun backupFolderExists(uri: String? = null): Boolean {
    try {
        uri?.let {
            if (StorageFile.fromUri(it).exists())
                return true
        }
        if (NeoApp.backupRoot?.exists() ?: false)
            return true
    } catch (e: Throwable) {
        logException(e)
        return false
    }
    return false
}

fun setBackupDir(uri: Uri): String {
    val fullUri = try {
        DocumentsContract
            .buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
    } catch (e: Throwable) {
        uri
    }
    val fullUriString = fullUri.toString()
    if (pref_pathBackupFolder.value != fullUriString)
        pref_pathBackupFolder.value = fullUriString
    else {
        if (fullUri.scheme == "file" || fullUri.scheme == null)
            if (!pref_shadowRootFile.value) // prevent recursion
                pref_shadowRootFile.value = true
        RefreshBackupsWorker.enqueueFullRefresh()
    }
    return fullUriString
}

val Context.canReadExternalStorage: Boolean
    get() {
        val externalStorage = FileUtils.getExternalStorageDirectory(this)
        return externalStorage?.canRead() ?: false
    }

val Context.canWriteExternalStorage: Boolean
    get() {
        val externalStorage = FileUtils.getExternalStorageDirectory(this)
        return externalStorage?.canWrite() ?: false
    }

val isBackupDeviceProtectedData: Boolean
    get() = pref_backupDeviceProtectedData.value

val isBackupExternalData: Boolean
    get() = pref_backupExternalData.value

val isBackupObbData: Boolean
    get() = pref_backupObbData.value

val isBackupMediaData: Boolean
    get() = pref_backupMediaData.value

val isRestoreDeviceProtectedData: Boolean
    get() = pref_restoreDeviceProtectedData.value

val isRestoreExternalData: Boolean
    get() = pref_restoreExternalData.value

val isRestoreObbData: Boolean
    get() = pref_restoreObbData.value

val isRestoreMediaData: Boolean
    get() = pref_restoreMediaData.value

val isDisableVerification: Boolean
    get() = pref_disableVerification.value

val isRestoreAllPermissions: Boolean
    get() = pref_giveAllPermissions.value

val isAllowDowngrade: Boolean
    get() = pref_allowDowngrade.value

class StorageLocationNotConfiguredException : Exception("Storage Location has not been configured")

var styleTheme: NeoTheme
    get() = pref_neoTheme.value
    set(value) {
        pref_neoTheme.value = value
    }

var stylePrimary: Int
    get() = pref_appAccentColor.value
    set(value) {
        pref_appAccentColor.value = value
    }

var styleSecondary: Int
    get() = pref_appSecondaryColor.value
    set(value) {
        pref_appSecondaryColor.value = value
    }

var language: String
    get() = pref_languages.value
    set(value) {
        pref_languages.value = value
    }

var specialBackupsEnabled: Boolean
    get() = pref_enableSpecialBackups.value
    set(value) {
        pref_enableSpecialBackups.value = value
    }

fun Context.getLocaleOfCode(localeCode: String): Locale = when {
    localeCode.isEmpty()      -> resources.configuration.locales[0]
    localeCode.contains("-r") -> Locale(
        localeCode.substring(0, 2),
        localeCode.substring(4)
    )

    localeCode.contains("_")  -> Locale(
        localeCode.substring(0, 2),
        localeCode.substring(3)
    )

    else                      -> Locale(localeCode)
}

fun Context.getLanguageList() =
    mapOf(PREFS_LANGUAGES_SYSTEM to resources.getString(R.string.prefs_language_system)) +
            BuildConfig.DETECTED_LOCALES
                .sorted()
                .associateWith { translateLocale(getLocaleOfCode(it)) }

private fun translateLocale(locale: Locale): String {
    val country = locale.getDisplayCountry(locale)
    val language = locale.getDisplayLanguage(locale)
    return (language.replaceFirstChar { it.uppercase(Locale.getDefault()) }
            + (if (country.isNotEmpty() && country.compareTo(language, true) != 0)
        "($country)" else ""))
}

