package com.machiav3lli.backup.ui.compose.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.ICON_SIZE_SMALL
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.ColoringState
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowsClockwise
import kotlin.math.max

@Composable
fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    coloring: ColoringState = ColoringState.Positive,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            contentColor = when (coloring) {
                ColoringState.Positive -> MaterialTheme.colorScheme.onPrimaryContainer
                ColoringState.Negative -> MaterialTheme.colorScheme.onTertiaryContainer
                else                   -> MaterialTheme.colorScheme.onSecondaryContainer
            },
            containerColor = when (coloring) {
                ColoringState.Positive -> MaterialTheme.colorScheme.primaryContainer
                ColoringState.Negative -> MaterialTheme.colorScheme.tertiaryContainer
                else                   -> MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        enabled = enabled,
        onClick = onClick,
    ) {
        if (icon != null) Icon(imageVector = icon, contentDescription = text)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
fun OutlinedActionButton(
    modifier: Modifier = Modifier,
    text: String,
    coloring: ColoringState = ColoringState.Positive,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = when (coloring) {
                ColoringState.Positive -> MaterialTheme.colorScheme.primary
                ColoringState.Negative -> MaterialTheme.colorScheme.tertiary
                else                   -> MaterialTheme.colorScheme.onSurface
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when (coloring) {
                ColoringState.Positive -> MaterialTheme.colorScheme.primary
                ColoringState.Negative -> MaterialTheme.colorScheme.tertiary
                else                   -> MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
        enabled = enabled,
        onClick = onClick,
    ) {
        if (icon != null) Icon(imageVector = icon, contentDescription = text)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    coloring: ColoringState = ColoringState.Positive,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                onClick = onClick,
                enabled = enabled,
            ),
        colors = ListItemDefaults.colors(
            leadingIconColor = when (coloring) {
                ColoringState.Positive -> MaterialTheme.colorScheme.onPrimaryContainer
                ColoringState.Negative -> MaterialTheme.colorScheme.onTertiaryContainer
                else                   -> MaterialTheme.colorScheme.onSurface
            },
            headlineColor = when (coloring) {
                ColoringState.Positive -> MaterialTheme.colorScheme.onPrimaryContainer
                ColoringState.Negative -> MaterialTheme.colorScheme.onTertiaryContainer
                else                   -> MaterialTheme.colorScheme.onSurface
            },
            containerColor = when (coloring) {
                ColoringState.Positive -> MaterialTheme.colorScheme.primaryContainer
                ColoringState.Negative -> MaterialTheme.colorScheme.tertiaryContainer
                else                   -> MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
        leadingContent = {
            Icon(imageVector = icon, contentDescription = description)
        },
        headlineContent = {
            Text(
                text = description,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                style = MaterialTheme.typography.titleSmall
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconTextButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    containerColor: Color = MaterialTheme.colorScheme.onSurface,
    description: String,
    enabled: Boolean = true,
    aspectRatio: Float = 2f,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                modifier = modifier
                    .aspectRatio(aspectRatio),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = enabled,
                onClick = onClick
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = description
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = description,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedIconTextButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.onSurface,
    description: String,
    enabled: Boolean = true,
    aspectRatio: Float = 2f,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OutlinedIconButton(
                modifier = modifier
                    .aspectRatio(aspectRatio),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    contentColor = containerColor,
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = containerColor,
                ),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = enabled,
                onClick = onClick
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = description
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = description,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
fun RoundButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    description: String = "",
    tint: Color = MaterialTheme.colorScheme.onSurface,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier,
        colors = if (filled) IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) else IconButtonDefaults.iconButtonColors(),
        shape = CircleShape,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            tint = if (filled) LocalContentColor.current
            else tint,
            contentDescription = description
        )
    }
}

@Composable
fun FilledRoundButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = ICON_SIZE_SMALL,
    tint: Color = MaterialTheme.colorScheme.primary,
    onTint: Color = MaterialTheme.colorScheme.onPrimary,
    description: String = "",
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = tint,
            contentColor = onTint,
        ),
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.size(size),
            imageVector = icon,
            contentDescription = description
        )
    }
}

@Composable
fun RefreshButton(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    hideIfNotBusy: Boolean = false,
    onClick: () -> Unit = {},
) {
    val isBusy by remember { NeoApp.busy }

    if (hideIfNotBusy && isBusy.not())
        return

    val angle by animateFloatAsState(
        if (isBusy) {
            val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")

            val animationProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 50000,
                        easing = LinearEasing
                    )
                ), label = "animationProgress"
            )
            val angle = 360f * animationProgress
            angle
        } else 0f, label = "iconAngle"
    )

    RoundButton(
        description = stringResource(id = R.string.refresh),
        icon = Phosphor.ArrowsClockwise,
        modifier = modifier
            .graphicsLayer {
                rotationZ = angle
            },
        onClick = onClick
    )
}


@Preview
@Composable
fun RefreshButtonPreview() {
    val level by remember { NeoApp.busyLevel }
    val factor = 1.0 / max(1, level)

    Column {
        Text("factor: $factor")
        Text("level: $level")
        Row {
            RefreshButton()
            ActionButton(text = "hit") {
                NeoApp.hitBusy()
            }
            ActionButton(text = "begin") {
                NeoApp.beginBusy()
            }
            ActionButton(text = "end") {
                NeoApp.endBusy()
            }
        }
    }
}