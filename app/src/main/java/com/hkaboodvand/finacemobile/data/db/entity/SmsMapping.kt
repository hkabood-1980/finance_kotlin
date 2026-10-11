package com.hkaboodvand.finacemobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_account_mapping")
data class SmsMapping(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountNumber: String,
    val cardLast4: String = "",
    val accountId: Long,
    val bankName: String = "",
    val autoApprove: Boolean = true,
    val createdAt: String
)