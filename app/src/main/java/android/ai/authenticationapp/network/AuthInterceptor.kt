package android.ai.authenticationapp.network

import android.ai.authenticationapp.auth.domain.session.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Network layer: Configures network clients, interceptors, and authenticators for API communication.
 * Injects the authentication token into outgoing requests.
 */
class AuthInterceptor(
    private val tokenManager: TokenManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Do not intercept login or refresh endpoints as they do not require/use the standard Bearer token
        if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh")) {
            return chain.proceed(request)
        }

        // Bridge synchronous OkHttp interceptor with suspend function using runBlocking.
        // runBlocking respects thread interruption, preserving OkHttp's cancellation mechanism.
        val token = runBlocking {
            tokenManager.getValidAccessToken()
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
