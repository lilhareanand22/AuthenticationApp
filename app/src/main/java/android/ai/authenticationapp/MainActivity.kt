package android.ai.authenticationapp

import android.ai.authenticationapp.auth.presentation.login.LoginRoute
import android.ai.authenticationapp.auth.presentation.login.LoginViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import android.ai.authenticationapp.ui.theme.AuthenticationAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get the application-level AuthContainer to supply the factory
        val app = application as AuthenticationApplication
        val factory = app.authContainer.loginViewModelFactory

        setContent {
            AuthenticationAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: LoginViewModel = viewModel(factory = factory)
                    
                    LoginRoute(
                        viewModel = viewModel,
                        onNavigateToHome = {
                            // TODO: Add real navigation graph logic later. 
                            // For now, this cleanly signals successful MVI flow.
                            println("Authentication Success - Navigating to Home!")
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
