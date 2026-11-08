package com.machiav3lli.backup.manager.handler

import android.content.Context
import android.net.Uri
import com.machiav3lli.backup.ui.pages.pref_pgpKey
import com.machiav3lli.backup.ui.pages.pref_pgpPasscode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.openpgp.api.MessageEncryptionMechanism
import org.bouncycastle.openpgp.api.OpenPGPCertificate
import org.bouncycastle.openpgp.api.OpenPGPKey
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.decryption_verification.DecryptionStream
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.EncryptionStream
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.InputStream
import java.io.OutputStream

// https://pgpainless.readthedocs.io
class PGPHandler(private val context: Context) {
    private val pgp = PGPainless.getInstance()
    val cc = Dispatchers.IO + SupervisorJob()

    private var secretKey: OpenPGPKey? = readSecretKey()
    private var certificate: OpenPGPCertificate? = secretKey?.toCertificate()

    private fun readSecretKey(): OpenPGPKey? = runCatching {
        pref_pgpKey.value
            .takeIf { it.isNotBlank() }
            ?.let { pgp.readKey().parseKey(it) }
    }.getOrNull()

    fun isKeyLoaded() = secretKey != null

    private fun reloadPublicKey() {
        certificate = secretKey?.toCertificate()
    }

    fun reloadKeys() = runCatching {
        secretKey = readSecretKey()
        reloadPublicKey()
    }

    private fun keyProtector(): SecretKeyRingProtector =
        pref_pgpPasscode.value.takeIf { it.isNotBlank() }
            ?.let { SecretKeyRingProtector.unlockAnyKeyWith(Passphrase(it.toCharArray())) }
            ?: SecretKeyRingProtector.unprotectedKeys()

    private fun encryptionOptions(certificate: OpenPGPCertificate): ProducerOptions =
        ProducerOptions.encrypt(
            EncryptionOptions.encryptDataAtRest(pgp)
                .addHiddenRecipient(certificate)
                .overrideEncryptionMechanism(
                    MessageEncryptionMechanism.integrityProtected(SymmetricKeyAlgorithmTags.AES_256)
                )
        )

    suspend fun loadKeyFromUri(uri: Uri): Result<Boolean> = runCatching {
        withContext(cc) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val byteArray = inputStream.readBytes()
                pref_pgpKey.value = byteArray.decodeToString()
                reloadKeys()
                true
            } == true
        }
    }

    suspend fun generateNewKey(
        userId: String,
        passcode: String,
        saveLocation: Uri
    ): Boolean {
        return withContext(cc) {
            runCatching {
                val key = pgp.generateKey()
                    .modernKeyRing(userId, passcode)
                val asciiKey = key.toAsciiArmoredString()

                context.contentResolver.openOutputStream(saveLocation)?.use { output ->
                    output.write(asciiKey.toByteArray())
                }
                secretKey = key
                reloadPublicKey()
                pref_pgpKey.value = asciiKey
                pref_pgpPasscode.value = passcode
                true
            }.getOrDefault(false)
        }
    }

    fun encryptStream(outputStream: OutputStream): Result<EncryptionStream?> =
        runCatching {
            val cert = certificate ?: throw IllegalStateException("No PGP key loaded")
            pgp.generateMessage()
                .onOutputStream(outputStream)
                .withOptions(encryptionOptions(cert))
        }

    fun decryptStream(inputStream: InputStream): Result<DecryptionStream> =
        runCatching {
            val key = secretKey ?: throw IllegalStateException("No PGP key loaded")

            pgp.processMessage()
                .onInputStream(inputStream)
                .withOptions(
                    ConsumerOptions.get(pgp)
                        .addDecryptionKey(key, keyProtector())
                )
        }
}
