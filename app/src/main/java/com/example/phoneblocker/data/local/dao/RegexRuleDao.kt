package com.example.phoneblocker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.phoneblocker.data.local.entity.RegexRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexRuleDao {
    @Query("SELECT * FROM regex_rule")
    fun getAllRules(): Flow<List<RegexRule>>

    @Insert
    suspend fun insertRule(rule: RegexRule)

    @Delete
    suspend fun deleteRule(rule: RegexRule)
}
