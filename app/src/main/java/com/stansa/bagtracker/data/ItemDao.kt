package com.stansa.bagtracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ItemDao {

    @Transaction
    @Query("SELECT * FROM items WHERE parentId IS NULL ORDER BY isContainer DESC, name ASC")
    fun getRootItemsWithTags(): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("SELECT * FROM items WHERE parentId = :parentId ORDER BY isContainer DESC, name ASC")
    fun getChildrenWithTags(parentId: Long): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItemsWithTags(): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("SELECT * FROM items WHERE isContainer = 0 ORDER BY name ASC")
    fun getAllObjectsSortedByName(): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("SELECT * FROM items WHERE isContainer = 0 ORDER BY expirationDate ASC")
    fun getAllObjectsSortedByExpiry(): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("SELECT * FROM items WHERE isContainer = 0 ORDER BY priceCents DESC")
    fun getAllObjectsSortedByValue(): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("SELECT * FROM items WHERE isContainer = 0 ORDER BY weightGrams DESC")
    fun getAllObjectsSortedByWeight(): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("SELECT * FROM items WHERE isContainer = 0 ORDER BY lastSeenAt ASC")
    fun getAllObjectsSortedByLastSeen(): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("SELECT * FROM items WHERE isContainer = 0 AND condition IN ('à remplacer','cassé') ORDER BY name ASC")
    fun getItemsToReplace(): LiveData<List<ItemWithTags>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemWithTagsById(id: Long): ItemWithTags?

    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): Item?

    @Transaction
    @Query("""
        SELECT DISTINCT i.* FROM items i
        LEFT JOIN item_tag_cross_ref ref ON i.id = ref.itemId
        LEFT JOIN tags t ON ref.tagId = t.id
        WHERE i.name LIKE '%' || :query || '%'
        OR i.notes LIKE '%' || :query || '%'
        OR t.name LIKE '%' || :query || '%'
        OR i.barcode LIKE '%' || :query || '%'
        ORDER BY i.name ASC
    """)
    fun searchItemsWithTags(query: String): LiveData<List<ItemWithTags>>

    @Transaction
    @Query("""
        SELECT i.* FROM items i
        INNER JOIN item_tag_cross_ref ref ON i.id = ref.itemId
        WHERE ref.tagId = :tagId ORDER BY i.name ASC
    """)
    fun getItemsByTag(tagId: Long): LiveData<List<ItemWithTags>>

    @Query("SELECT * FROM items WHERE isContainer = 1 ORDER BY name ASC")
    suspend fun getAllContainers(): List<Item>

    @Query("""
    WITH RECURSIVE descendants(id) AS (
        SELECT id FROM items WHERE id = :itemId
        UNION
        SELECT i.id
        FROM items i
        INNER JOIN descendants d ON i.parentId = d.id
    )
    SELECT EXISTS(
        SELECT 1 FROM descendants WHERE id = :candidateParentId
    )
""")
suspend fun wouldCreateParentCycle(
    itemId: Long,
    candidateParentId: Long
): Boolean
    
    @Query("""
        WITH RECURSIVE path(id, name, parentId, depth) AS (
            SELECT id, name, parentId, 0 FROM items WHERE id = :itemId
            UNION ALL
            SELECT i.id, i.name, i.parentId, p.depth + 1
            FROM items i INNER JOIN path p ON i.id = p.parentId
        )
        SELECT * FROM path ORDER BY depth DESC
    """)
    suspend fun getFullPath(itemId: Long): List<PathNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT COUNT(*) FROM items WHERE parentId = :containerId")
    suspend fun countChildren(containerId: Long): Int

    @Query("""
        SELECT * FROM items
        WHERE expirationDate IS NOT NULL
        AND expirationDate <= :threshold
        AND expirationDate >= :now
        ORDER BY expirationDate ASC
    """)
    fun getExpiringSoon(now: Long, threshold: Long): LiveData<List<Item>>

    @Query("SELECT * FROM items WHERE expirationDate IS NOT NULL AND expirationDate < :now ORDER BY expirationDate ASC")
    fun getExpiredItems(now: Long): LiveData<List<Item>>

    @Query("SELECT COUNT(*) FROM items WHERE isContainer = 0")
    suspend fun countObjects(): Int

    @Query("SELECT COUNT(*) FROM items WHERE isContainer = 1")
    suspend fun countContainers(): Int

    @Query("SELECT SUM(priceCents * quantity) FROM items WHERE priceCents IS NOT NULL")
    suspend fun totalValueCents(): Int?

    @Query("SELECT * FROM items WHERE lastSeenAt < :threshold AND isContainer = 0 ORDER BY lastSeenAt ASC")
    suspend fun getItemsNotSeenSince(threshold: Long): List<Item>

    @Query("UPDATE items SET lastSeenAt = :timestamp WHERE id = :itemId")
    suspend fun touchItem(itemId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM items WHERE parentId = :containerId")
    suspend fun getDirectChildren(containerId: Long): List<Item>

    @Query("SELECT * FROM items")
suspend fun getAllItemsSnapshot(): List<Item>

@Query("""
    SELECT i.* FROM items i
    INNER JOIN item_tag_cross_ref ref ON i.id = ref.itemId
    WHERE ref.tagId = :tagId
""")
suspend fun getItemsByTagSync(tagId: Long): List<Item>
}
