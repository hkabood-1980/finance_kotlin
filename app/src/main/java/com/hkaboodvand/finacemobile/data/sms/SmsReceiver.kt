package com.hkaboodvand.finacemobile.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.db.entity.SmsMapping
import com.hkaboodvand.finacemobile.data.db.entity.SmsQueue
import com.hkaboodvand.finacemobile.data.prefs.AppPreferences
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val repo = FinanceRepository(db)
            val prefs = AppPreferences(context)

            for (sms in messages) {
                val sender = sms.originatingAddress ?: ""
                val body = sms.messageBody ?: ""
                val timestamp = sms.timestampMillis

                try {
                    processSms(db, repo, sender, body, timestamp)
                    prefs.setLastSmsTimestamp(timestamp)
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error: ${e.message}")
                }
            }
        }
    }

    private suspend fun processSms(
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

        // ─── اصلاح مشکل ۱: شرط 85 ───
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