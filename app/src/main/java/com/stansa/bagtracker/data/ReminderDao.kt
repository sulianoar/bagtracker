package com.stansa.bagtracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE itemId = :itemId ORDER BY nextTriggerAt ASC")
    fun getRemindersForItem(itemId: Long): LiveData<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND nextTriggerAt <= :now")
    suspend fun getDueReminders(now: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND nextTriggerAt <= :now")
    fun getDueRemindersLive(now: Long): LiveData<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: Long)

    @Query("UPDATE reminders SET lastTriggeredAt = :timestamp, nextTriggerAt = :nextTrigger WHERE id = :id")
    suspend fun markTriggered(id: Long, timestamp: Long, nextTrigger: Long)
}