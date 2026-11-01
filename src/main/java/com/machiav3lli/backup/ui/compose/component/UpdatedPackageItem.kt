package com.machiav3lli.backup.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.data.entity.Package

@Composable
fun UpdatedPackageItem(
    item: Package,
    modifier: Modifier = Modifier,
    onClick: (Package) -> Unit = {},
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent
        ),
        onClick = { onClick(item) },
    ) {
        Column(
            modifier = Modifier
                .padding(4.dp)
                .requiredWidth(64.dp)
                .clip(shape = RoundedCornerShape(8.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PackageIcon(
                imageData = item.iconData,
                isSpecial = item.isSpecial,
                isSystem = item.isSystem,
            )

            Text(
                text = item.packageLabel,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}