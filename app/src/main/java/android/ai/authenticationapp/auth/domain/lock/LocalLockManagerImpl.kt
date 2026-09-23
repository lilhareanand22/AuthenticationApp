package android.ai.authenticationapp.auth.domain.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Domain layer: Concrete implementation of [LocalLockManager].
 */
class LocalLockManagerImpl : LocalLockManager {

    private val _state = MutableStateFlow<LocalUnlockState>(LocalUnlockState.Unlocked)
    override val state: StateFlow<LocalUnlockState> = _state.asStateFlow()

    override fun lock() {
        _state.value = LocalUnlockState.Locked
    }

    override fun unlock() {
        _state.value = LocalUnlockState.Unlocked
    }
}
