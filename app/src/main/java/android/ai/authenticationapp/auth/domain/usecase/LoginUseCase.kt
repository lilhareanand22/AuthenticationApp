package android.ai.authenticationapp.auth.domain.usecase

import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.repository.AuthRepository

/**
 * Domain layer: Contains core business logic, domain models, use cases, and repository interfaces, independent of UI or data frameworks.
 */
class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(credentials: Credentials): AuthSession {
        TODO("Not yet implemented")
    }
}
