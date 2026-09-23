package android.ai.authenticationapp.auth.presentation.dashboard

import android.ai.authenticationapp.auth.domain.model.User

data class DashboardState(
    val user: User,
    val isLoggingOut: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val error: DashboardError? = null
)
