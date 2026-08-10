package com.punishdave.homeapps

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class SettingsBackup(
    val mealKey: String = "",
    val todoKey: String = "",
    val todoCategory: String = "",
    val todoHabit: String = "",
    val workoutKey: String = "",
    val gameWithDaveKey: String = "",
    val gameWithDaveUsername: String = "",
    val gameWithDavePassword: String = "",
    val sophonUrl: String = "http://192.168.0.234:8096",
    val transmissionUsername: String = "",
    val transmissionPassword: String = "",
    val notificationsEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val gameNotificationsEnabled: Boolean = true,
    val temperatureNotificationsEnabled: Boolean = false,
    val lowTemperature: String = "8",
    val highTemperature: String = "25",
    val automaticRefreshEnabled: Boolean = false,
    val backgroundRefreshEnabled: Boolean = true,
    val refreshIntervalMinutes: Int = 30,
    val unmeteredOnly: Boolean = false
)

object SettingsBackupCodec {
    private const val Header = "HOMEAPPS-BACKUP-1"
    private const val Iterations = 210_000
    private val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(SettingsBackup::class.java)

    fun encode(value: SettingsBackup, password: String): String {
        require(password.length >= 8) { "Backup password is too short" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(adapter.toJson(value).toByteArray(StandardCharsets.UTF_8))
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf(Header, Iterations.toString(), encoder.encodeToString(salt), encoder.encodeToString(iv), encoder.encodeToString(encrypted)).joinToString(":")
    }

    fun decode(encoded: String, password: String): SettingsBackup {
        val parts = encoded.trim().split(':')
        require(parts.size == 5 && parts[0] == Header) { "Unsupported backup" }
        require(parts[1].toInt() == Iterations) { "Unsupported backup encryption" }
        val decoder = Base64.getUrlDecoder()
        val salt = decoder.decode(parts[2])
        val iv = decoder.decode(parts[3])
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        val json = String(cipher.doFinal(decoder.decode(parts[4])), StandardCharsets.UTF_8)
        return requireNotNull(adapter.fromJson(json)) { "Invalid backup" }
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, Iterations, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }
}
