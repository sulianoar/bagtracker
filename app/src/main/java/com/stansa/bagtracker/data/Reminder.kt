package com.stansa.bagtracker.data

import androidx.room.*

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = Item::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("itemId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val label: String,
    val intervalDays: Int,
    val lastTriggeredAt: Long? = null,
    val nextTriggerAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)