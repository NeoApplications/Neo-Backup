package com.machiav3lli.backup.ui.compose.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.Play
import com.machiav3lli.backup.utils.timeLeft

@Composable
fun ScheduleItem(
    schedule: Schedule,
    modifier: Modifier = Modifier,
    onClick: (Schedule) -> Unit = {},
    onRun: (Schedule) -> Unit = { _: Schedule -> },
    onCheckChanged: (Schedule, Boolean) -> Unit = { _: Schedule, _: Boolean -> },
) {
    val (checked, check) = mutableStateOf(schedule.enabled)
    val times by schedule.timeLeft().collectAsStateWithLifecycle()

    ListItem(
        modifier = modifier
            .fillMaxWidth(),
        onClick = {
            onClick(schedule)
        },
        shapes = ListItemDefaults.shapes(MaterialTheme.shapes.large),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
        leadingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    check(it)
                    onCheckChanged(schedule, it)
                }
            )
        },
        content = {
            Row {
                Text(
                    text = schedule.name,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
                ScheduleFilters(item = schedule)
            }
        },
        supportingContent = {
            Row {
                Text(
                    text = if (schedule.enabled)
                        "🕒 ${times.first}\n⏳ ${times.second}"    // TODO replace by resource icons
                    else
                        "🕒 ${times.first}",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style = MaterialTheme.typography.labelMedium,
                )
                ScheduleTypes(item = schedule)
            }
        },
        trailingContent = {
            IconButton(onClick = {
                onRun(schedule)
            }) {
                Icon(
                    imageVector = Phosphor.Play,
                    contentDescription = stringResource(id = R.string.sched_startingbackup)
                )
            }
        }
    )
}
