package com.hkaboodvand.finacemobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val date: String,
    val type: String,
    val category: String = "",
    val amount: Double,
    val place: String = "",
    val description: String = "",
    val balanceAfter: Double = 0.0
)