package android.ai.authenticationapp.auth.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Security layer: Android Keystore backed implementation of [Encryption] using AES-256-GCM.
 */
class AndroidKeystoreEncryption : Encryption {

    companion object {
        private const val KEY_ALIAS = "auth_credential_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getOrCreateKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE,
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("SecretKey not found in AndroidKeyStore under alias $KEY_ALIAS")
        return entry.secretKey
    }

    override fun encrypt(plainText: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Do not pass an IV spec for encryption when using Android Keystore.
            // Android Keystore requires generating its own IV for security.
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

            // Get the generated IV
            val iv = cipher.iv

            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext (IV + Ciphertext/Tag)
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed", e)
        }
    }

    override fun decrypt(cipherText: String): String {
        try {
            val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
            if (decoded.size < GCM_IV_LENGTH) {
                throw IllegalArgumentException("Invalid cipherText payload: too short to contain IV")
            }

            val iv = decoded.copyOfRange(0, GCM_IV_LENGTH)
            val actualCiphertext = decoded.copyOfRange(GCM_IV_LENGTH, decoded.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

            val plainBytes = cipher.doFinal(actualCiphertext)
            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }
}
