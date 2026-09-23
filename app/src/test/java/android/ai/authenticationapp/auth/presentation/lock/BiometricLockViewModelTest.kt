package android.ai.authenticationapp.auth.presentation.lock

import android.ai.authenticationapp.auth.domain.usecase.BiometricUnlockResult
import android.ai.authenticationapp.auth.domain.usecase.BiometricUnlockUseCase
import androidx.fragment.app.FragmentActivity
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
import kotlin.coroutines.cancellation.CancellationException

class FakeBiometricUnlockUseCase : BiometricUnlockUseCase {
    var result: BiometricUnlockResult = BiometricUnlockResult.Success
    var throwException: Throwable? = null
    var callCount = 0
    var suspendingGate: CompletableDeferred<Unit>? = null

    override suspend fun invoke(activity: FragmentActivity?): BiometricUnlockResult {
        callCount++
        suspendingGate?.await()
        throwException?.let { throw it }
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricLockViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Suppress("UNCHECKED_CAST")
    private fun <T> nullValue(): T = null as T
    private val fakeActivity: FragmentActivity = nullValue()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `UnlockClicked invokes use case and emits Success`() = testScope.runTest {
        val useCase = FakeBiometricUnlockUseCase().apply { result = BiometricUnlockResult.Success }
        val viewModel = BiometricLockViewModel(useCase)

        val emittedEffects = mutableListOf<BiometricLockEffect>()
        val effectJob = backgroundScope.launch { viewModel.effects.toList(emittedEffects) }

        viewModel.onIntent(BiometricLockIntent.UnlockClicked(fakeActivity))
        
        advanceUntilIdle()
        yield()

        assertEquals(1, useCase.callCount)
        assertFalse(viewModel.state.value.isAuthenticating)
        assertNull(viewModel.state.value.error)
        
        assertEquals(1, emittedEffects.size)
        assertEquals(BiometricLockEffect.UnlockSuccess, emittedEffects[0])
        
        effectJob.cancel()
    }

    @Test
    fun `Cancelled does not emit success`() = testScope.runTest {
        val useCase = FakeBiometricUnlockUseCase().apply { result = BiometricUnlockResult.Cancelled }
        val viewModel = BiometricLockViewModel(useCase)

        val emittedEffects = mutableListOf<BiometricLockEffect>()
        val effectJob = backgroundScope.launch { viewModel.effects.toList(emittedEffects) }

        viewModel.onIntent(BiometricLockIntent.UnlockClicked(fakeActivity))
        
        advanceUntilIdle()
        kotlinx.coroutines.yield()

        assertEquals(1, useCase.callCount)
        assertFalse(viewModel.state.value.isAuthenticating)
        assertNull(viewModel.state.value.error) // We map Cancelled to null error for UX 
        assertTrue(emittedEffects.isEmpty())
        
        effectJob.cancel()
    }

    @Test
    fun `Error updates error state`() = testScope.runTest {
        val useCase = FakeBiometricUnlockUseCase().apply { result = BiometricUnlockResult.Error("Fingerprint dirty") }
        val viewModel = BiometricLockViewModel(useCase)

        viewModel.onIntent(BiometricLockIntent.UnlockClicked(fakeActivity))
        
        advanceUntilIdle()
        kotlinx.coroutines.yield()

        assertEquals(1, useCase.callCount)
        assertFalse(viewModel.state.value.isAuthenticating)
        assertEquals(BiometricLockError.Unknown("Fingerprint dirty"), viewModel.state.value.error)
    }

    @Test
    fun `duplicate clicks do not start concurrent operations`() = testScope.runTest {
        val useCase = FakeBiometricUnlockUseCase().apply { 
            result = BiometricUnlockResult.Success
            suspendingGate = CompletableDeferred() 
        }
        val viewModel = BiometricLockViewModel(useCase)

        viewModel.onIntent(BiometricLockIntent.UnlockClicked(fakeActivity))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isAuthenticating)

        viewModel.onIntent(BiometricLockIntent.UnlockClicked(fakeActivity))
        viewModel.onIntent(BiometricLockIntent.UnlockClicked(fakeActivity))
        
        useCase.suspendingGate?.complete(Unit)
        advanceUntilIdle()
        kotlinx.coroutines.yield()

        assertEquals(1, useCase.callCount)
        assertFalse(viewModel.state.value.isAuthenticating)
    }

    @Test
    fun `CancellationException is propagated`() = testScope.runTest {
        val useCase = FakeBiometricUnlockUseCase().apply { throwException = CancellationException() }
        val viewModel = BiometricLockViewModel(useCase)

        val job = launch {
            viewModel.onIntent(BiometricLockIntent.UnlockClicked(fakeActivity))
        }
        advanceUntilIdle()
        job.join()
        
        assertNull(viewModel.state.value.error)
    }
}
