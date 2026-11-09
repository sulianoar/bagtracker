package com.stansa.bagtracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MovementDao {

    @Query("SELECT * FROM item_movements WHERE itemId = :itemId ORDER BY movedAt DESC")
    fun getMovementsForItem(itemId: Long): LiveData<List<ItemMovement>>

    @Query("SELECT * FROM item_movements ORDER BY movedAt DESC LIMIT 50")
    fun getRecentMovements(): LiveData<List<ItemMovement>>

    @Insert
    suspend fun insert(movement: ItemMovement)

    @Query("DELETE FROM item_movements WHERE movedAt < :threshold")
    suspend fun purgeOldMovements(threshold: Long)
}