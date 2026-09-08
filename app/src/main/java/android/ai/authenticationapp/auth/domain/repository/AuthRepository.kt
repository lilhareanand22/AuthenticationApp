package android.ai.authenticationapp.auth.domain.repository

import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials

/**
 * Domain layer: Contains core business logic, domain models, use cases, and repository interfaces, independent of UI or data frameworks.
 */
interface AuthRepository {
    suspend fun login(credentials: Credentials): AuthSession
    suspend fun logout()
    suspend fun restoreSession(): AuthSession?
}
