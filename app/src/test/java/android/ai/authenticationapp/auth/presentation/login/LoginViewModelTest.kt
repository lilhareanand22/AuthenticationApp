package android.ai.authenticationapp.auth.presentation.login

import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.Session
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class FakeLoginUseCase : LoginUseCase {
    var capturedEmail: String? = null
    var capturedPassword: String? = null
    var callCount = 0

    var result: AuthSession? = null
    var exception: Throwable? = null

    // For concurrency/double-click testing
    var suspendingGate: CompletableDeferred<Unit>? = null

    override suspend fun invoke(email: String, password: String): AuthSession {
        callCount++
        capturedEmail = email
        capturedPassword = password

        suspendingGate?.await()

        exception?.let { throw it }
        return requireNotNull(result)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeUseCase: FakeLoginUseCase
    private lateinit var viewModel: LoginViewModel

    private val dummyAuthSession = AuthSession(
        user = User("1", "test@example.com", "Test"),
        session = Session("sid", "did", "1"),
        credentials = Credentials("acc", "ref", Instant.now())
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeUseCase = FakeLoginUseCase()
        viewModel = LoginViewModel(fakeUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful login updates state and emits NavigateToHome`() = testScope.runTest {
        fakeUseCase.result = dummyAuthSession
        
        // Remove suspending gate immediately so the mock usecase doesn't hang
        fakeUseCase.suspendingGate = null 

        val emittedEffects = mutableListOf<LoginEffect>()
        val effectJob = backgroundScope.launch {
            viewModel.effects.toList(emittedEffects)
        }

        viewModel.onIntent(LoginIntent.EmailChanged("test@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("password"))

        assertEquals("test@example.com", viewModel.state.value.email)
        assertEquals("password", viewModel.state.value.password)

        viewModel.onIntent(LoginIntent.LoginClicked)

        // Initial loading state triggers immediately
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()

        // Give the background job a chance to collect the effect
        yield()

        // Assert use case parameters
        assertEquals("test@example.com", fakeUseCase.capturedEmail)
        assertEquals("password", fakeUseCase.capturedPassword)
        assertEquals(1, fakeUseCase.callCount)

        // Assert final state
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)

        // Assert effects
        assertEquals(1, emittedEffects.size)
        assertEquals(LoginEffect.NavigateToHome, emittedEffects[0])
        
        effectJob.cancel()
    }

    @Test
    fun `AuthException InvalidCredentials sets correct error state and prevents navigation`() = testScope.runTest {
        fakeUseCase.exception = AuthException(AuthError.InvalidCredentials)

        val emittedEffects = mutableListOf<LoginEffect>()
        val effectJob = backgroundScope.launch { viewModel.effects.toList(emittedEffects) }

        viewModel.onIntent(LoginIntent.EmailChanged("test@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("wrong"))
        viewModel.onIntent(LoginIntent.LoginClicked)

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(LoginError.InvalidCredentials, viewModel.state.value.error)
        assertTrue(emittedEffects.isEmpty())
        
        effectJob.cancel()
    }

    @Test
    fun `AuthException Network sets Network error state`() = testScope.runTest {
        fakeUseCase.exception = AuthException(AuthError.Network)

        viewModel.onIntent(LoginIntent.EmailChanged("a"))
        viewModel.onIntent(LoginIntent.PasswordChanged("b"))
        viewModel.onIntent(LoginIntent.LoginClicked)

        advanceUntilIdle()

        assertEquals(LoginError.Network, viewModel.state.value.error)
    }

    @Test
    fun `AuthException Server sets Server error state`() = testScope.runTest {
        fakeUseCase.exception = AuthException(AuthError.Server)

        viewModel.onIntent(LoginIntent.EmailChanged("a"))
        viewModel.onIntent(LoginIntent.PasswordChanged("b"))
        viewModel.onIntent(LoginIntent.LoginClicked)

        advanceUntilIdle()

        assertEquals(LoginError.Server, viewModel.state.value.error)
    }

    @Test
    fun `Unexpected exception sets Unknown error state`() = testScope.runTest {
        fakeUseCase.exception = RuntimeException("Unknown crash")

        viewModel.onIntent(LoginIntent.EmailChanged("a"))
        viewModel.onIntent(LoginIntent.PasswordChanged("b"))
        viewModel.onIntent(LoginIntent.LoginClicked)

        advanceUntilIdle()

        assertEquals(LoginError.Unknown, viewModel.state.value.error)
    }

    @Test
    fun `CancellationException propagates and is not swallowed`() = testScope.runTest {
        fakeUseCase.exception = CancellationException("Cancelled")
        
        viewModel.onIntent(LoginIntent.EmailChanged("a"))
        viewModel.onIntent(LoginIntent.PasswordChanged("b"))
        
        val job = launch {
            viewModel.onIntent(LoginIntent.LoginClicked)
        }
        advanceUntilIdle()
        job.join()
        
        // When launched inside viewModelScope (which in tests routes through TestDispatcher), 
        // the CancellationException throws up through the launch boundary directly.
        // It should *not* update the StateFlow error explicitly to LoginError.Unknown.
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `Empty inputs prevent LoginUseCase invocation`() = testScope.runTest {
        viewModel.onIntent(LoginIntent.EmailChanged(""))
        viewModel.onIntent(LoginIntent.PasswordChanged("password"))
        viewModel.onIntent(LoginIntent.LoginClicked)
        advanceUntilIdle()
        
        assertEquals(0, fakeUseCase.callCount)

        viewModel.onIntent(LoginIntent.EmailChanged("email@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged(""))
        viewModel.onIntent(LoginIntent.LoginClicked)
        advanceUntilIdle()

        assertEquals(0, fakeUseCase.callCount)
    }

    @Test
    fun `Duplicate LoginClicked intents while loading only trigger UseCase once`() = testScope.runTest {
        fakeUseCase.result = dummyAuthSession
        fakeUseCase.suspendingGate = CompletableDeferred() // Block the execution

        viewModel.onIntent(LoginIntent.EmailChanged("test"))
        viewModel.onIntent(LoginIntent.PasswordChanged("test"))
        
        // Initial click (should pass)
        viewModel.onIntent(LoginIntent.LoginClicked)
        
        // Wait for coroutine to process the click and set isLoading = true
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isLoading)

        // Click again while still loading
        viewModel.onIntent(LoginIntent.LoginClicked)
        viewModel.onIntent(LoginIntent.LoginClicked)

        // Unblock the execution
        fakeUseCase.suspendingGate?.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        // Should only be called EXACTLY once
        assertEquals(1, fakeUseCase.callCount)
    }

    @Test
    fun `Changing email clears existing error`() = testScope.runTest {
        fakeUseCase.exception = AuthException(AuthError.InvalidCredentials)
        
        viewModel.onIntent(LoginIntent.EmailChanged("a"))
        viewModel.onIntent(LoginIntent.PasswordChanged("b"))
        viewModel.onIntent(LoginIntent.LoginClicked)
        advanceUntilIdle()

        assertEquals(LoginError.InvalidCredentials, viewModel.state.value.error)

        viewModel.onIntent(LoginIntent.EmailChanged("a2"))
        assertNull(viewModel.state.value.error)
    }
    
    @Test
    fun `Changing password clears existing error`() = testScope.runTest {
        fakeUseCase.exception = AuthException(AuthError.InvalidCredentials)
        
        viewModel.onIntent(LoginIntent.EmailChanged("a"))
        viewModel.onIntent(LoginIntent.PasswordChanged("b"))
        viewModel.onIntent(LoginIntent.LoginClicked)
        advanceUntilIdle()

        assertEquals(LoginError.InvalidCredentials, viewModel.state.value.error)

        viewModel.onIntent(LoginIntent.PasswordChanged("b2"))
        assertNull(viewModel.state.value.error)
    }
}
