package com.stansa.bagtracker.data

import androidx.room.*

@Entity(
    tableName = "movement_logs",
    foreignKeys = [ForeignKey(
        entity = Item::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("itemId")]
)
data class MovementLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val fromContainerId: Long? = null,
    val toContainerId: Long? = null,
    val fromContainerName: String = "",
    val toContainerName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val action: String = "MOVE"
)