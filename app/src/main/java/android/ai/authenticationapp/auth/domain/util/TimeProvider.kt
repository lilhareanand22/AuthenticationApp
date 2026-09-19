package android.ai.authenticationapp.auth.domain.util

import java.time.Instant

/**
 * Domain layer: Abstract provider for current time to ensure testability across expiry math.
 */
interface TimeProvider {
    fun now(): Instant
}
