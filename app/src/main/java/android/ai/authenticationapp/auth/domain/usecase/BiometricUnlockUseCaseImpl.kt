package android.ai.authenticationapp.auth.domain.usecase

import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.lock.LocalLockManager
import android.ai.authenticationapp.auth.security.BiometricAuthenticator
import android.ai.authenticationapp.auth.security.BiometricAvailability
import android.ai.authenticationapp.auth.security.BiometricResult
import androidx.fragment.app.FragmentActivity

class BiometricUnlockUseCaseImpl(
    private val biometricPreferenceStore: BiometricPreferenceStore,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val localLockManager: LocalLockManager
) : BiometricUnlockUseCase {

    override suspend fun invoke(activity: FragmentActivity?): BiometricUnlockResult {
        // 1. Check biometric preference
        val isEnabled = biometricPreferenceStore.isEnabled()
        if (!isEnabled) {
            return BiometricUnlockResult.NotEnabled
        }

        // 2. Check biometric availability
        val availability = biometricAuthenticator.checkAvailability()
        if (availability != BiometricAvailability.Available) {
            return BiometricUnlockResult.NotAvailable
        }

        // 3. Call BiometricAuthenticator.authenticate()
        val result = biometricAuthenticator.authenticate(
            activity = activity,
            title = "Unlock App",
            subtitle = "Use biometrics to unlock"
        )

        // 4. Handle result
        return when (result) {
            is BiometricResult.Success -> {
                localLockManager.unlock()
                BiometricUnlockResult.Success
            }
            is BiometricResult.Cancelled -> {
                BiometricUnlockResult.Cancelled
            }
            is BiometricResult.NotAvailable,
            is BiometricResult.NotEnrolled -> {
                BiometricUnlockResult.NotAvailable
            }
            is BiometricResult.Error -> {
                BiometricUnlockResult.Error(result.message)
            }
            else -> {
                // Catch any other result (e.g. Failed, LockedOut)
                BiometricUnlockResult.Error("Authentication failed")
            }
        }
    }
}
