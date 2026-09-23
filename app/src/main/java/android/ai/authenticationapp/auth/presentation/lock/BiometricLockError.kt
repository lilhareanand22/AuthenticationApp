package android.ai.authenticationapp.auth.presentation.lock

sealed interface BiometricLockError {
    data object NotAvailable : BiometricLockError
    data object Cancelled : BiometricLockError
    data class Unknown(val message: String?) : BiometricLockError
}
