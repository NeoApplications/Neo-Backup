package com.machiav3lli.backup.ui.dialogs

sealed class DialogKey {
    open class Warning(
        val message: String,
        val action: () -> Unit,
    ) : DialogKey()

    data class Error(val message: String) : DialogKey()
}