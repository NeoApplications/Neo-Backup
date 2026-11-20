package com.machiav3lli.backup.data.preferences

import android.content.Context
import com.machiav3lli.backup.ENCRYPTION
import com.machiav3lli.backup.ui.pages.pref_deviceLock
import com.machiav3lli.backup.ui.pages.pref_encryption_mode
import com.machiav3lli.backup.utils.isBiometricLockAvailable
import com.machiav3lli.backup.utils.isDeviceLockAvailable

val PrefsIsEnabled: Map<String, (Context) -> Boolean> = mapOf(
    UserPrefKey.DEVICE_LOCK.name to { it.isDeviceLockAvailable() },
    UserPrefKey.BIOMETRIC_LOCK.name to { it.isBiometricLockAvailable() && pref_deviceLock.value },
    ServicePrefKey.ENCRYPTION_PASSWORD.name to { pref_encryption_mode.value == ENCRYPTION.PASSWORD.ordinal },
    ServicePrefKey.PGP_KEY.name to { pref_encryption_mode.value == ENCRYPTION.PGP.ordinal },
    ServicePrefKey.PGP_PASSCODE.name to { pref_encryption_mode.value == ENCRYPTION.PGP.ordinal },
)

val PrefsDependency: Map<String, String> = mapOf(
    UserPrefKey.BIOMETRIC_LOCK.name to UserPrefKey.DEVICE_LOCK.name,
    ServicePrefKey.ENCRYPTION_PASSWORD.name to ServicePrefKey.ENCRYPTION_MODE.name,
    ServicePrefKey.PGP_KEY.name to ServicePrefKey.ENCRYPTION_MODE.name,
    ServicePrefKey.PGP_PASSCODE.name to ServicePrefKey.ENCRYPTION_MODE.name,
)