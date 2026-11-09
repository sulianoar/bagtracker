package com.stansa.bagtracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ReminderDao {

    @Query("SELECT * FROM item_reminders WHERE itemId = :itemId ORDER BY nextTriggerAt ASC")
    fun getRemindersForItem(itemId: Long): LiveData<List<ItemReminder>>

    @Query("SELECT * FROM item_reminders WHERE isActive = 1 AND nextTriggerAt <= :now ORDER BY nextTriggerAt ASC")
    suspend fun getDueReminders(now: Long): List<ItemReminder>

    @Query("SELECT * FROM item_reminders WHERE isActive = 1 ORDER BY nextTriggerAt ASC")
    fun getAllActiveReminders(): LiveData<List<ItemReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ItemReminder): Long

    @Update
    suspend fun update(reminder: ItemReminder)

    @Delete
    suspend fun delete(reminder: ItemReminder)

    @Query("DELETE FROM item_reminders WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: Long)
}