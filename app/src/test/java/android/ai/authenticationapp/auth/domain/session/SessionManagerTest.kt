package android.ai.authenticationapp.auth.domain.session

import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.LoginRequest
import android.ai.authenticationapp.auth.domain.model.Session
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FakeTokenManager : TokenManager {
    var tokenToReturn: String? = null
    var throwException: Exception? = null
    
    override suspend fun getValidAccessToken(): String? {
        if (throwException != null) throw throwException!!
        return tokenToReturn
    }

    override suspend fun refresh(): String? {
        return tokenToReturn
    }

    override suspend fun clear() {}
}

class FakeUserRepository : AuthRepository {
    var userToReturn: User = User("1", "test@test.com", "Test User")
    var throwException: Exception? = null

    override suspend fun getCurrentUser(): User {
        if (throwException != null) throw throwException!!
        return userToReturn
    }

    override suspend fun login(request: LoginRequest): AuthSession { throw NotImplementedError() }
    override suspend fun refreshToken(refreshToken: String): Credentials { throw NotImplementedError() }
}

class SessionManagerTest {

    private val fakeTokenManager = FakeTokenManager()
    private val fakeAuthRepository = FakeUserRepository()
    private val sessionManager = SessionManager(fakeTokenManager, fakeAuthRepository)

    @Test
    fun `Initial state is Unknown`() = runTest {
        assertEquals(AuthenticationState.Unknown, sessionManager.authState.value)
    }

    @Test
    fun `restoreSession with no credentials results in Unauthenticated`() = runTest {
        fakeTokenManager.tokenToReturn = null
        
        sessionManager.restoreSession()
        
        assertEquals(AuthenticationState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun `restoreSession with valid tokens fetches user and becomes Authenticated`() = runTest {
        fakeTokenManager.tokenToReturn = "valid_token"
        val expectedUser = User("2", "a@a.com", "Name")
        fakeAuthRepository.userToReturn = expectedUser
        
        sessionManager.restoreSession()
        
        val state = sessionManager.authState.value
        assertTrue(state is AuthenticationState.Authenticated)
        assertEquals(expectedUser, (state as AuthenticationState.Authenticated).user)
    }

    @Test
    fun `Terminal AuthException during restore clears state to Unauthenticated`() = runTest {
        fakeTokenManager.throwException = AuthException(AuthError.RefreshTokenInvalid)
        
        sessionManager.restoreSession()
        
        assertEquals(AuthenticationState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun `Temporary network error during restore does NOT log out`() = runTest {
        fakeTokenManager.tokenToReturn = "valid_token"
        // Token is valid, but API fetch fails with timeout
        fakeAuthRepository.throwException = RuntimeException("Timeout")
        
        sessionManager.restoreSession()
        
        // Keeps state Unknown (does not force Unauthenticated)
        assertEquals(AuthenticationState.Unknown, sessionManager.authState.value)
    }

    @Test
    fun `Successful login updates state`() = runTest {
        val user = User("3", "b@b.com", "Login User")
        val session = AuthSession(
            user = user, 
            session = Session("sid", "did", "3"),
            credentials = Credentials("acc", "ref", Instant.now())
        )
        
        sessionManager.onLogin(session)
        
        val state = sessionManager.authState.value
        assertTrue(state is AuthenticationState.Authenticated)
        assertEquals(user, (state as AuthenticationState.Authenticated).user)
    }

    @Test
    fun `revalidateSession with valid token retains state`() = runTest {
        // Assume already logged in
        sessionManager.onLogin(AuthSession(User("1", "a", "a"), Session("1", "2", "1"), Credentials("a","b", Instant.now())))
        
        fakeTokenManager.tokenToReturn = "still_valid"
        sessionManager.revalidateSession()
        
        assertTrue(sessionManager.authState.value is AuthenticationState.Authenticated)
    }

    @Test
    fun `revalidateSession with terminal error transitions to Unauthenticated`() = runTest {
        // Assume already logged in
        sessionManager.onLogin(AuthSession(User("1", "a", "a"), Session("1", "2", "1"), Credentials("a","b", Instant.now())))
        
        fakeTokenManager.throwException = AuthException(AuthError.SessionRevoked)
        sessionManager.revalidateSession()
        
        assertEquals(AuthenticationState.Unauthenticated, sessionManager.authState.value)
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown`() = runTest {
        fakeTokenManager.throwException = CancellationException("cancelled")
        
        sessionManager.restoreSession()
    }
}
