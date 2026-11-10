
fun upsertItem(
    item: Item,
    tags: List<Tag>,
    reminders: List<Reminder> = emptyList()
) = viewModelScope.launch {

    val id = dao.insert(item)
    val savedItem = item.copy(id = id)

    syncTags(id, tags)
    syncReminders(id, reminders)

    val parentName = item.parentId?.let {
        dao.getItemById(it)?.name ?: ""
    } ?: "Racine"

    movementLogDao.insert(
        MovementLog(
            itemId = id,
            itemName = savedItem.name,
            toContainerId = item.parentId,
            toContainerName = parentName,
            action = "ADD"
        )
    )

    BagTrackerWidgetProvider.refresh(getApplication())
}
