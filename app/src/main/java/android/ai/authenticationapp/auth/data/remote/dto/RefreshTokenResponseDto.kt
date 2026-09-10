package android.ai.authenticationapp.auth.data.remote.dto

/**
 * Data layer: API response DTO for token refresh endpoint (/auth/refresh).
 */
data class RefreshTokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
)
