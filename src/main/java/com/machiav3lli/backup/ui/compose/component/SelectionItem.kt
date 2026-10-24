package com.machiav3lli.backup.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.Check

@Composable
fun MultiSelectionListItem(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean,
    isEnabled: Boolean = true,
    onClick: (Boolean) -> Unit = {},
) {
    val checkbox =
        @Composable {
            Checkbox(
                checked = isChecked,
                enabled = isEnabled,
                onCheckedChange = onClick,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = { onClick(!isChecked) }, enabled = isEnabled),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        checkbox()
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val borderWidth = if (isSelected) 3.dp else 0.dp
    val borderColor = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent

    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(borderWidth, borderColor, CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Phosphor.Check,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SwitchRow(
    modifier: Modifier = Modifier,
    text: String,
    withContainer: Boolean = false,
    index: Int = 0,
    groupSize: Int = 1,
    initSelected: () -> Boolean,
    onCheckedChanged: (Boolean) -> Unit,
) {
    val (selected, select) = remember(initSelected()) { mutableStateOf(initSelected()) }
    val interactionSource = remember { MutableInteractionSource() }
    val base = index.toFloat() / groupSize
    val rank = (index + 1f) / groupSize

    ListItem(
        modifier = modifier
            .fillMaxWidth(),
        shapes = ListItemDefaults.shapes(
            shape = RoundedCornerShape(
                topStart = if (base == 0f) MaterialTheme.shapes.large.topStart
                else MaterialTheme.shapes.extraSmall.topStart,
                topEnd = if (base == 0f) MaterialTheme.shapes.large.topEnd
                else MaterialTheme.shapes.extraSmall.topEnd,
                bottomStart = if (rank == 1f) MaterialTheme.shapes.large.bottomStart
                else MaterialTheme.shapes.extraSmall.bottomStart,
                bottomEnd = if (rank == 1f) MaterialTheme.shapes.large.bottomEnd
                else MaterialTheme.shapes.extraSmall.bottomEnd
            )
        ),
        colors = ListItemDefaults.colors(
            containerColor = if (withContainer) MaterialTheme.colorScheme.surfaceContainer
            else Color.Transparent,
        ),
        content = {
            Text(
                text = text,
                maxLines = 2,
            )
        },
        trailingContent = {
            Switch(
                checked = selected,
                interactionSource = interactionSource,
                onCheckedChange = {
                    select(it)
                    onCheckedChanged(it)
                }
            )
        },
        interactionSource = interactionSource,
        onClick = {
            select(!selected)
            onCheckedChanged(!selected)
        }
    )
}