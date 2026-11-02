package com.example.phoneblocker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.phoneblocker.data.local.dao.BlockLogDao
import com.example.phoneblocker.data.local.dao.RegexRuleDao
import com.example.phoneblocker.data.local.entity.BlockLog
import com.example.phoneblocker.data.local.entity.RegexRule

@Database(entities = [BlockLog::class, RegexRule::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockLogDao(): BlockLogDao
    abstract fun regexRuleDao(): RegexRuleDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_block_log_timestamp` ON `block_log` (`timestamp`)")
                // Clean up any duplicate patterns before creating the unique index to prevent SQLiteConstraintException and destructive reset
                db.execSQL("DELETE FROM regex_rule WHERE id NOT IN (SELECT MIN(id) FROM regex_rule GROUP BY pattern)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_regex_rule_pattern` ON `regex_rule` (`pattern`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app_database")
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
