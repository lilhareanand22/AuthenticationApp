package android.ai.authenticationapp.auth.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    DashboardScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
