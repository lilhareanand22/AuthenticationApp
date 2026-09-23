package android.ai.authenticationapp.auth.presentation.lock

data class BiometricLockState(
    val isAuthenticating: Boolean = false,
    val error: BiometricLockError? = null
)
