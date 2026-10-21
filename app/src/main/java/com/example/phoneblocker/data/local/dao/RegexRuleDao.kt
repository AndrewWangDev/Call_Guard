package com.example.phoneblocker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.phoneblocker.data.local.entity.RegexRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexRuleDao {
    @Query("SELECT * FROM regex_rule")
    fun getAllRules(): Flow<List<RegexRule>>

    @Query("SELECT * FROM regex_rule")
    suspend fun getAllRulesDirect(): List<RegexRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RegexRule)

    @Delete
    suspend fun deleteRule(rule: RegexRule)
}
