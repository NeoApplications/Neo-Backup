/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.utils

import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Crypto. The class to handle encryption and decryption of streams.
 * Call `encryptStream` or `decryptStream` with a password and a salt or a better a secret key
 * (for performance reasons) and the class will wrap the given stream in return.
 *
 *
 * Android Keystore API is not used on purpose, because the key material needs to be portable for
 * uses cases when the device has been wiped or when backups are restored on another device.
 */

/**
 * Default salt, if no user specified salt is available to improve security.
 * Better a constant salt for the app that using no salt.
 */
val FALLBACK_SALT = "oandbackupx".toByteArray(StandardCharsets.UTF_8)
private const val ENCRYPTION_SETUP_FAILED = "Could not setup encryption"

/**
 * https://developer.android.com/guide/topics/security/cryptography#Cipher
 * TODO Starting SDK28 ChaCha20 is supported, which is far more faster than standard AES
 * Maybe will implement it in the future as an option AES/ChaCha20
 *
 * The original choice was inspired by this blog post:
 * https://www.raywenderlich.com/778533-encryption-tutorial-for-android-getting-started
 */
private const val DEFAULT_SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2withHmacSHA256"
const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
val DEFAULT_IV = ByteArray(Cipher.getInstance(CIPHER_ALGORITHM).blockSize) { 0 }
private const val DEFAULT_IV_BLOCK_SIZE = 32 // 256 bit
private const val ITERATION_COUNT = 2020
private const val KEY_LENGTH = 256

@Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
fun generateKeyFromPassword(
    password: String,
    salt: ByteArray?,
    keyFactoryAlgorithm: String? = DEFAULT_SECRET_KEY_FACTORY_ALGORITHM,
    cipherAlgorithm: String = CIPHER_ALGORITHM
): SecretKey {
    val factory = SecretKeyFactory.getInstance(keyFactoryAlgorithm)
    val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
    val keyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(keyBytes, cipherAlgorithm.split(File.separator).toTypedArray()[0])
}

class CryptoSetupException(cause: Throwable? = null) : Exception(ENCRYPTION_SETUP_FAILED, cause)

fun handleCryptoException(e: Throwable): Throwable {
    Timber.e("$ENCRYPTION_SETUP_FAILED: ${e.message}")
    return CryptoSetupException(e)
}

@Throws(CryptoSetupException::class)
fun OutputStream.encryptStream(
    password: String,
    salt: ByteArray?,
    iv: ByteArray?
): CipherOutputStream = try {
    val secret = generateKeyFromPassword(password, salt)
    this.encryptStream(secret, iv)
} catch (e: NoSuchAlgorithmException) {
    throw handleCryptoException(e)
} catch (e: InvalidKeySpecException) {
    throw handleCryptoException(e)
}

@Throws(CryptoSetupException::class)
fun OutputStream.encryptStream(
    secret: SecretKey?,
    iv: ByteArray?,
    cipherAlgorithm: String = CIPHER_ALGORITHM
): CipherOutputStream = try {
    val cipher = Cipher.getInstance(cipherAlgorithm)
    val ivParams = IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, secret, ivParams)
    CipherOutputStream(this, cipher)
} catch (e: NoSuchAlgorithmException) {
    throw handleCryptoException(e)
} catch (e: InvalidKeyException) {
    throw handleCryptoException(e)
} catch (e: InvalidAlgorithmParameterException) {
    throw handleCryptoException(e)
} catch (e: NoSuchPaddingException) {
    throw handleCryptoException(e)
}

@Throws(CryptoSetupException::class)
fun InputStream.decryptStream(
    password: String,
    salt: ByteArray?,
    iv: ByteArray?
): CipherInputStream = try {
    val secret = generateKeyFromPassword(password, salt)
    decryptStream(secret, iv)
} catch (e: NoSuchAlgorithmException) {
    throw handleCryptoException(e)
} catch (e: InvalidKeySpecException) {
    throw handleCryptoException(e)
}

@Throws(CryptoSetupException::class)
fun InputStream.decryptStream(
    secret: SecretKey?,
    iv: ByteArray?,
    cipherAlgorithm: String = CIPHER_ALGORITHM
): CipherInputStream = try {
    val cipher = Cipher.getInstance(cipherAlgorithm)
    val ivParams = IvParameterSpec(iv ?: DEFAULT_IV)
    cipher.init(Cipher.DECRYPT_MODE, secret, ivParams)
    CipherInputStream(this, cipher)
} catch (e: NoSuchPaddingException) {
    throw handleCryptoException(e)
} catch (e: NoSuchAlgorithmException) {
    throw handleCryptoException(e)
} catch (e: InvalidAlgorithmParameterException) {
    throw handleCryptoException(e)
} catch (e: InvalidKeyException) {
    throw handleCryptoException(e)
}

fun initIv(cipherAlgorithm: String): ByteArray {
    val blockSize: Int = try {
        val cipher = Cipher.getInstance(cipherAlgorithm)
        cipher.blockSize
    } catch (e: NoSuchAlgorithmException) {
        // Fallback if the cipher has issues. Might lead to another exception later, but saves
        // the situation here. The use cipher might not match or will cause other exceptions
        // when used like this.
        DEFAULT_IV_BLOCK_SIZE
    } catch (e: NoSuchPaddingException) {
        DEFAULT_IV_BLOCK_SIZE
    }
    // IV is nothing secret. Could also be constant, but why not spend a few cpu cycles to have
    // it dynamic, if the algorithm changes?
    return Random.nextBytes(blockSize)
}
