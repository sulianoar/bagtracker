package com.stansa.bagtracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MovementLogDao {

    @Query("SELECT * FROM movement_logs WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getMovementsForItem(itemId: Long): LiveData<List<MovementLog>>

    @Query("SELECT * FROM movement_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentMovements(): LiveData<List<MovementLog>>

    @Insert
    suspend fun insert(log: MovementLog): Long

    @Query("DELETE FROM movement_logs WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: Long)
}