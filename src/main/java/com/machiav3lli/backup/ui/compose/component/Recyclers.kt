package com.machiav3lli.backup.ui.compose.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.imageLoader
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.entity.ChipItem
import com.machiav3lli.backup.data.entity.InfoChipItem
import com.machiav3lli.backup.data.entity.Log
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.ui.pages.pref_multilineInfoChips
import com.machiav3lli.backup.ui.pages.pref_singularBackupRestore
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.coroutines.launch

@Composable
fun HomePackageRecycler(
    modifier: Modifier = Modifier,
    productsList: PersistentList<Package>,
    selected: (String) -> Boolean,
    imageLoader: ImageLoader = LocalContext.current.imageLoader,
    onLongClick: (Package) -> Unit = {},
    onClick: (Package) -> Unit = {},
) {
    VerticalItemList(
        modifier = modifier,
        list = productsList,
        itemKey = { it.packageName }
    ) {
        MainPackageItem(
            it,
            selected(it.packageName),
            imageLoader,
            onLongClick,
            onClick
        )
    }
}

@Composable
fun UpdatedPackageRecycler(
    modifier: Modifier = Modifier,
    productsList: PersistentList<Package>?,
    imageLoader: ImageLoader = LocalContext.current.imageLoader,
    onClick: (Package) -> Unit = {},
) {
    HorizontalItemList(
        modifier = modifier,
        list = productsList,
        itemKey = { it.packageName }
    ) {
        UpdatedPackageItem(
            it,
            imageLoader,
            Modifier.animateItem(),
            onClick,
        )
    }
}

@Composable
fun BatchPackageRecycler(
    modifier: Modifier = Modifier,
    productsList: PersistentList<Package>?,
    restore: Boolean = false,
    apkBackupCheckedList: SnapshotStateMap<String, Int>,
    dataBackupCheckedList: SnapshotStateMap<String, Int>,
    onBackupApkClick: (String, Boolean, Int) -> Unit = { _: String, _: Boolean, _: Int -> },
    onBackupDataClick: (String, Boolean, Int) -> Unit = { _: String, _: Boolean, _: Int -> },
    onClick: (Package, Boolean, Boolean) -> Unit = { _: Package, _: Boolean, _: Boolean -> },
) {
    VerticalItemList(
        modifier = modifier,
        list = productsList,
        itemKey = { it.packageName }
    ) {
        val apkBackupChecked = remember(apkBackupCheckedList[it.packageName]) {
            mutableStateOf(apkBackupCheckedList[it.packageName])
        }
        val dataBackupChecked = remember(dataBackupCheckedList[it.packageName]) {
            mutableStateOf(dataBackupCheckedList[it.packageName])
        }

        if (restore && pref_singularBackupRestore.value) RestorePackageItem(
            it,
            apkBackupChecked,
            dataBackupChecked,
            onClick,
            onBackupApkClick,
            onBackupDataClick,
        )
        else BatchPackageItem(
            it,
            restore,
            apkBackupChecked.value == 0,
            dataBackupChecked.value == 0,
            onClick,
            onApkClick = { p, b ->
                onBackupApkClick(p.packageName, b, 0)
            },
            onDataClick = { p, b ->
                onBackupDataClick(p.packageName, b, 0)
            }
        )
    }
}

@Composable
fun ScheduleRecycler(
    modifier: Modifier = Modifier,
    enabledSchedules: PersistentList<Schedule>,
    disabledSchedules: PersistentList<Schedule>,
    onClick: (Schedule) -> Unit = {},
    onRun: (Schedule) -> Unit = {},
    onCheckChanged: (Schedule, Boolean) -> Unit = { _: Schedule, _: Boolean -> },
) {
    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Absolute.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item(key = R.string.enabled_schedules) {
            PrefsGroupHeading(heading = stringResource(id = R.string.enabled_schedules))
        }
        if (enabledSchedules.isEmpty()) item(key = R.string.empty_filtered_list + 9000) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.empty_filtered_list)
            )
        }
        items(
            items = enabledSchedules,
            key = { it.id },
        ) {
            ScheduleItem(
                schedule = it,
                modifier = Modifier.animateItem(),
                onClick = onClick,
                onRun = onRun,
                onCheckChanged = onCheckChanged,
            )
        }
        item(key = R.string.disabled_schedules) {
            PrefsGroupHeading(heading = stringResource(id = R.string.disabled_schedules))
        }
        if (disabledSchedules.isEmpty()) item(key = R.string.empty_filtered_list + 8000) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.empty_filtered_list)
            )
        }
        items(
            items = disabledSchedules,
            key = { it.id },
        ) {
            ScheduleItem(
                schedule = it,
                modifier = Modifier.animateItem(),
                onClick = onClick,
                onRun = onRun,
                onCheckChanged = onCheckChanged,
            )
        }
        item {
            Spacer(
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
fun ExportedScheduleRecycler(
    modifier: Modifier = Modifier,
    productsList: PersistentList<Pair<Schedule, StorageFile>>?,
    onImport: (Schedule) -> Unit = {},
    onDelete: (StorageFile) -> Unit = {},
) {
    VerticalItemList(
        modifier = modifier,
        list = productsList
    ) {
        ExportedScheduleItem(it.first, onImport) { onDelete(it.second) }
    }
}

@Composable
fun LogRecycler(
    modifier: Modifier = Modifier,
    productsList: PersistentList<Log>,
    onShare: (Log) -> Unit = {},
    onDelete: (Log) -> Unit = {},
) {
    VerticalItemList(
        modifier = modifier,
        list = productsList
    ) {
        LogItem(it, onShare, onDelete)
    }
}

@Composable
fun InfoChipsBlock(
    modifier: Modifier = Modifier,
    list: List<InfoChipItem>,
) {
    if (pref_multilineInfoChips.value)
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            list.forEach { chip ->
                InfoChip(item = chip)
            }
        }
    else LazyRow(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        items(list) { chip ->
            InfoChip(item = chip)
        }
    }
}


@Composable
fun SelectableChipGroup(
    //TODO hg42 move to item/Icons.kt ?
    modifier: Modifier = Modifier,
    list: PersistentList<ChipItem>,
    selectedFlag: Int,
    onClick: (Int) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        list.forEach { item ->
            SelectionChip(
                item = item,
                isSelected = item.flag == selectedFlag,
            ) {
                onClick(item.flag)
            }
        }
    }
}

