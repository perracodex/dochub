/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.base.database.schema.document.types

import kdoc.base.persistence.utils.IEnumWithId

/**
 * Represents the type of document.
 *
 * @property id The unique identifier of the document.
 */
public enum class DocumentType(override val id: Int) : IEnumWithId {

    /** General document type. */
    GENERAL(id = 100),

    /** Certificate document type. */
    CERTIFICATE(id = 101),

    /** Contract document type. */
    CONTRACT(id = 102),

    /** Invoice document type. */
    INVOICE(id = 103),

    /** Passport document type. */
    PASSPORT(id = 104),

    /** Personal ID document type. */
    PERSONAL_ID(id = 105),

    /** Receipt document type. */
    RECEIPT(id = 106),

    /** Report document type. */
    REPORT(id = 107);

    public companion object {
        private val map: Map<Int, DocumentType> = DocumentType.entries.associateBy(DocumentType::id)

        /**
         * Retrieves the [DocumentType] item corresponding to the given [id].
         *
         * @param id The enum element unique identifier.
         * @return The [DocumentType] item corresponding to the given [id].
         */
        internal fun fromId(id: Int): DocumentType? = map[id]

        /**
         * Parses the given [value] into a [DocumentType] item.
         *
         * @param value The value to parse.
         * @return The [DocumentType] item corresponding to the given [value].
         * @throws IllegalArgumentException If the given [value] is invalid or missing.
         */
        public fun parse(value: String): DocumentType {
            if (value.isBlank()) {
                throw IllegalArgumentException("Missing document type.")
            }

            val target: String = value.trim()
            entries.forEach { documentType ->
                if (documentType.name.equals(other = target, ignoreCase = true)) {
                    return documentType
                }
            }

            throw IllegalArgumentException("Invalid document type: $value")
        }
    }
}
