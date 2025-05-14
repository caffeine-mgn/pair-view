package pw.binom.entities

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pw.binom.JsonValue
import pw.binom.MessageContent
import pw.binom.date.DateTime
import pw.binom.db.serialization.TableName
import pw.binom.uuid.UUID

@Serializable
@TableName("messages")
class MessageEntity(
    @Contextual
    val id: UUID,
    @SerialName("chat_id")
    val chatId: Long,
    @Contextual
    val date: DateTime,
    val content: JsonValue<MessageContent>,
)