/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.core.settings.catalog.section.security.node

import kdoc.core.settings.catalog.section.security.SecuritySettings
import kotlinx.serialization.Serializable

/**
 * Encryption key settings.
 *
 * @property atRest Settings related to encryption at rest.
 * @property atTransit Settings related to encryption in transit.
 * @property atTransitExpiration Expiration time for encryption in transit, in seconds.
 * @property hmac Settings related to HMAC encryption.
 */
public data class EncryptionSettings(
    val atRest: Spec,
    val atTransit: Spec,
    val atTransitExpiration: Long,
    val hmac: Hmac
) {

    /**
     * Configuration settings for a specific encryption.
     *
     * @property algorithm Algorithm used for encrypting/decrypting data.
     * @property salt Salt used for encrypting/decrypting data.
     * @property key Secret key for encrypting/decrypting data.
     * @property sign Signature key to sign the encrypted data.
     */
    public data class Spec(
        val algorithm: String,
        val salt: String,
        val key: String,
        val sign: String
    ) {
        init {
            require(algorithm.isNotBlank()) { "Missing encryption algorithm." }
            require(salt.isNotBlank()) { "Missing encryption salt." }
            require(key.isNotBlank() && (key.length >= SecuritySettings.MIN_KEY_LENGTH)) {
                "Invalid encryption key. Must be >= ${SecuritySettings.MIN_KEY_LENGTH} characters long."
            }
            require(sign.isNotBlank() && (sign.length >= SecuritySettings.MIN_KEY_LENGTH)) {
                "Invalid sign key. Must be >= ${SecuritySettings.MIN_KEY_LENGTH} characters long."
            }
        }
    }

    /**
     * Configuration settings for HMAC encryption.
     *
     * @property algorithm Algorithm used for encrypting/decrypting data.
     * @property key Secret key for encrypting/decrypting data.
     */
    @Serializable
    public data class Hmac(
        val algorithm: String,
        val key: String
    ) {
        init {
            require(algorithm.isNotBlank()) { "Missing HMAC algorithm." }
            require(key.isNotBlank() && (key.length >= SecuritySettings.MIN_KEY_LENGTH)) {
                "Invalid HMAC key. Must be >= ${SecuritySettings.MIN_KEY_LENGTH} characters long."
            }
        }
    }
}
