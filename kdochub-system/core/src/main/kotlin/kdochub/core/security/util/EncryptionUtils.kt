/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.core.security.util

import kdochub.core.settings.AppSettings
import kdochub.core.settings.catalog.section.security.node.EncryptionSettings
import org.jetbrains.exposed.crypt.Algorithms
import org.jetbrains.exposed.crypt.Encryptor
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Utility class for database field encryption.
 */
@Suppress("MagicNumber")
@OptIn(ExperimentalEncodingApi::class)
public object EncryptionUtils {
    private enum class AlgorithmName {
        AES_256_PBE_CBC,
        AES_256_PBE_GCM,
        BLOW_FISH,
        TRIPLE_DES
    }

    /**
     * The type of encryption to use.
     */
    public enum class Type {
        /** Stable encryption for data at rest, such as encrypted database fields. */
        AT_REST,

        /** Transient encryption for data in transit, such as encrypted URLs. */
        AT_TRANSIT
    }

    /**
     * Get the [Encryptor] based on the encryption configuration settings.
     * Used for example to encrypt database fields.
     *
     * @param type The target [EncryptionUtils.Type] of encryption to use.
     */
    public fun getEncryptor(type: Type): Encryptor {
        val encryptionSpec: EncryptionSettings.Spec = when (type) {
            Type.AT_REST -> AppSettings.security.encryption.atRest
            Type.AT_TRANSIT -> AppSettings.security.encryption.atTransit
        }

        val algorithm: AlgorithmName = AlgorithmName.valueOf(encryptionSpec.algorithm)
        val key: String = encryptionSpec.key
        val salt: String = encryptionSpec.salt

        return when (algorithm) {
            AlgorithmName.AES_256_PBE_CBC -> Algorithms.AES_256_PBE_CBC(password = key, salt = salt)
            AlgorithmName.AES_256_PBE_GCM -> Algorithms.AES_256_PBE_GCM(password = key, salt = salt)
            AlgorithmName.BLOW_FISH -> Algorithms.BLOW_FISH(key = key)
            AlgorithmName.TRIPLE_DES -> Algorithms.TRIPLE_DES(secretKey = key)
        }
    }

    /**
     * Converts a hexadecimal string to a ByteArray of a specified length.
     * Pads with zeros or truncates as necessary, suitable for key generation in encryption.
     *
     * @param length The desired length of the key in bytes.
     * @return A ByteArray of the specified length.
     */
    public fun String.toByteKey(length: Int): ByteArray {
        val requiredHexLength: Int = length * 2 // Each byte is represented by two hex characters.
        return (if (this.length < requiredHexLength) padEnd(requiredHexLength, padChar = '0') else take(requiredHexLength))
            .chunked(size = 2)
            .map { it.toInt(radix = 16).toByte() }
            .toByteArray()
    }

    /**
     * Converts a hexadecimal string as input and converts it to a ByteArray.
     * Each pair of hexadecimal characters in the input string represents a byte in the output ByteArray.
     * The resulting ByteArray is created by parsing the input string and converting each pair of characters
     * to a byte value, which is then stored in the output ByteArray.
     *
     * Example:
     * ```
     *      Input: "48656C6C6F"
     *      Output: [72, 101, 108, 108, 111]
     *      Explanation: Each pair of characters is converted to a byte value.
     *                   '48' -> 72, '65' -> 101, '6C' -> 108, '6F' -> 111
     * ```
     *
     * @param value The hexadecimal string to convert to a ByteArray.
     * @return The ByteArray representation of the hexadecimal string.
     */
    public fun hexStringToByteArray(value: String): ByteArray {
        return ByteArray(size = value.length / 2).apply {
            for (index in indices) {
                val byteValue: Int = (Character.digit(value[index * 2], 16) shl 4) +
                        Character.digit(value[index * 2 + 1], 16)
                this[index] = byteValue.toByte()
            }
        }
    }

    /**
     * Encrypts the input data using the provided key using AES encryption.
     *
     * @param data The data to encrypt.
     * @param key The key to use for encryption.
     * @return The encrypted data.
     */
    public fun aesEncrypt(data: String, key: String): String {
        val normalizedKey: ByteArray = key.toByteKey(length = 32)
        val secretKey = SecretKeySpec(normalizedKey, "AES")

        // Generate a random IV.
        val iv = ByteArray(size = 16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedData: ByteArray = cipher.doFinal(data.toByteArray())

        // Concatenate IV and encrypted data and encode to Base64.
        val ivAndEncryptedData: ByteArray = iv + encryptedData
        return Base64.UrlSafe.encode(source = ivAndEncryptedData)
    }

    /**
     * Decrypts the input data using the provided key using AES encryption.
     *
     * @param data The data to decrypt.
     * @param key The key to use for decryption.
     * @return The decrypted data.
     */
    public fun aesDecrypt(data: String, key: String): String {
        val normalizedKey: ByteArray = key.toByteKey(length = 32)
        val secretKey = SecretKeySpec(normalizedKey, "AES")

        // Decode the Base64 encoded string.
        val ivAndEncryptedData: ByteArray = Base64.UrlSafe.decode(source = data)

        // Extract IV and encrypted data.
        val iv: ByteArray = ivAndEncryptedData.copyOfRange(fromIndex = 0, toIndex = 16)
        val encryptedData: ByteArray = ivAndEncryptedData.copyOfRange(fromIndex = 16, toIndex = ivAndEncryptedData.size)
        val ivSpec = IvParameterSpec(iv)

        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedData: ByteArray = cipher.doFinal(encryptedData)
        return String(decryptedData)
    }
}
