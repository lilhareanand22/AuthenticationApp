package android.ai.authenticationapp.auth.data.local

import android.content.Context
import android.ai.authenticationapp.auth.domain.model.Session
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionMetadataDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_metadata_prefs")

/**
 * Data layer: Concrete implementation of [SessionMetadataStore] backed by DataStore.
 * Persists session metadata separately from encrypted credentials.
 */
class SessionMetadataStoreImpl(context: Context) : SessionMetadataStore {

    private val appContext = context.applicationContext

    companion object {
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
    }

    override suspend fun save(session: Session) {
        appContext.sessionMetadataDataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = session.sessionId
            prefs[KEY_DEVICE_ID] = session.deviceId
            prefs[KEY_USER_ID] = session.userId
        }
    }

    override suspend fun get(): Session? {
        val prefs = appContext.sessionMetadataDataStore.data.first()
        val sessionId = prefs[KEY_SESSION_ID]
        val deviceId = prefs[KEY_DEVICE_ID]
        val userId = prefs[KEY_USER_ID]

        // All fields must be present to form a valid Session metadata object
        if (sessionId == null || deviceId == null || userId == null) {
            return null
        }

        return Session(
            sessionId = sessionId,
            deviceId = deviceId,
            userId = userId
        )
    }

    override suspend fun clear() {
        appContext.sessionMetadataDataStore.edit { prefs ->
            prefs.remove(KEY_SESSION_ID)
            prefs.remove(KEY_DEVICE_ID)
            prefs.remove(KEY_USER_ID)
        }
    }
}
