package android.ai.authenticationapp.auth.data.repository

import android.ai.authenticationapp.auth.data.remote.AuthRemoteDataSource
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.repository.AuthRepository

/**
 * Data layer: Implements repositories, data sources, and network APIs to fetch and persist data.
 */
class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource
) : AuthRepository {
    override suspend fun login(credentials: Credentials): AuthSession {
        TODO("Not yet implemented")
    }

    override suspend fun logout() {
        TODO("Not yet implemented")
    }

    override suspend fun restoreSession(): AuthSession? {
        TODO("Not yet implemented")
    }
}
