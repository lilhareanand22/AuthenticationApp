package android.ai.authenticationapp.auth.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Domain layer: Contains core business logic, domain models, use cases, and repository interfaces, independent of UI or data frameworks.
 */
data class Credentials @OptIn(ExperimentalTime::class) constructor(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Instant
)
