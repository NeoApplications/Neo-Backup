package tests.research

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException

interface IPreferenceDataStore {
    suspend fun <T> getPrefFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T>
    suspend fun <T> getPref(key: Preferences.Key<T>, defaultValue: T): T
    suspend fun <T> putPref(key: Preferences.Key<T>, value: T)
    suspend fun <T> removePref(key: Preferences.Key<T>)
    suspend fun <T> clearPrefs()
}

private val Context.dataStore by preferencesDataStore(
    name = "settings",
)

class PreferencesDataStore : IPreferenceDataStore {

    val store = context.dataStore

    companion object {
        val intKey = intPreferencesKey("int")
        val boolKey = booleanPreferencesKey("bool")
        val strKey = stringPreferencesKey("str")
    }

    /* This returns us a flow of data from DataStore.
    Basically as soon we update the value in Datastore,
    the values returned by it also changes. */
    override suspend fun <T> getPrefFlow(key: Preferences.Key<T>, defaultValue: T):
            Flow<T> = store.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        val result = preferences[key] ?: defaultValue
        result
    }

    /* This returns the last saved value of the key. If we change the value,
        it wont effect the values produced by this function */
    override suspend fun <T> getPref(key: Preferences.Key<T>, defaultValue: T):
            T = store.data.first()[key] ?: defaultValue

    // This Sets the value based on the value passed in value parameter.
    override suspend fun <T> putPref(key: Preferences.Key<T>, value: T) {
        store.edit { preferences ->
            preferences[key] = value
        }
    }

    // This Function removes the Key Value pair from the datastore, hereby removing it completely.
    override suspend fun <T> removePref(key: Preferences.Key<T>) {
        store.edit { preferences ->
            preferences.remove(key)
        }
    }

    // This function clears the entire Preference Datastore.
    override suspend fun <T> clearPrefs() {
        store.edit { preferences ->
            preferences.clear()
        }
    }
}

val prefStore = PreferencesDataStore()

open class DPref

class DPrefInt(name: String, var default: Int) : DPref() {

    val key = intPreferencesKey(name)

    var value
        get() = runBlocking { prefStore.getPref(key, default) }
        set(value) = runBlocking { prefStore.putPref(key, value) }
}

class DPrefBoolean(name: String, var default: Boolean) : DPref() {

    val key = booleanPreferencesKey(name)

    var value
        get() = runBlocking { prefStore.getPref(key, default) }
        set(value) = runBlocking { prefStore.putPref(key, value) }
}

class DPrefString(name: String, var default: String) : DPref() {

    val key = stringPreferencesKey(name)

    var value
        get() = runBlocking { prefStore.getPref(key, default) }
        set(value) = runBlocking { prefStore.putPref(key, value) }
}

class Try_DataStore {

    @Test
    fun test_Prefs() {

        val int = DPrefInt("int", 123)
        val bool = DPrefBoolean("bool", false)
        val str = DPrefString("str", "abc")

        val newInt = 777
        val newBool = true
        val newStr = "xyz"

        assert(int.value in listOf(int.default, newInt))
        assert(bool.value in listOf(bool.default, newBool))
        assert(str.value in listOf(str.default, newStr))

        int.value = newInt
        bool.value = newBool
        str.value = newStr

        assertEquals(newInt, int.value)
        assertEquals(newBool, bool.value)
        assertEquals(newStr, str.value)

        runBlocking {
            println(prefStore.store.data.first().toString())
        }
    }
}
