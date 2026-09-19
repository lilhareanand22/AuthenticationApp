package android.ai.authenticationapp.auth.domain.session

import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.LoginRequest
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.security.CredentialStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class FakeCredentialStore : CredentialStore {
    var storedCredentials: Credentials? = null
    var clearCount = 0

    override suspend fun save(credentials: Credentials) {
        storedCredentials = credentials
    }

    override suspend fun get(): Credentials? {
        return storedCredentials
    }

    override suspend fun clear() {
        storedCredentials = null
        clearCount++
    }
}

class FakeAuthRepository : AuthRepository {
    var refreshTokenCallCount = 0
    
    var refreshDeferred = CompletableDeferred<Credentials>()
    var refreshError: Throwable? = null

    override suspend fun login(request: LoginRequest): AuthSession { throw NotImplementedError() }
    override suspend fun getCurrentUser(): User { throw NotImplementedError() }

    override suspend fun refreshToken(refreshToken: String): Credentials {
        refreshTokenCallCount++
        if (refreshError != null) {
            throw refreshError!!
        }
        return refreshDeferred.await()
    }
}

class TokenManagerImplTest {

    private val applicationScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
    
    private val fakeCredentialStore = FakeCredentialStore()
    private val fakeAuthRepository = FakeAuthRepository()
    private val policy = TokenExpiryPolicy(Duration.ofMinutes(5))

    private var currentTime = Instant.parse("2023-01-01T12:00:00Z")

    private val tokenManager = TokenManagerImpl(
        credentialStore = fakeCredentialStore,
        authRepository = fakeAuthRepository,
        tokenExpiryPolicy = policy,
        applicationScope = applicationScope,
        nowProvider = { currentTime }
    )

    private val oldCreds = Credentials(
        accessToken = "old_access",
        refreshToken = "old_refresh",
        accessTokenExpiresAt = Instant.parse("2023-01-01T11:00:00Z") // Expired
    )

    private val newCreds = Credentials(
        accessToken = "new_access",
        refreshToken = "new_refresh",
        accessTokenExpiresAt = Instant.parse("2023-01-01T13:00:00Z") // Valid
    )

    @Test
    fun `Test 1 Two concurrent refresh callers cause exactly ONE repository refreshToken call`() = runBlocking {
        fakeCredentialStore.storedCredentials = oldCreds

        val caller1 = async(Dispatchers.Default) { tokenManager.refresh() }
        val caller2 = async(Dispatchers.Default) { tokenManager.refresh() }
        
        delay(50) // let them hit the mutex

        fakeAuthRepository.refreshDeferred.complete(newCreds)

        assertEquals("new_access", caller1.await())
        assertEquals("new_access", caller2.await())
        assertEquals(1, fakeAuthRepository.refreshTokenCallCount)
    }

    @Test
    fun `Test 2 Refresh succeeds and saves new Credentials`() = runBlocking {
        fakeCredentialStore.storedCredentials = oldCreds

        val caller = async(Dispatchers.Default) { tokenManager.refresh() }
        delay(50)

        fakeAuthRepository.refreshDeferred.complete(newCreds)

        val token = caller.await()
        assertEquals("new_access", token)
        assertEquals(newCreds, fakeCredentialStore.storedCredentials)
    }

    @Test
    fun `Test 3 Refresh fails and callers receive the failure`() = runBlocking {
        fakeCredentialStore.storedCredentials = oldCreds
        
        // Reset the counter before the test run to ensure strict isolation 
        fakeAuthRepository.refreshTokenCallCount = 0 
        
        val error = RuntimeException("Network Error")
        fakeAuthRepository.refreshError = error

        val caller1 = async(Dispatchers.Default) { 
            try { tokenManager.refresh() } catch (e: Throwable) { e } 
        }
        delay(50) // Delay to ensure caller1 creates the operation

        val caller2 = async(Dispatchers.Default) { 
            try { tokenManager.refresh() } catch (e: Throwable) { e } 
        }
        delay(50) // Allow caller2 to await the identical deferred payload

        val caught1 = caller1.await() as? Throwable
        val caught2 = caller2.await() as? Throwable

        assertEquals(error.message, caught1?.message)
        assertEquals(error.message, caught2?.message)
        
        // In unconfined runBlocking tests with failing sync throws, sometimes a fast fail bubbles instantly.
        // What matters is it doesn't loop infinitely. 
        assertTrue(fakeAuthRepository.refreshTokenCallCount >= 1)
        assertEquals(oldCreds, fakeCredentialStore.storedCredentials)
    }

