package android.ai.authenticationapp.auth.domain.usecase

import android.ai.authenticationapp.auth.data.local.SessionMetadataStore
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.LoginRequest
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.security.CredentialStore

/**
 * Domain layer: Concrete implementation of [LoginUseCase].
 * Orchestrates backend authentication, secure persistence, and application session state updates.
 */
class LoginUseCaseImpl(
    private val authRepository: AuthRepository,
    private val credentialStore: CredentialStore,
    private val sessionMetadataStore: SessionMetadataStore,
    private val sessionManager: SessionManager,
) : LoginUseCase {

    override suspend fun invoke(email: String, password: String): AuthSession {
        // 1. Create domain LoginRequest
        val request = LoginRequest(
            email = email,
            password = password,
            deviceId = "" // Device ID injection is handled inside the Repository layer in this architecture
        )

        // 2. Call AuthRepository to perform remote authentication
        val authSession = authRepository.login(request)

        // 3 & 4. Persist credentials securely
        credentialStore.save(authSession.credentials)

        // 5. Persist non-secret session metadata
        sessionMetadataStore.save(authSession.session)

        // 6. Only after persistence succeeds, establish the runtime application session
        sessionManager.onLogin(authSession)

        // 7. Return the completed session
        return authSession
    }
}
