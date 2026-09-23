package android.ai.authenticationapp.auth.presentation.lock

import androidx.fragment.app.FragmentActivity

sealed interface BiometricLockIntent {
    data class UnlockClicked(val activity: FragmentActivity? = null) : BiometricLockIntent
}
