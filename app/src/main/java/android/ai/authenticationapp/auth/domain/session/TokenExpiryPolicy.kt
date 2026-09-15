package android.ai.authenticationapp.auth.domain.session

import android.annotation.SuppressLint
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.Instant

/**
 * Domain layer: Policy defining safety thresholds for token refresh decisions.
 */
data class TokenExpiryPolicy(
    val refreshSafetyWindow: Duration,
)

/**
 * Determines whether credentials are effectively expired or within the refresh safety window.
 * Returns true when now >= accessTokenExpiresAt - safetyWindow.
 */

@RequiresApi(Build.VERSION_CODES.O)
fun Credentials.isEffectivelyExpired(
    now: Instant,
    safetyWindow: Duration,
): Boolean {
    val threshold = accessTokenExpiresAt.minus(safetyWindow)
    return !now.isBefore(threshold)
}
