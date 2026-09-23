package android.ai.authenticationapp.auth.presentation.dashboard

import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.usecase.LogoutUseCase
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
import androidx.fragment.app.FragmentActivity

class FakeLogoutUseCase : LogoutUseCase {
    var callCount = 0
    var suspendingGate: CompletableDeferred<Unit>? = null

    override suspend fun invoke() {
        callCount++
        suspendingGate?.await()
    }
}

class FakeBiometricPreferenceStore : BiometricPreferenceStore {
    var enabled = false
    override suspend fun isEnabled(): Boolean = enabled
    override suspend fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}

class FakeDashboardBiometricAuthenticator(
    var availability: BiometricAvailability = BiometricAvailability.Available
) : BiometricAuthenticator {
    override fun checkAvailability(): BiometricAvailability = availability
    
    override suspend fun authenticate(
        activity: FragmentActivity?,
        title: String,
        subtitle: String?
    ): BiometricResult {
        error("Not used")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private val testUser = User(id = "user123", email = "test@example.com", name = "Test User")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stored preference true + available - enabled`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        val fakePrefs = FakeBiometricPreferenceStore().apply { enabled = true }
        val fakeAuthenticator = FakeDashboardBiometricAuthenticator(BiometricAvailability.Available)
        
        val viewModel = DashboardViewModel(testUser, fakeLogout, fakePrefs, fakeAuthenticator)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isBiometricEnabled)
        assertTrue(viewModel.state.value.isBiometricAvailable)
    }

    @Test
    fun `stored preference true + temporarily unavailable - UI disabled but stored preference remains true`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        // The user previously enabled biometric unlock.
        val fakePrefs = FakeBiometricPreferenceStore().apply { enabled = true }
        // The hardware is temporarily unavailable (e.g. cold conditions, or device lock rules).
        val fakeAuthenticator = FakeDashboardBiometricAuthenticator(BiometricAvailability.NotAvailable)
        
        val viewModel = DashboardViewModel(testUser, fakeLogout, fakePrefs, fakeAuthenticator)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isBiometricAvailable)
        // It drops out of enabled state for the UI because it's currently unusable
        assertFalse(viewModel.state.value.isBiometricEnabled)
        // CRITICAL CHECK: Ensure the persisted preference was not wiped just because of a temporary hardware outage.
        assertTrue(fakePrefs.enabled)
    }

    @Test
    fun `stored preference false + available - disabled`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        val fakePrefs = FakeBiometricPreferenceStore().apply { enabled = false }
        val fakeAuthenticator = FakeDashboardBiometricAuthenticator(BiometricAvailability.Available)
        
        val viewModel = DashboardViewModel(testUser, fakeLogout, fakePrefs, fakeAuthenticator)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertTrue(viewModel.state.value.isBiometricAvailable)
    }

    @Test
    fun `enable when available - persisted true`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        val fakePrefs = FakeBiometricPreferenceStore().apply { enabled = false }
        val fakeAuthenticator = FakeDashboardBiometricAuthenticator(BiometricAvailability.Available)
        
        val viewModel = DashboardViewModel(testUser, fakeLogout, fakePrefs, fakeAuthenticator)
        advanceUntilIdle()

        viewModel.onIntent(DashboardIntent.BiometricEnabledChanged(true))
        advanceUntilIdle()

        assertTrue(fakePrefs.enabled)
        assertTrue(viewModel.state.value.isBiometricEnabled)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `enable when unavailable - not persisted`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        val fakePrefs = FakeBiometricPreferenceStore().apply { enabled = false }
        val fakeAuthenticator = FakeDashboardBiometricAuthenticator(BiometricAvailability.NotEnrolled)
        
        val viewModel = DashboardViewModel(testUser, fakeLogout, fakePrefs, fakeAuthenticator)
        advanceUntilIdle()

        viewModel.onIntent(DashboardIntent.BiometricEnabledChanged(true))
        advanceUntilIdle()

        // It should reject the action and keep the store unmodified.
        assertFalse(fakePrefs.enabled)
        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertEquals(DashboardError.BiometricUnavailable, viewModel.state.value.error)
    }

    @Test
    fun `disable - persisted false without logout`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        val fakePrefs = FakeBiometricPreferenceStore().apply { enabled = true }
        val fakeAuthenticator = FakeDashboardBiometricAuthenticator(BiometricAvailability.Available)
        
        val viewModel = DashboardViewModel(testUser, fakeLogout, fakePrefs, fakeAuthenticator)
        advanceUntilIdle()

        viewModel.onIntent(DashboardIntent.BiometricEnabledChanged(false))
        advanceUntilIdle()

        assertFalse(fakePrefs.enabled)
        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertNull(viewModel.state.value.error)
        
        // Assert logout was NOT called
        assertEquals(0, fakeLogout.callCount)
    }

    @Test
    fun `logout remains independent of biometric preference`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        val fakePrefs = FakeBiometricPreferenceStore().apply { enabled = true }
        val fakeAuthenticator = FakeDashboardBiometricAuthenticator(BiometricAvailability.Available)
        val viewModel = DashboardViewModel(testUser, fakeLogout, fakePrefs, fakeAuthenticator)
        
        viewModel.onIntent(DashboardIntent.LogoutClicked)
        advanceUntilIdle()
        
        // Assert logout executed
        assertEquals(1, fakeLogout.callCount)
        // Assert biometric preference is completely untouched by the logout action locally
        assertTrue(fakePrefs.enabled)
    }
}
