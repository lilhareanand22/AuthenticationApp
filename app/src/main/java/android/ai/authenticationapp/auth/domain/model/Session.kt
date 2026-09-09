package android.ai.authenticationapp.auth.domain.model

/**
 * Domain layer: Contains core business logic, domain models, use cases, and repository interfaces, independent of UI or data frameworks.
 */
data class Session(
    val sessionId: String,
    val deviceId: String,
    val userId: String
)
