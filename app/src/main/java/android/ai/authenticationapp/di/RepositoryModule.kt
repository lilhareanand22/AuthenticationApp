package android.ai.authenticationapp.di

import android.ai.authenticationapp.auth.data.repository.DummyJsonAuthRepositoryAdapter
import android.ai.authenticationapp.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: DummyJsonAuthRepositoryAdapter): AuthRepository
}
