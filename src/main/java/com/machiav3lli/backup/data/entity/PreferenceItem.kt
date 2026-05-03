package com.machiav3lli.backup.data.entity

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.preferences.PrefDelegate
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.data.preferences.tracePrefs
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.ui.compose.component.BooleanPreference
import com.machiav3lli.backup.ui.compose.component.EnumPreference
import com.machiav3lli.backup.ui.compose.component.IntPreference
import com.machiav3lli.backup.ui.compose.component.LaunchPreference
import com.machiav3lli.backup.ui.compose.component.ListPreference
import com.machiav3lli.backup.ui.compose.component.PasswordPreference
import com.machiav3lli.backup.ui.compose.component.StringEditPreference
import com.machiav3lli.backup.ui.compose.component.StringPreference
import com.machiav3lli.backup.ui.pages.publicPreferences
import com.machiav3lli.backup.utils.getDefaultSharedPreferences
import com.machiav3lli.backup.utils.getPrivateSharedPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// hg42:

// I guess, the purpose of PrefBuilder is to separate preferences from their UI
// though a builder with when() is not a clean object oriented solution
// (actually when() is always an indicator for imperative programming)
// e.g. it could not be extended by a loaded class or a plugin etc. and it binds all PrefItems and PrefUIs together
// PrefItems are not really abstract, e.g. icon is not symbolic
// so adding the UI here doesn't hurt much and is more practical
// it would allow to put the different PrefItem classes into separate entities (files, modules, plugins)

// I know it could be more abstracted, to allow different "Views" of the PrefItems
// (View in the abstract sense, e.g. UI, Serializer, ...)
// - we have a namespace (Serializer, UI, ...) and type name (Int, String, Launch, ...)
// - the View would know the Pref, but not vice versa
// - the View would need to register itself to the namespace object (builder, factory)
//   or to the Pref object with the namespace as key
// - as an alternative, the connection between View and Pref could be established
//   by naming conventions e.g. IntPref -> IntPrefUI, IntPrefSerializer, ...
//   the name cxould be    used to access a plugin etc.

// for now it's done the wrong way, but object oriented

typealias PrefUI = @Composable (pref: Pref, onDialogPref: (Pref) -> Unit, index: Int, groupSize: Int) -> Unit

// dirty is used to force recomposition when value, summary, icon, tint change
// dirty as a state would trigger recomposition, but only where dirty is used,
// so the dirty parameter in the composables ensures that it triggers all levels
// the alternative would be to use a state for value, summary, icon, tint
// but this would mean, that much more needs to be checked for recomposition

