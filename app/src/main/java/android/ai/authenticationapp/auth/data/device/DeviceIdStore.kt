package android.ai.authenticationapp.auth.data.device

/**
 * Data layer: Persistence abstraction for the installation-specific device ID.
 */
interface DeviceIdStore {
    suspend fun get(): String?
    suspend fun save(deviceId: String)
}
