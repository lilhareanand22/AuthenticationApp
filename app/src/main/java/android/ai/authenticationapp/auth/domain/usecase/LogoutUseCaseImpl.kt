package android.ai.authenticationapp.auth.domain.usecase

import android.ai.authenticationapp.auth.data.local.SessionMetadataStore
import android.ai.authenticationapp.auth.domain.lock.LocalLockManager
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.domain.session.TokenManager
import kotlinx.coroutines.CancellationException

class LogoutUseCaseImpl(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val sessionMetadataStore: SessionMetadataStore,
    private val sessionManager: SessionManager,
    private val localLockManager: LocalLockManager
) : LogoutUseCase {

    override suspend fun invoke() {
        val session = sessionMetadataStore.get()

        if (session != null) {
            try {
                authRepository.logout(session.sessionId, session.deviceId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // If AuthRepository.logout() throws Network, Server, or Unknown errors, 
                // the user should STILL be logged out locally. We swallow these exceptions 
                // to ensure the local persistence wipe below always executes.
            }
        }

        // TokenManager natively owns CredentialStore clearing to invalidate any active refreshes.
        tokenManager.clear()
        
        sessionMetadataStore.clear()

        localLockManager.unlock()
        
        sessionManager.onLogout()
    }
}
