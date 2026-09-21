package android.ai.authenticationapp.auth.presentation.dashboard

sealed interface DashboardError {
    data object Network : DashboardError
    data object Server : DashboardError
    data object Unknown : DashboardError
}
