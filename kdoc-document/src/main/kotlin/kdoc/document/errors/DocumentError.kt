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
 * @property statusCode The [HttpStatusCode] associated with this error.
 * @property errorCode A unique code identifying the type of error.
 * @property description A human-readable description of the error.
 * @property reason An optional human-readable reason for the exception, providing more context.
 * @property cause The underlying cause of the exception, if any.
 */
internal sealed class DocumentError(
    statusCode: HttpStatusCode,
    errorCode: String,
    description: String,
    reason: String? = null,
    cause: Throwable? = null
) : AppException(
    statusCode = statusCode,
    errorCode = errorCode,
    context = "DOCUMENT",
    description = description,
    reason = reason,
    cause = cause
) {
    /**
     * Error for when a document is not found.
     *
     * @param documentId The document id that was not found.
     */
    class DocumentNotFound(documentId: Uuid, reason: String? = null, cause: Throwable? = null) : DocumentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Document not found. Document Id: $documentId",
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.NotFound
            const val ERROR_CODE: String = "DOCUMENT_NOT_FOUND"
        }
    }

    /**
     * Error for when no document has been provided to upload.
     *
     * @param ownerId The owner id of the document.
     */
    class NoDocumentProvided(ownerId: Uuid, reason: String? = null, cause: Throwable? = null) : DocumentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "No document provided to upload. Owner Id: $ownerId",
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.BadRequest
            const val ERROR_CODE: String = "NO_DOCUMENT_PROVIDED"
        }
    }

    /**
     * Error for when there was an issue streaming a download.
     *
     * @param ownerId The owner id of the document.
     */
    class FailedToStreamDownload(ownerId: Uuid, reason: String? = null, cause: Throwable? = null) : DocumentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Failed to stream download. Owner Id: $ownerId",
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.BadRequest
            const val ERROR_CODE: String = "FAILED_TO_STREAM_DOWNLOAD"
        }
    }

    /**
     * Error for when there was an issue persisting an upload.
     *
     * @param ownerId The owner id of the document.
     */
    class FailedToPersistUpload(ownerId: Uuid, reason: String? = null, cause: Throwable? = null) : DocumentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Failed to persist upload. Owner Id: $ownerId",
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.BadRequest
            const val ERROR_CODE: String = "FAILED_TO_PERSIST_UPLOAD"
        }
    }
}
