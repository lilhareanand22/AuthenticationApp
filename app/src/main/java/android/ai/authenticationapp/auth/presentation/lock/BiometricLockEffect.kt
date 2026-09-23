package android.ai.authenticationapp.auth.presentation.lock

sealed interface BiometricLockEffect {
    data object UnlockSuccess : BiometricLockEffect
}
