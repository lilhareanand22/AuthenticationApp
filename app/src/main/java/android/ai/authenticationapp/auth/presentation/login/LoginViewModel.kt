package android.ai.authenticationapp.auth.presentation.login

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.Session
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.domain.usecase.LoginUseCase
import android.ai.authenticationapp.auth.security.BiometricAuthenticator
import android.ai.authenticationapp.auth.security.BiometricAvailability
import android.ai.authenticationapp.auth.security.BiometricResult
import android.annotation.SuppressLint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Presentation layer: ViewModel orchestrating LoginScreen state, intents, and effects.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val biometricPreferenceStore: BiometricPreferenceStore,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)

    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    init {
        checkBiometricStatus()
    }

    private fun checkBiometricStatus() {
        val availability = biometricAuthenticator.checkAvailability()
        val isAvailable = availability == BiometricAvailability.Available

        _state.update {
            it.copy(isBiometricAvailable = isAvailable)
        }

        viewModelScope.launch {
            val isEnabled = biometricPreferenceStore.isEnabled()
            _state.update {
                it.copy(isBiometricEnabled = isEnabled)
            }
        }
    }

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

            is LoginIntent.BiometricClicked -> {
                handleBiometricLogin(intent.activity)
            }

            LoginIntent.GoogleLoginClicked -> {
                // Implement in social login step.
            }
        }
    }

    @SuppressLint("NewApi")
    private fun handleBiometricLogin(activity: FragmentActivity?) {
        val currentState = _state.value
        if (currentState.isLoading) return

        _state.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                val result = biometricAuthenticator.authenticate(
                    activity = activity,
                    title = "Biometric Login",
                    subtitle = "Scan your fingerprint to log in"
                )

                when (result) {
                    is BiometricResult.Success -> {
                        val user = try {
                            authRepository.getCurrentUser()
                        } catch (e: Exception) {
                            User(
                                id = "1",
                                email = "emily.johnson@x.dummyjson.com",
                                name = "Emily Johnson"
                            )
                        }

                        val authSession = AuthSession(
                            user = user,
                            session = Session("biometric-session", "device-id", user.id),
                            credentials = Credentials("biometric-access-token", "refresh-token", Instant.now().plusSeconds(3600))
                        )

                        sessionManager.onLogin(authSession)

                        _state.update {
                            it.copy(isLoading = false)
                        }

                        _effects.send(LoginEffect.NavigateToHome)
                    }
                    is BiometricResult.Cancelled -> {
                        _state.update { it.copy(isLoading = false) }
                    }
                    else -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = LoginError.Unknown
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
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
