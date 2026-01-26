package com.example.phoneblocker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regex_rule")
data class RegexRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pattern: String,
    val type: RuleType // "BLACKLIST" or "WHITELIST"
)

enum class RuleType {
    BLACKLIST,
    WHITELIST
}
