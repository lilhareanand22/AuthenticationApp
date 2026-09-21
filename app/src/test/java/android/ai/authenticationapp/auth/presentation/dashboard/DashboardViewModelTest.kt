package android.ai.authenticationapp.auth.presentation.dashboard

import android.ai.authenticationapp.auth.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
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
    fun `initial user is displayed`() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(testUser)
        assertEquals(testUser, viewModel.state.value.user)
    }

    @Test
    fun `initial state has isLoggingOut = false`() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(testUser)
        assertFalse(viewModel.state.value.isLoggingOut)
    }

    @Test
    fun `LogoutClicked does not crash`() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(testUser)
        viewModel.onIntent(DashboardIntent.LogoutClicked)
        // No crash occurs
    }
}
