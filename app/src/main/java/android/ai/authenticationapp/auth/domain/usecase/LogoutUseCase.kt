package android.ai.authenticationapp.auth.domain.usecase

import android.ai.authenticationapp.auth.domain.repository.AuthRepository

/**
 * Domain layer: Contains core business logic, domain models, use cases, and repository interfaces, independent of UI or data frameworks.
 */
class LogoutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke() {
        TODO("Not yet implemented")
    }
}
