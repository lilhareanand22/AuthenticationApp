package android.ai.authenticationapp.auth.domain.model

/**
 * Domain layer: Contains core business logic, domain models, use cases, and repository interfaces, independent of UI or data frameworks.
 */
data class User(
    val id: String,
    val email: String
)
