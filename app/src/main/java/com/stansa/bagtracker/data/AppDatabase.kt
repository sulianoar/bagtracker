package com.stansa.bagtracker.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        Item::class,
        Tag::class,
        ItemTagCrossRef::class,
        ItemMovement::class,
        ItemReminder::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun tagDao(): TagDao
    abstract fun movementDao(): MovementDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bagtracker_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}