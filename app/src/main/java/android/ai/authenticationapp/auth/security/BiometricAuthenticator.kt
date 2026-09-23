package android.ai.authenticationapp.auth.security

import androidx.fragment.app.FragmentActivity

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
     * @param activity Required by AndroidX Biometric API to attach the dialog.
     * @param title The title for the prompt.
     * @param subtitle Optional subtitle for the prompt.
     * @return The result of the authentication attempt.
     */
    suspend fun authenticate(
        activity: FragmentActivity?,
        title: String,
        subtitle: String? = null
    ): BiometricResult
}
