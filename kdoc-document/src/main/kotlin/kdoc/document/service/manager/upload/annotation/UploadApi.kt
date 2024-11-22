/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service.manager.upload.annotation

/**
 * Annotation for controlled access to the Upload API.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only to be used within the Upload API.")
@Retention(AnnotationRetention.BINARY)
internal annotation class UploadApi
