package android.ai.authenticationapp.auth.presentation.login

/**
 * MVI Intents representing user actions on the LoginScreen.
 */
sealed interface LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data object LoginClicked : LoginIntent
    data object BiometricClicked : LoginIntent
    data object GoogleLoginClicked : LoginIntent
}

/**
 * MVI State representing the entire UI state of the LoginScreen.
 */
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val error: LoginError? = null,
)

/**
 * Presentation-layer specific error mappings decoupled from the domain exceptions.
 */
sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data object Network : LoginError
    data object RateLimited : LoginError
    data object Server : LoginError
    data object Unknown : LoginError
}

/**
 * One-time UI effects triggered by the ViewModel.
 */
sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
}
