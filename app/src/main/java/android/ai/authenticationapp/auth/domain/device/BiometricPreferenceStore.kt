package android.ai.authenticationapp.auth.domain.device

/**
 * Domain layer: Abstract storage for the user's opt-in preference to use biometric authentication.
 * 
 * Note: This owns only the boolean preference toggle. 
 * [CredentialStore] owns the actual secure tokens.
 */
interface BiometricPreferenceStore {
    suspend fun isEnabled(): Boolean
    suspend fun setEnabled(enabled: Boolean)
}
