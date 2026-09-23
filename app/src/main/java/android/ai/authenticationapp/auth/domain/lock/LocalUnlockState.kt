package android.ai.authenticationapp.auth.domain.lock

/**
 * Domain layer: Represents the local screen/app lock state.
 * This is strictly separate from the backend AuthenticationState.
 */
sealed interface LocalUnlockState {
    data object Locked : LocalUnlockState
    data object Unlocked : LocalUnlockState
}
