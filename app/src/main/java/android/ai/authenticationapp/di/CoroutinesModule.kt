package android.ai.authenticationapp.di

import android.annotation.SuppressLint
import android.ai.authenticationapp.auth.domain.util.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Instant
import javax.inject.Singleton

/**
 * Hilt module for application-wide Coroutine scopes and Time utilities.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @SuppressLint("NewApi")
    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider {
        return object : TimeProvider {
            override fun now(): Instant = Instant.now()
        }
    }
}