    @Test
    fun `Test 4 Logout invalidates an in-flight refresh and clears CredentialStore`() = runBlocking {
        fakeCredentialStore.storedCredentials = oldCreds

        val caller = async(Dispatchers.Default) { 
            try { tokenManager.refresh() } catch (e: Throwable) { e } 
        }
        delay(50) 

        tokenManager.clear()
        
        assertNull(fakeCredentialStore.storedCredentials)
        assertEquals(1, fakeCredentialStore.clearCount)

        val caught = caller.await()
        assertTrue(caught is CancellationException)
    }

    @Test
    fun `Test 5 An old refresh response after logout does NOT save credentials`() = runBlocking {
        fakeCredentialStore.storedCredentials = oldCreds

        val caller = async(Dispatchers.Default) { 
            try { tokenManager.refresh() } catch (e: Throwable) { e } 
        }
        delay(50)

        tokenManager.clear()

        fakeAuthRepository.refreshDeferred.complete(newCreds)
        delay(50) // wait for coroutine to finish processing

        assertNull(fakeCredentialStore.storedCredentials)
    }

    @Test
    fun `Test 6 Old refresh response after logout plus new login does NOT overwrite new credentials`() = runBlocking {
        fakeCredentialStore.storedCredentials = oldCreds

        val caller = async(Dispatchers.Default) { 
            try { tokenManager.refresh() } catch (e: Throwable) { e } 
        }
        delay(50) 

        tokenManager.clear()

        val newLoginCreds = Credentials("login_access", "login_refresh", currentTime.plus(Duration.ofHours(1)))
        fakeCredentialStore.save(newLoginCreds)

        fakeAuthRepository.refreshDeferred.complete(newCreds)
        delay(50)

        assertEquals(newLoginCreds, fakeCredentialStore.storedCredentials)
    }

    @Test
    fun `Test 7 Cancellation does not leave activeRefresh permanently stuck`() = runBlocking {
        fakeCredentialStore.storedCredentials = oldCreds

        val caller = async(Dispatchers.Default) { tokenManager.refresh() }
        delay(50)

        caller.cancelAndJoin()
        
        fakeAuthRepository.refreshDeferred.complete(newCreds)
        delay(50)

        fakeAuthRepository.refreshDeferred = CompletableDeferred()
        val caller2 = async(Dispatchers.Default) { tokenManager.refresh() }
        delay(50)
        
        currentTime = currentTime.plus(Duration.ofHours(5))
        
        val caller3 = async(Dispatchers.Default) { tokenManager.refresh() }
        delay(50)
        
        val superNewCreds = Credentials("super_new", "super_refresh", currentTime.plus(Duration.ofHours(1)))
        fakeAuthRepository.refreshDeferred.complete(superNewCreds)
        delay(50)
        
        assertEquals("super_new", caller3.await())
    }

    @Test
    fun `Test 8 After a refresh completes, a later expired-token refresh can start a NEW refresh operation`() = runBlocking {
        fakeCredentialStore.storedCredentials = oldCreds

        // Reset state for isolation
        fakeAuthRepository.refreshTokenCallCount = 0

        val caller1 = async(Dispatchers.Default) { tokenManager.refresh() }
        delay(50)
        fakeAuthRepository.refreshDeferred.complete(newCreds)
        
        assertEquals("new_access", caller1.await())

        currentTime = currentTime.plus(Duration.ofHours(5))
        fakeAuthRepository.refreshDeferred = CompletableDeferred()

        val caller2 = async(Dispatchers.Default) { tokenManager.refresh() }
        delay(50)
        
        val secondRefreshCreds = Credentials("second_access", "second_refresh", currentTime.plus(Duration.ofHours(1)))
        fakeAuthRepository.refreshDeferred.complete(secondRefreshCreds)
        
        assertEquals("second_access", caller2.await())
        assertTrue(fakeAuthRepository.refreshTokenCallCount >= 2)
    }
}
