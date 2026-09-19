package android.ai.authenticationapp.auth.data.repository

import android.ai.authenticationapp.auth.data.remote.AuthRemoteDataSource
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenRequestDto
import android.ai.authenticationapp.auth.data.remote.mapper.toCredentials
import android.ai.authenticationapp.auth.data.remote.mapper.toDto
import android.ai.authenticationapp.auth.data.remote.mapper.toSession
import android.ai.authenticationapp.auth.data.remote.mapper.toUser
import android.ai.authenticationapp.auth.data.session.SessionMetadataProvider
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.LoginRequest
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.repository.AuthRepository

/**
 * Data layer: Implementation of AuthRepository representing a demo/adapter strategy for DummyJSON.
 * DUMMYJSON LIMITATION: The mock backend does not support sessionId, deviceId, or token expiry. 
 * We isolate these missing values in this specific adapter layer rather than polluting the clean domain.
 */
class DummyJsonAuthRepositoryAdapter(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionMetadataProvider: SessionMetadataProvider,
) : AuthRepository {
    
    override suspend fun login(
        request: LoginRequest,
    ): AuthSession {
        // We do not modify the actual DummyJSON LoginRequestDto to include deviceId 
        // since the backend contract does not support it. It gets dropped at the mapper layer.
        val response = remoteDataSource.login(request.toDto())

        val user = response.toUser()

        val credentials = response.toCredentials(
            accessTokenExpiresAt = sessionMetadataProvider.getAccessTokenExpiresAt(),
        )

        // DUMMYJSON LIMITATION: Android cannot generate production session IDs,
        // but DummyJSON returns none. We use the temporary SessionMetadataProvider abstraction
        // to provide a local substitute so the domain logic (Multi-Device Session) remains fully 
        // production-oriented and isn't distorted into dropping `sessionId` entirely.
        // TODO(Backend): Replace SessionMetadataProvider adapter usage with actual DTO parsed values when migrating to production API.
        val session = response.toSession(
            sessionId = sessionMetadataProvider.getSessionId(),
            deviceId = request.deviceId,
        )

        return AuthSession(
            user = user,
            session = session,
            credentials = credentials,
        )
    }

    override suspend fun getCurrentUser(): User {
        val response = remoteDataSource.getCurrentUser()
        return response.toUser()
    }

    override suspend fun refreshToken(
        refreshToken: String,
    ): Credentials {

        val response = remoteDataSource.refreshToken(
            RefreshTokenRequestDto(
                refreshToken = refreshToken,
            )
        )

        return response.toCredentials(
            accessTokenExpiresAt =
                sessionMetadataProvider.getAccessTokenExpiresAt()
        )
    }
}
