package android.ai.authenticationapp.network

import android.ai.authenticationapp.auth.domain.session.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Network layer: Authenticator to automatically handle HTTP 401 Unauthorized responses.
 * Triggers a centralized token refresh and retries the original request with the new token.
 * Uses a provider lambda to prevent circular dependency issues during DI graph construction.
 */
class AuthAuthenticator(
    private val tokenManagerProvider: () -> TokenManager,
) : Authenticator {

    // Secondary constructor for direct TokenManager injection (e.g., in unit tests)
    constructor(tokenManager: TokenManager) : this({ tokenManager })

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath

        if (path.endsWith("/auth/refresh")) {
            return null
        }

        if (responseCount(response) >= 2) {
            return null
        }

        // Bridge synchronous OkHttp authenticator with suspend function using runBlocking
        val newToken = runBlocking {
            tokenManagerProvider().refresh()
        }

        if (newToken == null) {
            return null
        }

        // Create a new request based on the original, replacing the Authorization header
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}

private fun responseCount(response: Response): Int {
    var result = 1
    var current = response.priorResponse

    while (current != null) {
        result++
        current = current.priorResponse
    }

    return result
}
