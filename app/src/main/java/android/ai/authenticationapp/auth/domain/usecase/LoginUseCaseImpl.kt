package android.ai.authenticationapp.auth.domain.usecase


import android.ai.authenticationapp.auth.data.local.SessionMetadataStore
import android.ai.authenticationapp.auth.domain.device.DeviceIdProvider
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
    private val deviceIdProvider: DeviceIdProvider,
    private val credentialStore: CredentialStore,
    private val sessionMetadataStore: SessionMetadataStore,
    private val sessionManager: SessionManager,
) : LoginUseCase {

    override suspend fun invoke(
        email: String,
        password: String
    ): AuthSession {

        val deviceId = deviceIdProvider.getDeviceId()

        val request = LoginRequest(
            email = email,
            password = password,
            deviceId = deviceId
        )

        val authSession = authRepository.login(request)

        credentialStore.save(authSession.credentials)

        sessionMetadataStore.save(authSession.session)

        sessionManager.onLogin(authSession)

        return authSession
    }
}