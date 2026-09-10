package android.ai.authenticationapp.auth.data.remote.dto

/**
 * Data layer: API response DTO for the current user endpoint (/auth/me).
 */
data class CurrentUserResponseDto(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
)
