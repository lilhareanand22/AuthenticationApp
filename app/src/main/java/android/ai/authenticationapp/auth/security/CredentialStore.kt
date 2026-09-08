package android.ai.authenticationapp.auth.security

/**
 * Security layer: Handles security-sensitive operations such as credential storage and biometric authentication abstractions.
 */
interface CredentialStore {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clear()
}
