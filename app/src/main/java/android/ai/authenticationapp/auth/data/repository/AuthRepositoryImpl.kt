package android.ai.authenticationapp.auth.data.repository

import android.ai.authenticationapp.auth.data.local.DeviceIdProvider
import android.ai.authenticationapp.auth.data.local.SessionMetadataStore
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
import java.util.UUID

/**
 * Data layer: Implements repositories, data sources, and network APIs to fetch and persist data.
 */
class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionMetadataProvider: SessionMetadataProvider,
    private val deviceIdProvider: DeviceIdProvider,
    private val sessionMetadataStore: SessionMetadataStore,
) : AuthRepository {
    
    override suspend fun login(
        request: LoginRequest,
    ): AuthSession {
        // Enforce the requirement that the multi-device integration passes deviceId downwards.
        // If the domain model does not specify it, we grab it from the DeviceIdProvider.
        // We do not modify the actual DummyJSON LoginRequestDto to include deviceId 
        // since the backend contract does not support it, but we preserve it into the Session metadata.
        val deviceId = deviceIdProvider.getDeviceId()

        val response = remoteDataSource.login(request.toDto())

        val user = response.toUser()

        val credentials = response.toCredentials(
            accessTokenExpiresAt = sessionMetadataProvider.getAccessTokenExpiresAt(),
        )

        // DummyJSON adapter/demo strategy: Android cannot generate production session IDs,
        // but since DummyJSON returns none, we fake it at the adapter mapping layer to satisfy domain rules.
        // We log it as a documented limitation.
        val fakeSessionId = UUID.randomUUID().toString() 
        val session = response.toSession(
            sessionId = fakeSessionId,
            deviceId = deviceId,
        )

        // Persist the session metadata separate from the credentials
        sessionMetadataStore.save(session)

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
