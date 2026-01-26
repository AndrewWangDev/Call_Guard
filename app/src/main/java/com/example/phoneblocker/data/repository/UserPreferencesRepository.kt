package com.example.phoneblocker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    private val IS_BLOCKING_ENABLED = booleanPreferencesKey("is_blocking_enabled")

    val isBlockingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_BLOCKING_ENABLED] ?: false
        }

    suspend fun saveBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_BLOCKING_ENABLED] = enabled
        }
    }
}