open class Pref(
    var key: String,
    val private: Boolean = false,
    val defaultValue: Any? = null,
    @StringRes val titleId: Int,
    @StringRes val summaryId: Int,
    var summary: String? = null,
    val UI: PrefUI? = null,
    val icon: ImageVector? = null,
    var iconTint: ((Pref) -> Color)? = null,
    val enableIf: (() -> Boolean)? = null,
    val onChanged: (suspend (Pref) -> Unit)? = null,
    var group: String = "",
) {
    var dirty = mutableStateOf(false)

    init {
        try {
            val (g, k) = key.split(".", limit = 2)
            if (k.isNotEmpty()) {
                group = g
                key = k
            }
        } catch (e: Throwable) {
            // ignore
        }
        //Timber.d("add pref $group - $key")

        prefGroups.getOrPut(group) { mutableListOf() }.add(this)
    }

    override fun toString(): String = ""

    companion object {

        val prefGroups: MutableMap<String, MutableList<Pref>> = mutableMapOf()
        val prefs get() = prefGroups.values.flatten()
        var prefTransactionRunning = 0

        val prefChangeListeners = mutableStateMapOf<Pref, (pref: Pref) -> Unit>()
        private fun onPrefChange(name: String) {
            prefChangeListeners.forEach { (pref, listener) ->
                listener(pref)
            }
            prefs.find { it.key == name }?.let { pref ->
                pref.dirty.value = true
                pref.onChanged?.let { onChanged ->
                    CoroutineScope(Dispatchers.Main).launch {
                        while (prefTransactionRunning > 0)
                            delay(500)
                        tracePrefs { "pref changed: ${pref.dirty} ${pref.key} -> ${pref}" }
                        onChanged(pref)
                        //delay(200)
                        traceDebug { "pref: ${pref.dirty} ${pref.key} -> ${pref.icon?.name} ${pref.iconTint} (onchanged)" }
                        pref.dirty.value = true
                    }
                }
            }
        }

        private fun getPrefs(private: Boolean = false) =
            if (private)
                NeoApp.context.getPrivateSharedPrefs()
            else
                NeoApp.context.getDefaultSharedPreferences()

        fun prefFlag(name: String, default: Boolean, private: Boolean = false) =
            try {
                getPrefs(private).getBoolean(name, default)
            } catch (e: Throwable) {
                default
            }

        fun setPrefFlag(name: String, value: Boolean, private: Boolean = false) {
            if (!private) tracePrefs { "set pref $name = $value" }
            getPrefs(private).edit().putBoolean(name, value).apply()
            onPrefChange(name)
        }

        fun prefString(name: String, default: String, private: Boolean = false) =
            try {
                getPrefs(private).getString(name, default) ?: default
            } catch (e: Throwable) {
                default
            }

        fun setPrefString(name: String, value: String, private: Boolean = false) {
            if (!private) tracePrefs { "set pref $name = '$value'" }
            getPrefs(private).edit().putString(name, value).apply()
            onPrefChange(name)
        }

        fun prefInt(name: String, default: Int, private: Boolean = false) =
            try {
                getPrefs(private).getInt(name, default)
            } catch (e: Throwable) {
                default
            }

        fun setPrefInt(name: String, value: Int, private: Boolean = false) {
            if (!private) tracePrefs { "set pref $name = $value" }
            getPrefs(private).edit().putInt(name, value).apply()
            onPrefChange(name)
        }

        private val toBeEscaped =
            Regex("""[\\"\n\r\t]""")      // blacklist, only escape those that are necessary

        private val toBeUnescaped =
            Regex("""\\(.)""")      // blacklist, only escape those that are necessary

        fun escape(value: String): String {
            return value.replace(toBeEscaped) {
                when (it.value) {
                    "\n" -> "\\n"
                    "\r" -> "\\r"
                    "\t" -> "\\t"
                    else -> "\\${it.value}"
                }
            }
        }

        fun unescape(value: String): String {
            return value.replace(toBeUnescaped) { match ->
                match.groupValues[1].let {
                    when (it) {
                        "n"  -> "\n"
                        "r"  -> "\r"
                        "t"  -> "\t"
                        else -> it
                    }
                }
            }
        }

        fun toSimpleFormat(entries: Map<String, Any>): String {
            return entries.toSortedMap().mapNotNull {
                when (it.value) {
                    is String  -> it.key to "\"" + escape(it.value as String) + "\""
                    is Int     -> it.key to (it.value as Int).toString()
                    is Boolean -> it.key to (it.value as Boolean).toString()
                    else       -> null
                }
            }.joinToString("\n") { (key, value) ->
                "$key: $value"
            }
        }

        fun fromSimpleFormat(serialized: String): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            serialized.lineSequence().forEach {
                var (key, value) = it.split(":", limit = 2)
                value = value.trim()
                runCatching {
                    when {
                        value.startsWith('"')
                                && value.endsWith('"') -> {
                            value = unescape(value.removeSurrounding("\""))
                            map.put(key, value)
                        }

                        value == "true"                -> {
                            map.put(key, true)
                        }

                        value == "false"               -> {
                            map.put(key, false)
                        }

                        else                           -> {
                            map.put(key, value.toInt())
                        }
                    }
                }
            }
            return map
        }

        fun preferencesToSerialized(): String {

            val prefs: Map<String, Any> =
                publicPreferences().mapNotNull { pref ->
                    try {
                        when (pref) {
                            // order from derived to base classes (otherwise base would obscure derived)
                            is EnumPref    -> pref.key to pref.value
                            is ListPref    -> pref.key to pref.value
                            //is PasswordPref -> pref.key to pref.value     // don't store
                            is StringPref  -> pref.key to pref.value
                            is BooleanPref -> pref.key to pref.value
                            is IntPref     -> pref.key to pref.value
                            else           -> null
                        }
                    } catch (e: Throwable) {
                        LogsHandler.unexpectedException(e)
                        null
                    }
                }.toMap()

            val serialized = try {
                //OABX.toSerialized(OABX.prefsSerializer, prefs)
                toSimpleFormat(prefs)
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e)
                ""
            }

            return serialized
        }

        fun preferencesFromSerialized(serialized: String) {

            val prefs = fromSimpleFormat(serialized)
            //OABX.fromSerialized<Map<String, Any>>(serialized)

            synchronized(Pref) { prefTransactionRunning++ }
            prefs.forEach { (key, value) ->
                when (value) {
                    is String  -> setPrefString(key, value)
                    is Int     -> setPrefInt(key, value)
                    is Boolean -> setPrefFlag(key, value)
                }
            }
            synchronized(Pref) { prefTransactionRunning-- }
        }
    }
}

// keep all other Pref classes final, because they are used in when clauses
// and derived classes would need to come first, which is worth more than the savings

