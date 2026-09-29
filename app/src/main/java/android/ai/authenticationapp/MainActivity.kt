package android.ai.authenticationapp

import android.ai.authenticationapp.auth.domain.lock.LocalLockManager
import android.ai.authenticationapp.auth.domain.lock.LocalUnlockState
import android.ai.authenticationapp.auth.domain.session.AuthenticationState
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.presentation.dashboard.DashboardRoute
import android.ai.authenticationapp.auth.presentation.dashboard.DashboardViewModel
import android.ai.authenticationapp.auth.presentation.lock.BiometricLockRoute
import android.ai.authenticationapp.auth.presentation.lock.BiometricLockViewModel
import android.ai.authenticationapp.auth.presentation.login.LoginRoute
import android.ai.authenticationapp.auth.presentation.login.LoginViewModel
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.ai.authenticationapp.ui.theme.AuthenticationAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var localLockManager: LocalLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AuthenticationAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    val authState by sessionManager.authState.collectAsStateWithLifecycle()
                    val lockState by localLockManager.state.collectAsStateWithLifecycle()

                    // Restore session on app launch
                    LaunchedEffect(Unit) {
                        sessionManager.restoreSession()
                    }

                    // React to Root Navigation State changes
                    LaunchedEffect(authState, lockState) {
                        when (authState) {
                            is AuthenticationState.Unknown -> {
                                // Waiting for session restoration
                            }
                            is AuthenticationState.Unauthenticated -> {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            is AuthenticationState.Authenticated -> {
                                if (lockState == LocalUnlockState.Locked) {
                                    navController.navigate("lock") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("dashboard") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        composable("login") {
                            val viewModel: LoginViewModel = hiltViewModel()
                            
                            LoginRoute(
                                viewModel = viewModel,
                                onNavigateToHome = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("lock") {
                            val viewModel: BiometricLockViewModel = hiltViewModel()

                            BiometricLockRoute(
                                viewModel = viewModel,
                                onUnlockSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("lock") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            val viewModel: DashboardViewModel = hiltViewModel()

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
