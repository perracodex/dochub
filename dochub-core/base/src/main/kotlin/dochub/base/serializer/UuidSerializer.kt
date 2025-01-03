/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.base.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.uuid.Uuid

/**
 * Serializer for [Uuid] objects.
 */
internal object UuidSerializer : KSerializer<Uuid> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "kotlin_uuid",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value = value.toString())
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return Uuid.parse(uuidString = decoder.decodeString())
    }
}

/**
 * Represents a serializable [Uuid].
 *
 * @property Uuid The type representing the serializable [Uuid].
 *
 * @see [Uuid]
 * @see [UuidSerializer]
 */
public typealias Uuid = @Serializable(with = UuidSerializer::class) Uuid
