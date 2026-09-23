package android.ai.authenticationapp.auth.domain.lock

import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.session.AuthenticationState
import android.ai.authenticationapp.auth.domain.session.SessionManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Domain layer: Observes the whole application lifecycle to lock the app when backgrounded.
 * Respects user preferences and active authentication state.
 */
class AppLifecycleLocker(
    private val sessionManager: SessionManager,
    private val biometricPreferenceStore: BiometricPreferenceStore,
    private val localLockManager: LocalLockManager,
    private val applicationScope: CoroutineScope
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // If the user drops to the background, immediately lock if they are logged in and have opted in to biometrics.
        applicationScope.launch {
            val authState = sessionManager.authState.value
            if (authState is AuthenticationState.Authenticated && biometricPreferenceStore.isEnabled()) {
                localLockManager.lock()
            }
        }
    }
}
