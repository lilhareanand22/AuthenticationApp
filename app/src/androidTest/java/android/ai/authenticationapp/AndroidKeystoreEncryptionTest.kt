package android.ai.authenticationapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import android.ai.authenticationapp.auth.security.AndroidKeystoreEncryption
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreEncryptionTest {

    @Test
    fun testEncryption() {
        val encryption = AndroidKeystoreEncryption()
        val plainText = "Hello, world!"
        val cipherText = encryption.encrypt(plainText)
        val decryptedText = encryption.decrypt(cipherText)
        assertEquals(plainText, decryptedText)
    }
}
