package android.ai.authenticationapp.auth.domain.model

/**
 * Domain layer: Represents the local device unlock state (e.g., biometric gatekeeper).
 * Distinct from [AuthenticationState] which represents server session state.
 */
sealed interface LocalUnlockState {
    data object NotRequired : LocalUnlockState
    data object Locked : LocalUnlockState
    data object Unlocking : LocalUnlockState
    data object Unlocked : LocalUnlockState
}