@Composable
fun MultiSelectableChipGroup(
    //TODO hg42 move to item/Icons.kt ?
    modifier: Modifier = Modifier,
    list: PersistentList<ChipItem>,
    selectedFlags: Int,
    onClick: (Int, Int) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        list.forEach { item ->
            SelectionChip(
                item = item,
                isSelected = item.flag and selectedFlags != 0,
            ) {
                onClick(selectedFlags xor item.flag, item.flag)
            }
        }
    }
}

@Composable
fun MultiSelectableChipGroup(
    modifier: Modifier = Modifier,
    list: PersistentSet<String>,
    selected: Set<String>,
    onClick: (Set<String>) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        list.forEach { item ->
            SelectionChip(
                label = item,
                isSelected = item in selected,
            ) {
                onClick(if (item in selected) selected - item else selected + item)
            }
        }
    }
}

@Composable
fun <T : Any> VerticalItemList(
    modifier: Modifier = Modifier,
    list: PersistentList<T>?,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("VerticalItemList"),
        contentAlignment = if (list.isNullOrEmpty()) Alignment.Center else Alignment.TopStart
    ) {
        when {
            list == null   -> Text(
                text = stringResource(id = R.string.loading_list),
                color = MaterialTheme.colorScheme.onSurface
            )

            list.isEmpty() -> Text(
                text = stringResource(id = R.string.empty_filtered_list),
                color = MaterialTheme.colorScheme.onSurface
            )

            else           -> {
                // TODO add scrollbars
                val state = rememberLazyListState()

                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .testTag("VerticalItemList.Column"),
                    verticalArrangement = Arrangement.Absolute.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(
                        items = list,
                        itemContent = { _: Int, it: T ->
                            itemContent(it)
                        },
                        key = { index: Int, it: T ->
                            itemKey?.invoke(it) ?: index
                        }
                    )
                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)  // workaround for floating buttons hiding the elements
                            // unfortunately the sizes are private
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> HorizontalItemList(
    modifier: Modifier = Modifier,
    list: PersistentList<T>?,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = if (list.isNullOrEmpty()) Alignment.Center else Alignment.CenterStart
    ) {
        when {
            list == null   -> Text(
                text = stringResource(id = R.string.loading_list),
                color = MaterialTheme.colorScheme.onSurface
            )

            list.isEmpty() -> Text(
                text = stringResource(id = R.string.empty_filtered_list),
                color = MaterialTheme.colorScheme.onSurface
            )

            else           -> {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                ) {
                    items(items = list, key = itemKey, itemContent = itemContent)
                }
            }
        }
    }
}

@Composable
fun CarouselIndicators(
    modifier: Modifier = Modifier,
    size: Int = 1,
    dimension: Dp = 8.dp,
    enableScrolling: Boolean = true,
    state: PagerState,
) {
    val scope = rememberCoroutineScope()
    val currentPage by remember { derivedStateOf { state.currentPage } }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimension / 4, Alignment.CenterHorizontally),
    ) {
        items(size) { i ->
            val isSelected by remember {
                derivedStateOf {
                    currentPage == i
                }
            }
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer,
                label = "indicatorColor"
            )
            val width by animateDpAsState(
                targetValue = if (isSelected) dimension.times(2) else dimension,
                label = "indicatorWidth"
            )

            Box(
                modifier = Modifier
                    .size(height = dimension, width = width)
                    .clip(CircleShape)
                    .background(color = color)
                    .clickable(enabled = enableScrolling) {
                        scope.launch { state.animateScrollToPage(i) }
                    }
            )
        }
    }
}
