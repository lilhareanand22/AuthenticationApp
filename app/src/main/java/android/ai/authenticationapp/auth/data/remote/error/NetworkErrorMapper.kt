package android.ai.authenticationapp.auth.data.remote.error

import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import retrofit2.HttpException
import java.io.IOException

/**
 * Data layer: Maps infrastructure exceptions (IOException, HttpException, etc.) into domain [AuthException].
 */
class NetworkErrorMapper {

    fun map(
        throwable: Throwable,
        operation: AuthOperation,
    ): AuthException {
        val authError = when (throwable) {
            is AuthException -> return throwable
            is IOException -> AuthError.Network
            is HttpException -> {
                when (throwable.code()) {
                    401 -> when (operation) {
                        AuthOperation.LOGIN -> AuthError.InvalidCredentials
                        AuthOperation.GET_CURRENT_USER -> AuthError.AccessTokenExpired
                        AuthOperation.REFRESH -> AuthError.RefreshTokenInvalid
                    }
                    403 -> AuthError.Forbidden
                    429 -> AuthError.RateLimited
                    in 500..599 -> AuthError.Server
                    else -> AuthError.Unknown
                }
            }
            else -> AuthError.Unknown
        }

        return AuthException(error = authError, cause = throwable)
    }
}
