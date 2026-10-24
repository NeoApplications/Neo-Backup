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

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.machiav3lli.backup.PREFS_LANGUAGES_SYSTEM
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.ActionResult
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.manager.handler.LogsHandler
import java.util.Locale

fun Context.setCustomTheme() {
    val appTheme = styleTheme
    AppCompatDelegate.setDefaultNightMode(appTheme.nightMode)
    if (!(appTheme.dynamicColor && DynamicColors.isDynamicColorAvailable())) {
        setTheme(R.style.AppTheme)
    } // TODO allow fine control on using custom accent/secondary colors?
    if (appTheme.blackOnDark && isNightMode()) theme.applyStyle(R.style.Black, true)
}

private var sysLocale: LocaleList? = null
private var sysLocaleJVM: Locale? = null

fun Context.setLanguage(lang: String = ""): Configuration {

    // only works on start of activity

    var setLocaleCode = if (lang.isEmpty()) language else lang // parameter for tests
    traceDebug { "Locale.set: ${setLocaleCode}" }

    val config = resources.configuration

    // TODO hg42 for now, cache the initial value, but this doesn't change with system settings
    // TODO hg42 look for another method to retrieve the system setting
    // TODO hg42 maybe asking the unwrapped app or activity context, but how to get this?
    if (sysLocale == null) {
        sysLocale = config.locales
        sysLocaleJVM = Locale.getDefault()
        traceDebug { "Locale.sys: $sysLocale $sysLocaleJVM" }
    }

    var wantSystem = false
    if (setLocaleCode == PREFS_LANGUAGES_SYSTEM) {
        wantSystem = true
        setLocaleCode = sysLocale.toString()
    }

    if (wantSystem) {
        config.setLocales(sysLocale)
        sysLocaleJVM?.let { Locale.setDefault(it) }
    } else {
        val newLocale = getLocaleOfCode(setLocaleCode)
        traceDebug { "Locale.new: ${newLocale}" }
        config.setLocale(newLocale)
        Locale.setDefault(newLocale)
    }
    traceDebug { "Locale.===: ${config.locales} ${Locale.getDefault()}" }

    return config
}

// TODO hg42 at least one of the following Save buttons was reported as not doing anything
fun Activity.showActionResult(result: ActionResult, saveMethod: DialogInterface.OnClickListener) =
    runOnUiThread {
        val builder = MaterialAlertDialogBuilder(this).setPositiveButton(R.string.dialogOK, null)
        if (!result.succeeded) {
            builder.setNegativeButton(R.string.dialogSave, saveMethod) // TODO hg42 either this
            builder
                .setTitle(R.string.errorDialogTitle)
                .setMessage(LogsHandler.handleErrorMessages(this, result.message))
            builder.show()
        }
    }

val Context.isDarkTheme: Boolean
    get() =
        when (styleTheme.nightMode) {
            AppCompatDelegate.MODE_NIGHT_NO  -> false
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else                             -> isNightMode()
        }

fun Context.isNightMode() =
    resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
