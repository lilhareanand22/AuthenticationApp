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
 */
class AuthAuthenticator(
    private val tokenManager: TokenManager,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Enforce retry-once by checking if the response has a priorResponse.
        // If priorResponse is not null, it means we already tried to authenticate this request.
        val path = response.request.url.encodedPath

        if (path.endsWith("/auth/refresh")) {
            return null
        }

        if (responseCount(response) >= 2) {
            return null
        }

        // Bridge synchronous OkHttp authenticator with suspend function using runBlocking
        val newToken = runBlocking {
            tokenManager.refresh()
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