package android.ai.authenticationapp.auth.data.local

import android.ai.authenticationapp.auth.domain.model.Session

/**
 * Data layer: Secure persistence abstraction for storing non-secret session metadata.
 */
interface SessionMetadataStore {
    suspend fun save(session: Session)
    suspend fun get(): Session?
    suspend fun clear()
}
