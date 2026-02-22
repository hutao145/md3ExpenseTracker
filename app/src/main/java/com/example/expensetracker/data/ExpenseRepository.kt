package com.example.expensetracker.data

import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {

    fun observeExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeExpenses()

    suspend fun getAllExpensesSnapshot(): List<ExpenseEntity> = expenseDao.getAllExpensesSnapshot()

    suspend fun addExpense(amountCent: Long, type: Int, category: String, note: String, dateMillis: Long = System.currentTimeMillis()) {
        val normalizedCategory = category.trim().ifEmpty { "其他" }

        expenseDao.insert(
            ExpenseEntity(
                amountCent = amountCent,
                type = type,
                category = normalizedCategory,
                note = note.trim(),
                createdAtEpochMillis = dateMillis
            )
        )
    }

    suspend fun insertAllExpenses(expenses: List<ExpenseEntity>) {
        expenseDao.insertAll(expenses)
    }

    suspend fun updateExpense(id: Long, amountCent: Long, type: Int, category: String, note: String): Boolean {
        val normalizedCategory = category.trim().ifEmpty { "其他" }
        return expenseDao.updateById(
            id = id,
            amountCent = amountCent,
            type = type,
            category = normalizedCategory,
            note = note.trim()
        ) > 0
    }

    suspend fun deleteExpense(id: Long): Boolean {
        return expenseDao.deleteById(id) > 0
    }
}
