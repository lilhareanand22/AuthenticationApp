package android.ai.authenticationapp.auth.security

/**
 * Security layer: Encryption abstraction interface for encrypting and decrypting sensitive data strings.
 */
interface Encryption {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}
