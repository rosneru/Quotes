package de.tysw.quotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map


private val Context.dataStore by preferencesDataStore("settings")

object SettingsPrefs {
    private val KEY_URL = stringPreferencesKey("url")

    suspend fun saveUrl(context: Context, url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_URL] = url
        }
    }

    fun readUrl(context: Context) =
        context.dataStore.data.map { prefs ->
            prefs[KEY_URL] ?: ""
        }
}
