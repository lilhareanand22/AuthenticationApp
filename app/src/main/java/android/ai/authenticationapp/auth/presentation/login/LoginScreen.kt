package android.ai.authenticationapp.auth.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.ai.authenticationapp.R
import android.ai.authenticationapp.ui.theme.AuthenticationAppTheme

/**
 * Pure state-driven MVI Login Screen.
 * Contains no business logic and does not instantiate ViewModels.
 */
@Composable
fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val loginEnabled = state.email.isNotBlank() && state.password.isNotBlank() && !state.isLoading

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Logo Placeholder
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Sign in to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = state.email,
            onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = state.password,
            onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    // Placeholder for visibility icon
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            }
        )

        // Error Display
        if (state.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val errorMessage = when (state.error) {
                LoginError.InvalidCredentials -> "Invalid email or password"
                LoginError.Network -> "Unable to connect. Please check your network."
                LoginError.RateLimited -> "Too many attempts. Please try again later."
                LoginError.Server -> "Something went wrong. Please try again."
                LoginError.Unknown -> "Unable to login. Please try again."
            }
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot Password Placeholder
        TextButton(
            onClick = { /* Ignored: Out of scope */ },
            modifier = Modifier.align(Alignment.End),
            enabled = !state.isLoading
        ) {
            Text("Forgot password?")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login Button
        Button(
            onClick = { onIntent(LoginIntent.LoginClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = loginEnabled
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Optional Biometric Section
        if (state.isBiometricAvailable && state.isBiometricEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { onIntent(LoginIntent.BiometricClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !state.isLoading
            ) {
                Text("Use biometrics")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Social Login Placeholder
        OutlinedButton(
            onClick = { /* Ignored: Out of scope */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !state.isLoading
        ) {
            Text("Continue with Google")
        }
    }
}

// ---------------------------------------------------------
// Previews
// ---------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun LoginScreenEmptyPreview() {
    AuthenticationAppTheme {
        LoginScreen(
            state = LoginState(),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenFilledPreview() {
    AuthenticationAppTheme {
        LoginScreen(
            state = LoginState(email = "user@example.com", password = "password123"),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenLoadingPreview() {
    AuthenticationAppTheme {
        LoginScreen(
            state = LoginState(email = "user@example.com", password = "password123", isLoading = true),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenInvalidCredentialsPreview() {
    AuthenticationAppTheme {
        LoginScreen(
            state = LoginState(email = "user@example.com", error = LoginError.InvalidCredentials),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenBiometricAvailablePreview() {
    AuthenticationAppTheme {
        LoginScreen(
            state = LoginState(
                email = "user@example.com",
                isBiometricAvailable = true,
                isBiometricEnabled = true
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenNetworkErrorPreview() {
    AuthenticationAppTheme {
        LoginScreen(
            state = LoginState(error = LoginError.Network),
            onIntent = {}
        )
    }
}
