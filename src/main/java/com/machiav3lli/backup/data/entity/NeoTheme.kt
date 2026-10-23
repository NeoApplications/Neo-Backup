package com.machiav3lli.backup.data.entity

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.theme.presetColors
import com.machiav3lli.backup.utils.isDarkTheme
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed class NeoTheme {
    abstract val resId: Int
    abstract val nightMode: Int
    abstract val dynamicColor: Boolean
    abstract val seedColor: Long
    abstract val paletteStyle: Int
    abstract val blackOnDark: Boolean
    abstract val contrast: Double

    fun themeSummary(context: Context): String {
        val nightModeStr = when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_NO
                 -> context.getString(R.string.prefs_theme_light)

            AppCompatDelegate.MODE_NIGHT_YES
                 -> context.getString(R.string.prefs_theme_dark)

            else -> context.getString(R.string.system)
        }

        if (dynamicColor) return "${context.getString(R.string.dynamic_theme)} $nightModeStr"

        val colorStr = context.getString(
            presetColors[seedColor]
                ?: R.string.color_green
        )

        val base = if (blackOnDark && context.isDarkTheme) {
            "${context.getString(R.string.prefs_theme_black)} $colorStr"
        } else {
            "$nightModeStr $colorStr"
        }

        return if (contrast != Contrast.Default.value) {
            val contrastStr = when (contrast) {
                Contrast.Medium.value -> context.getString(R.string.contrast_medium)
                Contrast.High.value   -> context.getString(R.string.contrast_high)
                else                  -> context.getString(R.string.contrast_default)
            }
            "$base ($contrastStr)"
        } else {
            base
        }
    }

    @Serializable
    data class CustomTheme(
        override val resId: Int,
        override val nightMode: Int,
        override val dynamicColor: Boolean,
        override val seedColor: Long,
        override val paletteStyle: Int,
        override val blackOnDark: Boolean,
        override val contrast: Double,
    ) : NeoTheme()

    @Serializable
    data object DynamicSystem : NeoTheme() {
        override val resId: Int
            get() = -1

        override val nightMode: Int
            get() = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        override val dynamicColor: Boolean
            get() = true

        override val seedColor: Long
            get() = 0xFF336644

        override val paletteStyle: Int
            get() = PaletteStyle.TonalSpot.ordinal

        override val blackOnDark: Boolean
            get() = false

        override val contrast: Double
            get() = Contrast.Default.value
    }

    @Serializable
    data object SystemBlack : NeoTheme() {
        override val resId: Int
            get() = R.style.AppTheme

        override val nightMode: Int
            get() = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        override val dynamicColor: Boolean
            get() = false

        override val seedColor: Long
            get() = 0xFF336644

        override val paletteStyle: Int
            get() = PaletteStyle.TonalSpot.ordinal

        override val blackOnDark: Boolean
            get() = true

        override val contrast: Double
            get() = Contrast.Default.value
    }

    @Serializable
    data object Light : NeoTheme() {
        override val resId: Int
            get() = R.style.AppTheme

        override val nightMode: Int
            get() = AppCompatDelegate.MODE_NIGHT_NO

        override val dynamicColor: Boolean
            get() = false

        override val seedColor: Long
            get() = 0xFF336644

        override val paletteStyle: Int
            get() = PaletteStyle.TonalSpot.ordinal

        override val blackOnDark: Boolean
            get() = false

        override val contrast: Double
            get() = Contrast.Default.value
    }
}
