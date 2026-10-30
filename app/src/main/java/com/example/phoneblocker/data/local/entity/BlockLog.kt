package com.example.phoneblocker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "block_log",
    indices = [Index(value = ["timestamp"])]
)
data class BlockLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val reason: String // The regex rule that matched
)
