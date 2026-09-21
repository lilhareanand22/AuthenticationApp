package android.ai.authenticationapp.auth.presentation.dashboard

import android.ai.authenticationapp.auth.domain.model.User

data class DashboardState(
    val user: User? = null,
    val isLoggingOut: Boolean = false,
    val error: DashboardError? = null
)
