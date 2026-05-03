package com.machiav3lli.backup.data.preferences

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.machiav3lli.backup.ui.compose.component.BooleanPreference
import com.machiav3lli.backup.ui.compose.component.EnumPreference
import com.machiav3lli.backup.ui.compose.component.IntPreference
import com.machiav3lli.backup.ui.compose.component.ListPreference
import com.machiav3lli.backup.ui.compose.component.StringEditPreference
import com.machiav3lli.backup.ui.compose.component.StringPreference
import com.machiav3lli.backup.ui.compose.component.StringSetPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

typealias PrefUI = @Composable (pref: PrefDelegate<out Any>, onDialogPref: (PrefDelegate<out Any>) -> Unit, index: Int, groupSize: Int) -> Unit

class PrefBoolean(
    dataStore: DataStore<Preferences>,
    dataStoreKey: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    private: Boolean = false,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((PrefDelegate<out Any>) -> Color)? = null,
    onChanged: (suspend (PrefDelegate<out Any>) -> Unit)? = null,
) : PrefDelegate<Boolean>(
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        BooleanPreference(pref = pref as PrefBoolean, index = index, groupSize = groupSize)
    },
    icon = icon,
    iconTint = iconTint,
    onChange = onChanged,
    dataStore = dataStore,
    dataStoreKey = dataStoreKey,
)

class PrefInt(
    dataStore: DataStore<Preferences>,
    dataStoreKey: Preferences.Key<Int>,
    defaultValue: Int,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    private: Boolean = false,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((PrefDelegate<out Any>) -> Color)? = null,
    val entries: List<Int>,
    onChanged: (suspend (PrefDelegate<out Any>) -> Unit)? = null,
) : PrefDelegate<Int>(
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        IntPreference(pref = pref as PrefInt, index = index, groupSize = groupSize)
    },
    icon = icon,
    iconTint = iconTint,
    onChange = onChanged,
    dataStore = dataStore,
    dataStoreKey = dataStoreKey,
)

open class PrefString(
    private: Boolean = false,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((PrefDelegate<out Any>) -> Color)? = null,
    onChanged: (suspend (PrefDelegate<out Any>) -> Unit)? = null,
    dataStore: DataStore<Preferences>,
    dataStoreKey: Preferences.Key<String>,
) : PrefDelegate<String>(
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        StringPreference(
            pref = pref as PrefString,
            index = index,
            groupSize = groupSize,
            onClick = { onDialogUI(pref) },
        )
    },
    icon = icon,
    iconTint = iconTint,
    onChange = onChanged,
    dataStore = dataStore,
    dataStoreKey = dataStoreKey,
)

class PrefEditString(
    dataStore: DataStore<Preferences>,
    dataStoreKey: Preferences.Key<String>,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    private: Boolean = true,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((PrefDelegate<out Any>) -> Color)? = null,
    enableIf: () -> Boolean = { true },
    onChanged: (suspend (PrefDelegate<out Any>) -> Unit)? = null,
) : PrefString(
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, _, index, groupSize ->
        StringEditPreference(pref = pref as PrefEditString, index = index, groupSize = groupSize)
    },
    icon = icon,
    iconTint = iconTint,
    onChanged = onChanged,
    dataStore = dataStore,
    dataStoreKey = dataStoreKey,
)

class PrefList(
    dataStore: DataStore<Preferences>,
    dataStoreKey: Preferences.Key<String>,
    defaultValue: String,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    private: Boolean = false,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((PrefDelegate<out Any>) -> Color)? = null,
    val entries: Map<String, String>,
    enableIf: () -> Boolean = { true },
    onChanged: (suspend (PrefDelegate<out Any>) -> Unit)? = null,
) : PrefString(
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        ListPreference(
            pref = pref as PrefList,
            index = index,
            groupSize = groupSize,
            onClick = { onDialogUI(pref) },
        )
    },
    icon = icon,
    iconTint = iconTint,
    onChanged = onChanged,
    dataStore = dataStore,
    dataStoreKey = dataStoreKey,
)

class PrefStringSet(
    dataStore: DataStore<Preferences>,
    dataStoreKey: Preferences.Key<Set<String>>,
    defaultValue: Set<String> = emptySet(),
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    private: Boolean = false,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((PrefDelegate<out Any>) -> Color)? = null,
    onChanged: (suspend (PrefDelegate<out Any>) -> Unit)? = null,
) : PrefDelegate<Set<String>>(
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        StringSetPreference(pref = pref as PrefStringSet, index = index, groupSize = groupSize)
    },
    icon = icon,
    iconTint = iconTint,
    onChange = onChanged,
    dataStore = dataStore,
    dataStoreKey = dataStoreKey,
)

class PrefEnum(
    dataStore: DataStore<Preferences>,
    dataStoreKey: Preferences.Key<Int>,
    defaultValue: Int,
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    summary: String? = null,
    private: Boolean = false,
    UI: PrefUI? = null,
    icon: ImageVector? = null,
    iconTint: ((PrefDelegate<out Any>) -> Color)? = null,
    val entries: Map<Int, Int>,
    onChanged: (suspend (PrefDelegate<out Any>) -> Unit)? = null,
) : PrefDelegate<Int>(
    private = private,
    defaultValue = defaultValue,
    titleId = titleId,
    summaryId = summaryId,
    summary = summary,
    UI = UI ?: { pref, onDialogUI, index, groupSize ->
        EnumPreference(
            pref = pref as PrefEnum,
            index = index,
            groupSize = groupSize,
            onClick = { onDialogUI(pref) },
        )
    },
    icon = icon,
    iconTint = iconTint,
    onChange = onChanged,
    dataStore = dataStore,
    dataStoreKey = dataStoreKey,
)

abstract class PrefDelegate<T : Any>(
    private val dataStore: DataStore<Preferences>,
    private val dataStoreKey: Preferences.Key<T>,
    val defaultValue: T,
    @StringRes val titleId: Int,
    @StringRes val summaryId: Int,
    var summary: String? = null,
    val private: Boolean = false,
    val icon: ImageVector? = null,
    var iconTint: ((PrefDelegate<out Any>) -> Color)? = null,
    val UI: PrefUI? = null,
    val onChange: (suspend (PrefDelegate<out Any>) -> Unit)? = null,
) {
    val key: String = run {
        dataStoreKey.name
    }

    val group: String = run {
        val parts = dataStoreKey.name.split(".", limit = 2)
        parts.getOrElse(0) { "" }
    }

    val dirty = mutableStateOf(false)

    private val flow: Flow<T> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[dataStoreKey] ?: defaultValue
        }
        .distinctUntilChanged()

    var value: T
        get() = runBlocking(Dispatchers.IO) {
            currentValue()
        }
        set(value) = runBlocking(Dispatchers.IO) {
            set(value)
        }

    val state: State<T>
        @Composable
        get() = flow.collectAsState(initial = defaultValue)

    fun flow(): Flow<T> = flow

    suspend fun set(newValue: T) {
        if (newValue == currentValue()) return
        dataStore.edit { prefs -> prefs[dataStoreKey] = newValue }
        onChange?.invoke(this)
    }

    private suspend fun currentValue(): T {
        return flow.first()
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(dataStoreKey) }
    }

    //init {
    //    Pref.prefGroups.getOrPut(group) { mutableListOf() }.add(NeoPrefAdapter(this))
    //}
}