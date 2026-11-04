package com.machiav3lli.backup.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.machiav3lli.backup.data.dbs.entity.SpecialInfo
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.preferences.traceTiming
import com.machiav3lli.backup.utils.TraceUtils.beginNanoTimer
import com.machiav3lli.backup.utils.TraceUtils.endNanoTimer
import com.machiav3lli.backup.utils.TraceUtils.logNanoTiming
import com.machiav3lli.backup.utils.TraceUtils.nanoTiming
import com.machiav3lli.backup.utils.getFormattedDate

@Composable
fun MainPackageItem(
    pkg: Package,
    selected: Boolean,
    onLongClick: (Package) -> Unit = {},
    onAction: (Package) -> Unit = {},
) {
    beginNanoTimer("item")

    ListItem(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = { onAction(pkg) },
        onLongClick = { onLongClick(pkg) },
        shapes = ListItemDefaults.shapes(MaterialTheme.shapes.large),
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else Color.Transparent,
        ),
        leadingContent = {
            PackageIcon(
                modifier = Modifier.alpha(if (pkg.isSpecial && pkg.packageInfo !is SpecialInfo) 0.5f else 1f),  //TODO hg42 pkg.cannotHandle or similar
                imageData = pkg.iconData,
                isSpecial = pkg.isSpecial,
                isSystem = pkg.isSystem,
            )
        },
        content = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = pkg.packageLabel,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
                beginNanoTimer("item.labels")
                PackageLabels(item = pkg)
                endNanoTimer("item.labels")
            }

        },
        supportingContent = {
            Row(modifier = Modifier.fillMaxWidth()) {

                val hasBackups = pkg.hasBackups
                val latestBackup = pkg.latestBackup
                val nBackups = pkg.numberOfBackups

                beginNanoTimer("item.package")
                Text(
                    text = pkg.packageName,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                )
                endNanoTimer("item.package")

                beginNanoTimer("item.backups")
                AnimatedVisibility(visible = hasBackups) {
                    Text(
                        text = (latestBackup?.backupDate?.getFormattedDate(
                            false
                        ) ?: "") + " • $nBackups",
                        modifier = Modifier.align(Alignment.CenterVertically),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                endNanoTimer("item.backups")
            }
        },
    )

    endNanoTimer("item")

    if (traceTiming.pref.value)
        nanoTiming["item.package"]?.let {
            if (it.second > 0 && it.second % logEachN == 0L) {
                logNanoTiming()
            }
        }
}
