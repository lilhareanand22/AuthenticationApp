package android.ai.authenticationapp.auth.data.remote.dto

/**
 * Data layer: API response DTO for login.
 */
data class LoginResponseDto(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val accessToken: String,
    val refreshToken: String,
)
