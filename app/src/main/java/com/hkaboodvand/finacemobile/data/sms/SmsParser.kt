package com.hkaboodvand.finacemobile.data.sms

/**
 * پارسر هوشمند پیامک بانکی
 * پورت‌شده از Python
 */
object SmsParser {

    data class ParsedSms(
        val amount: Double? = null,
        val amountUnit: String? = null,
        val type: String? = null,
        val accountNumber: String? = null,
        val cardLast4: String? = null,
        val balance: Double? = null,
        val referenceCode: String? = null,
        val bank: String = "نامشخص",
        val raw: String = ""
    )

    // =====================================================
    // ۱. نرمال‌سازی متن
    // =====================================================
    private fun normalize(text: String): String {
        var t = text

        // حذف کاراکترهای مخفی Unicode
        val invisible = listOf(
            "\u200b", "\u200c", "\u200d",
            "\u200e", "\u200f",
            "\u202a", "\u202b", "\u202c",
            "\u202d", "\u202e",
            "\u2066", "\u2067", "\u2068", "\u2069",
            "\ufeff"
        )
        invisible.forEach { t = t.replace(it, "") }

        // حروف عربی → فارسی
        val replacements = mapOf(
            "ي" to "ی", "ك" to "ک", "ﻻ" to "لا",
            "ة" to "ه", "ۀ" to "ه", "ؤ" to "و",
            "إ" to "ا", "أ" to "ا", "ٱ" to "ا"
        )
        replacements.forEach { (arabic, persian) ->
            t = t.replace(arabic, persian)
        }

        // اعداد فارسی/عربی → انگلیسی
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
        for (i in 0..9) {
            t = t.replace(persianDigits[i], i.digitToChar())
            t = t.replace(arabicDigits[i], i.digitToChar())
        }

        // جداکننده‌های عددی
        t = t.replace("،", ",").replace("٬", ",")

        // یکسان‌سازی فاصله‌ها
        t = t.replace(Regex("[ \t]+"), " ")
        t = t.replace(Regex("\n+"), "\n")

        return t.trim()
    }

    // =====================================================
    // ۲. تشخیص پیامک بانکی
    // =====================================================
    private val nonBankSenders = listOf(
        "ایرانسل", "همراه اول", "رایتل", "شاتل", "آسیاتک",
        "MTN", "MCI", "Irancell", "Rightel",
        "دیجی‌کالا", "بامیلو", "اسنپ", "تپسی",
        "اسنپ‌فود", "علی‌بابا", "کافه‌بازار"
    )

    private val nonBankKeywords = listOf(
        "رمز پویا", "رمز پويا", "رمز یکبار مصرف",
        "اعتبار تا", "کد تایید", "کد ورود", "OTP",
        "شماره تایید", "کد فعال‌سازی", "کد فعال سازی",
        "تخفیف", "حراج", "فروش ویژه", "کد تخفیف"
    )

    private val bankKeywords = listOf(
        "بانک", "حساب", "کارت", "مانده", "موجودی",
        "واریز", "برداشت", "بستانکار", "بدهکار",
        "خرید", "پرداخت", "انتقال",
        "ریال", "تومان"
    )

    fun isBankSms(body: String, sender: String = ""): Boolean {
        if (body.isBlank()) return false
        val text = normalize(body)

        nonBankSenders.forEach {
            if (sender.contains(it) || text.contains(it)) return false
        }

        nonBankKeywords.forEach {
            if (text.contains(it)) return false
        }

        val score = bankKeywords.count { text.contains(it) }
        return score >= 2
    }

