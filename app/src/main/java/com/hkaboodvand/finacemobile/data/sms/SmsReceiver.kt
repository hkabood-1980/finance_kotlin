package com.hkaboodvand.finacemobile.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.db.entity.SmsMapping
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

                try {
                    processSms(db, repo, sender, body)
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing SMS: ${e.message}")
                }
            }
        }
    }

    private suspend fun processSms(
        db: AppDatabase,
        repo: FinanceRepository,
        sender: String,
        body: String
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = dateFormat.format(Date())

        // ─── مرحله ۱: تحلیل پیامک ───
        val parsed = try {
            SmsParser.parse(body, sender)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Parse error: ${e.message}")
            null
        }

        // ─── مرحله ۲: چک کردن بانکی بودن ───
        val isBankSms = try {
            SmsParser.isBankSms(body, sender)
        } catch (e: Exception) {
            false
        }

        // ─── اگه پیامک بانکی نبود → ذخیره نکن ───
        if (!isBankSms) {
            Log.d("SmsReceiver", "Not a bank SMS, skipping")
            return
        }

        // ─── مرحله ۳: جستجوی mapping (فقط اگه parse شد) ───
        var mapping: SmsMapping? = null

        if (parsed != null) {
            // سطح ۱: cardLast4
            if (!parsed.cardLast4.isNullOrEmpty()) {
                mapping = db.smsDao().getMappingByCardLast4(parsed.cardLast4!!)
                Log.d("SmsReceiver", "L1 (cardLast4=${parsed.cardLast4}): ${mapping != null}")
            }

            // سطح ۲: accountNumber
            if (mapping == null && !parsed.accountNumber.isNullOrEmpty()) {
                mapping = db.smsDao().getMappingByAccountNumber(parsed.accountNumber!!)
                Log.d("SmsReceiver", "L2 (accountNumber): ${mapping != null}")
            }

            // سطح ۳: bankName
            if (mapping == null && parsed.bank != "نامشخص") {
                mapping = db.smsDao().getMappingByBankName(parsed.bank)
                Log.d("SmsReceiver", "L3 (bank=${parsed.bank}): ${mapping != null}")
            }
        }

        // ─── مرحله ۴: تصمیم‌گیری ───

        // اگه mapping پیدا شد و autoApprove فعاله
        if (mapping != null && mapping.autoApprove && parsed != null) {
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
                    // ثبت به‌عنوان approved
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
                    Log.d("SmsReceiver", "✅ Auto-approved to account ${mapping.accountId}")
                    return
                } else {
                    Log.e("SmsReceiver", "❌ Auto-approve failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }

        // ─── در غیر این صورت: ذخیره در صف (pending) ───
        // این حتی وقتی parse نشده هم انجام می‌شه
        db.smsDao().insertSms(
            SmsQueue(
                sender = sender,
                body = body,
                receivedAt = now,
                status = "pending",
                parsedAmount = parsed?.amount,
                parsedType = parsed?.type,
                parsedCard = parsed?.cardLast4,
                accountId = null,
                rawParsed = if (parsed != null) parsedToJson(parsed) else ""
            )
        )
        Log.d("SmsReceiver", "📩 SMS queued (parsed=${parsed != null})")
    }

    private fun parsedToJson(parsed: SmsParser.ParsedSms): String {
        return try {
            JSONObject().apply {
                put("amount", parsed.amount)
                put("type", parsed.type)
                put("card_last4", parsed.cardLast4)
                put("account_number", parsed.accountNumber)
                put("balance", parsed.balance)
                put("reference_code", parsed.referenceCode)
                put("bank", parsed.bank)
            }.toString()
        } catch (e: Exception) {
            ""
        }
    }
}