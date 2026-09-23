package android.ai.authenticationapp.auth.presentation.login

import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.Session
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.usecase.LoginUseCase
import android.ai.authenticationapp.auth.security.BiometricAuthenticator
import android.ai.authenticationapp.auth.security.BiometricAvailability
import android.ai.authenticationapp.auth.security.BiometricResult
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.yield

import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.domain.usecase.FakeAuthRepository
import android.ai.authenticationapp.auth.domain.usecase.FakeSessionManager

class FakeLoginBiometricPreferenceStore : BiometricPreferenceStore {
    var enabled = false
    override suspend fun isEnabled(): Boolean = enabled
    override suspend fun setEnabled(enabled: Boolean) { this.enabled = enabled }
}

class FakeLoginUseCase : LoginUseCase {
    var capturedEmail: String? = null
    var capturedPassword: String? = null
    var callCount = 0

    var result: AuthSession? = null
    var exception: Throwable? = null

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

class FakeBiometricAuthenticator(
    private val availability: BiometricAvailability
) : BiometricAuthenticator {
    override fun checkAvailability(): BiometricAvailability {
        return availability
    }

    override suspend fun authenticate(
        activity: FragmentActivity?,
        title: String,
        subtitle: String?
    ): BiometricResult {
        error("Not used in this test")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeUseCase: FakeLoginUseCase
    private val dummyAuthSession = AuthSession(
        user = User("1", "test@example.com", "Test"),
        session = Session("sid", "did", "1"),
        credentials = Credentials("acc", "ref", Instant.now())
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeUseCase = FakeLoginUseCase()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        availability: BiometricAvailability = BiometricAvailability.Available,
        preferenceEnabled: Boolean = false
    ): LoginViewModel {
        val fakePrefs = FakeLoginBiometricPreferenceStore().apply { enabled = preferenceEnabled }
        val fakeRepo = FakeAuthRepository()
        val fakeSessionMgr = FakeSessionManager()
        return LoginViewModel(fakeUseCase, FakeBiometricAuthenticator(availability), fakePrefs, fakeSessionMgr, fakeRepo)
    }

    @Test
    fun `BiometricAvailable sets state to true`() = testScope.runTest {
        val viewModel = createViewModel(BiometricAvailability.Available)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isBiometricAvailable)
    }

    @Test
    fun `NotAvailable sets state to false`() = testScope.runTest {
        val viewModel = createViewModel(BiometricAvailability.NotAvailable)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isBiometricAvailable)
    }

    @Test
    fun `NotEnrolled sets state to false`() = testScope.runTest {
        val viewModel = createViewModel(BiometricAvailability.NotEnrolled)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isBiometricAvailable)
    }

    @Test
    fun `SecurityUpdateRequired sets state to false`() = testScope.runTest {
        val viewModel = createViewModel(BiometricAvailability.SecurityUpdateRequired)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isBiometricAvailable)
    }

    @Test
    fun `Error sets state to false`() = testScope.runTest {
        val viewModel = createViewModel(BiometricAvailability.Error(1, "Error"))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isBiometricAvailable)
    }

    @Test
    fun `Biometric preference enabled sets isBiometricEnabled to true`() = testScope.runTest {
        val viewModel = createViewModel(
            availability = BiometricAvailability.Available,
            preferenceEnabled = true
        )
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isBiometricAvailable)
        assertTrue(viewModel.state.value.isBiometricEnabled)
    }

    @Test
    fun `successful login updates state and emits NavigateToHome`() = testScope.runTest {
        val viewModel = createViewModel()
        fakeUseCase.result = dummyAuthSession

        val emittedEffects = mutableListOf<LoginEffect>()
        val effectJob = backgroundScope.launch {
            viewModel.effects.toList(emittedEffects)
        }

        viewModel.onIntent(LoginIntent.EmailChanged("test@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("password"))
        viewModel.onIntent(LoginIntent.LoginClicked)

        advanceUntilIdle()
        yield()

        assertEquals("test@example.com", fakeUseCase.capturedEmail)
        assertEquals("password", fakeUseCase.capturedPassword)
        assertEquals(1, fakeUseCase.callCount)

        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
        assertEquals(1, emittedEffects.size)
        assertEquals(LoginEffect.NavigateToHome, emittedEffects[0])
        
        effectJob.cancel()
    }

    @Test
    fun `AuthException InvalidCredentials sets correct error state and prevents navigation`() = testScope.runTest {
        val viewModel = createViewModel()
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
}
