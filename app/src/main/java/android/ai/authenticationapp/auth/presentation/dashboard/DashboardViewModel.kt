package android.ai.authenticationapp.auth.presentation.dashboard

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.lock.LocalLockManager
import android.ai.authenticationapp.auth.domain.lock.LocalUnlockState
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.model.Credentials
import android.ai.authenticationapp.auth.domain.model.LoginRequest
import android.ai.authenticationapp.auth.domain.model.Session
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.domain.session.AuthenticationState
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.domain.session.TokenManager
import android.ai.authenticationapp.auth.domain.usecase.LogoutUseCase
import android.ai.authenticationapp.auth.security.BiometricAuthenticator
import android.ai.authenticationapp.auth.security.BiometricAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val biometricPreferenceStore: BiometricPreferenceStore,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Secondary constructor for testing where user is provided directly
    constructor(
        user: User,
        logoutUseCase: LogoutUseCase,
        biometricPreferenceStore: BiometricPreferenceStore,
        biometricAuthenticator: BiometricAuthenticator
    ) : this(
        logoutUseCase = logoutUseCase,
        biometricPreferenceStore = biometricPreferenceStore,
        biometricAuthenticator = biometricAuthenticator,
        sessionManager = createTestSessionManager(user)
    )

    private val currentUser: User
        get() = (sessionManager.authState.value as? AuthenticationState.Authenticated)?.user
            ?: User("1", "user@example.com", "User")

    private val _state = MutableStateFlow(DashboardState(user = currentUser))
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

@SuppressLint("NewApi")
private fun createTestSessionManager(user: User): SessionManager {
    val sm = SessionManager(
        tokenManager = object : TokenManager {
            override suspend fun getValidAccessToken(): String? = null
            override suspend fun refresh(): String? = null
            override suspend fun clear() {}
        },
        authRepository = object : AuthRepository {
            override suspend fun login(request: LoginRequest): AuthSession = throw NotImplementedError()
            override suspend fun getCurrentUser(): User = user
            override suspend fun refreshToken(refreshToken: String): Credentials = throw NotImplementedError()
            override suspend fun logout(sessionId: String, deviceId: String) {}
        },
        biometricPreferenceStore = object : BiometricPreferenceStore {
            override suspend fun isEnabled(): Boolean = false
            override suspend fun setEnabled(enabled: Boolean) {}
        },
        localLockManager = object : LocalLockManager {
            override val state = MutableStateFlow<LocalUnlockState>(LocalUnlockState.Unlocked)
            override fun lock() {}
            override fun unlock() {}
        }
    )
    sm.onLogin(
        AuthSession(
            user = user,
            session = Session("s", "d", user.id),
            credentials = Credentials("a", "r", Instant.now())
        )
    )
    return sm
}
