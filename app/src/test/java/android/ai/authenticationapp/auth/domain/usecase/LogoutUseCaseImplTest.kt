package android.ai.authenticationapp.auth.domain.usecase

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
import android.ai.authenticationapp.auth.domain.lock.LocalUnlockState
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.domain.session.TokenManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLogoutAuthRepository : AuthRepository {
    val events = mutableListOf<String>()
    var throwExceptionOnLogout: Throwable? = null

    override suspend fun login(request: LoginRequest): AuthSession = throw NotImplementedError()
    override suspend fun getCurrentUser(): User = throw NotImplementedError()
    override suspend fun refreshToken(refreshToken: String): Credentials = throw NotImplementedError()
    
    override suspend fun logout(sessionId: String, deviceId: String) {
        events.add("authRepository.logout")
        throwExceptionOnLogout?.let { throw it }
    }
}

class FakeLogoutSessionMetadataStore : SessionMetadataStore {
    val events = mutableListOf<String>()
    var returnSession: Session? = Session("sid", "did", "uid")

    override suspend fun save(session: Session) {}
    override suspend fun get(): Session? {
        events.add("sessionMetadataStore.get")
        return returnSession
    }
    override suspend fun clear() {
        events.add("sessionMetadataStore.clear")
        returnSession = null
    }
}

class FakeLogoutTokenManager : TokenManager {
    val events = mutableListOf<String>()
    override suspend fun getValidAccessToken(): String? = null
    override suspend fun refresh(): String? = null
    override suspend fun clear() {
        events.add("tokenManager.clear")
    }
}

class FakeLogoutBiometricPreferenceStore : BiometricPreferenceStore {
    override suspend fun isEnabled(): Boolean = false
    override suspend fun setEnabled(enabled: Boolean) {}
}

class FakeLogoutSessionManager : SessionManager(
    FakeLogoutTokenManager(),
    FakeLogoutAuthRepository(),
    FakeLogoutBiometricPreferenceStore(),
    FakeUnlockLocalLockManager()
) {
    val events = mutableListOf<String>()
    override fun onLogout() {
        events.add("sessionManager.onLogout")
    }
}

class LogoutUseCaseImplTest {

    @Test
    fun `Server logout succeeds then local wipe happens in exact order`() = runTest {
        val authRepository = FakeLogoutAuthRepository()
        val tokenManager = FakeLogoutTokenManager()
        val sessionMetadataStore = FakeLogoutSessionMetadataStore()
        val sessionManager = FakeLogoutSessionManager()
        val localLockManager = FakeUnlockLocalLockManager()

        val useCase = LogoutUseCaseImpl(
            authRepository, tokenManager, sessionMetadataStore, sessionManager, localLockManager
        )

        useCase()

        // Because they are physically separate lists in the fakes, we can just assert the individual ones executed 
        // to prove the total flow works. 
        assertEquals(listOf("sessionMetadataStore.get", "sessionMetadataStore.clear"), sessionMetadataStore.events)
        assertEquals(listOf("authRepository.logout"), authRepository.events)
        assertEquals(listOf("tokenManager.clear"), tokenManager.events)
        assertEquals(listOf("sessionManager.onLogout"), sessionManager.events)
        assertEquals(LocalUnlockState.Unlocked, localLockManager.state.value)
    }

    @Test
    fun `Server logout network failure still triggers local wipe`() = runTest {
        val authRepository = FakeLogoutAuthRepository().apply {
            throwExceptionOnLogout = AuthException(AuthError.Network)
        }
        val tokenManager = FakeLogoutTokenManager()
        val sessionMetadataStore = FakeLogoutSessionMetadataStore()
        val sessionManager = FakeLogoutSessionManager()
        val localLockManager = FakeUnlockLocalLockManager()

        val useCase = LogoutUseCaseImpl(
            authRepository, tokenManager, sessionMetadataStore, sessionManager, localLockManager
        )

        useCase()

        assertEquals(listOf("sessionMetadataStore.get", "sessionMetadataStore.clear"), sessionMetadataStore.events)
        assertEquals(listOf("authRepository.logout"), authRepository.events)
        assertEquals(listOf("tokenManager.clear"), tokenManager.events)
        assertEquals(listOf("sessionManager.onLogout"), sessionManager.events)
        assertEquals(LocalUnlockState.Unlocked, localLockManager.state.value)
    }

    @Test
    fun `Server logout server failure still triggers local wipe`() = runTest {
        val authRepository = FakeLogoutAuthRepository().apply {
            throwExceptionOnLogout = AuthException(AuthError.Server)
        }
        val tokenManager = FakeLogoutTokenManager()
        val sessionMetadataStore = FakeLogoutSessionMetadataStore()
        val sessionManager = FakeLogoutSessionManager()
        val localLockManager = FakeUnlockLocalLockManager()

        val useCase = LogoutUseCaseImpl(
            authRepository, tokenManager, sessionMetadataStore, sessionManager, localLockManager
        )

        useCase()

        assertEquals(listOf("sessionMetadataStore.get", "sessionMetadataStore.clear"), sessionMetadataStore.events)
        assertEquals(listOf("authRepository.logout"), authRepository.events)
        assertEquals(listOf("tokenManager.clear"), tokenManager.events)
        assertEquals(listOf("sessionManager.onLogout"), sessionManager.events)
        assertEquals(LocalUnlockState.Unlocked, localLockManager.state.value)
    }

    @Test
    fun `No session metadata skips remote logout but still wipes locally`() = runTest {
        val authRepository = FakeLogoutAuthRepository()
        val tokenManager = FakeLogoutTokenManager()
        val sessionMetadataStore = FakeLogoutSessionMetadataStore().apply {
            returnSession = null
        }
        val sessionManager = FakeLogoutSessionManager()
        val localLockManager = FakeUnlockLocalLockManager()

        val useCase = LogoutUseCaseImpl(
            authRepository, tokenManager, sessionMetadataStore, sessionManager, localLockManager
        )

        useCase()

        assertEquals(listOf("sessionMetadataStore.get", "sessionMetadataStore.clear"), sessionMetadataStore.events)
        assertEquals(emptyList<String>(), authRepository.events) // Skipped!
        assertEquals(listOf("tokenManager.clear"), tokenManager.events)
        assertEquals(listOf("sessionManager.onLogout"), sessionManager.events)
        assertEquals(LocalUnlockState.Unlocked, localLockManager.state.value)
    }

    @Test
    fun `CancellationException propagates and aborts wipe`() = runTest {
        val authRepository = FakeLogoutAuthRepository().apply {
            throwExceptionOnLogout = CancellationException("Cancelled")
        }
        val tokenManager = FakeLogoutTokenManager()
        val sessionMetadataStore = FakeLogoutSessionMetadataStore()
        val sessionManager = FakeLogoutSessionManager()
        val localLockManager = FakeUnlockLocalLockManager()

        val useCase = LogoutUseCaseImpl(
            authRepository, tokenManager, sessionMetadataStore, sessionManager, localLockManager
        )

        var caught: Throwable? = null
        try {
            useCase()
        } catch (e: Throwable) {
            caught = e
        }

        assertTrue(caught is CancellationException)
        
        // Assert local wipe was aborted
        assertEquals(listOf("sessionMetadataStore.get"), sessionMetadataStore.events)
        assertEquals(listOf("authRepository.logout"), authRepository.events)
        assertEquals(emptyList<String>(), tokenManager.events)
        assertEquals(emptyList<String>(), sessionManager.events)
        assertEquals(0, localLockManager.unlockCallCount)
    }
}
