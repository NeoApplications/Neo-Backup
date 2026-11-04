package com.machiav3lli.backup.ui.compose.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.data.entity.ChipItem
import com.machiav3lli.backup.data.entity.InfoChipItem
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.CheckCircle
import com.machiav3lli.backup.ui.compose.icons.phosphor.Circle
import com.machiav3lli.backup.ui.compose.ifThen
import com.machiav3lli.backup.utils.launchView

@Composable
fun SelectionChip(
    item: ChipItem,
    isSelected: Boolean,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        iconColor = MaterialTheme.colorScheme.onSurface,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    onClick: () -> Unit,
) {
    FilterChip(
        colors = colors,
        shapes = FilterChipDefaults.shapes(
            shape = MaterialTheme.shapes.extraLarge,
            selectedShape = MaterialTheme.shapes.extraSmall,
        ),
        border = null,
        selected = isSelected,
        leadingIcon = {
            ButtonIcon(item.icon, item.textId)
        },
        onClick = onClick,
        label = {
            Text(text = stringResource(id = item.textId))
        }
    )
}

@Composable
fun SelectionChip(
    label: String,
    isSelected: Boolean,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        iconColor = MaterialTheme.colorScheme.onSurface,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    onClick: () -> Unit,
) {
    FilterChip(
        colors = colors,
        shapes = FilterChipDefaults.shapes(
            shape = MaterialTheme.shapes.extraLarge,
            selectedShape = MaterialTheme.shapes.extraSmall,
        ),
        border = null,
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(text = label)
        }
    )
}

@Composable
fun InfoChip(
    item: InfoChipItem,
    colors: ChipColors = AssistChipDefaults.assistChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        labelColor = MaterialTheme.colorScheme.onSurface,
        leadingIconContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    highLightColors: ChipColors = AssistChipDefaults.assistChipColors(
        containerColor = item.color ?: MaterialTheme.colorScheme.onPrimaryContainer,
        labelColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        leadingIconContentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
) {
    SuggestionChip(
        icon = {
            if (item.icon != null) Icon(
                imageVector = item.icon,
                contentDescription = item.text,
            )
        },
        shape = MaterialTheme.shapes.small,
        border = null,
        label = {
            Text(text = item.text)
        },
        colors = if (item.color != null) highLightColors
        else colors,
        onClick = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    color: Color,
    checked: Boolean,
    onClick: (Boolean) -> Unit,
) {
    val cornerRadius by animateDpAsState(
        when {
            checked -> 4.dp
            else    -> 28.dp
        }, label = "chip_corner"
    )
    val contentColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.surfaceContainerLowest else color,
        label = "contentColor"
    )
    val backgroundColor by animateColorAsState(
        if (checked) color else Color.Transparent,
        label = "backgroundColor"
    )

    TooltipBox(
        modifier = modifier
            .defaultMinSize(minWidth = 24.dp, minHeight = 1.dp),
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
    ) {
        Surface(
            contentColor = contentColor,
            color = backgroundColor,
            checked = checked,
            onCheckedChange = onClick,
            shape = RoundedCornerShape(cornerRadius),
            border = BorderStroke(1.dp, color),
        ) {
            Icon(
                modifier = Modifier.padding(8.dp),
                imageVector = icon,
                contentDescription = text,
            )
        }
    }
}

@Composable
fun CheckChip(
    modifier: Modifier = Modifier,
    checked: Boolean,
    textId: Int,
    checkedTextId: Int,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        labelColor = MaterialTheme.colorScheme.onBackground,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        iconColor = MaterialTheme.colorScheme.onBackground,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = Color.Transparent,
        selectedContainerColor = MaterialTheme.colorScheme.primary
    ),
    onCheckedChange: (Boolean) -> Unit,
) {
    val (checked, check) = remember(checked) { mutableStateOf(checked) }

    FilterChip(
        modifier = modifier,
        selected = checked,
        colors = colors,
        shapes = FilterChipDefaults.shapes(
            shape = MaterialTheme.shapes.extraExtraLarge,
            selectedShape = MaterialTheme.shapes.extraSmall,
        ),
        leadingIcon = {
            Icon(
                imageVector = when {
                    checked -> Phosphor.CheckCircle
                    else    -> Phosphor.Circle
                },
                contentDescription = null,
            )
        },
        onClick = {
            onCheckedChange(!checked)
            check(!checked)
        },
        label = {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(id = if (checked) checkedTextId else textId),
            )
        }
    )
}

@Composable
fun ActionChip(
    modifier: Modifier = Modifier,
    text: String = "",
    icon: ImageVector? = null,
    positive: Boolean,
    fullWidth: Boolean = false,
    onClick: () -> Unit = {},
) {
    AssistChip(
        modifier = modifier,
        label = {
            Text(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .ifThen(fullWidth) {
                        fillMaxWidth()
                    },
                text = text,
                textAlign = TextAlign.Center,
            )
        },
        leadingIcon = {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = text
                )
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (positive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.tertiary,
            labelColor = if (positive) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onTertiary,
            leadingIconContentColor = if (positive) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onTertiary,
        ),
        border = null,
        onClick = onClick
    )
}

@Composable
fun LinkChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    url: String,
) {
    val context = LocalContext.current

    AssistChip(
        modifier = modifier,
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        onClick = {
            context.launchView(url)
        }
    )
}

@Composable
fun ChipsSwitch(
    firstTextId: Int,
    firstIcon: ImageVector,
    secondTextId: Int,
    secondIcon: ImageVector,
    firstSelected: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val (firstSelected, selectFirst) = remember { mutableStateOf(firstSelected) }

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.extraLarge,
            )
            .padding(horizontal = 4.dp)
            .fillMaxWidth(),
    ) {
        SegmentedTabButton(
            text = stringResource(id = firstTextId),
            icon = firstIcon,
            selected = { firstSelected },
            onClick = {
                onCheckedChange(true)
                selectFirst(true)
            }
        )
        SegmentedTabButton(
            text = stringResource(id = secondTextId),
            icon = secondIcon,
            selected = { !firstSelected },
            onClick = {
                onCheckedChange(false)
                selectFirst(false)
            }
        )
    }
}