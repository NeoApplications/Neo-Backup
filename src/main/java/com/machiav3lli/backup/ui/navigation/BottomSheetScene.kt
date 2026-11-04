package com.machiav3lli.backup.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene

@OptIn(ExperimentalMaterial3Api::class)
internal data class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val modalBottomSheetProperties: ModalBottomSheetProperties,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        ModalBottomSheet(
            onDismissRequest = onBack,
            properties = modalBottomSheetProperties,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            sheetState = rememberBottomSheetState(
                SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            )
        ) {
            entry.Content()
        }
    }

    companion object {
        internal const val BOTTOM_SHEET_KEY = "bottomsheet"

        @OptIn(ExperimentalMaterial3Api::class)
        fun bottomSheet(
            modalBottomSheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties()
        ) = mapOf(BOTTOM_SHEET_KEY to modalBottomSheetProperties)
    }
}