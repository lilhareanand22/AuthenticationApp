package android.ai.authenticationapp.auth.domain.usecase

import android.ai.authenticationapp.auth.domain.model.AuthSession

/**
 * Domain layer: Coordinates application login logic.
 */
interface LoginUseCase {
    suspend operator fun invoke(
        email: String,
        password: String
    ): AuthSession
}
