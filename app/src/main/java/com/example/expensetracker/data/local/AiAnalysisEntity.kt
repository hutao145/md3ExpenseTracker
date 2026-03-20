package com.example.expensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_analyses")
data class AiAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,
    val endDate: String,
    val rawDataJson: String,
    val aiResponseJson: String,
    val status: Int = STATUS_ANALYZING,
    val createdAtEpochMillis: Long
) {
    companion object {
        const val STATUS_ANALYZING = 0
        const val STATUS_COMPLETED = 1
        const val STATUS_FAILED = 2
    }
}
