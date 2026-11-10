
package com.stansa.bagtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Item::class,
        Tag::class,
        ItemTagCrossRef::class,
        Reminder::class,
        MovementLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun tagDao(): TagDao
    abstract fun reminderDao(): ReminderDao
    abstract fun movementLogDao(): MovementLogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
                    CREATE TABLE movement_logs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        itemName TEXT NOT NULL DEFAULT '',
                        fromContainerId INTEGER,
                        toContainerId INTEGER,
                        fromContainerName TEXT NOT NULL,
                        toContainerName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        action TEXT NOT NULL
                    )
                """)

                db.execSQL("""
                    INSERT INTO movement_logs_new (
                        id,
                        itemId,
                        itemName,
                        fromContainerId,
                        toContainerId,
                        fromContainerName,
                        toContainerName,
                        timestamp,
                        action
                    )
                    SELECT
                        m.id,
                        m.itemId,
                        COALESCE(i.name, ''),
                        m.fromContainerId,
                        m.toContainerId,
                        m.fromContainerName,
                        m.toContainerName,
                        m.timestamp,
                        m.action
                    FROM movement_logs m
                    LEFT JOIN items i ON i.id = m.itemId
                """)

                db.execSQL("DROP TABLE movement_logs")

                db.execSQL("""
                    ALTER TABLE movement_logs_new
                    RENAME TO movement_logs
                """)

                db.execSQL("""
                    CREATE INDEX index_movement_logs_itemId
                    ON movement_logs(itemId)
                """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bagtracker_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
