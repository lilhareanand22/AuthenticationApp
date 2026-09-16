package android.ai.authenticationapp.auth.domain.session

import android.annotation.SuppressLint
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.security.CredentialStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

private data class RefreshOperation(
    val generation: Long,
    val deferred: CompletableDeferred<String?>
)

private sealed class RefreshDecision {
    data class ValidToken(val token: String) : RefreshDecision()
    data class AwaitRefresh(val deferred: CompletableDeferred<String?>) : RefreshDecision()
    data object NoCredentials : RefreshDecision()
}

/**
 * Domain layer: Implementation of [TokenManager] handling token lifecycle, validation, and refresh coordination.
 */
@SuppressLint("NewApi")
class TokenManagerImpl(
    private val credentialStore: CredentialStore,
    private val authRepository: AuthRepository,
    private val tokenExpiryPolicy: TokenExpiryPolicy,
    private val applicationScope: CoroutineScope,
    private val nowProvider: () -> Instant = { Instant.now() },
) : TokenManager {

    private val mutex = Mutex()
    private var sessionGeneration: Long = 0L
    private var activeRefresh: RefreshOperation? = null

    override suspend fun getValidAccessToken(): String? {
        val credentials = credentialStore.get() ?: return null
        val now = nowProvider()

        if (credentials.isEffectivelyExpired(now, tokenExpiryPolicy.refreshSafetyWindow)) {
            return refresh()
        }

        return credentials.accessToken
    }

    override suspend fun refresh(): String? {
        val decision = mutex.withLock {
            // 1. Read credentials
            val credentials = credentialStore.get() ?: return@withLock RefreshDecision.NoCredentials
            val now = nowProvider()

            // 2. Double-check whether credentials are still effectively expired
            if (!credentials.isEffectivelyExpired(now, tokenExpiryPolicy.refreshSafetyWindow)) {
                return@withLock RefreshDecision.ValidToken(credentials.accessToken)
            }

            // 3. Piggyback on an existing refresh operation if available
            if (activeRefresh != null) {
                return@withLock RefreshDecision.AwaitRefresh(activeRefresh!!.deferred)
            } 
            
            // 4. Create a new refresh operation
            val capturedGeneration = sessionGeneration
            val operation = RefreshOperation(
                generation = capturedGeneration,
                deferred = CompletableDeferred()
            )
            activeRefresh = operation
            
            val refreshTokenToUse = credentials.refreshToken

            // 5. Launch the network request unattached from the caller's scope
            applicationScope.launch {
                performNetworkRefresh(operation, refreshTokenToUse)
            }
            
            RefreshDecision.AwaitRefresh(operation.deferred)
        }

        // Await the result outside the Mutex
        return when (decision) {
            is RefreshDecision.ValidToken -> decision.token
            is RefreshDecision.NoCredentials -> null
            is RefreshDecision.AwaitRefresh -> decision.deferred.await()
        }
    }

    private suspend fun performNetworkRefresh(operation: RefreshOperation, refreshToken: String) {
        try {
            // Network I/O outside Mutex
            val newCredentials = authRepository.refreshToken(refreshToken)
            
            mutex.withLock {
                // Generation check prevents stale writes if logout occurred during network request
                if (operation.generation != sessionGeneration) {
                    operation.deferred.complete(null)
                    return@withLock
                }
                
                credentialStore.save(newCredentials)
                operation.deferred.complete(newCredentials.accessToken)
            }
        } catch (e: CancellationException) {
            // Preserve CancellationException strictly
            operation.deferred.completeExceptionally(e)
            throw e
        } catch (e: Throwable) {
            // Complete exceptionally for waiting callers
            operation.deferred.completeExceptionally(e)
        } finally {
            mutex.withLock {
                // Identity-based cleanup prevents an old operation from clearing a newer one
                if (activeRefresh === operation) {
                    activeRefresh = null
                }
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            sessionGeneration++
            activeRefresh?.deferred?.cancel()
            activeRefresh = null
            credentialStore.clear()
        }
    }
}
