package android.ai.authenticationapp.auth.presentation.dashboard

sealed interface DashboardEffect {
    data object NavigateToLogin : DashboardEffect
}
