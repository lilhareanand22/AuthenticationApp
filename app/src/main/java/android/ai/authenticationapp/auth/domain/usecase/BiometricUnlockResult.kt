package android.ai.authenticationapp.auth.domain.usecase

sealed interface BiometricUnlockResult {
    data object Success : BiometricUnlockResult
    data object NotEnabled : BiometricUnlockResult
    data object NotAvailable : BiometricUnlockResult
    data object Cancelled : BiometricUnlockResult
    data class Error(val message: String?) : BiometricUnlockResult
}
