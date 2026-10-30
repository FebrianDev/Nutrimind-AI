package com.fdev.nutrimindai.feature.coach.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fdev.nutrimindai.feature.coach.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_message_table ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_message_table ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesDesc(limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_message_table")
    suspend fun clearAllMessages()

    @Query("DELETE FROM chat_message_table WHERE id = :id")
    suspend fun deleteMessage(id: String)
}
