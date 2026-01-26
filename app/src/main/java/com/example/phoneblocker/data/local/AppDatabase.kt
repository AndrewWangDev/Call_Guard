package com.example.phoneblocker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.phoneblocker.data.local.dao.BlockLogDao
import com.example.phoneblocker.data.local.dao.RegexRuleDao
import com.example.phoneblocker.data.local.entity.BlockLog
import com.example.phoneblocker.data.local.entity.RegexRule

@Database(entities = [BlockLog::class, RegexRule::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockLogDao(): BlockLogDao
    abstract fun regexRuleDao(): RegexRuleDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
