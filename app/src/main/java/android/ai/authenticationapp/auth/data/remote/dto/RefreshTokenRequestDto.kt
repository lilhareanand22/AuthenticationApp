package android.ai.authenticationapp.auth.data.remote.dto

/**
 * Data layer: API request DTO for token refresh endpoint (/auth/refresh).
 */
data class RefreshTokenRequestDto(
    val refreshToken: String,
    val expiresInMins: Int? = null,
)
