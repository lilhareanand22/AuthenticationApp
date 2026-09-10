package android.ai.authenticationapp.auth.data.remote.dto

/**
 * Data layer: API request DTO for login.
 */
data class LoginRequestDto(
    val username: String,
    val password: String,
    val expiresInMins: Int? = null,
)
