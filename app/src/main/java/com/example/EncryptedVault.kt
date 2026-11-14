package com.example

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedVault(context: Context) {
  private val file = AtomicFile(File(context.noBackupFilesDir, "tonino-state.bin"))
  private val alias = "tonino-state-v1"
  private val aad = "Tonino local state v1".toByteArray()
  private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    if (store.containsAlias(alias)) return store.getKey(alias, null) as SecretKey
    check(!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) { "Ключ хранилища недоступен. Не удаляйте данные приложения." }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
      init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
    }.generateKey()
  }
  @Synchronized fun read(): String? {
    if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
    val bytes = file.openRead().use { it.readBytes() }
    require(bytes.size in 32..16000000 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(84, 79, 78, 1))) { "Повреждено хранилище" }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(4, 16)))
    cipher.updateAAD(aad)
    return String(cipher.doFinal(bytes.copyOfRange(16, bytes.size)), Charsets.UTF_8)
  }
  @Synchronized fun write(state: String) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key()); cipher.updateAAD(aad)
    check(cipher.iv.size == 12)
    val encrypted = cipher.doFinal(state.toByteArray())
    if (encrypted.size + 16 > 16000000) throw java.io.IOException("Local encrypted store is full")
    val stream = file.startWrite()
    try {
      stream.write(byteArrayOf(84, 79, 78, 1)); stream.write(cipher.iv); stream.write(encrypted)
      stream.fd.sync()
      file.finishWrite(stream)
    } catch (e: Throwable) { file.failWrite(stream); throw e }
  }
}
