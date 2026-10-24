package com.machiav3lli.backup.ui.dialogs

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.NeoTheme
import com.machiav3lli.backup.data.preferences.PrefTheme
import com.machiav3lli.backup.ui.compose.component.ColorCircle
import com.machiav3lli.backup.ui.compose.component.DialogNegativeButton
import com.machiav3lli.backup.ui.compose.component.DialogPositiveButton
import com.machiav3lli.backup.ui.compose.component.SwitchRow
import com.machiav3lli.backup.ui.compose.theme.presetColors
import com.machiav3lli.backup.utils.extensions.Android
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle

@Composable
fun ThemePrefDialogUI(
    pref: PrefTheme,
    openDialogCustom: MutableState<Boolean>,
) {
    val current by pref.state
    var nightMode by remember(current) { mutableIntStateOf(current.nightMode) }
    var dynamicColor by remember(current) { mutableStateOf(current.dynamicColor) }
    var blackOnDark by remember(current) { mutableStateOf(current.blackOnDark) }
    var seedColor by remember(current) { mutableLongStateOf(current.seedColor) }
    var contrast by remember(current) { mutableDoubleStateOf(current.contrast) }
    var paletteStyle by remember(current) { mutableIntStateOf(current.paletteStyle) }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.prefs_theme),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(R.string.night_mode),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SegmentedButton(
                    selected = nightMode == AppCompatDelegate.MODE_NIGHT_NO,
                    onClick = { nightMode = AppCompatDelegate.MODE_NIGHT_NO },
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(text = stringResource(R.string.prefs_theme_light))
                }
                SegmentedButton(
                    selected = nightMode == AppCompatDelegate.MODE_NIGHT_YES,
                    onClick = { nightMode = AppCompatDelegate.MODE_NIGHT_YES },
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(text = stringResource(R.string.prefs_theme_dark))
                }
                if (Android.minSDK(Build.VERSION_CODES.Q)) SegmentedButton(
                    selected = nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                    onClick = { nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM },
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(text = stringResource(R.string.system))
                }
            }

            if (Android.minSDK(Build.VERSION_CODES.S)) SwitchRow(
                text = stringResource(R.string.dynamic_colors),
                initSelected = { dynamicColor },
                onCheckedChanged = { dynamicColor = it },
            )

            SwitchRow(
                text = stringResource(R.string.black_theme),
                initSelected = { blackOnDark },
                onCheckedChanged = { blackOnDark = it },
            )

            AnimatedVisibility(visible = !dynamicColor) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.seed_color),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            12.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        items(presetColors.toList()) { (color, nameId) ->
                            ColorCircle(
                                color = Color(color),
                                isSelected = seedColor == color,
                                contentDescription = stringResource(nameId),
                                onClick = { seedColor = color }
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.contrast_level),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        SegmentedButton(
                            selected = contrast == Contrast.Default.value,
                            onClick = { contrast = Contrast.Default.value },
                            border = BorderStroke(0.dp, Color.Transparent),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text(text = stringResource(R.string.contrast_default))
                        }
                        SegmentedButton(
                            selected = contrast == Contrast.Medium.value,
                            onClick = { contrast = Contrast.Medium.value },
                            border = BorderStroke(0.dp, Color.Transparent),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text(text = stringResource(R.string.contrast_medium))
                        }
                        SegmentedButton(
                            selected = contrast == Contrast.High.value,
                            onClick = { contrast = Contrast.High.value },
                            border = BorderStroke(0.dp, Color.Transparent),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text(text = stringResource(R.string.contrast_high))
                        }
                    }

                    Text(
                        text = stringResource(R.string.palette_style),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        SegmentedButton(
                            selected = paletteStyle == PaletteStyle.Neutral.ordinal,
                            onClick = { paletteStyle = PaletteStyle.Neutral.ordinal },
                            border = BorderStroke(0.dp, Color.Transparent),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text(text = stringResource(R.string.palette_neutral))
                        }
                        SegmentedButton(
                            selected = paletteStyle == PaletteStyle.TonalSpot.ordinal,
                            onClick = { paletteStyle = PaletteStyle.TonalSpot.ordinal },
                            border = BorderStroke(0.dp, Color.Transparent),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text(text = stringResource(R.string.palette_tonal))
                        }
                        SegmentedButton(
                            selected = paletteStyle == PaletteStyle.Expressive.ordinal,
                            onClick = { paletteStyle = PaletteStyle.Expressive.ordinal },
                            border = BorderStroke(0.dp, Color.Transparent),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text(text = stringResource(R.string.palette_expressive))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogNegativeButton(
                    onClick = { openDialogCustom.value = false }
                )
                DialogPositiveButton(
                    modifier = Modifier.padding(start = 16.dp),
                    onClick = {
                        val newTheme = NeoTheme.CustomTheme(
                            resId = if (dynamicColor) -1
                            else if (blackOnDark) R.style.Black
                            else R.style.AppTheme,
                            nightMode = nightMode,
                            dynamicColor = dynamicColor,
                            blackOnDark = blackOnDark,
                            seedColor = seedColor,
                            contrast = contrast,
                            paletteStyle = paletteStyle,
                        )
                        pref.value = newTheme
                        openDialogCustom.value = false
                    }
                )
            }
        }
    }
}
