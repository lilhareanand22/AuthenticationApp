package android.ai.authenticationapp.auth.domain.usecase

import androidx.fragment.app.FragmentActivity

interface BiometricUnlockUseCase {
    suspend operator fun invoke(activity: FragmentActivity?): BiometricUnlockResult
}
