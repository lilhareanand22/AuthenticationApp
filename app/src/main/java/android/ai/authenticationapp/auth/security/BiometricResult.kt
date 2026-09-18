package android.ai.authenticationapp.auth.security

/**
 * Security layer: Represents the result of a local biometric authentication attempt.
 */
sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Failed : BiometricResult
    data object Cancelled : BiometricResult
    data object NotAvailable : BiometricResult
    data object NotEnrolled : BiometricResult
    data object LockedOut : BiometricResult
    data class Error(val errorCode: Int, val errString: CharSequence) : BiometricResult
}
