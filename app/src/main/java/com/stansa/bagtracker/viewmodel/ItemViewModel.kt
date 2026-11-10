package com.stansa.bagtracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.stansa.bagtracker.data.*
import com.stansa.bagtracker.ui.widget.BagTrackerWidgetProvider
import com.stansa.bagtracker.util.ExportUtil
import com.stansa.bagtracker.util.ImageUtil
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ItemViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.itemDao()
    private val tagDao = db.tagDao()
    private val reminderDao = db.reminderDao()
    private val movementLogDao = db.movementLogDao()

    val rootItems = dao.getRootItemsWithTags()
    val allItems = dao.getAllItemsWithTags()
    val allTags = tagDao.getAllTags()

    private val _sortMode = MutableLiveData("name")
    val flatItems: LiveData<List<ItemWithTags>> = _sortMode.switchMap { mode ->
        when (mode) {
            "expiry"   -> dao.getAllObjectsSortedByExpiry()
            "value"    -> dao.getAllObjectsSortedByValue()
            "weight"   -> dao.getAllObjectsSortedByWeight()
            "lastSeen" -> dao.getAllObjectsSortedByLastSeen()
            else       -> dao.getAllObjectsSortedByName()
        }
    }
    fun setSortMode(mode: String) { _sortMode.value = mode }

    private val _searchQuery = MutableLiveData("")
    val searchResults: LiveData<List<ItemWithTags>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) MutableLiveData(emptyList())
        else dao.searchItemsWithTags(query)
    }
    fun search(query: String) { _searchQuery.value = query }

    private val _filterTagId = MutableLiveData<Long?>(null)
    val tagFilteredItems: LiveData<List<ItemWithTags>> = _filterTagId.switchMap { tagId ->
        tagId?.let { dao.getItemsByTag(it) } ?: rootItems
    }
    fun filterByTag(tagId: Long?) { _filterTagId.value = tagId }

    private val now = System.currentTimeMillis()
    val expiringSoon   = dao.getExpiringSoon(now, now + TimeUnit.DAYS.toMillis(7))
    val expiredItems   = dao.getExpiredItems(now)
    val dueReminders   = reminderDao.getDueRemindersLive(System.currentTimeMillis())
    val itemsToReplace = dao.getItemsToReplace()
    val recentMovements = movementLogDao.getRecentMovements()

    fun getChildrenOf(parentId: Long)      = dao.getChildrenWithTags(parentId)
    fun getMovementsForItem(itemId: Long)  = movementLogDao.getMovementsForItem(itemId)
    fun getRemindersForItem(itemId: Long)  = reminderDao.getRemindersForItem(itemId)
    fun getRecentMovements()               = movementLogDao.getRecentMovements()

    suspend fun getAllContainers()                = dao.getAllContainers()
    suspend fun getItemById(id: Long)            = dao.getItemById(id)
    suspend fun getItemWithTagsById(id: Long)    = dao.getItemWithTagsById(id)
    suspend fun getItemByBarcode(barcode: String) = dao.getItemByBarcode(barcode)
    suspend fun getFullPath(itemId: Long)         = dao.getFullPath(itemId)
    suspend fun getAllTagsSync()                   = tagDao.getAllTagsSync()

    fun upsertItem(item: Item, tags: List<Tag>, reminders: List<Reminder> = emptyList()) =
        viewModelScope.launch {
            val id = dao.insert(item)
            syncTags(id, tags)
            syncReminders(id, reminders)
            if (item.parentId != null) {
                val parentName = dao.getItemById(item.parentId)?.name ?: ""
                movementLogDao.insert(
                    MovementLog(
                        itemId = id,
                        toContainerId = item.parentId,
                        toContainerName = parentName,
                        action = "ADD"
                    )
                )
            }
            BagTrackerWidgetProvider.refresh(getApplication())
        }

    fun updateItem(item: Item, tags: List<Tag>, reminders: List<Reminder> = emptyList()) =
        viewModelScope.launch {
            val oldItem = dao.getItemById(item.id)
            dao.update(item)
            syncTags(item.id, tags)
            syncReminders(item.id, reminders)
            if (oldItem?.parentId != item.parentId) {
                val fromName = oldItem?.parentId?.let { dao.getItemById(it)?.name } ?: "Racine"
                val toName   = item.parentId?.let { dao.getItemById(it)?.name } ?: "Racine"
                movementLogDao.insert(
                    MovementLog(
                        itemId = item.id,
                        fromContainerId = oldItem?.parentId,
                        toContainerId = item.parentId,
                        fromContainerName = fromName,
                        toContainerName = toName,
                        action = "MOVE"
                    )
                )
            }
            BagTrackerWidgetProvider.refresh(getApplication())
        }

    fun deleteItem(item: Item) = viewModelScope.launch {
        item.photoPath?.let { ImageUtil.deletePhoto(it) }
        dao.delete(item)
        BagTrackerWidgetProvider.refresh(getApplication())
    }

    fun markReminderTriggered(reminderId: Long) = viewModelScope.launch {
        val r = reminderDao.getById(reminderId) ?: return@launch
        val now = System.currentTimeMillis()
        reminderDao.markTriggered(
            reminderId,
            now,
            now + TimeUnit.DAYS.toMillis(r.intervalDays.toLong())
        )
    }

    fun touchItem(itemId: Long) = viewModelScope.launch {
        dao.touchItem(itemId)
    }

    fun savePhoto(uri: Uri, itemId: Long): String? =
        ImageUtil.saveImageFromUri(getApplication(), uri, itemId)

    suspend fun computeTotalWeight(containerId: Long): Int {
        var total = 0
        val stack = ArrayDeque<Long>()
        stack.add(containerId)
        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            val children = dao.getDirectChildren(current)
            children.forEach { child ->
                total += (child.weightGrams ?: 0) * child.quantity
                if (child.isContainer) stack.add(child.id)
            }
        }
        return total
    }

    suspend fun computeTotalValue(containerId: Long): Int {
        var total = 0
        val stack = ArrayDeque<Long>()
        stack.add(containerId)
        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            val children = dao.getDirectChildren(current)
            children.forEach { child ->
                total += (child.priceCents ?: 0) * child.quantity
                if (child.isContainer) stack.add(child.id)
            }
        }
        return total
    }

    fun exportAll(items: List<ItemWithTags>) = viewModelScope.launch {
        val file = ExportUtil.exportToJson(getApplication(), items)
        ExportUtil.shareExportFile(getApplication(), file)
        ExportUtil.cleanOldExports(getApplication())
    }

    fun importFromUri(uri: Uri) = viewModelScope.launch {
        val data = ExportUtil.parseImportFile(getApplication(), uri) ?: return@launch
        data.items.forEach { itemWithTags ->
            val newId = dao.insert(itemWithTags.item.copy(id = 0))
            syncTags(newId, itemWithTags.tags)
        }
        BagTrackerWidgetProvider.refresh(getApplication())
    }

    private suspend fun syncTags(itemId: Long, tags: List<Tag>) {
        tagDao.deleteAllTagsForItem(itemId)
        tags.forEach { tag ->
            val existing = tagDao.findByName(tag.name)
            val tagId = existing?.id ?: tagDao.insert(tag)
            tagDao.insertCrossRef(ItemTagCrossRef(itemId, tagId))
        }
    }

    private suspend fun syncReminders(itemId: Long, reminders: List<Reminder>) {
        reminderDao.deleteAllForItem(itemId)
        reminders.forEach { reminderDao.insert(it.copy(itemId = itemId)) }
    }
}