class BooleanPref(
    key: String,
    private: Boolean = false,
    defaultValue: Boolean,
    @StringRes summaryId: Int = -1,
    @StringRes titleId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : Pref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        BooleanPreference(pref = pref as BooleanPref, index = index, groupSize = groupSize)
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
) {
    var state: Boolean? by mutableStateOf(null)
    var value: Boolean
        get() {
            if (state == null)
                state = prefFlag(key, defaultValue as Boolean, private)
            return state!!
        }
        set(value) {
            state = value
            setPrefFlag(key, value, private)
        }

    override fun toString(): String = value.toString()
}

class IntPref(
    key: String,
    private: Boolean = false,
    defaultValue: Int,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    val entries: List<Int>,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : Pref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        IntPreference(pref = pref as IntPref, index = index, groupSize = groupSize)
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
) {
    var state: Int? by mutableStateOf(null)
    var value: Int
        get() {
            if (state == null)
                state = prefInt(key, defaultValue as Int, private)
            return state!!
        }
        set(value) {
            state = value
            setPrefInt(key, value, private)
        }

    override fun toString(): String = value.toString()
}

open class StringPref(
    key: String,
    private: Boolean = false,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : Pref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        StringPreference(
            pref = pref as StringPref,
            index = index,
            groupSize = groupSize,
            onClick = { onDialogUI(pref) },
        )
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
) {
    var state: String? by mutableStateOf(null)
    var value: String
        get() {
            if (state == null)
                state = prefString(key, defaultValue as String, private)
            return state!!
        }
        set(value) {
            state = value
            setPrefString(key, value, private)
        }

    override fun toString(): String = value
}

class StringEditPref(
    key: String,
    private: Boolean = true,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : StringPref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, _, index, groupSize ->
        StringEditPreference(pref = pref as StringEditPref, index = index, groupSize = groupSize)
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
)


class PasswordPref(
    key: String,
    private: Boolean = true,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : StringPref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        PasswordPreference(
            pref = pref as PasswordPref,
            index = index,
            groupSize = groupSize,
            onClick = { onDialogUI(pref) },
        )
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
)

class KeyPref(
    key: String,
    private: Boolean = true,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : StringPref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI,
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
)

class ListPref(
    key: String,
    private: Boolean = false,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    val entries: Map<String, String>,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : StringPref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        ListPreference(
            pref = pref as ListPref,
            index = index,
            groupSize = groupSize,
            onClick = { onDialogUI(pref) },
        )
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
)

class EnumPref(
    key: String,
    private: Boolean = false,
    defaultValue: Int,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    val entries: Map<Int, Int>,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : Pref(
    key = key,
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        EnumPreference(
            pref = pref as EnumPref,
            index = index,
            groupSize = groupSize,
            onClick = { onDialogUI(pref) },
        )
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
) {
    var state: Int? by mutableStateOf(null)
    var value: Int
        get() {
            if (state == null)
                state = prefInt(key, defaultValue as Int, private)
            return state!!
        }
        set(value) {
            state = value
            setPrefInt(key, value, private)
        }

    override fun toString(): String = value.toString()
}


class LinkPref(
    key: String,
    private: Boolean = false,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
) : Pref(
    key = key,
    private = private,
    defaultValue = null,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI,
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
)


class LaunchPref(
    key: String,
    private: Boolean = false,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((Pref) -> Color)? = null,
    enableIf: (() -> Boolean)? = null,
    onChanged: (suspend (Pref) -> Unit)? = null,
    val onClick: () -> Unit = {},
) : Pref(
    key = key,
    private = private,
    defaultValue = null,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, _, index, groupSize ->
        LaunchPreference(
            pref = pref as LaunchPref,
            index = index,
            groupSize = groupSize,
            summary = pref.summary,
            onClick = pref.onClick
        )
    },
    icon = icon,
    iconTint = iconTint,
    enableIf = enableIf,
    onChanged = onChanged
)

class NeoPrefAdapter(val dsPref: PrefDelegate<out Any>) : Pref(
    key = dsPref.key,
    private = dsPref.private,
    defaultValue = dsPref.defaultValue,
    titleId = dsPref.titleId,
    summaryId = dsPref.summaryId,
    summary = dsPref.summary,
    icon = dsPref.icon,
    iconTint = dsPref.iconTint?.let { tint -> { p -> tint(dsPref) } },
    enableIf = dsPref.enableIf,
    onChanged = {
        dsPref.onChange?.invoke(dsPref)
    },
    UI = { pref, onDialogUI, index, groupSize ->
        dsPref.UI?.invoke(dsPref, {
            onDialogUI(NeoPrefAdapter(dsPref))
        }, index, groupSize)
    },
    group = dsPref.group,
) {
    init {
        dirty.value = true
    }
}