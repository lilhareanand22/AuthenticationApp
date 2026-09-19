package android.ai.authenticationapp.auth.domain.usecase

import android.ai.authenticationapp.auth.data.local.SessionMetadataStore
import android.ai.authenticationapp.auth.domain.device.DeviceIdProvider
import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.LoginRequest
import android.ai.authenticationapp.auth.domain.model.Session
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.domain.session.TokenManager
import android.ai.authenticationapp.auth.security.CredentialStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FakeAuthRepository : AuthRepository {
    var capturedLoginRequest: LoginRequest? = null
    var throwException: Throwable? = null
    var authSessionToReturn: AuthSession? = null

    override suspend fun login(request: LoginRequest): AuthSession {
        capturedLoginRequest = request
        if (throwException != null) throw throwException!!
        return authSessionToReturn!!
    }

    override suspend fun getCurrentUser(): User = throw NotImplementedError()
    override suspend fun refreshToken(refreshToken: String): Credentials = throw NotImplementedError()
}

class FakeDeviceIdProvider : DeviceIdProvider {
    override suspend fun getDeviceId(): String = "test-device-id"
}

class FakeCredentialStore : CredentialStore {
    var savedCredentials: Credentials? = null
    var throwException: Throwable? = null

    override suspend fun save(credentials: Credentials) {
        if (throwException != null) throw throwException!!
        savedCredentials = credentials
    }
    override suspend fun get(): Credentials? = null
    override suspend fun clear() {}
}

class FakeSessionMetadataStore : SessionMetadataStore {
    var savedSession: Session? = null
    var throwException: Throwable? = null

    override suspend fun save(session: Session) {
        if (throwException != null) throw throwException!!
        savedSession = session
    }
    override suspend fun get(): Session? = null
    override suspend fun clear() {}
}

class FakeSessionManager : SessionManager(FakeTokenManager(), FakeAuthRepository()) {
    var capturedAuthSession: AuthSession? = null
    override fun onLogin(session: AuthSession) {
        capturedAuthSession = session
    }
}

class FakeTokenManager : TokenManager {
    override suspend fun getValidAccessToken(): String? = null
    override suspend fun refresh(): String? = null
    override suspend fun clear() {}
}


class LoginUseCaseImplTest {

    private val authRepository = FakeAuthRepository()
    private val deviceIdProvider = FakeDeviceIdProvider()
    private val credentialStore = FakeCredentialStore()
    private val sessionMetadataStore = FakeSessionMetadataStore()
    private val sessionManager = FakeSessionManager()

    private val useCase = LoginUseCaseImpl(
        authRepository,
        deviceIdProvider,
        credentialStore,
        sessionMetadataStore,
        sessionManager
    )

    private val sampleSession = AuthSession(
        user = User("1", "test@test.com", "Test"),
        session = Session("sid-123", "test-device-id", "1"),
        credentials = Credentials("acc", "ref", Instant.now())
    )

    @Test
    fun `Successful login uses DeviceIdProvider, AuthRepository receives correct request, saves metadata, and updates SessionManager`() = runTest {
        authRepository.authSessionToReturn = sampleSession

        val result = useCase("test@test.com", "password")

        // 1. DeviceIdProvider was called, and returned deviceId is placed into LoginRequest
        assertEquals("test-device-id", authRepository.capturedLoginRequest?.deviceId)
        
        // 3/4. AuthRepository receives the correct email/password
        assertEquals("test@test.com", authRepository.capturedLoginRequest?.email)
        assertEquals("password", authRepository.capturedLoginRequest?.password)

        // 5. Credentials are persisted after successful login
        assertEquals(sampleSession.credentials, credentialStore.savedCredentials)

        // 6. Session metadata is persisted after successful login
        assertEquals(sampleSession.session, sessionMetadataStore.savedSession)

        // 7. SessionManager.onLogin() is called after persistence succeeds
        assertEquals(sampleSession, sessionManager.capturedAuthSession)

        // 8. AuthSession returned by AuthRepository is returned by LoginUseCase
        assertEquals(sampleSession, result)
    }

    @Test
    fun `AuthException from AuthRepository propagates`() = runTest {
        val authException = AuthException(AuthError.InvalidCredentials)
        authRepository.throwException = authException

        var caught: Throwable? = null
        try {
            useCase("test@test.com", "wrong_password")
        } catch (e: Throwable) {
            caught = e
        }

        assertEquals(authException, caught)
        assertNull(credentialStore.savedCredentials)
        assertNull(sessionMetadataStore.savedSession)
        assertNull(sessionManager.capturedAuthSession)
    }

    @Test
    fun `Credential save failure prevents SessionMetadataStore and SessionManager execution`() = runTest {
        authRepository.authSessionToReturn = sampleSession
        val exception = RuntimeException("Storage full")
        credentialStore.throwException = exception

        var caught: Throwable? = null
        try {
            useCase("test@test.com", "password")
        } catch (e: Throwable) {
            caught = e
        }

        assertEquals(exception, caught)
        assertNull(sessionMetadataStore.savedSession) 
        assertNull(sessionManager.capturedAuthSession)
    }

    @Test
    fun `SessionMetadataStore save failure prevents SessionManager execution`() = runTest {
        authRepository.authSessionToReturn = sampleSession
        val exception = RuntimeException("Storage full")
        sessionMetadataStore.throwException = exception

        var caught: Throwable? = null
        try {
            useCase("test@test.com", "password")
        } catch (e: Throwable) {
            caught = e
        }

        assertEquals(exception, caught)
        assertEquals(sampleSession.credentials, credentialStore.savedCredentials) // Passed cred store
        assertNull(sessionManager.capturedAuthSession) // But never reached session manager
    }

    @Test
    fun `CancellationException propagates`() = runTest {
        authRepository.throwException = CancellationException("Cancelled")

        var caught: Throwable? = null
        try {
            useCase("test@test.com", "password")
        } catch (e: Throwable) {
            caught = e
        }

        assertTrue(caught is CancellationException)
        assertNull(sessionManager.capturedAuthSession)
    }
}
