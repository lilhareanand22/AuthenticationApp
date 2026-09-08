package android.ai.authenticationapp.auth.security

/**
 * Security layer: Handles security-sensitive operations such as credential storage and biometric authentication abstractions.
 */
interface BiometricManager {
    fun isBiometricAvailable(): Boolean
    // TODO: Add biometric authentication methods
}
