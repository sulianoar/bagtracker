package com.stansa.bagtracker.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.stansa.bagtracker.data.AppDatabase
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class AppStats(
    val objectCount: Int = 0,
    val containerCount: Int = 0,
    val totalValueCents: Long = 0L,
    val expiredCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val notSeenCount: Int = 0,
    val toReplaceCount: Int = 0,
    val tagDistribution: Map<String, Int> = emptyMap()
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val itemDao = db.itemDao()
    private val tagDao = db.tagDao()

    private val _stats = MutableLiveData<AppStats>()
    val stats: LiveData<AppStats> = _stats

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val sevenDays = now + TimeUnit.DAYS.toMillis(7)
        val sixMonthsAgo = now - TimeUnit.DAYS.toMillis(180)

        val objectCount     = itemDao.countObjects()
        val containerCount  = itemDao.countContainers()
        val totalValue = itemDao.totalValueCents()
        val notSeenItems    = itemDao.getItemsNotSeenSince(sixMonthsAgo)
        val allTags         = tagDao.getAllTagsSync()

        // Tag distribution via items
        val tagDist = mutableMapOf<String, Int>()
        allTags.forEach { tag ->
            val items = itemDao.getItemsByTagSync(tag.id)
            if (items.isNotEmpty()) tagDist[tag.name] = items.size
        }

        // Expired + expiring soon (snapshot)
        var expiredCount = 0
        var expiringSoonCount = 0
        var toReplaceCount = 0

        val allItems = itemDao.getAllItemsSnapshot()
        allItems.forEach { item ->
            item.expirationDate?.let { exp ->
                if (exp < now) expiredCount++
                else if (exp <= sevenDays) expiringSoonCount++
            }
            if (item.condition == "à remplacer" || item.condition == "cassé") toReplaceCount++
        }

        _stats.postValue(AppStats(
            objectCount = objectCount,
            containerCount = containerCount,
            totalValueCents = totalValue,
            expiredCount = expiredCount,
            expiringSoonCount = expiringSoonCount,
            notSeenCount = notSeenItems.size,
            toReplaceCount = toReplaceCount,
            tagDistribution = tagDist
        ))
    }
}
