package com.machiav3lli.backup.data.preferences

import android.content.Context
import com.machiav3lli.backup.ui.pages.pref_deviceLock
import com.machiav3lli.backup.utils.isBiometricLockAvailable
import com.machiav3lli.backup.utils.isDeviceLockAvailable

val PrefsIsEnabled: Map<String, (Context) -> Boolean> = mapOf(
    UserPrefKey.DEVICE_LOCK.name to { it.isDeviceLockAvailable() },
    UserPrefKey.BIOMETRIC_LOCK.name to { it.isBiometricLockAvailable() && pref_deviceLock.value },
)

val PrefsDependency: Map<String, String> = mapOf(
    UserPrefKey.BIOMETRIC_LOCK.name to UserPrefKey.DEVICE_LOCK.name,
)