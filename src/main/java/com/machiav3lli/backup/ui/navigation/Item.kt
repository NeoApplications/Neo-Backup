package com.machiav3lli.backup.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArchiveTray
import com.machiav3lli.backup.ui.compose.icons.phosphor.Bug
import com.machiav3lli.backup.ui.compose.icons.phosphor.CalendarX
import com.machiav3lli.backup.ui.compose.icons.phosphor.ClockCounterClockwise
import com.machiav3lli.backup.ui.compose.icons.phosphor.Detective
import com.machiav3lli.backup.ui.compose.icons.phosphor.Flask
import com.machiav3lli.backup.ui.compose.icons.phosphor.House
import com.machiav3lli.backup.ui.compose.icons.phosphor.Infinity
import com.machiav3lli.backup.ui.compose.icons.phosphor.Key
import com.machiav3lli.backup.ui.compose.icons.phosphor.SlidersHorizontal
import com.machiav3lli.backup.ui.compose.icons.phosphor.UserGear
import com.machiav3lli.backup.ui.compose.icons.phosphor.Wrench
import com.machiav3lli.backup.ui.pages.AdvancedPrefsPage
import com.machiav3lli.backup.ui.pages.BatchPage
import com.machiav3lli.backup.ui.pages.HomePage
import com.machiav3lli.backup.ui.pages.SchedulerPage
import com.machiav3lli.backup.ui.pages.ServicePrefsPage
import com.machiav3lli.backup.ui.pages.ToolsPrefsPage
import com.machiav3lli.backup.ui.pages.UserPrefsPage
import com.machiav3lli.backup.utils.extensions.koinNeoViewModel
import com.machiav3lli.backup.viewmodels.BackupBatchVM
import com.machiav3lli.backup.viewmodels.RestoreBatchVM
import kotlinx.serialization.Serializable

sealed class NavItem(
    val title: Int,
    val icon: ImageVector,
    val destination: String,
    val content: @Composable () -> Unit = {}
) {
    data object Home :
        NavItem(
            R.string.home,
            when {
                NeoApp.isNeo -> Phosphor.Infinity
                NeoApp.isDebug -> Phosphor.Bug
                NeoApp.isHg42 -> Phosphor.Detective
                else -> Phosphor.House
            },
            "home",
            { HomePage() }
        )

    data object Backup :
        NavItem(R.string.backup, Phosphor.ArchiveTray, "batch_backup", {
            BatchPage(viewModel = koinNeoViewModel<BackupBatchVM>(), backupBoolean = true)
        })

    data object Restore :
        NavItem(R.string.restore, Phosphor.ClockCounterClockwise, "batch_restore", {
            BatchPage(viewModel = koinNeoViewModel<RestoreBatchVM>(), backupBoolean = false)
        })

    data object Scheduler :
        NavItem(R.string.sched_title, Phosphor.CalendarX, "scheduler", {
            SchedulerPage()
        })

    data object UserPrefs :
        NavItem(R.string.prefs_user_short, Phosphor.UserGear, "prefs_user", {
            UserPrefsPage()
        })

    data object ServicePrefs :
        NavItem(R.string.prefs_service_short, Phosphor.SlidersHorizontal, "prefs_service", {
            ServicePrefsPage()
        })

    data object AdvancedPrefs :
        NavItem(R.string.prefs_advanced_short, Phosphor.Flask, "prefs_advanced", {
            AdvancedPrefsPage()
        })

    data object ToolsPrefs : NavItem(R.string.prefs_tools_short, Phosphor.Wrench, "prefs_tools", {
        ToolsPrefsPage()
    })

    data object Encryption : NavItem(
        R.string.prefs_encryption,
        Phosphor.Key,
        "prefs_service/encryption"
    )

    data object Terminal :
        NavItem(R.string.prefs_tools_terminal, Phosphor.Bug, "prefs_tools/terminal")

    data object Exports : NavItem(
        R.string.prefs_schedulesexportimport,
        Phosphor.CalendarX,
        "prefs_tools/exports"
    )

    data object Logs : NavItem(
        R.string.prefs_logviewer,
        Phosphor.Bug,
        "prefs_tools/logs"
    )
}

@Serializable
sealed class NavRoute : NavKey {
    @Serializable
    data object Lock : NavRoute()

    @Serializable
    data object Onboarding : NavRoute()

    @Serializable
    data object Main : NavRoute()

    @Serializable
    data object Terminal : NavRoute()

    @Serializable
    data object Encryption : NavRoute()

    @Serializable
    data object Exports : NavRoute()

    @Serializable
    data object Logs : NavRoute()

    @Serializable
    data object Info : NavRoute()

    @Serializable
    data class SortFilter(val page: String) : NavRoute()

    @Serializable
    data class BatchPrefs(val backup: Boolean) : NavRoute()

    @Serializable
    data class Prefs(val page: Int = 0) : NavRoute()
}
