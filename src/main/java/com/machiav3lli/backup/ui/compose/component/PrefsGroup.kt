package com.machiav3lli.backup.ui.compose.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machiav3lli.backup.BUTTON_SIZE_MEDIUM
import com.machiav3lli.backup.ICON_SIZE_MEDIUM
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.data.entity.Pref
import com.machiav3lli.backup.data.preferences.PrefDelegate
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PrefsGroupCollapsed(prefs: ImmutableList<Pref>, heading: String) {
    if (prefs.isNotEmpty())
        ExpandableBlock(
            heading = heading,
        ) {
            PrefsGroup(prefs = prefs, heading = null)
        }
}

@Composable
fun PrefsGroup(
    modifier: Modifier = Modifier,
    heading: String? = null,
    content: @Composable () -> Unit,
) {
    PrefsGroupHeading(heading)
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.primary
    ) {
        Surface(color = Color.Transparent) {
            Column(modifier = modifier) {
                content()
            }
        }
    }
}

@Composable
fun PrefsGroup(
    modifier: Modifier = Modifier,
    heading: String? = null,
    prefs: ImmutableList<Pref>,
    onPrefDialog: (Pref) -> Unit = {},
) {
    val size = prefs.size

    PrefsGroup(
        modifier = modifier,
        heading = heading
    ) {
        if (prefs.isNotEmpty()) {
            prefs.forEachIndexed { index, pref ->
                traceDebug { "${pref.key} = $pref" }
                PrefsBuilder(
                    pref,
                    onPrefDialog,
                    index,
                    size,
                )
                if (index < size - 1)
                    Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun NeoPrefsGroup(
    modifier: Modifier = Modifier,
    heading: String? = null,
    prefs: ImmutableList<PrefDelegate<out Any>>,
    onPrefDialog: (PrefDelegate<out Any>) -> Unit = {},
) {
    val size = prefs.size

    PrefsGroup(
        modifier = modifier,
        heading = heading
    ) {
        if (prefs.isNotEmpty()) {
            prefs.forEachIndexed { index, pref ->
                traceDebug { "${pref.key} = $pref" }
                PrefsBuilder(
                    pref,
                    onPrefDialog,
                    index,
                    size,
                )
                if (index < size - 1)
                    Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun PrefsGroupHeading(
    heading: String? = null,
    modifier: Modifier = Modifier,
) =
    if (heading != null) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .height(BUTTON_SIZE_MEDIUM)
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        Spacer(modifier = Modifier.requiredHeight(8.dp))
    }


@Composable
fun PrefsExpandableGroupHeader(
    modifier: Modifier = Modifier,
    @StringRes titleId: Int,
    @StringRes summaryId: Int = -1,
    icon: ImageVector,
    onClick: (() -> Unit),
) {
    HorizontalDivider(thickness = 2.dp, modifier = Modifier.clip(MaterialTheme.shapes.extraLarge))
    Spacer(modifier = Modifier.height(8.dp))

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            leadingIconColor = MaterialTheme.colorScheme.onSurface,
        ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = titleId),
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(ICON_SIZE_MEDIUM),
            )
        },
        headlineContent = {
            Text(
                text = stringResource(id = titleId),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = {
            if (summaryId != -1) {
                Text(
                    text = stringResource(id = summaryId),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
    )
}