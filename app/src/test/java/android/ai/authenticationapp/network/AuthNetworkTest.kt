package android.ai.authenticationapp.network

import android.ai.authenticationapp.auth.domain.session.TokenManager
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class FakeNetworkTokenManager : TokenManager {
    var validToken: String? = "mock_access_token"
    var refreshTokenResult: String? = "mock_refreshed_access_token"
    var refreshCallCount = 0

    override suspend fun getValidAccessToken(): String? = validToken

    override suspend fun refresh(): String? {
        refreshCallCount++
        validToken = refreshTokenResult
        return refreshTokenResult
    }

    override suspend fun clear() {
        validToken = null
    }
}

class FakeInterceptorChain(
    private val request: Request,
    val onProceed: (Request) -> Response
) : Interceptor.Chain {
    override fun request(): Request = request
    override fun proceed(request: Request): Response = onProceed(request)
    override fun connection() = null
    override fun call() = throw NotImplementedError()
    override fun connectTimeoutMillis() = 1000
    override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
    override fun readTimeoutMillis() = 1000
    override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
    override fun writeTimeoutMillis() = 1000
    override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
}

class AuthNetworkTest {

    private val tokenManager = FakeNetworkTokenManager()

    @Test
    fun `AuthInterceptor injects Bearer token into outgoing non-auth requests`() = runTest {
        val interceptor = AuthInterceptor { tokenManager }
        val request = Request.Builder().url("https://dummyjson.com/user/profile").build()

        var capturedRequest: Request? = null
        val chain = FakeInterceptorChain(request) { req ->
            capturedRequest = req
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        interceptor.intercept(chain)

        assertEquals("Bearer mock_access_token", capturedRequest?.header("Authorization"))
    }

    @Test
    fun `AuthInterceptor skips Authorization header on login endpoint`() = runTest {
        val interceptor = AuthInterceptor { tokenManager }
        val request = Request.Builder().url("https://dummyjson.com/auth/login").build()

        var capturedRequest: Request? = null
        val chain = FakeInterceptorChain(request) { req ->
            capturedRequest = req
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        interceptor.intercept(chain)

        assertNull(capturedRequest?.header("Authorization"))
    }

    @Test
    fun `AuthAuthenticator triggers refresh on HTTP 401 and returns retried request`() = runTest {
        val authenticator = AuthAuthenticator { tokenManager }
        val initialRequest = Request.Builder()
            .url("https://dummyjson.com/protected/data")
            .header("Authorization", "Bearer mock_access_token")
            .build()

        val response401 = Response.Builder()
            .request(initialRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val retriedRequest = authenticator.authenticate(null, response401)

        assertEquals("Bearer mock_refreshed_access_token", retriedRequest?.header("Authorization"))
        assertEquals(1, tokenManager.refreshCallCount)
    }
}
