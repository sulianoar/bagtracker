package com.stansa.bagtracker.data

import androidx.room.*

enum class ReminderFrequency(val label: String) {
    DAILY("Chaque jour"),
    EVERY_N_DAYS("Tous les N jours"),
    WEEKLY("Chaque semaine"),
    MONTHLY("Chaque mois"),
    CUSTOM_HOURS("Toutes les N heures")
}

@Entity(
    tableName = "item_reminders",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class ItemReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val label: String,
    val frequency: ReminderFrequency = ReminderFrequency.EVERY_N_DAYS,
    val intervalDays: Int = 1,
    val intervalHours: Int = 24,
    val lastTriggeredAt: Long? = null,
    val nextTriggerAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)