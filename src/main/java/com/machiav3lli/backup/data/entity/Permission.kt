package com.machiav3lli.backup.data.entity

import androidx.compose.ui.graphics.vector.ImageVector
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.AddressBook
import com.machiav3lli.backup.ui.compose.icons.phosphor.ChatDots
import com.machiav3lli.backup.ui.compose.icons.phosphor.FolderNotch
import com.machiav3lli.backup.ui.compose.icons.phosphor.Leaf
import com.machiav3lli.backup.ui.compose.icons.phosphor.PhoneIncoming
import com.machiav3lli.backup.ui.compose.icons.phosphor.ShieldStar
import com.machiav3lli.backup.ui.compose.icons.phosphor.Warning

data class Permission(
    val nameId: Int,
    val icon: ImageVector,
    val descriptionId: Int,
    val warningTextId: Int = -1,
) {

    companion object {
        val UsageStats = Permission(
            R.string.grant_usage_access_title,
            Phosphor.ShieldStar,
            R.string.intro_permission_usageaccess,
            R.string.intro_permission_usageaccess_warning,
        )
        val BatteryOptimization = Permission(
            R.string.ignore_battery_optimization_title,
            Phosphor.Leaf,
            R.string.intro_permission_batteryoptimization
        )
        val StorageAccess = Permission(
            R.string.storage_access,
            Phosphor.FolderNotch,
            R.string.intro_permission_storage
        )
        val StorageLocation = Permission(
            R.string.prefs_pathbackupfolder,
            Phosphor.FolderNotch,
            R.string.intro_permission_storage_location,
            R.string.intro_permission_storage_location_warning
        )
        val SMSMMS = Permission(
            R.string.smsmms_permission_title,
            Phosphor.ChatDots,
            R.string.intro_permission_smsmms
        )
        val CallLogs = Permission(
            R.string.calllogs_permission_title,
            Phosphor.PhoneIncoming,
            R.string.intro_permission_calllogs
        )
        val Contacts = Permission(
            R.string.contacts_permission_title,
            Phosphor.AddressBook,
            R.string.intro_permission_contacts
        )
        val PostNotifications = Permission(
            R.string.post_notifications_permission_title,
            Phosphor.Warning,
            R.string.post_notifications_permission_message
        )
    }
}