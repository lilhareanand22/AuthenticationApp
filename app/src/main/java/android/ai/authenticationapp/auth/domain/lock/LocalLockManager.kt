package android.ai.authenticationapp.auth.domain.lock

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain layer: Manages the local screen/app lock state.
 */
interface LocalLockManager {
    val state: StateFlow<LocalUnlockState>
    
    fun lock()
    fun unlock()
}
