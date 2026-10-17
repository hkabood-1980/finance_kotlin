package com.hkaboodvand.finacemobile.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_queue")
data class SmsQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: String,
    val status: String = "pending",
    val parsedAmount: Double? = null,
    val parsedType: String? = null,
    val parsedCard: String? = null,
    val accountId: Long? = null,
    val rawParsed: String = "",
    // ─── فیلدهای جدید ───
    val confidence: Int = 0,
    val isFailed: Boolean = false,
    val isReversal: Boolean = false,
    val jalaliDateTime: String? = null
)