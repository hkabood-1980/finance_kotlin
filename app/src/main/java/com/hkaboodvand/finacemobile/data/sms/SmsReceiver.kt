package com.hkaboodvand.finacemobile.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.db.entity.SmsQueue
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
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

            for (sms in messages) {
                val sender = sms.originatingAddress ?: ""
                val body = sms.messageBody ?: ""

                Log.d("SmsReceiver", "SMS from $sender: $body")

                processSms(db, repo, sender, body)
            }
        }
    }

    private suspend fun processSms(
        db: AppDatabase,
        repo: FinanceRepository,
        sender: String,
        body: String
    ) {
        val parsed = SmsParser.parse(body, sender) ?: return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = dateFormat.format(Date())

        // ─── جستجوی mapping با ۳ سطح ───
        var mapping: com.hkaboodvand.finacemobile.data.db.entity.SmsMapping? = null

        // سطح ۱: cardLast4 (مهم‌ترین)
        if (!parsed.cardLast4.isNullOrEmpty()) {
            mapping = db.smsDao().getMappingByCardLast4(parsed.cardLast4!!)
            Log.d("SmsReceiver", "Level 1 (cardLast4=${parsed.cardLast4}): ${mapping != null}")
        }

        // سطح ۲: accountNumber
        if (mapping == null && !parsed.accountNumber.isNullOrEmpty()) {
            mapping = db.smsDao().getMappingByAccountNumber(parsed.accountNumber!!)
            Log.d("SmsReceiver", "Level 2 (accountNumber): ${mapping != null}")
        }

        // سطح ۳: bankName
        if (mapping == null && parsed.bank != "نامشخص") {
            mapping = db.smsDao().getMappingByBankName(parsed.bank)
            Log.d("SmsReceiver", "Level 3 (bank=${parsed.bank}): ${mapping != null}")
        }

        // ─── اگه mapping پیدا شد → خودکار ثبت کن ───
        if (mapping != null && mapping.autoApprove) {
            val amount = parsed.amount
            val type = parsed.type

            if (amount != null && type != null) {
                val result = repo.registerTransaction(
                    accountId = mapping.accountId,
                    type = type,
                    category = "پیامک بانکی (خودکار)",
                    amount = amount,
                    place = parsed.bank,
                    description = body.take(100)
                )

                if (result.isSuccess) {
                    db.smsDao().insertSms(
                        SmsQueue(
                            sender = sender,
                            body = body,
                            receivedAt = now,
                            status = "approved",
                            parsedAmount = parsed.amount,
                            parsedType = parsed.type,
                            parsedCard = parsed.cardLast4,
                            accountId = mapping.accountId,
                            rawParsed = parsedToJson(parsed)
                        )
                    )
                    Log.d("SmsReceiver", "✅ Auto-approved SMS to account ${mapping.accountId}")
                } else {
                    Log.e("SmsReceiver", "❌ Auto-approve failed: ${result.exceptionOrNull()?.message}")
                }
                return
            }
        }

        // ─── در غیر این صورت، به صف اضافه کن ───
        db.smsDao().insertSms(
            SmsQueue(
                sender = sender,
                body = body,
                receivedAt = now,
                status = "pending",
                parsedAmount = parsed.amount,
                parsedType = parsed.type,
                parsedCard = parsed.cardLast4,
                rawParsed = parsedToJson(parsed)
            )
        )
        Log.d("SmsReceiver", "📩 SMS queued for approval")
    }
    private fun parsedToJson(parsed: SmsParser.ParsedSms): String {
        return JSONObject().apply {
            put("amount", parsed.amount)
            put("type", parsed.type)
            put("card_last4", parsed.cardLast4)
            put("account_number", parsed.accountNumber)
            put("balance", parsed.balance)
            put("reference_code", parsed.referenceCode)
            put("bank", parsed.bank)
        }.toString()
    }
}