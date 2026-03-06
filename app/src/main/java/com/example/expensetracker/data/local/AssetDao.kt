package com.example.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: Long): AssetEntity?

    @Query(
        """
        SELECT *
        FROM assets
        ORDER BY createdAtEpochMillis DESC
        """
    )
    fun observeAssets(): Flow<List<AssetEntity>>

    @Query(
        """
        UPDATE assets
        SET name = :name,
            amountCent = :amountCent,
            type = :type
        WHERE id = :id
        """
    )
    suspend fun updateById(
        id: Long,
        name: String,
        amountCent: Long,
        type: Int
    ): Int

    @Query("UPDATE assets SET amountCent = amountCent + :diffCent WHERE id = :id")
    suspend fun updateAssetBalance(id: Long, diffCent: Long): Int

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
