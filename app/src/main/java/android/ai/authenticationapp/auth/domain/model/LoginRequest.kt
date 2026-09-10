package android.ai.authenticationapp.auth.domain.model

/**
 * Domain layer: Represents the application's authentication login request model,
 * independent of any backend API DTO contract.
 */
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String,
)
