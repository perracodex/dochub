/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.document.error

import io.ktor.http.*
import kdochub.core.error.AppException
import kotlin.uuid.Uuid

/**
 * Concrete errors for the Document domain.
 *
 * @param statusCode The [HttpStatusCode] associated with this error.
 * @param errorCode A unique code identifying the type of error.
 * @param description A human-readable description of the error.
 * @param field Optional field identifier, typically the input field that caused the error.
 * @param reason Optional human-readable reason for the exception, providing more context.
 * @param cause Optional underlying cause of the exception, if any.
 */
internal sealed class DocumentError(
    statusCode: HttpStatusCode,
    errorCode: String,
    description: String,
    field: String? = null,
    reason: String? = null,
    cause: Throwable? = null
) : AppException(
    statusCode = statusCode,
    errorCode = errorCode,
    context = "DOCUMENT",
    description = description,
    field = field,
    reason = reason,
    cause = cause
) {
    /**
     * Error for when a document is not found.
     *
     * @param documentId The document id that was not found.
     * @param field Optional field identifier, typically the input field that caused the error.
     * @param reason Optional human-readable reason for the exception, providing more context.
     * @param cause Optional underlying cause of the exception, if any.
     */
    class DocumentNotFound(
        documentId: Uuid,
        field: String? = null,
        reason: String? = null,
        cause: Throwable? = null
    ) : DocumentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Document not found. Document Id: $documentId",
        field = field,
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
     * @param field Optional field identifier, typically the input field that caused the error.
     * @param reason Optional human-readable reason for the exception, providing more context.
     * @param cause Optional underlying cause of the exception, if any.
     */
    class NoDocumentProvided(
        ownerId: Uuid,
        field: String? = null,
        reason: String? = null,
        cause: Throwable? = null
    ) : DocumentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "No document provided to upload. Owner Id: $ownerId",
        field = field,
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
     * @param field Optional field identifier, typically the input field that caused the error.
     * @param reason Optional human-readable reason for the exception, providing more context.
     * @param cause Optional underlying cause of the exception, if any.
     */
    class FailedToStreamDownload(
        ownerId: Uuid,
        field: String? = null,
        reason: String? = null,
        cause: Throwable? = null
    ) : DocumentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Failed to stream download. Owner Id: $ownerId",
        field = field,
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
     * @param field Optional field identifier, typically the input field that caused the error.
     * @param reason Optional human-readable reason for the exception, providing more context.
     * @param cause Optional underlying cause of the exception, if any.
     */
    class FailedToPersistUpload(
        ownerId: Uuid,
        field: String? = null,
        reason: String? = null,
        cause: Throwable? = null
    ) : DocumentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Failed to persist upload. Owner Id: $ownerId",
        field = field,
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.BadRequest
            const val ERROR_CODE: String = "FAILED_TO_PERSIST_UPLOAD"
        }
    }
}
