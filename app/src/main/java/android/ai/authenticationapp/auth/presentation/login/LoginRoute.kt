package android.ai.authenticationapp.auth.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier

/**
 * Container layer connecting the [LoginViewModel] state and effects to the pure [LoginScreen].
 */
@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Collect the persistent UI state from the ViewModel immutably, respecting Android lifecycle
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Collect one-time side effects (e.g., navigation) safely inside a LaunchedEffect 
    // bound to the composable's lifecycle and ViewModel's instance.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateToHome -> {
                    onNavigateToHome()
                }
            }
        }
    }

    // Render the pure UI component
    LoginScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