    // =====================================================
    // ۳. استخراج مبلغ
    // =====================================================
    private fun extractAmount(text: String): Triple<Double?, String?, String?> {
        // الگوی جمله‌ای
        val sentence = Regex(
            "(?:برداشت|واریز|خرید|پرداخت|دریافت|کسر|افزایش)\\s+از\\s+کارت\\s+(\\d{10,})\\s+به\\s+مبلغ\\s+([\\d,]+(?:\\.\\d+)?)\\s*(ریال|تومان)?"
        ).find(text)
        if (sentence != null) {
            val amount = sentence.groupValues[2].replace(",", "").toDoubleOrNull()
                ?: return Triple(null, null, null)
            val unit = sentence.groupValues[3].ifEmpty { "ریال" }
            val type = if (text.contains("برداشت") || text.contains("خرید") || text.contains("پرداخت"))
                "هزینه" else "درآمد"
            return Triple(amount, unit, type)
        }

        // کلید:مقدار با علامت
        val keywordSign = Regex(
            "(?:واریز|برداشت|پرداخت|دریافت|کسر|افزایش|خرید|خريد|انتقال|انتقالی)\\s*[:\\s]\\s*([\\d,]+(?:\\.\\d+)?)\\s*([+-])"
        ).find(text)
        if (keywordSign != null) {
            val amount = keywordSign.groupValues[1].replace(",", "").toDoubleOrNull()
            val sign = keywordSign.groupValues[2]
            val type = if (sign == "+") "درآمد" else "هزینه"
            return Triple(amount, "ریال", type)
        }

        // کلید:مقدار ساده
        val keyword = Regex(
            "(?:واریز|برداشت|پرداخت|دریافت|کسر|افزایش|خرید|خريد|انتقال|انتقالی)\\s*[:\\s]\\s*([\\d,]+(?:\\.\\d+)?)\\s*(ریال|تومان)?"
        ).find(text)
        if (keyword != null) {
            val amount = keyword.groupValues[1].replace(",", "").toDoubleOrNull()
            val unit = keyword.groupValues[2].ifEmpty { "ریال" }
            val type = if (text.contains("واریز") || text.contains("دریافت") || text.contains("افزایش"))
                "درآمد" else "هزینه"
            return Triple(amount, unit, type)
        }

        // علامت قبل
        val signBefore = Regex("(?:^|\\s)([+-])\\s*([\\d,]+(?:\\.\\d+)?)\\s*(ریال|تومان)?").find(text)
        if (signBefore != null) {
            val sign = signBefore.groupValues[1]
            val amount = signBefore.groupValues[2].replace(",", "").toDoubleOrNull()
            val unit = signBefore.groupValues[3].ifEmpty { "ریال" }
            val type = if (sign == "+") "درآمد" else "هزینه"
            return Triple(amount, unit, type)
        }

        // علامت بعد
        val signAfter = Regex("([\\d,]+(?:\\.\\d+)?)\\s*([+-])(?:\\s|$)").find(text)
        if (signAfter != null) {
            val amount = signAfter.groupValues[1].replace(",", "").toDoubleOrNull()
            val sign = signAfter.groupValues[2]
            val type = if (sign == "+") "درآمد" else "هزینه"
            return Triple(amount, "ریال", type)
        }

        // مبلغ عمومی
        val general = Regex("مبلغ\\s*[:\\s]\\s*([\\d,]+(?:\\.\\d+)?)\\s*(ریال|تومان)?").find(text)
        if (general != null) {
            val amount = general.groupValues[1].replace(",", "").toDoubleOrNull()
            val unit = general.groupValues[2].ifEmpty { "ریال" }
            return Triple(amount, unit, null)
        }

        // X ریال / تومان
        val unitPattern = Regex("([\\d,]+(?:\\.\\d+)?)\\s*(ریال|تومان)").find(text)
        if (unitPattern != null) {
            val amount = unitPattern.groupValues[1].replace(",", "").toDoubleOrNull()
            val unit = unitPattern.groupValues[2]
            return Triple(amount, unit, null)
        }

        return Triple(null, null, null)
    }

    // =====================================================
    // ۴. استخراج شماره کارت
    // =====================================================
    private fun extractCard(text: String): Pair<String?, String?> {
        val patterns = listOf(
            Regex("از\\s+کارت\\s+(\\d{13,})"),
            Regex("کارت\\s+(\\d{13,})"),
            Regex("حساب\\s*[:\\s]\\s*(\\d{10,})"),
            Regex("حساب\\s*[:\\s]\\s*(\\d{4,})"),
            Regex("کارت\\s*[:\\s]\\s*(\\d{4})\\b"),
            Regex("\\*+(\\d{4})\\b")
        )
        for (p in patterns) {
            val m = p.find(text) ?: continue
            val number = m.groupValues[1]
            return if (number.length >= 10) {
                number to number.takeLast(4)
            } else {
                null to number
            }
        }
        return null to null
    }

