package android.ai.authenticationapp.auth.data.device

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.deviceIdDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_id_prefs")

/**
 * Data layer: Concrete implementation of [DeviceIdStore] backed by Preferences DataStore.
 */
class DataStoreDeviceIdStore(
    private val dataStore: DataStore<Preferences>,
) : DeviceIdStore {

    override suspend fun get(): String? {
        return dataStore.data
            .first()[DEVICE_ID_KEY]
    }

    override suspend fun save(deviceId: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = deviceId
        }
    }

    companion object {
        private val DEVICE_ID_KEY =
            stringPreferencesKey("device_id")
    }
}
