package com.machiav3lli.backup.ui.compose.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.R

@Composable
fun DialogPositiveButton(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.dialogOK),
    onClick: () -> Unit = {},
) {
    TextButton(
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = modifier,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp),
        )
    }
}

@Composable
fun DialogPositiveButton(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.dialogOK),
    icon: ImageVector?,
    onClick: () -> Unit = {},
) {
    ElevatedButton(
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = modifier,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
            )
        }
        Text(
            text = text,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp),
        )
    }
}

@Composable
fun DialogNegativeButton(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.dialogCancel),
    onClick: () -> Unit = {},
) {
    TextButton(
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 8.dp),
        )
    }
}
