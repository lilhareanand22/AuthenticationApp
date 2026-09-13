package android.ai.authenticationapp.auth.security

import android.ai.authenticationapp.auth.domain.model.Credentials

/**
 * Security layer: Handles secure credential persistence abstractions.
 */
interface CredentialStore {

    suspend fun save(credentials: Credentials)

    suspend fun get(): Credentials?

    suspend fun clear()
}
