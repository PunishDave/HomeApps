package com.punishdave.homeapps

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CredentialCipher {
    private const val Alias = "homeapps_credentials_v1"
    private const val Prefix = "enc:v1:"

    fun encrypt(value: String): String {
        if (value.isEmpty() || value.startsWith(Prefix)) return value
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return Prefix + Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        if (!value.startsWith(Prefix)) return value
        return decryptEncrypted(value).getOrDefault("")
    }

    /** Returns the original encrypted value when it cannot be decrypted.
     * This is intentionally safe for migrations: a temporary Keystore failure must never
     * turn a stored credential into an empty string. */
    fun migrate(value: String): String = when {
        value.isEmpty() || value.startsWith(Prefix) -> value
        else -> encrypt(value)
    }

    private fun decryptEncrypted(value: String): Result<String> = runCatching {
            val payload = Base64.decode(value.removePrefix(Prefix), Base64.NO_WRAP)
            require(payload.size > 12) { "Invalid encrypted credential" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
            String(cipher.doFinal(payload.copyOfRange(12, payload.size)))
        }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(Alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(Alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
}
