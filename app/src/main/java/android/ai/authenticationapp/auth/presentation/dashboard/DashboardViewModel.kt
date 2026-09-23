package android.ai.authenticationapp.auth.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.usecase.LogoutUseCase
import android.ai.authenticationapp.auth.security.BiometricAuthenticator
import android.ai.authenticationapp.auth.security.BiometricAvailability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val user: User,
    private val logoutUseCase: LogoutUseCase,
    private val biometricPreferenceStore: BiometricPreferenceStore,
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState(user = user))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val isAvailable = isHardwareAvailable()
            val isEnabled = biometricPreferenceStore.isEnabled()

            _state.update {
                it.copy(
                    isBiometricAvailable = isAvailable,
                    isBiometricEnabled = isEnabled && isAvailable
                )
            }
        }
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.LogoutClicked -> {
                handleLogoutClicked()
            }
            is DashboardIntent.BiometricEnabledChanged -> {
                handleBiometricToggle(intent.enabled)
            }
        }
    }

    private fun handleBiometricToggle(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // Double check availability before persisting true
                if (isHardwareAvailable()) {
                    biometricPreferenceStore.setEnabled(true)
                    _state.update { 
                        it.copy(
                            isBiometricEnabled = true, 
                            error = null 
                        ) 
                    }
                } else {
                    _state.update { 
                        it.copy(error = DashboardError.BiometricUnavailable) 
                    }
                }
            } else {
                // Disabling biometric strictly modifies the local preference ONLY.
                // It absolutely MUST NOT touch the active CredentialStore or logout the user.
                biometricPreferenceStore.setEnabled(false)
                _state.update { 
                    it.copy(
                        isBiometricEnabled = false, 
                        error = null 
                    ) 
                }
            }
        }
    }

    private fun handleLogoutClicked() {
        if (_state.value.isLoggingOut) return

        _state.update {
            it.copy(
                isLoggingOut = true,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                logoutUseCase()
                _state.update { it.copy(isLoggingOut = false) }
                _effects.send(DashboardEffect.NavigateToLogin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoggingOut = false,
                        error = DashboardError.Unknown
                    )
                }
            }
        }
    }

    private fun isHardwareAvailable(): Boolean {
        return biometricAuthenticator.checkAvailability() == BiometricAvailability.Available
    }
}
