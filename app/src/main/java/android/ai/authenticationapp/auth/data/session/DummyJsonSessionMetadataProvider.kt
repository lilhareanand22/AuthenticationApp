package android.ai.authenticationapp.auth.data.session

import android.annotation.SuppressLint
import android.ai.authenticationapp.auth.domain.util.TimeProvider
import java.time.Instant
import java.util.UUID

/**
 * Data layer: Temporary adapter fulfilling [SessionMetadataProvider] limitations imposed by DummyJSON.
 */
@SuppressLint("NewApi")
class DummyJsonSessionMetadataProvider(
    private val timeProvider: TimeProvider
) : SessionMetadataProvider {

    override fun getDeviceId(): String = "" // Handled specifically in Domain / Repository

    override fun getSessionId(): String = UUID.randomUUID().toString()

    override fun getAccessTokenExpiresAt(): Instant {
        // DummyJSON does not return expiry metadata. Fake a 30 minute mock expiration.
        return timeProvider.now().plusSeconds(30 * 60)
    }
}
