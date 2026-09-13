package android.ai.authenticationapp.auth.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_auth_prefs")

/**
 * Security layer: Concrete implementation of [SecureStorage] using AndroidX Preferences DataStore.
 */
class DataStoreSecureStorage(
    context: Context,
) : SecureStorage {

    private val appContext = context.applicationContext

    override suspend fun saveEncrypted(key: String, value: String) {
        val prefsKey = stringPreferencesKey(key)
        appContext.dataStore.edit { preferences ->
            preferences[prefsKey] = value
        }
    }

    override suspend fun readEncrypted(key: String): String? {
        val prefsKey = stringPreferencesKey(key)
        val preferences = appContext.dataStore.data.first()
        return preferences[prefsKey]
    }

    override suspend fun clearAll() {
        appContext.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
