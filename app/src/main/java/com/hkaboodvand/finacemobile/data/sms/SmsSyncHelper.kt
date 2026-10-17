package com.hkaboodvand.finacemobile.data.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.db.entity.SmsMapping
import com.hkaboodvand.finacemobile.data.db.entity.SmsQueue
import com.hkaboodvand.finacemobile.data.prefs.AppPreferences
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsSyncHelper {

    suspend fun syncMissedSms(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val repo = FinanceRepository(db)
        val prefs = AppPreferences(context)

        val currentTimestamp = System.currentTimeMillis()
        var lastTimestamp = prefs.lastSmsTimestamp.first()

        // اگه اولین باره، از الان شروع کن
        if (lastTimestamp == null || lastTimestamp == 0L) {
            lastTimestamp = currentTimestamp
            prefs.setLastSmsTimestamp(lastTimestamp)
            Log.d("SmsSync", "First run: Setting timestamp to now ($currentTimestamp)")
            return@withContext
        }

        Log.d("SmsSync", "Last timestamp: $lastTimestamp")

        val cursor = try {
            context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                "date > ?",
                arrayOf(lastTimestamp.toString()),
                "date ASC"
            )
        } catch (e: Exception) {
            Log.e("SmsSync", "Query error: ${e.message}")
            null
        }

        cursor?.use {
            val count = it.count
            Log.d("SmsSync", "Found $count new messages")

            while (it.moveToNext()) {
                val sender = it.getString(0) ?: ""
                val body = it.getString(1) ?: ""
                val timestamp = it.getLong(2)

                try {
                    if (SmsParser.isBankSms(body, sender)) {
                        processMissedSms(db, repo, sender, body, timestamp)
                    }
                    prefs.setLastSmsTimestamp(timestamp)
                } catch (e: Exception) {
                    Log.e("SmsSync", "Error processing: ${e.message}")
                }
            }
        }
    }

    private suspend fun processMissedSms(
        db: AppDatabase,
        repo: FinanceRepository,
        sender: String,
        body: String,
        timestamp: Long
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = dateFormat.format(Date(timestamp))

        val parsed = SmsParser.parse(body, sender) ?: return
        if (parsed.isFailed || parsed.confidence < 50) return

        if (db.smsDao().isSmsExists(body, now) > 0) return

        var mapping: SmsMapping? = null
        if (!parsed.cardLast4.isNullOrEmpty()) {
            mapping = db.smsDao().getMappingByCardLast4(parsed.cardLast4!!)
        }
        if (mapping == null && !parsed.accountNumber.isNullOrEmpty()) {
            mapping = db.smsDao().getMappingByAccountNumber(parsed.accountNumber!!)
        }
        if (mapping == null && parsed.bank != SmsParser.UNKNOWN_BANK) {
            mapping = db.smsDao().getMappingByBankName(parsed.bank)
        }

        // ─── خودکار با اطمینان >= 85 ───
        if (mapping != null && mapping.autoApprove &&
            parsed.confidence >= 85 &&
            parsed.amount != null &&
            parsed.type != null
        ) {
            val result = repo.registerTransaction(
                accountId = mapping.accountId,
                type = parsed.type,
                category = "پیامک بانکی (خودکار)",
                amount = parsed.amount,
                place = parsed.bank,
                description = body.take(100)
            )

            if (result.isSuccess) {
                db.smsDao().insertSms(
                    SmsQueue(
                        sender = sender, body = body, receivedAt = now, status = "approved",
                        parsedAmount = parsed.amount, parsedType = parsed.type,
                        parsedCard = parsed.cardLast4, accountId = mapping.accountId,
                        confidence = parsed.confidence, isFailed = parsed.isFailed,
                        isReversal = parsed.isReversal, jalaliDateTime = parsed.jalaliDateTime
                    )
                )
                return
            }
        }

        // صف
        db.smsDao().insertSms(
            SmsQueue(
                sender = sender, body = body, receivedAt = now, status = "pending",
                parsedAmount = parsed.amount, parsedType = parsed.type,
                parsedCard = parsed.cardLast4, accountId = mapping?.accountId,
                confidence = parsed.confidence, isFailed = parsed.isFailed,
                isReversal = parsed.isReversal, jalaliDateTime = parsed.jalaliDateTime
            )
        )
    }
}