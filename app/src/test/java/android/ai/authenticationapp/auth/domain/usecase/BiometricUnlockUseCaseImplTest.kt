package android.ai.authenticationapp.auth.domain.usecase

import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.lock.LocalLockManager
import android.ai.authenticationapp.auth.domain.lock.LocalUnlockState
import android.ai.authenticationapp.auth.security.BiometricAuthenticator
import android.ai.authenticationapp.auth.security.BiometricAvailability
import android.ai.authenticationapp.auth.security.BiometricResult
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeUnlockBiometricPreferenceStore : BiometricPreferenceStore {
    var enabled = false
    override suspend fun isEnabled(): Boolean = enabled
    override suspend fun setEnabled(enabled: Boolean) { this.enabled = enabled }
}

class FakeUnlockLocalLockManager : LocalLockManager {
    private val _state = MutableStateFlow<LocalUnlockState>(LocalUnlockState.Locked)
    override val state: StateFlow<LocalUnlockState> = _state.asStateFlow()
    
    var unlockCallCount = 0

    override fun lock() {
        _state.value = LocalUnlockState.Locked
    }

    override fun unlock() {
        unlockCallCount++
        _state.value = LocalUnlockState.Unlocked
    }
}

class FakeUnlockBiometricAuthenticator : BiometricAuthenticator {
    var availability: BiometricAvailability = BiometricAvailability.Available
    var authResult: BiometricResult = BiometricResult.Success
    var authenticateCallCount = 0

    override fun checkAvailability(): BiometricAvailability = availability

    override suspend fun authenticate(
        activity: FragmentActivity?,
        title: String,
        subtitle: String?
    ): BiometricResult {
        authenticateCallCount++
        return authResult
    }
}

class BiometricUnlockUseCaseImplTest {

    private val fakeActivity: FragmentActivity? = null

    @Test
    fun `biometric disabled returns NotEnabled and does not invoke prompt or unlock`() = runTest {
        val prefs = FakeUnlockBiometricPreferenceStore().apply { enabled = false }
        val authenticator = FakeUnlockBiometricAuthenticator()
        val lockManager = FakeUnlockLocalLockManager()

        val useCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = useCase(fakeActivity)

        assertEquals(BiometricUnlockResult.NotEnabled, result)
        assertEquals(0, authenticator.authenticateCallCount)
        assertEquals(0, lockManager.unlockCallCount)
    }

    @Test
    fun `biometric unavailable returns NotAvailable and does not invoke prompt or unlock`() = runTest {
        val prefs = FakeUnlockBiometricPreferenceStore().apply { enabled = true }
        val authenticator = FakeUnlockBiometricAuthenticator().apply { 
            availability = BiometricAvailability.NotEnrolled 
        }
        val lockManager = FakeUnlockLocalLockManager()

        val useCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = useCase(fakeActivity)

        assertEquals(BiometricUnlockResult.NotAvailable, result)
        assertEquals(0, authenticator.authenticateCallCount)
        assertEquals(0, lockManager.unlockCallCount)
    }

    @Test
    fun `successful authentication unlocks LocalLockManager and returns Success`() = runTest {
        val prefs = FakeUnlockBiometricPreferenceStore().apply { enabled = true }
        val authenticator = FakeUnlockBiometricAuthenticator().apply { 
            availability = BiometricAvailability.Available
            authResult = BiometricResult.Success
        }
        val lockManager = FakeUnlockLocalLockManager()

        val useCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = useCase(fakeActivity)

        assertEquals(BiometricUnlockResult.Success, result)
        assertEquals(1, authenticator.authenticateCallCount)
        assertEquals(1, lockManager.unlockCallCount)
        assertEquals(LocalUnlockState.Unlocked, lockManager.state.value)
    }

    @Test
    fun `user cancellation returns Cancelled and does not unlock`() = runTest {
        val prefs = FakeUnlockBiometricPreferenceStore().apply { enabled = true }
        val authenticator = FakeUnlockBiometricAuthenticator().apply { 
            availability = BiometricAvailability.Available
            authResult = BiometricResult.Cancelled
        }
        val lockManager = FakeUnlockLocalLockManager()

        val useCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = useCase(fakeActivity)

        assertEquals(BiometricUnlockResult.Cancelled, result)
        assertEquals(1, authenticator.authenticateCallCount)
        assertEquals(0, lockManager.unlockCallCount)
        assertEquals(LocalUnlockState.Locked, lockManager.state.value)
    }

    @Test
    fun `authentication error returns Error and does not unlock`() = runTest {
        val prefs = FakeUnlockBiometricPreferenceStore().apply { enabled = true }
        val authenticator = FakeUnlockBiometricAuthenticator().apply { 
            availability = BiometricAvailability.Available
            authResult = BiometricResult.Error(7, "Too many attempts")
        }
        val lockManager = FakeUnlockLocalLockManager()

        val useCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = useCase(fakeActivity)

        assertEquals(BiometricUnlockResult.Error("Too many attempts"), result)
        assertEquals(1, authenticator.authenticateCallCount)
        assertEquals(0, lockManager.unlockCallCount)
        assertEquals(LocalUnlockState.Locked, lockManager.state.value)
    }

    @Test
    fun `authentication not enrolled returns NotAvailable and does not unlock`() = runTest {
        val prefs = FakeUnlockBiometricPreferenceStore().apply { enabled = true }
        val authenticator = FakeUnlockBiometricAuthenticator().apply { 
            availability = BiometricAvailability.Available
            authResult = BiometricResult.NotEnrolled
        }
        val lockManager = FakeUnlockLocalLockManager()

        val useCase = BiometricUnlockUseCaseImpl(prefs, authenticator, lockManager)

        val result = useCase(fakeActivity)

        assertEquals(BiometricUnlockResult.NotAvailable, result)
        assertEquals(1, authenticator.authenticateCallCount)
        assertEquals(0, lockManager.unlockCallCount)
        assertEquals(LocalUnlockState.Locked, lockManager.state.value)
    }
}
