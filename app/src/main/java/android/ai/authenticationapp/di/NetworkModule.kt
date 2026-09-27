package android.ai.authenticationapp.di

import android.ai.authenticationapp.auth.data.remote.AuthApi
import android.ai.authenticationapp.auth.data.remote.AuthRemoteDataSource
import android.ai.authenticationapp.auth.data.remote.AuthRemoteDataSourceImpl
import android.ai.authenticationapp.auth.data.remote.error.NetworkErrorMapper
import android.ai.authenticationapp.auth.domain.session.TokenManager
import android.ai.authenticationapp.network.AuthAuthenticator
import android.ai.authenticationapp.network.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Hilt module for Network layer dependencies (Retrofit, OkHttp, AuthApi, DataSources).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkErrorMapper(): NetworkErrorMapper {
        return NetworkErrorMapper()
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManagerProvider: Provider<TokenManager>
    ): AuthInterceptor {
        return AuthInterceptor { tokenManagerProvider.get() }
    }

    @Provides
    @Singleton
    fun provideAuthAuthenticator(
        tokenManagerProvider: Provider<TokenManager>
    ): AuthAuthenticator {
        return AuthAuthenticator { tokenManagerProvider.get() }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authAuthenticator: AuthAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://dummyjson.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(
        authApi: AuthApi,
        errorMapper: NetworkErrorMapper
    ): AuthRemoteDataSource {
        return AuthRemoteDataSourceImpl(authApi, errorMapper)
    }
}
