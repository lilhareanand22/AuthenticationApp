package android.ai.authenticationapp.auth.domain.error

/**
 * Domain layer: Represents standardized authentication errors independent of network or backend frameworks.
 */
sealed interface AuthError {

    data object InvalidCredentials : AuthError

    data object AccessTokenExpired : AuthError

    data object RefreshTokenInvalid : AuthError

    data object SessionRevoked : AuthError

    data object Forbidden : AuthError

    data object Network : AuthError

    data object RateLimited : AuthError

    data object Server : AuthError

    data object Unknown : AuthError
}

/**
 * Domain layer: Exception wrapper encapsulating an [AuthError] and an optional cause.
 */
class AuthException(
    val error: AuthError,
    cause: Throwable? = null,
) : Exception(cause)
