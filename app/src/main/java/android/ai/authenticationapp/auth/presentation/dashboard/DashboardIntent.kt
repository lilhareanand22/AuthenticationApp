package android.ai.authenticationapp.auth.presentation.dashboard

sealed interface DashboardIntent {
    data object LogoutClicked : DashboardIntent
    data class BiometricEnabledChanged(val enabled: Boolean) : DashboardIntent
}
