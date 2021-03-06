@file:UseSerializers(UUIDSerializer::class, NameLowercaseSerializer::class, InstantSerializer::class)

package pl.pawelkielb.xchat.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import pl.pawelkielb.xchat.InstantSerializer
import pl.pawelkielb.xchat.NameLowercaseSerializer
import pl.pawelkielb.xchat.UUIDSerializer
import pl.pawelkielb.xchat.data.Message
import pl.pawelkielb.xchat.data.Name
import java.time.Instant
import java.util.*


@Serializable
data class ChannelMongoEntry(
    val _id: UUID = UUID.randomUUID(),
    val name: Name?,
    val members: Set<Name>,
    val creationTimestamp: Instant = Instant.now()
)

@Serializable
data class MessageMongoEntry(
    val message: Message,
    val channel: UUID,
)
