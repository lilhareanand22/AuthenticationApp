package android.ai.authenticationapp.auth.presentation

/**
 * Presentation layer: Manages UI state and user interactions, presenting data from the domain layer using Jetpack Compose.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)
