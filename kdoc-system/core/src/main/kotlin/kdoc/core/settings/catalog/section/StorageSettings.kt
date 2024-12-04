/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.core.settings.catalog.section

/**
 * Database related settings.
 *
 * @property uploadsRootPath The root path where uploaded files are stored.
 * @property downloadsBasePath The base URL for downloading files.
 * @property cipher Whether to cipher files at rest.
 * @property cipherAlgorithm The algorithm used to cipher and de-cipher storage files.
 * @property cipherTransformation The transformation used to cipher and de-cipher storage files.
 * @property cipherKey The key used to cipher and de-cipher storage files.
 */
public data class StorageSettings(
    val uploadsRootPath: String,
    val downloadsBasePath: String,
    val cipher: Boolean,
    val cipherAlgorithm: String,
    val cipherTransformation: String,
    val cipherKey: String
) {
    init {
        require(uploadsRootPath.isNotBlank()) { "Uploads root path must not be blank." }
        require(downloadsBasePath.isNotBlank()) { "Downloads base URL must not be blank." }
        require(cipherKey.isNotBlank()) { "Cipher key must not be blank." }
        require(cipherAlgorithm.isNotBlank()) { "Cipher algorithm must not be blank." }
        require(cipherTransformation.isNotBlank()) { "Cipher transformation must not be blank." }
    }
}
