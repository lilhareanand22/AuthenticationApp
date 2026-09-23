package android.ai.authenticationapp.auth.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Security layer: Android-specific implementation of [BiometricAuthenticator] utilizing AndroidX Biometric.
 */
class AndroidBiometricAuthenticator(
    private val biometricManager: BiometricManager,
) : BiometricAuthenticator {

    override fun checkAvailability(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NotAvailable
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.NotAvailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SecurityUpdateRequired
            else -> BiometricAvailability.Error(-1, "Unknown Biometric Error")
        }
    }

    override suspend fun authenticate(
        activity: FragmentActivity?,
        title: String,
        subtitle: String?
    ): BiometricResult = suspendCancellableCoroutine { continuation ->

        val hostActivity = activity ?: run {
            if (continuation.isActive) {
                continuation.resume(BiometricResult.Error(-1, "Activity Context Missing"))
            }
            return@suspendCancellableCoroutine
        }

        val executor = ContextCompat.getMainExecutor(hostActivity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) {
                    continuation.resume(BiometricResult.Success)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) {
                    val biometricResult = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> BiometricResult.Cancelled
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricResult.NotAvailable
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricResult.NotEnrolled
                        else -> BiometricResult.Error(errorCode, errString.toString())
                    }
                    continuation.resume(biometricResult)
                }
            }

            override fun onAuthenticationFailed() {
                // Not resuming here to allow the user to try again while prompt is visible.
                // It will eventually error out (lockout), succeed, or be cancelled by the user.
            }
        }

        val biometricPrompt = BiometricPrompt(hostActivity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }

        biometricPrompt.authenticate(promptInfo)
    }
}
