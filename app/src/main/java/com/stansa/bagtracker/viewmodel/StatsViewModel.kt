package com.stansa.bagtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stansa.bagtracker.data.AppDatabase
import com.stansa.bagtracker.data.Tag
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class StatsData(
    val totalObjects: Int,
    val totalContainers: Int,
    val totalValueCents: Int,
    val expiredCount: Int,
    val expiringSoonCount: Int,
    val notSeenCount: Int,
    val tagDistribution: Map<String, Int>
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.itemDao()
    private val tagDao = db.tagDao()

    private val _stats = MutableLiveData<StatsData>()
    val stats: LiveData<StatsData> = _stats

    fun refresh() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val sevenDays = now + TimeUnit.DAYS.toMillis(7)
        val sixMonths = now - TimeUnit.DAYS.toMillis(180)

        val totalObjects = dao.countObjects()
        val totalContainers = dao.countContainers()
        val totalValue = dao.totalValueCents() ?: 0
        val expired = dao.getExpiredItems(now)
        val expiringSoon = dao.getExpiringSoon(now, sevenDays)
        val notSeen = dao.getItemsNotSeenSince(sixMonths)

        val allTags = tagDao.getAllTagsSync()
        val tagDist = mutableMapOf<String, Int>()
        val allItems = dao.getAllItemsWithTags()

        // On calcule la distribution une seule fois depuis la query existante
        val db2 = db.openHelper.readableDatabase
        val tagCursor = db2.query(
            """
            SELECT t.name, COUNT(DISTINCT ref.itemId) as cnt
            FROM tags t
            INNER JOIN item_tag_cross_ref ref ON t.id = ref.tagId
            GROUP BY t.id
            ORDER BY cnt DESC
            """,
            emptyArray()
        )
        while (tagCursor.moveToNext()) {
            tagDist[tagCursor.getString(0)] = tagCursor.getInt(1)
        }
        tagCursor.close()

        _stats.postValue(
            StatsData(
                totalObjects = totalObjects,
                totalContainers = totalContainers,
                totalValueCents = totalValue,
                expiredCount = 0,
                expiringSoonCount = 0,
                notSeenCount = notSeen.size,
                tagDistribution = tagDist
            )
        )
    }
}