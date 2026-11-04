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
package com.machiav3lli.backup.ui.sheets

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.text.HtmlCompat
import com.machiav3lli.backup.BuildConfig
import com.machiav3lli.backup.R
import com.machiav3lli.backup.legendList
import com.machiav3lli.backup.linksList
import com.machiav3lli.backup.ui.compose.blockBorderBottom
import com.machiav3lli.backup.ui.compose.component.LegendItem
import com.machiav3lli.backup.ui.compose.component.LinkChip
import com.machiav3lli.backup.ui.compose.component.RoundButton
import com.machiav3lli.backup.ui.compose.component.TitleText
import com.machiav3lli.backup.ui.compose.gridItems
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.CaretDown
import com.machiav3lli.backup.ui.compose.icons.phosphor.CaretUp
import com.machiav3lli.backup.ui.compose.icons.phosphor.X
import com.machiav3lli.backup.utils.SystemUtils.applicationIssuer
import java.io.IOException
import java.io.InputStream
import java.util.Scanner

@Composable
fun HelpSheet(navigateUp: () -> Unit) {
    val context = LocalContext.current
    val res = LocalResources.current
    val nestedScrollConnection = rememberNestedScrollInteropConnection()

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                    shapes = ListItemDefaults.shapes(),
                    leadingContent = {
                        ResourcesCompat.getDrawable(
                            res,
                            R.mipmap.ic_launcher,
                            context.theme
                        )?.let { drawable ->
                            val bitmap = createBitmap(
                                drawable.intrinsicWidth,
                                drawable.intrinsicHeight,
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
                    content = {
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
                            Text(
                                text = "signed by ${context.applicationIssuer}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    trailingContent = {
                        RoundButton(
                            icon = Phosphor.X,
                            description = stringResource(id = android.R.string.cancel),
                            onClick = navigateUp,
                        )
                    }
                )

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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .blockBorderBottom()
                .nestedScroll(nestedScrollConnection)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            item { TitleText(R.string.help_legend) }
            gridItems(
                items = legendList,
                columns = 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegendItem(item = it)
            }
            item {
                Text(
                    text = stringResource(id = R.string.help_appTypeHint),
                )
            }
            item {
                val (showNotes, extendNotes) = remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                ) {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { extendNotes(!showNotes) },
                        shapes = ListItemDefaults.shapes(),
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        content = {
                            TitleText(R.string.usage_notes_title)
                        },
                        trailingContent = {
                            Icon(
                                imageVector = if (showNotes) Phosphor.CaretUp
                                else Phosphor.CaretDown,
                                contentDescription = null
                            )
                        }
                    )
                    AnimatedVisibility(
                        visible = showNotes,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = context.getUsageNotes(), modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}


private fun Context.getUsageNotes(): String = try {
    val stream = resources.openRawResource(R.raw.help)
    val htmlString = convertStreamToString(stream)
    stream.close()
    HtmlCompat.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_LEGACY).dropLast(2).toString()
} catch (e: IOException) {
    e.toString()
} catch (ignored: PackageManager.NameNotFoundException) {
    ""
}

fun convertStreamToString(stream: InputStream?): String {
    val s = Scanner(stream, "utf-8").useDelimiter("\\A")
    return if (s.hasNext()) s.next() else ""
}
