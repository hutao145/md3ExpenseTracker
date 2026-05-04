package com.example.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query(
        """
        SELECT *
        FROM expenses
        ORDER BY createdAtEpochMillis DESC
        """
    )
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT *
        FROM expenses
        ORDER BY createdAtEpochMillis DESC
        """
    )
    suspend fun getAllExpensesSnapshot(): List<ExpenseEntity>

    @Query(
        """
        UPDATE expenses
        SET amountCent = :amountCent,
            type = :type,
            category = :category,
            note = :note,
            assetId = :assetId,
            createdAtEpochMillis = :createdAtEpochMillis
        WHERE id = :id
        """
    )
    suspend fun updateById(
        id: Long,
        amountCent: Long,
        type: Int,
        category: String,
        note: String,
        assetId: Long?,
        createdAtEpochMillis: Long
    ): Int

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM expenses WHERE id IN (:ids)")
    suspend fun getExpensesByIds(ids: List<Long>): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("UPDATE expenses SET category = :category WHERE id IN (:ids)")
    suspend fun updateCategoryByIds(ids: List<Long>, category: String): Int
}
