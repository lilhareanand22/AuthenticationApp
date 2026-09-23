package android.ai.authenticationapp.auth.presentation.dashboard

import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.usecase.LogoutUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeLogoutUseCase : LogoutUseCase {
    var callCount = 0
    var suspendingGate: CompletableDeferred<Unit>? = null

    override suspend fun invoke() {
        callCount++
        suspendingGate?.await()
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
    fun `Initial user is displayed and isLoggingOut is false`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        val viewModel = DashboardViewModel(testUser, fakeLogout)
        
        assertEquals(testUser, viewModel.state.value.user)
        assertFalse(viewModel.state.value.isLoggingOut)
    }

    @Test
    fun `LogoutClicked sets loading, calls UseCase, and emits NavigateToLogin`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        val viewModel = DashboardViewModel(testUser, fakeLogout)
        
        val emittedEffects = mutableListOf<DashboardEffect>()
        val effectJob = backgroundScope.launch { viewModel.effects.toList(emittedEffects) }

        viewModel.onIntent(DashboardIntent.LogoutClicked)
        
        // Assert synchronous state change
        assertTrue(viewModel.state.value.isLoggingOut)
        
        advanceUntilIdle()
        yield()

        assertEquals(1, fakeLogout.callCount)
        assertFalse(viewModel.state.value.isLoggingOut)
        
        assertEquals(1, emittedEffects.size)
        assertEquals(DashboardEffect.NavigateToLogin, emittedEffects[0])
        
        effectJob.cancel()
    }

    @Test
    fun `Duplicate LogoutClicked ignores extra calls while loading`() = testScope.runTest {
        val fakeLogout = FakeLogoutUseCase()
        fakeLogout.suspendingGate = CompletableDeferred()
        val viewModel = DashboardViewModel(testUser, fakeLogout)
        
        viewModel.onIntent(DashboardIntent.LogoutClicked)
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isLoggingOut)
        
        viewModel.onIntent(DashboardIntent.LogoutClicked)
        viewModel.onIntent(DashboardIntent.LogoutClicked)
        
        fakeLogout.suspendingGate?.complete(Unit)
        advanceUntilIdle()
        
        assertEquals(1, fakeLogout.callCount)
    }
}
