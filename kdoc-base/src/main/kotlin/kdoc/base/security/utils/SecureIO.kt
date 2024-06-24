/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.base.security.utils

import kdoc.base.settings.AppSettings
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides methods to securely encrypt and decrypt IO data streams.
 * This utility class facilitates the use of symmetric key encryption for data protection.
 */
object SecureIO {
    /** Length in `bytes` of the initialization vector (IV) used in GCM mode of operation. */
    private const val GCM_IV_LENGTH: Int = 12

    /** Length in `bits` of the authentication tag used in GCM mode of operation. */
    private const val GCM_TAG_LENGTH: Int = 128

    private enum class Mode(val id: Int) {
        ENCRYPT(id = Cipher.ENCRYPT_MODE),
        DECRYPT(id = Cipher.DECRYPT_MODE)
    }

    /**
     * Creates a [Cipher] instance configured for encryption or decryption.
     *
     * @param mode The operation [Mode] of the cipher.
     * @param iv Optional initialization vector for the cipher when decrypting. It must be provided for decryption.
     * @return A [Cipher] instance configured with the specified mode and initialization vector.
     * @throws IllegalArgumentException if IV is null during decryption mode initialization.
     */
    private fun getCipher(mode: Mode, iv: ByteArray? = null): Cipher {
        val key: String = AppSettings.storage.cipherKey
        val algorithm: String = AppSettings.storage.cipherAlgorithm
        val transformation: String = AppSettings.storage.cipherTransformation

        // Create a secret key from the provided key.
        val normalizedKey: ByteArray = EncryptionUtils.hexStringToByteArray(key)
        val secretKey = SecretKeySpec(normalizedKey, algorithm)
        val cipher: Cipher = Cipher.getInstance(transformation)

        // Initialize the cipher with the secret key and IV.
        if (mode == Mode.ENCRYPT) {
            val ivBytes: ByteArray = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val ivSpec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
            cipher.init(mode.id, secretKey, ivSpec)
        } else {
            requireNotNull(iv) { "IV is required for decryption" }
            val ivSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(mode.id, secretKey, ivSpec)
        }

        return cipher
    }

    /**
     * Ciphers the input stream and writes the result to the output stream.
     *
     * @param input The input stream containing the plain data.
     * @param output The output stream where the encrypted data will be written.
     */
    fun cipher(input: InputStream, output: OutputStream) {
        val cipher: Cipher = getCipher(mode = Mode.ENCRYPT)
        cipher.iv.let {
            output.write(it.size)
            output.write(it)
        }
        CipherOutputStream(output, cipher).use { outputStream ->
            input.copyTo(out = outputStream)
        }
    }

    /**
     * Deciphers the input stream and writes the result to the output stream.
     *
     * @param input The encrypted input stream to decipher.
     * @param output The output stream to write the deciphered data to.
     */
    fun decipher(input: InputStream, output: OutputStream) {
        val ivSize: Int = input.read()
        val iv = ByteArray(ivSize)
        input.read(iv)
        val cipher: Cipher = getCipher(Mode.DECRYPT, iv)
        CipherInputStream(input, cipher).use { inputStream ->
            inputStream.copyTo(out = output)
        }
    }
}
