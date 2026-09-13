package android.ai.authenticationapp.auth.data.remote

import android.ai.authenticationapp.auth.data.remote.dto.CurrentUserResponseDto
import android.ai.authenticationapp.auth.data.remote.dto.LoginRequestDto
import android.ai.authenticationapp.auth.data.remote.dto.LoginResponseDto
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenRequestDto
import android.ai.authenticationapp.auth.data.remote.dto.RefreshTokenResponseDto
import android.ai.authenticationapp.auth.data.remote.error.AuthOperation
import android.ai.authenticationapp.auth.data.remote.error.NetworkErrorMapper
import kotlinx.coroutines.CancellationException

/**
 * Data layer: Implementation of [AuthRemoteDataSource] using [AuthApi].
 */
class AuthRemoteDataSourceImpl(
    private val authApi: AuthApi,
    private val errorMapper: NetworkErrorMapper,
) : AuthRemoteDataSource {

    override suspend fun login(
        request: LoginRequestDto,
    ): LoginResponseDto {
        return try {
            authApi.login(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw errorMapper.map(
                e,
                AuthOperation.LOGIN,
            )
        }
    }

    override suspend fun getCurrentUser(): CurrentUserResponseDto {
        return try {
            authApi.getCurrentUser()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw  errorMapper.map(
                throwable = e,
                AuthOperation.GET_CURRENT_USER
            )
        }
    }

    override suspend fun refreshToken(
        request: RefreshTokenRequestDto,
    ): RefreshTokenResponseDto {
        return try {
            authApi.refreshToken(request)
        } catch ( e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw errorMapper.map(
                throwable = e,
                AuthOperation.REFRESH
            )
        }
    }
}
