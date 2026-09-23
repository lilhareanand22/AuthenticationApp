package android.ai.authenticationapp.auth.presentation.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.ai.authenticationapp.auth.domain.usecase.BiometricUnlockResult
import android.ai.authenticationapp.auth.domain.usecase.BiometricUnlockUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BiometricLockViewModel(
    private val biometricUnlockUseCase: BiometricUnlockUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BiometricLockState())
    val state: StateFlow<BiometricLockState> = _state.asStateFlow()

    private val _effects = Channel<BiometricLockEffect>(Channel.BUFFERED)
    val effects: Flow<BiometricLockEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: BiometricLockIntent) {
        when (intent) {
            is BiometricLockIntent.UnlockClicked -> {
                handleUnlockClicked(intent.activity)
            }
        }
    }

    private fun handleUnlockClicked(activity: FragmentActivity?) {
        if (_state.value.isAuthenticating) return

        _state.update {
            it.copy(
                isAuthenticating = true,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                val result = biometricUnlockUseCase(activity)

                when (result) {
                    is BiometricUnlockResult.Success -> {
                        _state.update { it.copy(isAuthenticating = false) }
                        _effects.send(BiometricLockEffect.UnlockSuccess)
                    }
                    is BiometricUnlockResult.Cancelled -> {
                        _state.update { 
                            it.copy(
                                isAuthenticating = false, 
                                error = null // Standard cancellation doesn't need an error flag for Android UX
                            ) 
                        }
                    }
                    is BiometricUnlockResult.NotAvailable,
                    is BiometricUnlockResult.NotEnabled -> {
                        _state.update { 
                            it.copy(
                                isAuthenticating = false, 
                                error = BiometricLockError.NotAvailable
                            ) 
                        }
                    }
                    is BiometricUnlockResult.Error -> {
                        _state.update { 
                            it.copy(
                                isAuthenticating = false, 
                                error = BiometricLockError.Unknown(result.message)
                            ) 
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAuthenticating = false,
                        error = BiometricLockError.Unknown("Unexpected error occurred")
                    )
                }
            }
        }
    }
}
