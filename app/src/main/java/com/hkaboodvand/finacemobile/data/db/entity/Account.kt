package com.hkaboodvand.finacemobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bank: String = "",
    val cardNumber: String = "",
    val balance: Double = 0.0,
    val type: String = ""
)