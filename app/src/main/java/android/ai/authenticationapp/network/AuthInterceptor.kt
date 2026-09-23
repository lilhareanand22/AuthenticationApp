package android.ai.authenticationapp.network

import android.ai.authenticationapp.auth.domain.session.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Network layer: Injects the authentication token into outgoing HTTP requests.
 * Uses a provider lambda to prevent circular dependency issues during DI graph construction.
 */
class AuthInterceptor(
    private val tokenManagerProvider: () -> TokenManager,
) : Interceptor {

    // Secondary constructor for direct TokenManager injection (e.g., in unit tests)
    constructor(tokenManager: TokenManager) : this({ tokenManager })

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Do not intercept login or refresh endpoints as they do not require/use the standard Bearer token
        if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh")) {
            return chain.proceed(request)
        }

        // Bridge synchronous OkHttp interceptor with suspend function using runBlocking.
        // Lazy evaluation of tokenManager ensures DI graph is fully initialized.
        val token = runBlocking {
            tokenManagerProvider().getValidAccessToken()
        }

        if (token != null) {
            val authenticatedRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            return chain.proceed(authenticatedRequest)
        }

        return chain.proceed(request)
    }
}
