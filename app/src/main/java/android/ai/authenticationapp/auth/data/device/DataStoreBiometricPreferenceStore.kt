package android.ai.authenticationapp.auth.data.device

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import kotlinx.coroutines.flow.first

/**
 * Data layer: Concrete implementation of [BiometricPreferenceStore] backed by Preferences DataStore.
 */
class DataStoreBiometricPreferenceStore(
    private val dataStore: DataStore<Preferences>
) : BiometricPreferenceStore {

    companion object {
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    override suspend fun isEnabled(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[BIOMETRIC_ENABLED_KEY] ?: false
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }
}
