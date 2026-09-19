package android.ai.authenticationapp.auth.data.device

import android.ai.authenticationapp.auth.domain.device.DeviceIdProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Data layer: Implementation of [DeviceIdProvider] that handles thread-safe initialization and generation.
 */
class DeviceIdProviderImpl(
    private val store: DeviceIdStore,
) : DeviceIdProvider {

    private val mutex = Mutex()

    override suspend fun getDeviceId(): String {
        val existingId = store.get()

        if (!existingId.isNullOrBlank()) {
            return existingId
        }

        return mutex.withLock {
            val idInsideLock = store.get()

            if (!idInsideLock.isNullOrBlank()) {
                return@withLock idInsideLock
            }

            val newId = UUID.randomUUID().toString()

            store.save(newId)

            newId
        }
    }
}