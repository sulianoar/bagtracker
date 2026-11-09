package com.stansa.bagtracker.data

import androidx.room.Entity

@Entity(tableName = "item_tag_cross_ref", primaryKeys = ["itemId", "tagId"])
data class ItemTagCrossRef(val itemId: Long, val tagId: Long)