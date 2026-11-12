
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
    version = 3,
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


private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE item_tag_cross_ref_new (
                itemId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY(itemId, tagId),
                FOREIGN KEY(itemId)
                    REFERENCES items(id)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE,
                FOREIGN KEY(tagId)
                    REFERENCES tags(id)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            INSERT OR IGNORE INTO item_tag_cross_ref_new
                (itemId, tagId)
            SELECT ref.itemId, ref.tagId
            FROM item_tag_cross_ref ref
            INNER JOIN items i ON i.id = ref.itemId
            INNER JOIN tags t ON t.id = ref.tagId
        """)

        db.execSQL("DROP TABLE item_tag_cross_ref")

        db.execSQL("""
            ALTER TABLE item_tag_cross_ref_new
            RENAME TO item_tag_cross_ref
        """)

        db.execSQL("""
            CREATE INDEX index_item_tag_cross_ref_itemId
            ON item_tag_cross_ref(itemId)
        """)

        db.execSQL("""
            CREATE INDEX index_item_tag_cross_ref_tagId
            ON item_tag_cross_ref(tagId)
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
                    .addMigrations(
    MIGRATION_1_2,
    MIGRATION_2_3
)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
