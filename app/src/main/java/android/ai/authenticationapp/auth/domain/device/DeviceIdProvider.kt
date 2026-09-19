package android.ai.authenticationapp.auth.domain.device

/**
 * Domain layer: Provides a stable unique identifier for the current application installation.
 */
interface DeviceIdProvider {
    suspend fun getDeviceId(): String
}
