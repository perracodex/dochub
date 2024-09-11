/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.errors

import io.ktor.http.*
import kdoc.base.errors.AppException
import kotlin.uuid.Uuid

/**
 * Concrete errors for the Document domain.
 *
 * @property status The [HttpStatusCode] associated with this error.
 * @property code A unique code identifying the type of error.
 * @property description A human-readable description of the error.
 * @property reason An optional human-readable reason for the exception, providing more context.
 * @property cause The underlying cause of the exception, if any.
 */
internal sealed class DocumentError(
    status: HttpStatusCode,
    code: String,
    description: String,
    reason: String? = null,
    cause: Throwable? = null
) : AppException(
    status = status,
    context = "DOCUMENT",
    code = code,
    description = description,
    reason = reason,
    cause = cause
) {

    /**
     * Error for when a document is not found.
     *
     * @property documentId The document id that was not found.
     */
    class DocumentNotFound(val documentId: Uuid, reason: String? = null, cause: Throwable? = null) : DocumentError(
        status = HttpStatusCode.NotFound,
        code = "DOCUMENT_NOT_FOUND",
        description = "Document not found. Document Id: $documentId",
        reason = reason,
        cause = cause
    )

    /**
     * Error for when no document has been provided to upload.
     *
     * @property ownerId The owner id of the document.
     */
    class NoDocumentProvided(val ownerId: Uuid, reason: String? = null, cause: Throwable? = null) : DocumentError(
        status = HttpStatusCode.BadRequest,
        code = "NO_DOCUMENT_PROVIDED",
        description = "No document provided to upload. Owner Id: $ownerId",
        reason = reason,
        cause = cause
    )

    /**
     * Error for when there was an issue streaming a download.
     *
     * @property ownerId The owner id of the document.
     */
    class FailedToStreamDownload(val ownerId: Uuid, reason: String? = null, cause: Throwable? = null) : DocumentError(
        status = HttpStatusCode.BadRequest,
        code = "FAILED_TO_STREAM_DOWNLOAD",
        description = "Failed to stream download. Owner Id: $ownerId",
        reason = reason,
        cause = cause
    )

    /**
     * Error for when there was an issue persisting an upload.
     *
     * @property ownerId The owner id of the document.
     */
    class FailedToPersistUpload(val ownerId: Uuid, reason: String? = null, cause: Throwable? = null) : DocumentError(
        status = HttpStatusCode.BadRequest,
        code = "FAILED_TO_PERSIST_UPLOAD",
        description = "Failed to persist upload. Owner Id: $ownerId",
        reason = reason,
        cause = cause
    )
}
