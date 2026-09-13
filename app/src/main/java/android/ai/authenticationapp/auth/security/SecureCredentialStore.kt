package android.ai.authenticationapp.auth.security

import android.annotation.SuppressLint
import android.ai.authenticationapp.auth.domain.model.Credentials

/**
 * Security layer: Concrete implementation of [CredentialStore] providing secure credential persistence
 * by orchestrating [SecureStorage], [Encryption], and [CredentialsSerializer].
 */
class SecureCredentialStore(
    private val secureStorage: SecureStorage,
    private val encryption: Encryption,
) : CredentialStore {

    companion object {
        private const val CREDENTIALS_KEY = "auth_credentials"
    }

    @SuppressLint("NewApi")
    override suspend fun save(credentials: Credentials) {
        val entity = credentials.toEntity()
        val json = CredentialsSerializer.serialize(entity)
        val encryptedJson = encryption.encrypt(json)
        secureStorage.saveEncrypted(CREDENTIALS_KEY, encryptedJson)
    }

    @SuppressLint("NewApi")
    override suspend fun get(): Credentials? {
        val encryptedJson = secureStorage.readEncrypted(CREDENTIALS_KEY) ?: return null
        val json = encryption.decrypt(encryptedJson)
        val entity = CredentialsSerializer.deserialize(json) ?: return null
        return entity.toDomain()
    }

    override suspend fun clear() {
        secureStorage.clearAll()
    }
}

/**
 * Security layer: Abstract platform/storage boundary for encrypted persistence.
 */
interface SecureStorage {
    suspend fun saveEncrypted(key: String, value: String)
    suspend fun readEncrypted(key: String): String?
    suspend fun clearAll()
}
