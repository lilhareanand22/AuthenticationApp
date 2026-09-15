package android.ai.authenticationapp.auth.domain.session

/**
 * Domain layer: Centralized authentication token lifecycle manager interface.
 */
interface TokenManager {

    suspend fun getValidAccessToken(): String?

    suspend fun refresh(): String?

    suspend fun clear()
}
