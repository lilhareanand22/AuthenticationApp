package android.ai.authenticationapp.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import android.ai.authenticationapp.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation layer: ViewModel orchestrating LoginScreen state, intents, and effects.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effects =
        Channel<LoginEffect>(Channel.BUFFERED)

    val effects: Flow<LoginEffect> =
        _effects.receiveAsFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {

            is LoginIntent.EmailChanged -> {
                _state.update {
                    it.copy(
                        email = intent.email,
                        error = null
                    )
                }
            }

            is LoginIntent.PasswordChanged -> {
                _state.update {
                    it.copy(
                        password = intent.password,
                        error = null
                    )
                }
            }

            LoginIntent.LoginClicked -> {
                handleLoginClicked()
            }

            LoginIntent.BiometricClicked -> {
                // Implement in biometric flow step.
            }

            LoginIntent.GoogleLoginClicked -> {
                // Implement in social login step.
            }
        }
    }

    private fun handleLoginClicked() {
        val currentState = _state.value

        if (currentState.isLoading) return

        if (
            currentState.email.isBlank() ||
            currentState.password.isBlank()
        ) {
            return
        }

        _state.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                loginUseCase(
                    email = currentState.email,
                    password = currentState.password
                )

                _state.update {
                    it.copy(isLoading = false)
                }

                _effects.send(
                    LoginEffect.NavigateToHome
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: AuthException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = mapAuthErrorToLoginError(e.error)
                    )
                }

            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = LoginError.Unknown
                    )
                }
            }
        }
    }

    private fun mapAuthErrorToLoginError(
        authError: AuthError
    ): LoginError {
        return when (authError) {
            AuthError.InvalidCredentials ->
                LoginError.InvalidCredentials

            AuthError.Network ->
                LoginError.Network

            AuthError.RateLimited ->
                LoginError.RateLimited

            AuthError.Server ->
                LoginError.Server

            else ->
                LoginError.Unknown
        }
    }
}
