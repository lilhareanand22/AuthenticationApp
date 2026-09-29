package android.ai.authenticationapp.di

import android.ai.authenticationapp.auth.domain.lock.LocalLockManager
import android.ai.authenticationapp.auth.domain.lock.LocalLockManagerImpl
import android.ai.authenticationapp.auth.domain.session.TokenManager
import android.ai.authenticationapp.auth.domain.session.TokenManagerImpl
import android.ai.authenticationapp.auth.domain.usecase.BiometricUnlockUseCase
import android.ai.authenticationapp.auth.domain.usecase.BiometricUnlockUseCaseImpl
import android.ai.authenticationapp.auth.domain.usecase.LoginUseCase
import android.ai.authenticationapp.auth.domain.usecase.LoginUseCaseImpl
import android.ai.authenticationapp.auth.domain.usecase.LogoutUseCase
import android.ai.authenticationapp.auth.domain.usecase.LogoutUseCaseImpl
import android.ai.authenticationapp.auth.domain.session.TokenExpiryPolicy
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Domain layer interfaces, Managers, and Use Case bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindTokenManager(impl: TokenManagerImpl): TokenManager

    @Binds
    @Singleton
    abstract fun bindLocalLockManager(impl: LocalLockManagerImpl): LocalLockManager

    @Binds
    @Singleton
    abstract fun bindLoginUseCase(impl: LoginUseCaseImpl): LoginUseCase

    @Binds
    @Singleton
    abstract fun bindLogoutUseCase(impl: LogoutUseCaseImpl): LogoutUseCase

    @Binds
    @Singleton
    abstract fun bindBiometricUnlockUseCase(impl: BiometricUnlockUseCaseImpl): BiometricUnlockUseCase

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        @Provides
        @Singleton
        fun provideTokenExpiryPolicy(): TokenExpiryPolicy {
            return TokenExpiryPolicy(refreshSafetyWindow = Duration.ofMinutes(5))
        }
    }
}