    // =====================================================
    // ۵. استخراج مانده
    // =====================================================
    private fun extractBalance(text: String): Double? {
        val m = Regex("مانده\\s*[:\\s]\\s*([\\d,]+(?:\\.\\d+)?)\\s*(ریال|تومان)?").find(text)
            ?: return null
        var balance = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val unit = m.groupValues[2].ifEmpty { "ریال" }
        if (unit == "ریال") balance /= 10
        return balance
    }

    // =====================================================
    // ۶. استخراج کد پیگیری
    // =====================================================
    private fun extractReference(text: String): String? {
        val patterns = listOf(
            Regex("(\\d+\\.\\d+\\.\\d+\\.\\d+)"),
            Regex("رمز\\s*[:\\s]\\s*(\\d+)"),
            Regex("کد\\s*پیگیری\\s*[:\\s]\\s*(\\d+)"),
            Regex("کدپیگیری\\s*[:\\s]\\s*(\\d+)")
        )
        for (p in patterns) {
            val m = p.find(text) ?: continue
            return m.groupValues[1]
        }
        return null
    }

    // =====================================================
    // ۷. تشخیص بانک
    // =====================================================
    private val bankSignatures = mapOf(
        "سپه" to listOf("سپه", "98300017"),
        "ملت" to listOf("ملت", "98300098"),
        "صادرات" to listOf("صادرات", "98300012"),
        "ملی" to listOf("ملی", "98300011"),
        "پاسارگاد" to listOf("پاسارگاد", "100085"),
        "پارسیان" to listOf("پارسیان"),
        "سامان" to listOf("سامان"),
        "تجارت" to listOf("تجارت", "98300020"),
        "کشاورزی" to listOf("کشاورزی", "98300016"),
        "رفاه" to listOf("رفاه", "98300013"),
        "اقتصاد نوین" to listOf("اقتصاد نوین", "اقتصادنوين"),
        "پست بانک" to listOf("پست بانک"),
        "بلوبانک" to listOf("بلوبانک")
    )

    fun detectBank(sender: String, body: String): String {
        val text = normalize(body)
        for ((bank, sigs) in bankSignatures) {
            for (sig in sigs) {
                if (text.contains(sig)) return bank
            }
        }
        for ((bank, sigs) in bankSignatures) {
            for (sig in sigs) {
                if (sender.contains(sig)) return bank
            }
        }
        return "نامشخص"
    }

    // =====================================================
    // ۸. پارس کامل
    // =====================================================
    fun parse(body: String, sender: String = ""): ParsedSms? {
        if (body.isBlank()) return null
        if (!isBankSms(body, sender)) return null

        val text = normalize(body)
        val (amount, unit, typeFromAmount) = extractAmount(text)
        val (accountNumber, cardLast4) = extractCard(text)
        val balance = extractBalance(text)
        val reference = extractReference(text)
        val bank = detectBank(sender, body)

        // تبدیل ریال به تومان
        var finalAmount = amount
        var finalUnit = unit
        if (amount != null && unit == "ریال") {
            finalAmount = amount / 10
            finalUnit = "تومان"
        }

        // تشخیص نوع
        var finalType = typeFromAmount
        if (finalType == null) {
            val incomeKw = listOf("واریز", "بستانکار", "دریافت", "افزایش", "دستورپرداخت وارده", "واریزی")
            val expenseKw = listOf("برداشت", "بدهکار", "پرداخت", "خرید", "خريد", "کسر", "کسری", "پایانه فروش", "هزینه")

            for (kw in incomeKw) {
                if (text.contains(kw)) {
                    finalType = "درآمد"
                    break
                }
            }
            if (finalType == null) {
                for (kw in expenseKw) {
                    if (text.contains(kw)) {
                        finalType = "هزینه"
                        break
                    }
                }
            }
        }

        return ParsedSms(
            amount = finalAmount,
            amountUnit = finalUnit,
            type = finalType,
            accountNumber = accountNumber,
            cardLast4 = cardLast4,
            balance = balance,
            referenceCode = reference,
            bank = bank,
            raw = body
        )
    }
}