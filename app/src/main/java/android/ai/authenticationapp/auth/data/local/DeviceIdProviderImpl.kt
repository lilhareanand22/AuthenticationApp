package android.ai.authenticationapp.auth.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private val Context.deviceIdDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_id_prefs")

/**
 * Data layer: Concrete implementation of [DeviceIdProvider] backed by DataStore.
 * Generates and persists a stable UUID on the first call.
 */
class DeviceIdProviderImpl(context: Context) : DeviceIdProvider {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    companion object {
        private val DEVICE_ID_KEY = stringPreferencesKey("app_device_id")
    }

    override suspend fun getDeviceId(): String {
        // Read without lock first for performance
        val existingId = readId()
        if (existingId != null) return existingId

        // Lock to ensure only one UUID is generated on concurrent first access
        return mutex.withLock {
            // Double-check inside lock
            val idInsideLock = readId()
            if (idInsideLock != null) {
                return@withLock idInsideLock
            }

            val newId = UUID.randomUUID().toString()
            appContext.deviceIdDataStore.edit { prefs ->
                prefs[DEVICE_ID_KEY] = newId
            }
            newId
        }
    }

    private suspend fun readId(): String? {
        val prefs = appContext.deviceIdDataStore.data.first()
        return prefs[DEVICE_ID_KEY]
    }
}
