/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.core.security.util

import kdoc.core.settings.AppSettings
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides methods to securely encrypt and decrypt IO data streams.
 * This utility class facilitates the use of symmetric key encryption for data protection.
 *
 * Note: CipherOutputStream and CipherInputStream have been avoided due to inefficiencies
 * caused by their internal fixed buffer size of 512 bytes, which cannot be overridden
 * and leads to suboptimal processing.
 */
public object SecureIO {
    /** Length in `bytes` of the initialization vector (IV) used in GCM mode of operation. */
    private const val GCM_IV_LENGTH: Int = 12

    /** Length in `bits` of the authentication tag used in GCM mode of operation. */
    private const val GCM_TAG_LENGTH: Int = 128

    private enum class Mode(val id: Int) {
        ENCRYPT(id = Cipher.ENCRYPT_MODE),
        DECRYPT(id = Cipher.DECRYPT_MODE)
    }

    /**
     * Ciphers the input stream and writes the result to the output stream.
     *
     * @param input The input stream containing the plain data.
     * @param output The output stream where the encrypted data will be written.
     */
    public fun cipher(input: InputStream, output: OutputStream) {
        val cipher: Cipher = getCipher(mode = Mode.ENCRYPT)
        cipher.iv.let {
            output.write(it.size)
            output.write(it)
        }
        write(input = input, output = output, cipher = cipher)
    }

    /**
     * Deciphers the input stream and writes the result to the output stream.
     *
     * @param input The encrypted input stream to decipher.
     * @param output The output stream to write the deciphered data to.
     */
    public fun decipher(input: InputStream, output: OutputStream) {
        val iv: ByteArray = ByteArray(input.read()).apply { input.read(this) }
        val cipher: Cipher = getCipher(mode = Mode.DECRYPT, iv = iv)
        write(input = input, output = output, cipher = cipher)
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
        val normalizedKey: ByteArray = EncryptionUtils.hexStringToByteArray(value = key)
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
     * Writes the ciphered/deciphered data from the input stream to the output stream.
     *
     * @param input The input stream to process.
     * @param output The output stream to write the processed data to.
     * @param cipher The initialized [Cipher] instance for processing the data.
     */
    @Suppress("MagicNumber")
    private fun write(input: InputStream, output: OutputStream, cipher: Cipher) {
        // Buffer size set to a multiple of the cipher block size for efficient processing.
        val bufferSize: Int = cipher.blockSize * 1024
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        var totalBytesCopied: Long = 0

        // Process the input data in manageable chunks through the cipher and write the transformed output.
        while (input.read(buffer).also { bytesRead = it } != -1) {
            val decryptedData: ByteArray? = cipher.update(buffer, 0, bytesRead)
            decryptedData?.let { data ->
                output.write(data)
                totalBytesCopied += data.size
            }
        }

        // Finalize the encryption/decryption and write any remaining bytes to the output stream.
        val finalBytes: ByteArray = cipher.doFinal()
        finalBytes.let { bytes ->
            output.write(bytes)
            totalBytesCopied += bytes.size
        }
    }
}
