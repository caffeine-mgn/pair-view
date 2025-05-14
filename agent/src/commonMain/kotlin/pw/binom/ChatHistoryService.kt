package pw.binom

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import pw.binom.date.DateTime
import pw.binom.db.serialization.DBAccess2
import pw.binom.db.serialization.DBContext
import pw.binom.db.serialization.param
import pw.binom.db.serialization.tableName
import pw.binom.entities.MessageEntity
import pw.binom.strong.inject
import pw.binom.uuid.nextUuid
import kotlin.random.Random

class ChatHistoryService {
    private val db: DBContext by inject()
    suspend fun pushMessages(charId: Long, message: List<MessageContent>) {
        val now = DateTime.nowTime
        db.re2 { db ->
            message.forEachIndexed { index, msg ->
                db.insert(
                    MessageEntity.serializer(), MessageEntity(
                        id = Random.nextUuid(),
                        chatId = charId,
                        date = DateTime(now + index),
                        content = JsonValue.create(msg)
                    )
                )
            }
        }
    }

    suspend fun clearForChar(chatId: Long) {
        db.re2 {
            it.update {
                """
                   delete from ${tableName<MessageEntity>()}
                   where chat_id=${param(chatId)}
                """
            }
        }
    }

    fun getAll(chatId: Long) =
        flow<MessageContent> {
            emitAll(db.su2 { db ->
                db.select(MessageEntity.serializer()) {
                    """
                   select * from ${tableName<MessageEntity>()}
                    where chat_id=${param(chatId)}
                    order by date asc
                """
                }.map { it.content.get() }
            })
        }
}