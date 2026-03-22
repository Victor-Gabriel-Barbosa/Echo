package com.example.mapa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.mapa.data.local.entity.ChatEntity
import com.example.mapa.data.local.entity.ChatLastMsg
import com.example.mapa.data.local.entity.MsgEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface que define os métodos do repositório local de conversas.
 */
@Dao
interface ChatDao {
    @Transaction
    @Query("SELECT * FROM chat WHERE visibleTo LIKE '%|' || :uid || '|%' ORDER BY lastTimestamp DESC")
    fun getChatsByUid(uid: String): Flow<List<ChatLastMsg>>
    @Transaction // Necessário para a @Relation funcionar corretamente
    @Query("SELECT * FROM chat WHERE id = :id")
    suspend fun getChatById(id: String): ChatLastMsg?
    @Query("SELECT * FROM msg WHERE id = :id")
    suspend fun getMsgById(id: String): MsgEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)
    @Query("UPDATE chat SET visibleTo = :visibleTo WHERE id = :id")
    suspend fun updateVisibleToById(id: String, visibleTo: List<String>)
    @Query("SELECT * FROM msg WHERE chatId = :id ORDER BY timestamp ASC")
    fun getMsgsById(id: String): Flow<List<MsgEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMsg(msg: MsgEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMsgs(msg: List<MsgEntity>)
    @Query("UPDATE msg SET read = :read WHERE id IN (:msgIds)")
    suspend fun updateReadByIds(msgIds: List<String>, read: Boolean)
    @Query("DELETE FROM msg WHERE id = :id")
    suspend fun deleteMsgById(id: String)
    @Query("DELETE FROM chat WHERE id = :id")
    suspend fun deleteChatById(id: String)
    @Query("DELETE FROM chat WHERE visibleTo LIKE '%|' || :uid || '|%'")
    suspend fun clearChatsByUid(uid: String)
    @Query("DELETE FROM msg WHERE chatId = :chatId")
    suspend fun clearMsgsByChatId(chatId: String)
    @Transaction
    suspend fun syncChatsByUid(uid: String, chats: List<ChatEntity>) {
        clearChatsByUid(uid)
        insertChats(chats)
    }
    @Transaction
    suspend fun syncMsgsByChatId(chatId: String, msgs: List<MsgEntity>) {
        clearMsgsByChatId(chatId)
        insertMsgs(msgs)
    }
}