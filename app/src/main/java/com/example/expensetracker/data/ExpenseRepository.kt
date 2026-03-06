package com.example.expensetracker.data

import com.example.expensetracker.data.local.AssetDao
import com.example.expensetracker.data.local.AssetEntity
import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val assetDao: AssetDao
) {

    fun observeExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeExpenses()

    suspend fun getAllExpensesSnapshot(): List<ExpenseEntity> = expenseDao.getAllExpensesSnapshot()

    suspend fun addExpense(amountCent: Long, type: Int, category: String, note: String, assetId: Long? = null, dateMillis: Long = System.currentTimeMillis()) {
        val normalizedCategory = category.trim().ifEmpty { "其他" }

        expenseDao.insert(
            ExpenseEntity(
                amountCent = amountCent,
                type = type,
                category = normalizedCategory,
                note = note.trim(),
                assetId = assetId,
                createdAtEpochMillis = dateMillis
            )
        )

        // Sync asset balance
        if (assetId != null) {
            val diffCent = if (type == 0) -amountCent else amountCent
            assetDao.updateAssetBalance(assetId, diffCent)
        }
    }

    suspend fun insertAllExpenses(expenses: List<ExpenseEntity>) {
        expenseDao.insertAll(expenses)
    }

    suspend fun updateExpense(id: Long, amountCent: Long, type: Int, category: String, note: String, assetId: Long? = null): Boolean {
        val oldExpense = expenseDao.getExpenseById(id) ?: return false

        val normalizedCategory = category.trim().ifEmpty { "其他" }
        val updated = expenseDao.updateById(
            id = id,
            amountCent = amountCent,
            type = type,
            category = normalizedCategory,
            note = note.trim(),
            assetId = assetId
        ) > 0

        if (updated) {
            // Revert old effect
            if (oldExpense.assetId != null) {
                val revertDiff = if (oldExpense.type == 0) oldExpense.amountCent else -oldExpense.amountCent
                assetDao.updateAssetBalance(oldExpense.assetId, revertDiff)
            }
            // Apply new effect
            if (assetId != null) {
                val applyDiff = if (type == 0) -amountCent else amountCent
                assetDao.updateAssetBalance(assetId, applyDiff)
            }
        }
        return updated
    }

    suspend fun deleteExpense(id: Long): Boolean {
        val oldExpense = expenseDao.getExpenseById(id)
        val deleted = expenseDao.deleteById(id) > 0
        
        if (deleted && oldExpense != null && oldExpense.assetId != null) {
            val revertDiff = if (oldExpense.type == 0) oldExpense.amountCent else -oldExpense.amountCent
            assetDao.updateAssetBalance(oldExpense.assetId, revertDiff)
        }
        return deleted
    }

    fun observeAssets(): Flow<List<AssetEntity>> = assetDao.observeAssets()

    suspend fun addAsset(name: String, amountCent: Long, type: Int, dateMillis: Long = System.currentTimeMillis()) {
        assetDao.insert(
            AssetEntity(
                name = name.trim(),
                amountCent = amountCent,
                type = type,
                createdAtEpochMillis = dateMillis
            )
        )
    }

    suspend fun updateAsset(id: Long, name: String, amountCent: Long, type: Int): Boolean {
        return assetDao.updateById(
            id = id,
            name = name.trim(),
            amountCent = amountCent,
            type = type
        ) > 0
    }

    suspend fun deleteAsset(id: Long): Boolean {
        return assetDao.deleteById(id) > 0
    }
}
