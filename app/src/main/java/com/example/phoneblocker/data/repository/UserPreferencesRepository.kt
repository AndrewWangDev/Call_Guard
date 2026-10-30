package com.example.phoneblocker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val IS_BLOCKING_ENABLED = booleanPreferencesKey("is_blocking_enabled")
    private val IS_BLOCK_UNKNOWN_ENABLED = booleanPreferencesKey("is_block_unknown_enabled")

    val isBlockingEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_BLOCKING_ENABLED] ?: false
        }

    val isBlockUnknownEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_BLOCK_UNKNOWN_ENABLED] ?: false
        }

    suspend fun saveBlockingEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[IS_BLOCKING_ENABLED] = enabled
        }
    }

    suspend fun saveBlockUnknownEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[IS_BLOCK_UNKNOWN_ENABLED] = enabled
        }
    }
}
