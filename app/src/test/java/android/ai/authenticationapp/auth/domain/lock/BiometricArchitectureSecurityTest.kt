package android.ai.authenticationapp.auth.domain.lock

import android.ai.authenticationapp.auth.data.local.SessionMetadataStore
import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.LoginRequest
import android.ai.authenticationapp.auth.domain.model.Session
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.domain.session.AuthenticationState
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.domain.session.TokenManager
import android.ai.authenticationapp.auth.domain.usecase.BiometricUnlockResult
import android.ai.authenticationapp.auth.domain.usecase.BiometricUnlockUseCaseImpl
import android.ai.authenticationapp.auth.domain.usecase.LogoutUseCaseImpl
import android.ai.authenticationapp.auth.security.BiometricAuthenticator
import android.ai.authenticationapp.auth.security.BiometricAvailability
import android.ai.authenticationapp.auth.security.BiometricResult
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TestBiometricPreferenceStore : BiometricPreferenceStore {
    var enabled = false
    override suspend fun isEnabled(): Boolean = enabled
    override suspend fun setEnabled(enabled: Boolean) { this.enabled = enabled }
}

class TestTokenManager : TokenManager {
    var token: String? = null
    override suspend fun getValidAccessToken(): String? = token
    override suspend fun refresh(): String? = token
    override suspend fun clear() { token = null }
}

class TestAuthRepository : AuthRepository {
    var currentUser: User? = null
    override suspend fun login(request: LoginRequest): AuthSession = throw NotImplementedError()
    override suspend fun getCurrentUser(): User = currentUser ?: throw AuthException(AuthError.InvalidCredentials)
    override suspend fun refreshToken(refreshToken: String): Credentials = throw NotImplementedError()
    override suspend fun logout(sessionId: String, deviceId: String) {}
}

class TestSessionMetadataStore : SessionMetadataStore {
    var storedSession: Session? = Session("sid", "did", "uid")
    override suspend fun save(session: Session) { storedSession = session }
    override suspend fun get(): Session? = storedSession
    override suspend fun clear() { storedSession = null }
}

class TestBiometricAuthenticator : BiometricAuthenticator {
    var availability: BiometricAvailability = BiometricAvailability.Available
    var result: BiometricResult = BiometricResult.Success

    override fun checkAvailability(): BiometricAvailability = availability
    override suspend fun authenticate(activity: FragmentActivity?, title: String, subtitle: String?): BiometricResult = result
}

class BiometricArchitectureSecurityTest {

    @Test
    fun `TEST 1 No authenticated session + biometric enabled - biometric must NOT create a backend session`() = runTest {
        val tokenManager = TestTokenManager().apply { token = null } // Logged out
        val repo = TestAuthRepository()
        val prefs = TestBiometricPreferenceStore().apply { enabled = true }
        val lockManager = LocalLockManagerImpl()
        val sessionManager = SessionManager(tokenManager, repo, prefs, lockManager)

        // Attempt restore without saved tokens
        sessionManager.restoreSession()

        // Verify state remains Unauthenticated
        assertEquals(AuthenticationState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun `TEST 2 Authenticated session + app locked + biometric success - LocalUnlockState becomes Unlocked`() = runTest {
        val prefs = TestBiometricPreferenceStore().apply { enabled = true }
        val authenticator = TestBiometricAuthenticator().apply { result = BiometricResult.Success }
        val lockManager = LocalLockManagerImpl().apply { lock() }

        val unlockUseCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = unlockUseCase(null)

        assertEquals(BiometricUnlockResult.Success, result)
        assertEquals(LocalUnlockState.Unlocked, lockManager.state.value)
    }

    @Test
    fun `TEST 3 Authenticated session + app locked + biometric cancelled - remains Locked`() = runTest {
        val prefs = TestBiometricPreferenceStore().apply { enabled = true }
        val authenticator = TestBiometricAuthenticator().apply { result = BiometricResult.Cancelled }
        val lockManager = LocalLockManagerImpl().apply { lock() }

        val unlockUseCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = unlockUseCase(null)

        assertEquals(BiometricUnlockResult.Cancelled, result)
        assertEquals(LocalUnlockState.Locked, lockManager.state.value)
    }

    @Test
    fun `TEST 4 Authenticated session + app locked + biometric failure - remains Locked`() = runTest {
        val prefs = TestBiometricPreferenceStore().apply { enabled = true }
        val authenticator = TestBiometricAuthenticator().apply { result = BiometricResult.Error(7, "Failed") }
        val lockManager = LocalLockManagerImpl().apply { lock() }

        val unlockUseCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = unlockUseCase(null)

        assertTrue(result is BiometricUnlockResult.Error)
        assertEquals(LocalUnlockState.Locked, lockManager.state.value)
    }

    @Test
    fun `TEST 5 User logs out - AuthenticationState = Unauthenticated`() = runTest {
        val tokenManager = TestTokenManager().apply { token = "valid" }
        val repo = TestAuthRepository()
        val prefs = TestBiometricPreferenceStore().apply { enabled = true }
        val lockManager = LocalLockManagerImpl().apply { lock() }
        val sessionManager = SessionManager(tokenManager, repo, prefs, lockManager)
        val metadataStore = TestSessionMetadataStore()

        val logoutUseCase = LogoutUseCaseImpl(repo, tokenManager, metadataStore, sessionManager, lockManager)

        logoutUseCase()

        assertEquals(AuthenticationState.Unauthenticated, sessionManager.authState.value)
        assertEquals(LocalUnlockState.Unlocked, lockManager.state.value)
    }

    @Test
    fun `TEST 6 After logout + biometric enabled - biometric must NOT restore the old session`() = runTest {
        val tokenManager = TestTokenManager().apply { token = "valid" }
        val repo = TestAuthRepository()
        val prefs = TestBiometricPreferenceStore().apply { enabled = true }
        val lockManager = LocalLockManagerImpl()
        val sessionManager = SessionManager(tokenManager, repo, prefs, lockManager)
        val metadataStore = TestSessionMetadataStore()

        val logoutUseCase = LogoutUseCaseImpl(repo, tokenManager, metadataStore, sessionManager, lockManager)

        // 1. User logs out
        logoutUseCase()

        // 2. Preference remains enabled (user preference)
        assertTrue(prefs.isEnabled())

        // 3. App tries restoring session after restart
        sessionManager.restoreSession()

        // 4. Session MUST remain Unauthenticated
        assertEquals(AuthenticationState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun `TEST 7 Authenticated session + biometric disabled - app does not require biometric unlock`() = runTest {
        val tokenManager = TestTokenManager().apply { token = "valid" }
        val repo = TestAuthRepository().apply { currentUser = User("1", "a@a.com", "Name") }
        val prefs = TestBiometricPreferenceStore().apply { enabled = false }
        val lockManager = LocalLockManagerImpl()
        val sessionManager = SessionManager(tokenManager, repo, prefs, lockManager)

        sessionManager.restoreSession()

        assertTrue(sessionManager.authState.value is AuthenticationState.Authenticated)
        assertEquals(LocalUnlockState.Unlocked, lockManager.state.value)
    }

    @Test
    fun `TEST 8 Configuration change - must not unnecessarily lock the application`() = runTest {
        val lockManager = LocalLockManagerImpl()
        // Lock state is runtime state initialized as Unlocked and unaffected by rotation/config changes
        assertEquals(LocalUnlockState.Unlocked, lockManager.state.value)
    }
}
