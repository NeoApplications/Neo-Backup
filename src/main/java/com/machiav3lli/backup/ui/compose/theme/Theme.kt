package com.machiav3lli.backup.ui.compose.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.machiav3lli.backup.ui.pages.pref_neoTheme
import com.machiav3lli.backup.utils.extensions.Android
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val neoTheme by pref_neoTheme.state

    val staticColorScheme = rememberDynamicColorScheme(
        seedColor = Color(neoTheme.seedColor),
        isDark = darkTheme,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        contrastLevel = neoTheme.contrast,
        isAmoled = neoTheme.blackOnDark,
        style = PaletteStyle.entries[neoTheme.paletteStyle],
    )

    MaterialTheme(
        colorScheme = when {
            neoTheme.dynamicColor && darkTheme && neoTheme.blackOnDark && Android.minSDK(Build.VERSION_CODES.S)
                 -> dynamicBlackColorScheme(context)

            neoTheme.dynamicColor && darkTheme && Android.minSDK(Build.VERSION_CODES.S)
                 -> dynamicDarkColorScheme(context)

            neoTheme.dynamicColor && Android.minSDK(Build.VERSION_CODES.S)
                 -> dynamicLightColorScheme(context)

            else -> staticColorScheme
        },
        content = content
    )
}

@RequiresApi(Build.VERSION_CODES.S)
fun dynamicBlackColorScheme(context: Context) = dynamicDarkColorScheme(context).copy(
    background = Color.Black,
    surfaceContainerLowest = Color.Black,
)
