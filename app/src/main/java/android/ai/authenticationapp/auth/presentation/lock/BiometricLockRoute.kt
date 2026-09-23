package android.ai.authenticationapp.auth.presentation.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BiometricLockRoute(
    viewModel: BiometricLockViewModel,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // We strictly enforce FragmentActivity requirement at the composition level to pass cleanly to the Intent
    val activity = LocalContext.current as? FragmentActivity 
        ?: throw IllegalStateException("BiometricLockRoute requires a FragmentActivity context")

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BiometricLockEffect.UnlockSuccess -> onUnlockSuccess()
            }
        }
    }

    BiometricLockScreen(
        state = state,
        activity = activity,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
