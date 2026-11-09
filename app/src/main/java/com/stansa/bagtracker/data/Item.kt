package com.stansa.bagtracker.data

import androidx.room.*

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = Item::class,
        parentColumns = ["id"],
        childColumns = ["parentId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("parentId"), Index("barcode")]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isContainer: Boolean = false,
    val quantity: Int = 1,
    val quantityUnit: String = "unités",
    val condition: String = "bon",
    val expirationDate: Long? = null,
    val parentId: Long? = null,
    val emoji: String = "📦",
    val notes: String = "",
    val photoPath: String? = null,
    val weightGrams: Int? = null,
    val volumeCm3: Int? = null,
    val priceCents: Int? = null,
    val locationName: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val barcode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis()
)