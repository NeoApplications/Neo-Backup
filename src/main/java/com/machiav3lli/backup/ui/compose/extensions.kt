package com.machiav3lli.backup.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.machiav3lli.backup.ui.pages.pref_altBlockLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun Modifier.vertical() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2)
        )
    }
}

@Composable
inline fun Modifier.ifThen(
    boolean: Boolean,
    crossinline modifier: @Composable Modifier.() -> Modifier,
): Modifier {
    return if (boolean) {
        modifier.invoke(this)
    } else {
        this
    }
}

@Composable
inline fun Modifier.ifThenElse(
    boolean: Boolean,
    crossinline modifier: @Composable Modifier.() -> Modifier,
    crossinline elseModifier: @Composable Modifier.() -> Modifier,
): Modifier {
    return if (boolean) {
        modifier.invoke(this)
    } else {
        elseModifier.invoke(this)
    }
}

fun Modifier.blockBorder(altStyle: Boolean = !pref_altBlockLayout.value) =
    composed {
        this
            .padding(2.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .ifThenElse(
                altStyle,
                modifier = {
                    border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                },
                elseModifier = {
                    background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                }
            )
    }

fun Modifier.blockShadow(altStyle: Boolean = !pref_altBlockLayout.value) =
    composed {
        this
            .clip(MaterialTheme.shapes.extraLarge)
            .ifThenElse(
                altStyle,
                modifier = {
                    border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                },
                elseModifier = {
                    shadow(elevation = 1.dp, shape = MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                }
            )
    }

@Composable
fun <T> ObservedEffect(flow: Flow<T?>, onChange: (T?) -> Unit) {
    val lcOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lcOwner.lifecycle) {
        lcOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(onChange)
        }
    }
}

@Composable
fun ObservedEffect(onChange: () -> Unit) {
    val lcOwner = LocalLifecycleOwner.current
    LaunchedEffect(lcOwner.lifecycle) {
        lcOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            onChange()
        }
    }
}

@Composable
fun LazyListState.isAtTop() = remember {
    derivedStateOf {
        firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
    }
}.value

@Composable
fun LazyListState.isAtBottom() = remember {
    derivedStateOf {
        try {
            layoutInfo.visibleItemsInfo.last().index >= layoutInfo.totalItemsCount - 1
        } catch (_: Throwable) {
            true
        }
    }
}.value

fun Color.mix(with: Color, factor: Float = 0.5f) = Color(
    red = (red * (1f - factor) + with.red * factor).coerceIn(0f, 1f),
    green = (green * (1f - factor) + with.green * factor).coerceIn(0f, 1f),
    blue = (blue * (1f - factor) + with.blue * factor).coerceIn(0f, 1f),
    alpha = alpha
)

fun <T> LazyListScope.gridItems(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    itemContent: @Composable BoxScope.(T) -> Unit,
) {
    val itemsCount = items.count()
    val rows = when {
        itemsCount >= 1 -> 1 + (itemsCount - 1) / columns
        else            -> 0
    }
    items(rows, key = { it.hashCode() }) { rowIndex ->
        Row(
            horizontalArrangement = horizontalArrangement,
            modifier = modifier
        ) {
            (0 until columns).forEach { columnIndex ->
                val itemIndex = columns * rowIndex + columnIndex
                if (itemIndex < itemsCount) {
                    Box(
                        modifier = Modifier.weight(1f),
                        propagateMinConstraints = true
                    ) {
                        itemContent(items[itemIndex])
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

fun SnackbarHostState.show(
    coroutineScope: CoroutineScope,
    message: String,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    coroutineScope.launch {
        showSnackbar(message = message, actionLabel = actionText, withDismissAction = true).apply {
            if (this == SnackbarResult.ActionPerformed) onAction()
        }
    }
}