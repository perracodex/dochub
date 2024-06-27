/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.errors

import io.ktor.http.*
import kdoc.base.errors.AppException
import kdoc.base.errors.ErrorCodeRegistry
import java.util.*

/**
 * Concrete errors for the Document domain.
 *
 * @property status The [HttpStatusCode] associated with this error.
 * @property code A unique code identifying the type of error.
 * @property description A human-readable description of the error.
 */
sealed class DocumentError(
    status: HttpStatusCode,
    code: String,
    description: String,
    reason: String? = null,
    cause: Throwable? = null
) : AppException(status = status, code = code, description = description, reason = reason, cause = cause) {

    /**
     * Error for when a document is not found.
     *
     * @property documentId The document id that was not found.
     */
    class DocumentNotFound(val documentId: UUID, reason: String? = null, cause: Throwable? = null) : DocumentError(
        status = HttpStatusCode.NotFound,
        code = "${TAG}DNF",
        description = "Document not found. Document Id: $documentId",
        reason = reason,
        cause = cause
    )

    /**
     * Error for when no document has been provided to upload.
     *
     * @property ownerId The owner id of the document.
     */
    class NoDocumentProvided(val ownerId: UUID, reason: String? = null, cause: Throwable? = null) : DocumentError(
        status = HttpStatusCode.BadRequest,
        code = "${TAG}NDP",
        description = "No document provided to upload. Owner Id: $ownerId",
        reason = reason,
        cause = cause
    )

    /**
     * Error for when there was an issue persisting an upload.
     *
     * @property ownerId The owner id of the document.
     */
    class FailedToPersistUpload(val ownerId: UUID, reason: String? = null, cause: Throwable? = null) : DocumentError(
        status = HttpStatusCode.BadRequest,
        code = "${TAG}FPU",
        description = "Failed to persist upload. Owner Id: $ownerId",
        reason = reason,
        cause = cause
    )

    companion object {
        private const val TAG: String = "DOC."

        init {
            ErrorCodeRegistry.registerTag(tag = TAG)
        }
    }
}
