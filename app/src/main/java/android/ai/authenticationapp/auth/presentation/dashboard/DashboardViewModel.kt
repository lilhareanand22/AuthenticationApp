package android.ai.authenticationapp.auth.presentation.dashboard

import androidx.lifecycle.ViewModel
import android.ai.authenticationapp.auth.domain.model.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class DashboardViewModel(
    private val user: User
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState(user = user))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.LogoutClicked -> {
                // TODO: Implement LogoutUseCase invocation here
                // For now, we leave this as a TODO per the instructions
            }
        }
    }
}
