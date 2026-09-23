package android.ai.authenticationapp.auth.security

/**
 * Security layer: Defines Android-specific biometric interactions, separating Android framework
 * dependencies from domain authentication logic.
 */
interface BiometricAuthenticator {
    
    /**
     * Checks if biometric authentication is available and enrolled.
     */
    fun checkAvailability(): BiometricAvailability

    /**
     * Launches the biometric prompt.
     * @param title The title for the prompt.
     * @param subtitle Optional subtitle for the prompt.
     * @return The result of the authentication attempt.
     */
    suspend fun authenticate(
        title: String,
        subtitle: String
    ): BiometricResult
}
