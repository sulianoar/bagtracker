package com.stansa.bagtracker.data

import androidx.room.*

enum class ItemCondition {
    NEW, GOOD, WORN, TO_REPLACE, BROKEN
}

enum class QuantityUnit(val label: String) {
    UNIT("unité(s)"),
    ML("ml"),
    CL("cl"),
    L("L"),
    G("g"),
    KG("kg"),
    CM("cm"),
    M("m"),
    DOSES("dose(s)"),
    TABLETS("comprimé(s)"),
    SHEETS("feuille(s)"),
    PAIRS("paire(s)")
}

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("parentId")]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isContainer: Boolean = false,
    val quantity: Float = 1f,
    val quantityUnit: QuantityUnit = QuantityUnit.UNIT,
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
    val templateName: String? = null,
    val condition: ItemCondition = ItemCondition.GOOD,
    val barcode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis()
)