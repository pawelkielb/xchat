package pl.pawelkielb.xchat.server

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import pl.pawelkielb.xchat.data.Name
import java.util.*


object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

object NameSerializer : KSerializer<Name> {
    override val descriptor = PrimitiveSerialDescriptor("Name", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Name) = encoder.encodeString(value.value())
    override fun deserialize(decoder: Decoder): Name = Name.of(decoder.decodeString())
}
