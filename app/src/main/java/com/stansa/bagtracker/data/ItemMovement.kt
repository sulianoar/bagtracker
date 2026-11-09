package com.stansa.bagtracker.data

import androidx.room.*

@Entity(
    tableName = "item_movements",
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
data class ItemMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val itemName: String,
    val fromContainerName: String?,
    val toContainerName: String?,
    val movedAt: Long = System.currentTimeMillis(),
    val note: String = ""
)