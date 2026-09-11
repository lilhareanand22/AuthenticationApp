package android.ai.authenticationapp.auth.data.repository

import android.ai.authenticationapp.auth.data.remote.AuthRemoteDataSource
import android.ai.authenticationapp.auth.data.remote.dto.CurrentUserResponseDto
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenRequestDto
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenResponseDto
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
 * Data layer: Implements repositories, data sources, and network APIs to fetch and persist data.
 */
class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionMetadataProvider: SessionMetadataProvider,
) : AuthRepository {
    override suspend fun login(
        request: LoginRequest,
    ): AuthSession {
        val response = remoteDataSource.login(request.toDto())

        val user = response.toUser()

        val credentials = response.toCredentials(
            accessTokenExpiresAt = sessionMetadataProvider.getAccessTokenExpiresAt(),
        )

        val session = response.toSession(
            sessionId = sessionMetadataProvider.getSessionId(),
            deviceId = sessionMetadataProvider.getDeviceId(),
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
