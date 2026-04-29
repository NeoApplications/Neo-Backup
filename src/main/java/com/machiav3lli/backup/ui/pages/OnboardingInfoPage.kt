/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.ui.pages

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.machiav3lli.backup.BuildConfig
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.entity.ColoringState
import com.machiav3lli.backup.linksList
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.blockBorderBottom
import com.machiav3lli.backup.ui.compose.component.ActionButton
import com.machiav3lli.backup.ui.compose.component.LinkChip
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowRight
import com.machiav3lli.backup.utils.SystemUtils.applicationIssuer

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OnboardingInfoPage(
    onNext: () -> Unit,
) {
    val main = LocalActivity.current as NeoActivity

    Column {
        LazyColumn(
            modifier = Modifier
                .blockBorderBottom()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            item(key = "appInfo") {
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                    leadingContent = {
                        ResourcesCompat.getDrawable(
                            LocalContext.current.resources,
                            R.mipmap.ic_launcher,
                            LocalContext.current.theme
                        )?.let { drawable ->
                            val bitmap = Bitmap.createBitmap(
                                drawable.intrinsicWidth,
                                drawable.intrinsicHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(bitmap)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .requiredSize(72.dp)
                                    .clip(MaterialTheme.shapes.large)
                            )
                        }
                    },
                    overlineContent = {
                        Text(
                            text = stringResource(
                                id = R.string.about_build_FORMAT,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    supportingContent = {
                        Column {
                            Text(
                                text = BuildConfig.APPLICATION_ID,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            main.applicationIssuer?.let {
                                Text(
                                    text = "signed by $it",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                )
            }
            item(key = "links") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp),
                ) {
                    items(linksList) { link ->
                        LinkChip(
                            icon = link.icon,
                            label = stringResource(id = link.nameId),
                            url = link.uri,
                        )
                    }
                }
            }
            item(key = "welcomeMessage") {
                Text(
                    text = stringResource(id = R.string.intro_welcome_message),
                )
            }
        }
        ActionButton(
            text = stringResource(id = R.string.dialog_start),
            icon = Phosphor.ArrowRight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            coloring = ColoringState.Positive,
            onClick = onNext,
        )
    }
}
