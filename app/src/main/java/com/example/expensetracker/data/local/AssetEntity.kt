package com.example.expensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amountCent: Long,
    val type: Int = 0, // 0 = Asset, 1 = Liability, 2 = Lent Out
    val createdAtEpochMillis: Long
)
