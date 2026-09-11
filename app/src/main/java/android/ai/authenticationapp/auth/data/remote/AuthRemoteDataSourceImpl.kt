package android.ai.authenticationapp.auth.data.remote

import android.ai.authenticationapp.auth.data.remote.dto.CurrentUserResponseDto
import android.ai.authenticationapp.auth.data.remote.dto.LoginRequestDto
import android.ai.authenticationapp.auth.data.remote.dto.LoginResponseDto
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenRequestDto
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenResponseDto

/**
 * Data layer: Implementation of [AuthRemoteDataSource] using [AuthApi].
 */
class AuthRemoteDataSourceImpl(
    private val authApi: AuthApi,
) : AuthRemoteDataSource {

    override suspend fun login(
        request: LoginRequestDto,
    ): LoginResponseDto {
        return authApi.login(request)
    }

    override suspend fun getCurrentUser(): CurrentUserResponseDto {
        return authApi.getCurrentUser()
    }

    override suspend fun refreshToken(
        request: RefreshTokenRequestDto,
    ): RefreshTokenResponseDto {
        return authApi.refreshToken(request)
    }
}
