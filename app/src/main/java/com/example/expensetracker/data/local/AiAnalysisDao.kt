package com.example.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiAnalysisDao {
    @Query("SELECT * FROM ai_analyses ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<AiAnalysisEntity>>

    @Insert
    suspend fun insert(entity: AiAnalysisEntity): Long

    @Query("UPDATE ai_analyses SET aiResponseJson = :response, rawDataJson = :rawData, status = :status WHERE id = :id")
    suspend fun updateResult(id: Long, response: String, rawData: String, status: Int)

    @Query("DELETE FROM ai_analyses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
