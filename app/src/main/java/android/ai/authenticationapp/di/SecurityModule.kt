package android.ai.authenticationapp.di

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import android.ai.authenticationapp.auth.data.device.DataStoreBiometricPreferenceStore
import android.ai.authenticationapp.auth.data.device.DataStoreDeviceIdStore
import android.ai.authenticationapp.auth.data.device.DeviceIdProviderImpl
import android.ai.authenticationapp.auth.data.device.DeviceIdStore
import android.ai.authenticationapp.auth.data.local.SessionMetadataStoreImpl
import android.ai.authenticationapp.auth.data.local.SessionMetadataStore
import android.ai.authenticationapp.auth.data.session.DummyJsonSessionMetadataProvider
import android.ai.authenticationapp.auth.data.session.SessionMetadataProvider
import android.ai.authenticationapp.auth.domain.device.BiometricPreferenceStore
import android.ai.authenticationapp.auth.domain.device.DeviceIdProvider
import android.ai.authenticationapp.auth.security.AndroidBiometricAuthenticator
import android.ai.authenticationapp.auth.security.AndroidKeystoreEncryption
import android.ai.authenticationapp.auth.security.BiometricAuthenticator
import android.ai.authenticationapp.auth.security.CredentialStore
import android.ai.authenticationapp.auth.security.DataStoreSecureStorage
import android.ai.authenticationapp.auth.security.Encryption
import android.ai.authenticationapp.auth.security.SecureCredentialStore
import android.ai.authenticationapp.auth.security.SecureStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceIdDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BiometricDataStore

private val Context.deviceIdDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_id_prefs")
private val Context.biometricDataStore: DataStore<Preferences> by preferencesDataStore(name = "biometric_prefs")

/**
 * Hilt module for Security, Persistence, and Device Storage bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindEncryption(impl: AndroidKeystoreEncryption): Encryption

    @Binds
    @Singleton
    abstract fun bindSecureStorage(impl: DataStoreSecureStorage): SecureStorage

    @Binds
    @Singleton
    abstract fun bindCredentialStore(impl: SecureCredentialStore): CredentialStore

    @Binds
    @Singleton
    abstract fun bindDeviceIdStore(impl: DataStoreDeviceIdStore): DeviceIdStore

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: DeviceIdProviderImpl): DeviceIdProvider

    @Binds
    @Singleton
    abstract fun bindSessionMetadataStore(impl: SessionMetadataStoreImpl): SessionMetadataStore

    @Binds
    @Singleton
    abstract fun bindSessionMetadataProvider(impl: DummyJsonSessionMetadataProvider): SessionMetadataProvider

    @Binds
    @Singleton
    abstract fun bindBiometricPreferenceStore(impl: DataStoreBiometricPreferenceStore): BiometricPreferenceStore

    @Binds
    @Singleton
    abstract fun bindBiometricAuthenticator(impl: AndroidBiometricAuthenticator): BiometricAuthenticator

    companion object {

        @Provides
        @Singleton
        @DeviceIdDataStore
        fun provideDeviceIdDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            return context.deviceIdDataStore
        }

        @Provides
        @Singleton
        @BiometricDataStore
        fun provideBiometricDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            return context.biometricDataStore
        }

        @Provides
        @Singleton
        fun provideBiometricManager(@ApplicationContext context: Context): BiometricManager {
            return BiometricManager.from(context)
        }
    }
}
