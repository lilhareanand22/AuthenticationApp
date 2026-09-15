package android.ai.authenticationapp.auth.domain.session

import android.annotation.SuppressLint
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.security.CredentialStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Domain layer: Implementation of [TokenManager] handling token lifecycle, validation, and refresh coordination with single-flight Mutex.
 */
@SuppressLint("NewApi")
class TokenManagerImpl(
    private val credentialStore: CredentialStore,
    private val authRepository: AuthRepository,
    private val tokenExpiryPolicy: TokenExpiryPolicy,
    private val nowProvider: () -> Instant = { Instant.now() },
) : TokenManager {

    private val mutex = Mutex()

    override suspend fun getValidAccessToken(): String? {
        val credentials = credentialStore.get() ?: return null
        val now = nowProvider()

        if (credentials.isEffectivelyExpired(now, tokenExpiryPolicy.refreshSafetyWindow)) {
            return refresh()
        }

        return credentials.accessToken
    }

    override suspend fun refresh(): String? = mutex.withLock {
        // Double-check: Read credentials from CredentialStore again after acquiring lock
        val credentials = credentialStore.get() ?: return@withLock null
        val now = nowProvider()

        // Check if the newly-read credentials are still effectively expired
        if (!credentials.isEffectivelyExpired(now, tokenExpiryPolicy.refreshSafetyWindow)) {
            // Another concurrent caller already refreshed and saved new credentials!
            return@withLock credentials.accessToken
        }

        // Still expired, perform network refresh
        val newCredentials = authRepository.refreshToken(credentials.refreshToken)
        credentialStore.save(newCredentials)
        return@withLock newCredentials.accessToken
    }

    override suspend fun clear() = mutex.withLock {
        credentialStore.clear()
    }
}
