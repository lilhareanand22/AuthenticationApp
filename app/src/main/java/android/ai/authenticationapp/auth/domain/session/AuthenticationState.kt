package android.ai.authenticationapp.auth.domain.session

import android.ai.authenticationapp.auth.domain.model.User

/**
 * Domain layer: Represents the application-wide backend session authentication state.
 */
sealed interface AuthenticationState {
    data object Unknown : AuthenticationState
    data object Unauthenticated : AuthenticationState
    data class Authenticated(val user: User) : AuthenticationState
}
