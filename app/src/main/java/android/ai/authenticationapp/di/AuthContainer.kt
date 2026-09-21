package android.ai.authenticationapp.di

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.ai.authenticationapp.auth.data.device.DataStoreDeviceIdStore
import android.ai.authenticationapp.auth.data.device.DeviceIdProviderImpl
import android.ai.authenticationapp.auth.data.local.SessionMetadataStore
import android.ai.authenticationapp.auth.data.local.SessionMetadataStoreImpl
import android.ai.authenticationapp.auth.data.remote.AuthApi
import android.ai.authenticationapp.auth.data.remote.AuthRemoteDataSource
import android.ai.authenticationapp.auth.data.remote.AuthRemoteDataSourceImpl
import android.ai.authenticationapp.auth.data.remote.error.NetworkErrorMapper
import android.ai.authenticationapp.auth.data.repository.DummyJsonAuthRepositoryAdapter
import android.ai.authenticationapp.auth.data.session.DummyJsonSessionMetadataProvider
import android.ai.authenticationapp.auth.data.session.SessionMetadataProvider
import android.ai.authenticationapp.auth.domain.device.DeviceIdProvider
import android.ai.authenticationapp.auth.domain.model.User
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import android.ai.authenticationapp.auth.domain.session.SessionManager
import android.ai.authenticationapp.auth.domain.session.TokenExpiryPolicy
import android.ai.authenticationapp.auth.domain.session.TokenManager
import android.ai.authenticationapp.auth.domain.session.TokenManagerImpl
import android.ai.authenticationapp.auth.domain.usecase.LoginUseCase
import android.ai.authenticationapp.auth.domain.usecase.LoginUseCaseImpl
import android.ai.authenticationapp.auth.domain.util.TimeProvider
import android.ai.authenticationapp.auth.presentation.dashboard.DashboardViewModel
import android.ai.authenticationapp.auth.presentation.login.LoginViewModel
import android.ai.authenticationapp.auth.security.AndroidKeystoreEncryption
import android.ai.authenticationapp.auth.security.CredentialStore
import android.ai.authenticationapp.auth.security.DataStoreSecureStorage
import android.ai.authenticationapp.auth.security.Encryption
import android.ai.authenticationapp.auth.security.SecureCredentialStore
import android.ai.authenticationapp.auth.security.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import java.time.Instant

private val Context.appDeviceIdDataStore by preferencesDataStore(name = "device_id_prefs")

/**
 * Manual Dependency Injection container for the Authentication flow.
 * Follows clean architecture boundaries enforcing constructor injection safely.
 */
@SuppressLint("NewApi")
class AuthContainer(private val context: Context) {

    // 1. Core Infrastructure & Scopes
    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    val timeProvider: TimeProvider = object : TimeProvider {
        override fun now(): Instant = Instant.now()
    }

    // 2. Networking
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://dummyjson.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val errorMapper = NetworkErrorMapper()

    val authRemoteDataSource: AuthRemoteDataSource by lazy {
        AuthRemoteDataSourceImpl(authApi, errorMapper)
    }

    // 3. Security & Persistence (Data)
    val secureStorage: SecureStorage by lazy { DataStoreSecureStorage(context) }
    val encryption: Encryption by lazy { AndroidKeystoreEncryption() }
    val credentialStore: CredentialStore by lazy { SecureCredentialStore(secureStorage, encryption) }

    val deviceIdStore by lazy { DataStoreDeviceIdStore(context.applicationContext.appDeviceIdDataStore) }
    val deviceIdProvider: DeviceIdProvider by lazy { DeviceIdProviderImpl(deviceIdStore) }

    val sessionMetadataStore: SessionMetadataStore by lazy { SessionMetadataStoreImpl(context) }
    val sessionMetadataProvider: SessionMetadataProvider by lazy { DummyJsonSessionMetadataProvider(timeProvider) }

    // 4. Repositories (Data)
    val authRepository: AuthRepository by lazy {
        DummyJsonAuthRepositoryAdapter(
            remoteDataSource = authRemoteDataSource,
            sessionMetadataProvider = sessionMetadataProvider
        )
    }

    // 5. Domain Lifecycle & UseCases (Domain)
    val tokenExpiryPolicy = TokenExpiryPolicy(refreshSafetyWindow = Duration.ofMinutes(5))

    val tokenManager: TokenManager by lazy {
        TokenManagerImpl(
            credentialStore = credentialStore,
            authRepository = authRepository,
            tokenExpiryPolicy = tokenExpiryPolicy,
            applicationScope = applicationScope,
            nowProvider = { timeProvider.now() }
        )
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(
            tokenManager = tokenManager,
            authRepository = authRepository
        )
    }

    val loginUseCase: LoginUseCase by lazy {
        LoginUseCaseImpl(
            authRepository = authRepository,
            deviceIdProvider = deviceIdProvider,
            credentialStore = credentialStore,
            sessionMetadataStore = sessionMetadataStore,
            sessionManager = sessionManager
        )
    }

    // 6. Presentation Factories
    val loginViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(loginUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    // Temporary solution for injecting the User into Dashboard until full restoration is built
    var currentUserForDashboard: User? = null

    val dashboardViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                val user = requireNotNull(currentUserForDashboard) { "User must be set before navigating to Dashboard" }
                return DashboardViewModel(user) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
