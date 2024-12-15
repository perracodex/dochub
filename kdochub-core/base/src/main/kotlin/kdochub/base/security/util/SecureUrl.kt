/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.base.security.util

import kdochub.base.settings.AppSettings
import kdochub.base.settings.catalog.section.security.node.EncryptionSettings
import kdochub.base.util.DateTimeUtils.current
import kdochub.base.util.DateTimeUtils.toEpochSeconds
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.crypt.Encryptor
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Provides utility functions for generating and verifying encrypted URLs that include a secure token.
 * This utility is essential for obscuring actual resources and ensuring that URLs expire
 * after a predefined duration, thereby enhancing security by preventing unauthorized access.
 *
 * The URL is composed of a base path, a securely encrypted token that encapsulates the original
 * data and an expiration time, and a signature to verify the integrity of the URL.
 *
 * URL Composition:
 * - Base URL: The starting URL to which the token and signature are appended.
 * - Token: Encrypted string that includes actor data and an expiration timestamp, formatted as "data:expiration".
 * - Signature: A cryptographic hash ensuring the integrity of the URL, generated using HMAC.
 *
 * Detailed Token and URL Format:
 * - Data: "SomeData"
 * - Expiration: "1700839395" (UNIX timestamp format, representing the time at which the token expires)
 * - Plain Token: "SomeData:1700839395" (concatenation of data and expiration timestamp)
 * - Encrypted Token: Encrypted and Base64 encoded version of the plain token.
 * - Signature: Base64 encoded HMAC of the URL containing the token.
 *
 * Full Example URL:
 *   https://example.com/resource?token=EncryptedBase64Token&signature=EncodedSignatureValue
 *
 * Example Usage:
 * - Generating a URL:
 * ```
 *      val secureUrl = SecureUrl.generate("https://example.com/resource", "SomeData")
 *      // Outputs: "https://example.com/endpoint?token=EncryptedBase64Token&signature=EncodedSignature"
 * ```
 *
 * - Verifying a URL token:
 * ```
 *      val data = SecureUrl.verify("https://example.com/resource", "EncryptedBase64Token", "EncodedSignature")
 *      // Outputs: "SomeData" if the token is valid and not expired, or null if invalid or expired
 * ```
 *
 * This structured approach ensures that even if a URL is intercepted, it cannot be used beyond its expiration time,
 * and any tampering with the URL will invalidate the signature, therefore protecting the underlying data.
 */
@OptIn(ExperimentalEncodingApi::class)
public object SecureUrl {
    private const val TOKEN_PART_INDEX: Int = 0
    private const val EXPIRATION_PART_INDEX: Int = 1
    private const val PARTS_COUNT: Int = 2
    private const val SEPARATOR: String = ":"
    private const val PARAM_TOKEN: String = "?token="
    private const val PARAM_SIGNATURE: String = "&signature="

    /**
     * Generates a secure (encrypted + signed + expiring) URL.
     *
     * @param basePath The base path for the URL.
     * @param data The original string data to be encrypted within the token.
     * @return A string representing the complete URL with the encrypted token as a query parameter.
     */
    public fun generate(basePath: String, data: String): String {
        val currentTime: Long = LocalDateTime.current().toEpochSeconds()
        val expiresAt: Long = currentTime + AppSettings.security.encryption.atTransitExpiration
        val plainToken = "$data$SEPARATOR$expiresAt"
        val encryptedToken: String = encryptToken(data = plainToken)
        val fullUrl = "$basePath$PARAM_TOKEN$encryptedToken"
        return signUrl(url = fullUrl)
    }

    /**
     * Verifies if a given token is valid based on its signature, decryption and expiration.
     *
     * @param basePath The base path for the URL.
     * @param token The encrypted and Base64 encoded token to verify.
     * @param signature The signature to verify the integrity of the token.
     * @return A boolean indicating whether the token is valid (true) or not (false).
     */
    public fun verify(basePath: String, token: String, signature: String): String? {
        // Verify the URL signature for integrity before decryption.
        val url = "$basePath$PARAM_TOKEN$token"
        if (!verifySignature(url = url, receivedSignature = signature)) return null

        // Decrypt the token and split into parts, which include the data and expiration.
        val encryptor: Encryptor = EncryptionUtils.getEncryptor(type = EncryptionUtils.Type.AT_TRANSIT)
        val decryptedToken: String = decryptToken(token = token, encryptor = encryptor)
        val tokenParts: List<String> = decryptedToken.split(SEPARATOR)

        // Get the expiration time and verify if the token is still valid.
        if (tokenParts.size != PARTS_COUNT) return null
        val expiration: Long = tokenParts.getOrNull(EXPIRATION_PART_INDEX)?.toLongOrNull() ?: return null

        // Return the original data if the token has not expired, otherwise null.
        return if (LocalDateTime.current().toEpochSeconds() <= expiration) {
            tokenParts.getOrNull(TOKEN_PART_INDEX)
        } else {
            null
        }
    }

    /**
     * Encrypts the given data and encodes it in Base64.
     *
     * @param data The data to encrypt.
     * @return The encrypted and Base64 encoded data.
     */
    private fun encryptToken(data: String): String {
        val encryptor: Encryptor = EncryptionUtils.getEncryptor(type = EncryptionUtils.Type.AT_TRANSIT)
        val encrypted: String = encryptor.encrypt(str = data)
        return Base64.UrlSafe.encode(source = encrypted.encodeToByteArray())
    }

    /**
     * Decrypts the given token and returns the original data.
     *
     * @param token The encrypted and Base64 encoded token to decrypt.
     * @param encryptor The encryptor to use for decryption.
     * @return The decrypted original data.
     */
    private fun decryptToken(token: String, encryptor: Encryptor): String {
        val decodedString: String = Base64.UrlSafe.decode(source = token).decodeToString()
        return encryptor.decrypt(str = decodedString)
    }

    /**
     * Signs the given URL with a secure signature and returns the signed URL.
     *
     * @param url The URL to sign.
     * @return The signed URL.
     */
    private fun signUrl(url: String): String {
        val hmac: EncryptionSettings.Hmac = AppSettings.security.encryption.hmac
        val mac: Mac = Mac.getInstance(hmac.algorithm)
        mac.init(SecretKeySpec(hmac.key.toByteArray(), hmac.algorithm))
        val signedUrl: ByteArray = mac.doFinal(url.toByteArray())
        val signature: String = Base64.UrlSafe.encode(source = signedUrl)
        return "$url$PARAM_SIGNATURE$signature"
    }

    /**
     * Verifies the signature of the given URL by recomputing the HMAC
     * and comparing it with the received signature.
     *
     * @param url The URL to verify, without the signature.
     * @param receivedSignature The signature to verify.
     * @return A boolean indicating whether the signature is valid (true) or not (false).
     */
    private fun verifySignature(url: String, receivedSignature: String): Boolean {
        val hmac: EncryptionSettings.Hmac = AppSettings.security.encryption.hmac
        val mac: Mac = Mac.getInstance(hmac.algorithm)
        mac.init(SecretKeySpec(hmac.key.toByteArray(), hmac.algorithm))
        val expectedSignature: ByteArray = mac.doFinal(url.toByteArray())
        val encodedExpectedSignature: String = Base64.UrlSafe.encode(expectedSignature)
        return encodedExpectedSignature == receivedSignature
    }
}
