package android.ai.authenticationapp.auth.domain.session

import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.error.AuthError
import android.ai.authenticationapp.auth.domain.error.AuthException
import android.ai.authenticationapp.auth.domain.lock.LocalLockManager
import android.ai.authenticationapp.auth.domain.model.AuthSession
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Domain layer: Orchestrates application-wide authentication state and startup session restoration.
 */
open class SessionManager(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val biometricPreferenceStore: BiometricPreferenceStore,
    private val localLockManager: LocalLockManager
) {

    private val _authState = MutableStateFlow<AuthenticationState>(AuthenticationState.Unknown)
    val authState: StateFlow<AuthenticationState> = _authState.asStateFlow()

    private val restoreMutex = Mutex()

    /**
     * Initializes the session on application startup.
     */
    suspend fun restoreSession() {
        restoreMutex.withLock {
            if (_authState.value !is AuthenticationState.Unknown) return

            try {
                // Check if we have valid or refreshable tokens
                val token = tokenManager.getValidAccessToken()
                if (token == null) {
                    _authState.value = AuthenticationState.Unauthenticated
                    return
                }

                // Tokens exist and are valid/refreshed. We need the current User profile.
                val user = authRepository.getCurrentUser()
                
                if (biometricPreferenceStore.isEnabled()) {
                    localLockManager.lock()
                }

                _authState.value = AuthenticationState.Authenticated(user)

            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthExceptionDuringRestore(e)
            } catch (e: Throwable) {
                // Temporary infrastructure failure (e.g. timeout, 5xx)
                // Cannot fetch User, but tokens exist.
                // We leave state as Unknown (or a degraded Authenticated state if cached User existed).
                // For now, without a persisted User cache, we cannot emit Authenticated(user).
                // So we do not automatically log the user out.
            }
        }
    }

    /**
     * Revalidates the session when returning from background.
     */
    suspend fun revalidateSession() {
        try {
            // This leverages TokenManager's expiry checking.
            // If near-expiry or expired, it performs a single-flight refresh.
            val token = tokenManager.getValidAccessToken()
            if (token == null) {
                _authState.value = AuthenticationState.Unauthenticated
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthException) {
            handleAuthExceptionDuringRestore(e)
        } catch (e: Throwable) {
            // Network failure during background revalidation. Do NOT automatically log out.
        }
    }

    /**
     * Called by LoginUseCase upon successful login completion.
     */
    open fun onLogin(session: AuthSession) {
        _authState.value = AuthenticationState.Authenticated(session.user)
    }

    /**
     * Transitions state to Unauthenticated actively via LogoutUseCase.
     */
    open fun onLogout() {
        _authState.value = AuthenticationState.Unauthenticated
    }

    /**
     * Transitions state to Unauthenticated on terminal auth failure.
     * (Logout coordination and TokenManager.clear() belong to LogoutUseCase, not here).
     */
    private fun handleAuthExceptionDuringRestore(e: AuthException) {
        when (e.error) {
            AuthError.InvalidCredentials,
            AuthError.AccessTokenExpired, // Should only happen if refresh explicitly failed
            AuthError.RefreshTokenInvalid,
            AuthError.SessionRevoked,
            AuthError.Forbidden -> {
                _authState.value = AuthenticationState.Unauthenticated
            }
            AuthError.Network,
            AuthError.RateLimited,
            AuthError.Server,
            AuthError.Unknown -> {
                // Do not automatically logout for temporary failures
            }
        }
    }
}
