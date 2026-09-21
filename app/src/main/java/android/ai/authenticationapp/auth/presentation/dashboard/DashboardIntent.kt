package android.ai.authenticationapp.auth.presentation.dashboard

sealed interface DashboardIntent {
    data object LogoutClicked : DashboardIntent
}
