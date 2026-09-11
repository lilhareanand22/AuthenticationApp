package android.ai.authenticationapp.auth.data.session

import java.time.Instant

/**
 * Data layer: Temporary session metadata provider interface for DummyJSON integration.
 */
interface SessionMetadataProvider {

    fun getDeviceId(): String

    fun getSessionId(): String

    fun getAccessTokenExpiresAt(): Instant
}
