
package com.stansa.bagtracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movement_logs",
    indices = [Index("itemId")]
)
data class MovementLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val itemId: Long,

    @ColumnInfo(defaultValue = "''")
    val itemName: String = "",

    val fromContainerId: Long? = null,
    val toContainerId: Long? = null,

    val fromContainerName: String = "",
    val toContainerName: String = "",

    val timestamp: Long = System.currentTimeMillis(),

    // Valeurs autorisées : ADD, MOVE, DELETE
    val action: String = "MOVE"
)
