package android.ai.authenticationapp.auth.data.local

/**
 * Data layer: Provides a stable unique identifier for the application installation.
 */
interface DeviceIdProvider {
    suspend fun getDeviceId(): String
}
