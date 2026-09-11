package android.ai.authenticationapp.auth.domain.repository

import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.LoginRequest
import android.ai.authenticationapp.auth.domain.model.User

/**
 * Domain layer: Repository interface for authentication operations.
 */
interface AuthRepository {

    suspend fun login(
        request: LoginRequest,
    ): AuthSession

    suspend fun getCurrentUser(): User

    suspend fun refreshToken(
        refreshToken: String,
    ): Credentials
}
