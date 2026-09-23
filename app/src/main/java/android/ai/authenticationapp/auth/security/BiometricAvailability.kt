package android.ai.authenticationapp.auth.security

/**
 * Security layer: Represents the availability of biometric hardware and enrollment on the device.
 */
sealed interface BiometricAvailability {
    data object Available : BiometricAvailability
    data object NotAvailable : BiometricAvailability
    data object NotEnrolled : BiometricAvailability
    data object SecurityUpdateRequired : BiometricAvailability
    data class Error(val code: Int, val message: String?) : BiometricAvailability
}
