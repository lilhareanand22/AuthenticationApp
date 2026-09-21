package android.ai.authenticationapp

import android.ai.authenticationapp.auth.domain.session.AuthenticationState
import android.ai.authenticationapp.auth.presentation.dashboard.DashboardRoute
import android.ai.authenticationapp.auth.presentation.dashboard.DashboardViewModel
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.ai.authenticationapp.ui.theme.AuthenticationAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get the application-level AuthContainer to supply the factory
        val app = application as AuthenticationApplication
        val authContainer = app.authContainer
        val loginFactory = authContainer.loginViewModelFactory
        val dashboardFactory = authContainer.dashboardViewModelFactory

        setContent {
            AuthenticationAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            val viewModel: LoginViewModel = viewModel(factory = loginFactory)
                            
                            LoginRoute(
                                viewModel = viewModel,
                                onNavigateToHome = {
                                    // Extract the authenticated user from the flow (if we need to pass it, we can grab it from ViewModel or SessionManager).
                                    // Since LoginViewModel updates LoginState.email/etc. we can actually just read the SessionManager's state,
                                    // but we have a temporary `currentUserForDashboard` property in `AuthContainer`.
                                    // Because LoginUseCase returns an AuthSession, ideally SessionManager.authState exposes it.
                                    
                                    // For this step, let's grab the user from the SessionManager's current state since LoginUseCase calls sessionManager.onLogin().
                                    val authState = authContainer.sessionManager.authState.value
                                    if (authState is AuthenticationState.Authenticated) {
                                        authContainer.currentUserForDashboard = authState.user
                                    }

                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            val viewModel: DashboardViewModel = viewModel(factory = dashboardFactory)

                            DashboardRoute(
                                viewModel = viewModel,
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
