package com.example.expensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountCent: Long,
    val type: Int = 0, // 0 = Expense, 1 = Income
    val category: String = "其他",
    val note: String = "",
    val assetId: Long? = null,
    val createdAtEpochMillis: Long
)
