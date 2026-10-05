package com.workcontrol.app.data.auth

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_ALIAS = "prelo_mobile_refresh_v1"

interface RefreshStore {
    fun exists(): Boolean
    fun read(): String?
    fun write(value: String)
    fun clear()
}

/** Refresh is encrypted at rest; key use requires recent local device authentication. */
@Singleton
class SecureRefreshStore @Inject constructor(@ApplicationContext context: Context) : RefreshStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "mobile_refresh.aesgcm"))
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override fun exists(): Boolean = file.baseFile.exists()

    private fun key(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= 30) {
            spec.setUserAuthenticationParameters(60, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            @Suppress("DEPRECATION")
            spec.setUserAuthenticationValidityDurationSeconds(60)
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(spec.build())
        return generator.generateKey()
    }

    override fun read(): String? {
        if (!exists()) return null
        val bytes = file.openRead().use { it.readBytes() }
        require(bytes.size > 13 && bytes[0] == 1.toByte()) { "invalid encrypted session" }
        val iv = bytes.copyOfRange(1, 13)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(bytes.copyOfRange(13, bytes.size)), Charsets.UTF_8)
    }

    override fun write(value: String) {
        require(value.isNotBlank())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try {
            output.write(byteArrayOf(1) + cipher.iv + encrypted)
            file.finishWrite(output)
        } catch (failure: Throwable) {
            file.failWrite(output)
            throw failure
        }
    }

    override fun clear() {
        file.delete()
        keyStore.deleteEntry(KEY_ALIAS)
    }
}
