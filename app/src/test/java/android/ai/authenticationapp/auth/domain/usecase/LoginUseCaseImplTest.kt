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

class FakeAuthRepository(private val events: MutableList<String>? = null) : AuthRepository {
    var capturedLoginRequest: LoginRequest? = null
    var throwException: Throwable? = null
    var authSessionToReturn: AuthSession? = null

    override suspend fun login(request: LoginRequest): AuthSession {
        capturedLoginRequest = request
        events?.add("authRepository.login")
        if (throwException != null) throw throwException!!
        return authSessionToReturn!!
    }

    override suspend fun getCurrentUser(): User = throw NotImplementedError()
    override suspend fun refreshToken(refreshToken: String): Credentials = throw NotImplementedError()
}

class FakeDeviceIdProvider(private val events: MutableList<String>? = null) : DeviceIdProvider {
    override suspend fun getDeviceId(): String {
        events?.add("deviceIdProvider.getDeviceId")
        return "device-123"
    }
}

class FakeCredentialStore(private val events: MutableList<String>? = null) : CredentialStore {
    var savedCredentials: Credentials? = null
    var throwException: Throwable? = null

    override suspend fun save(credentials: Credentials) {
        events?.add("credentialStore.save")
        if (throwException != null) throw throwException!!
        savedCredentials = credentials
    }
    override suspend fun get(): Credentials? = null
    override suspend fun clear() {}
}

class FakeSessionMetadataStore(private val events: MutableList<String>? = null) : SessionMetadataStore {
    var savedSession: Session? = null
    var throwException: Throwable? = null

    override suspend fun save(session: Session) {
        events?.add("sessionMetadataStore.save")
        if (throwException != null) throw throwException!!
        savedSession = session
    }
    override suspend fun get(): Session? = null
    override suspend fun clear() {}
}

class FakeSessionManager(private val events: MutableList<String>? = null) : SessionManager(FakeTokenManager(), FakeAuthRepository()) {
    var capturedAuthSession: AuthSession? = null
    override fun onLogin(session: AuthSession) {
        events?.add("sessionManager.onLogin")
        capturedAuthSession = session
    }
}

class FakeTokenManager : TokenManager {
    override suspend fun getValidAccessToken(): String? = null
    override suspend fun refresh(): String? = null
    override suspend fun clear() {}
}

class LoginUseCaseImplTest {

    private val events = mutableListOf<String>()
    
    private val authRepository = FakeAuthRepository(events)
    private val deviceIdProvider = FakeDeviceIdProvider(events)
    private val credentialStore = FakeCredentialStore(events)
    private val sessionMetadataStore = FakeSessionMetadataStore(events)
    private val sessionManager = FakeSessionManager(events)

    private val useCase = LoginUseCaseImpl(
        authRepository,
        deviceIdProvider,
        credentialStore,
        sessionMetadataStore,
        sessionManager
    )

    private val sampleSession = AuthSession(
        user = User("user-123", "user@example.com", "Anand"),
        session = Session("session-123", "device-123", "user-123"),
        credentials = Credentials("access-token", "refresh-token", Instant.parse("2026-09-19T12:00:00Z"))
    )

    @Test
    fun `Successful login persists credentials and session and updates SessionManager with EXACT ordering`() = runTest {
        authRepository.authSessionToReturn = sampleSession

        val result = useCase("user@example.com", "password")

        // 1. result equals expected AuthSession.
        assertEquals(sampleSession, result)

        // 2. DeviceIdProvider was called (verified via events list below)
        // 3. AuthRepository received correct parameters
        assertEquals("device-123", authRepository.capturedLoginRequest?.deviceId)
        assertEquals("user@example.com", authRepository.capturedLoginRequest?.email)
        assertEquals("password", authRepository.capturedLoginRequest?.password)

        // 4. CredentialStore received expected credentials
        assertEquals(sampleSession.credentials, credentialStore.savedCredentials)

        // 5. SessionMetadataStore received expected session
        assertEquals(sampleSession.session, sessionMetadataStore.savedSession)

        // 6. SessionManager.onLogin() received expected AuthSession
        assertEquals(sampleSession, sessionManager.capturedAuthSession)

        // VERIFY ORDERING INVARIANT:
        // SessionManager.onLogin() is called only after:
        // 1. credentials are successfully persisted
        // 2. session metadata is successfully persisted
        val expectedOrder = listOf(
            "deviceIdProvider.getDeviceId",
            "authRepository.login",
            "credentialStore.save",
            "sessionMetadataStore.save",
            "sessionManager.onLogin"
        )
        
        assertEquals(expectedOrder, events)
    }

    @Test
    fun `AuthException from AuthRepository propagates and stops execution`() = runTest {
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
        
        val expectedEvents = listOf(
            "deviceIdProvider.getDeviceId",
            "authRepository.login"
        )
        assertEquals(expectedEvents, events)
    }

    @Test
    fun `CredentialStore save failure prevents SessionMetadataStore and SessionManager execution`() = runTest {
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
        
        val expectedEvents = listOf(
            "deviceIdProvider.getDeviceId",
            "authRepository.login",
            "credentialStore.save"
        )
        assertEquals(expectedEvents, events)
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
        assertEquals(sampleSession.credentials, credentialStore.savedCredentials) 
        assertNull(sessionManager.capturedAuthSession) 
        
        val expectedEvents = listOf(
            "deviceIdProvider.getDeviceId",
            "authRepository.login",
            "credentialStore.save",
            "sessionMetadataStore.save"
        )
        assertEquals(expectedEvents, events)
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
