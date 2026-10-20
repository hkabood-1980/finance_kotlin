package com.hkaboodvand.finacemobile.data.sms

import java.math.BigDecimal
import java.math.RoundingMode

object SmsParser {

    const val TYPE_INCOME = "درآمد"
    const val TYPE_EXPENSE = "هزینه"
    const val UNKNOWN_BANK = "نامشخص"
    private const val UNIT_TOMAN = "تومان"

    data class ParsedSms(
        val amount: Double? = null,
        val amountUnit: String? = null,
        val type: String? = null,
        val accountNumber: String? = null,
        val cardLast4: String? = null,
        val balance: Double? = null,
        val referenceCode: String? = null,
        val bank: String = UNKNOWN_BANK,
        val raw: String = "",
        val isFailed: Boolean = false,
        val isReversal: Boolean = false,
        val jalaliDateTime: String? = null,
        val confidence: Int = 0
    )

    // =====================================================
    // ابزارهای پایه
    // =====================================================
    private val WS = Regex("[ \\t\\u00a0]+")
    private val MULTI_NL = Regex("\\n{2,}")
    private const val FA = "[\\u0600-\\u06FF]"

    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
                in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                'ي', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ة', 'ۀ' -> sb.append('ه')
                'ؤ' -> sb.append('و')
                'إ', 'أ', 'ٱ' -> sb.append('ا')
                'ﻻ', 'ﻼ' -> sb.append("لا")
                '٫' -> sb.append('.')
                '٬', '،' -> sb.append(',')
                '\u200c' -> sb.append(' ')
                '\u200b', '\u200d', '\u200e', '\u200f',
                in '\u202a'..'\u202e', in '\u2066'..'\u2069',
                '\ufeff', '\u0640', '\r' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
            .replace(WS, " ")
            .replace(MULTI_NL, "\n")
            .trim()
            .lowercase()
    }

    private fun nk(vararg words: String): List<String> = words.map { normalize(it) }

    private fun blankAll(src: String, re: Regex): String =
        re.replace(src) { " ".repeat(it.value.length) }

    private fun blankRange(src: String, r: IntRange): String =
        src.replaceRange(r, " ".repeat(r.last - r.first + 1))

    // =====================================================
    // ۱. تشخیص پیامک بانکی (با اولویت کلمه «مانده»)
    // =====================================================
    private val NON_BANK_SENDERS = nk(
        "ایرانسل", "همراه اول", "رایتل", "شاتل", "آسیاتک",
        "mtn", "mci", "irancell", "rightel",
        "دیجی کالا", "بامیلو", "اسنپ", "snapp", "تپسی", "علی بابا", "کافه بازار", "دیوار"
    )

    private val NON_BANK_KEYWORDS = nk(
        "رمز پویا", "رمز یکبار مصرف", "رمز یک بار مصرف", "رمز دوم",
        "اعتبار تا", "کد تایید", "کد ورود", "کد فعال سازی", "کد فعالسازی", "کد امنیتی",
        "شماره تایید", "otp", "cvv2", "verification code",
        "تخفیف", "حراج", "فروش ویژه", "جشنواره", "قرعه کشی", "کد تخفیف"
    )

    private val BANK_WORDS = nk(
        "بانک", "حساب", "کارت", "مانده", "موجودی",
        "واریز", "برداشت", "بستانکار", "بدهکار",
        "خرید", "پرداخت", "انتقال", "ریال", "تومان"
    )

    private val MONEY_HINT = Regex(
        "\\d[\\d,]*\\s*(?:ریال|تومان|irr|irt)" +
                "|[+-]\\s*\\d{1,3}(?:,\\d{3})+" +
                "|\\d{1,3}(?:,\\d{3})+[+-]" +
                "|(?:مبلغ|واریز|برداشت|خرید|پرداخت|انتقال|کسر|مانده|موجودی)\\s*:?\\s*[+-]?\\s*\\d"
    )

    fun isBankSms(body: String, sender: String = ""): Boolean {
        if (body.isBlank()) return false
        val text = normalize(body)
        val senderN = normalize(sender)

        if (NON_BANK_SENDERS.any { senderN.contains(it) }) return false
        if (NON_BANK_KEYWORDS.any { text.contains(it) }) return false

        // ─── اولویت ۱: کلمه «مانده» یا «موجودی» (مطمئن‌ترین نشانه) ───
        if (text.contains("مانده") || text.contains("موجودی")) {
            if (Regex("[\\d,]+").containsMatchIn(text)) return true
        }

        // ─── اولویت ۲: شماره حساب طولانی در خط اول ───
        val hasAccountInFirstLine = text.lines().firstOrNull()?.let {
            val cleaned = it.replace(" ", "").replace(".", "")
            cleaned.matches(Regex("^\\d{9,}$"))
        } ?: false
        if (hasAccountInFirstLine) return true

        // ─── اولویت ۳: مبلغ با علامت + یا - ───
        val hasSignedAmount = Regex("(?:^|\\s)([+-])\\s*[\\d,]+").containsMatchIn(text) ||
                Regex("[\\d,]+\\s*([+-])(?:\\s|$)").containsMatchIn(text)
        if (hasSignedAmount) return true

        // ─── اولویت ۴: بر اساس کلمات کلیدی ───
        val score = BANK_WORDS.count { text.contains(it) }
        if (score < 2) return false
        return MONEY_HINT.containsMatchIn(text) || score >= 3
    }

    // =====================================================
    // ۲. الگوهای عدد
    // =====================================================
    private const val NUM =
        "(?<![\\d,.])(\\d{1,3}(?:,\\d{3})+(?!\\d)(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)"
    private const val UNIT = "(ریال|تومان|irr|irt)"

    private fun parseNumber(token: String): BigDecimal? {
        val t = token.trim()
        val s = when {
            t.contains(',') -> t.replace(",", "")
            Regex("^\\d{1,3}(\\.\\d{3})+$").matches(t) -> t.replace(".", "")
            else -> t
        }
        return s.toBigDecimalOrNull()
    }

    private fun toToman(value: BigDecimal, unit: String?): Double {
        val isToman = unit == UNIT_TOMAN || unit == "irt"
        val v = if (isToman) value else value.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP)
        return v.toDouble()
    }

    private val MAX_RIAL = BigDecimal("1e13")

    // =====================================================
    // ۳. حذف شناسه‌ها
    // =====================================================
    private val DATE_RE = Regex("(?<!\\d)(1[34]\\d{2}|\\d{2})[/\\-](\\d{1,2})[/\\-](\\d{1,2})(?!\\d)")
    private val TIME_RE = Regex("(?<!\\d)(?:[01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?(?!\\d)")
    private val SHEBA_RE = Regex("ir\\s?\\d{2}[\\d ]{20,24}")
    private val CARD_16 = Regex("(?<!\\d)\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}(?!\\d)")
    private val MASKED_TOKEN = Regex("(?<!\\w)[\\d*•#×x][\\d*•#×x\\- ]{5,22}\\d{4}(?!\\d)")
    private val SHORT_MASK = Regex("(?:[*•#×]+|x{2,})[- ]?(\\d{4})(?!\\d)")
    private val ID_LABELED = Regex(
        "(?<!$FA)(?:کارت|حساب|سپرده|شماره|شبا|پیگیری|رهگیری|مرجع|رمز|کد|ref|rrn|stan)" +
                "\\s*(?:کارت|حساب|پیگیری|مرجع)?\\s*:?\\s*\\d{4,}(?![\\d,])(?!\\s*(?:ریال|تومان))"
    )
    private val LONG_DIGITS = Regex("(?<![\\d,.])\\d{10,}(?![\\d,])")
    // الگوی شماره حساب با پشتیبانی از نقطه
    private val LABELED_ACCT = Regex(
        "(?<!$FA)(?:کارت|حساب|سپرده)\\s*(?:شماره\\s*)?:?\\s*([\\d.]+)(?![\\d,*•#×x-])(?!\\s*(?:ریال|تومان))"
    )

    private fun maskCount(s: String) = s.count { it == '*' || it == '•' || it == '#' || it == '×' || it == 'x' }

    private fun luhn(d: String): Boolean {
        var sum = 0
        var alt = false
        for (i in d.length - 1 downTo 0) {
            var n = d[i] - '0'
            if (alt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }

    // =====================================================
    // ۴. کارت / حساب (با پشتیبانی از نقطه)
    // =====================================================
    private data class CardInfo(val accountNumber: String?, val last4: String?)

    private fun extractCard(text: String): CardInfo {
        // ۱) شماره کارت کامل ۱۶ رقمی (با Luhn یا برچسب «کارت»)
        for (m in CARD_16.findAll(text)) {
            val digits = m.value.filter { it.isDigit() }
            val before = text.substring(maxOf(0, m.range.first - 12), m.range.first)
            if (luhn(digits) || before.contains("کارت")) return CardInfo(digits, digits.takeLast(4))
        }

        // ۲) شماره حساب با برچسب (با پشتیبانی از نقطه)
        val labeledRaw = LABELED_ACCT.find(text)?.groupValues?.get(1)
        if (labeledRaw != null) {
            val cleaned = labeledRaw.replace(".", "")
            if (cleaned.length >= 9) {
                return CardInfo(cleaned, cleaned.takeLast(4))
            }
        }

        // ۳) کارت ماسک‌شده مثل 6037-99**-****-7890
        for (m in MASKED_TOKEN.findAll(text)) {
            if (maskCount(m.value) >= 2) return CardInfo(null, m.value.takeLast(4))
        }

        SHORT_MASK.find(text)?.let { return CardInfo(null, it.groupValues[1]) }

        // ۴) عدد کوتاه با برچسب
        if (labeledRaw != null) {
            val cleaned = labeledRaw.replace(".", "")
            return CardInfo(null, cleaned.takeLast(4))
        }

        // ۵) شماره حساب بدون برچسب در خط اول (با پشتیبانی از نقطه)
        val firstLine = text.lines().firstOrNull()?.trim() ?: ""
        val cleanedFirstLine = firstLine.replace(".", "").replace(" ", "")
        if (cleanedFirstLine.matches(Regex("^\\d{9,}$"))) {
            return CardInfo(cleanedFirstLine, cleanedFirstLine.takeLast(4))
        }

        return CardInfo(null, null)
    }

    // =====================================================
    // ۵. مانده
    // =====================================================
    private val BALANCE_RE = Regex(
        "(?:مانده|موجودی)(?:\\s*(?:حساب|کارت|شما|قابل\\s*برداشت|نهایی|فعلی|جدید|بعد))*" +
                "\\s*:?\\s*([+-])?\\s*$NUM([+-])?(?:\\s*$UNIT)?"
    )

    private data class BalanceHit(val toman: Double, val range: IntRange)

    private fun extractBalance(text: String): BalanceHit? {
        val m = BALANCE_RE.find(text) ?: return null
        var v = parseNumber(m.groupValues[2]) ?: return null
        val negative = m.groupValues[1] == "-" || m.groupValues[3] == "-"
        if (negative) v = v.negate()
        val unit = m.groupValues[4].ifEmpty { null }
        return BalanceHit(toToman(v, unit), m.range)
    }

    // =====================================================
    // ۶. مبلغ (با پشتیبانی از همه‌ی کلمات کلیدی)
    // =====================================================
    private data class AmountHit(
        val rial: BigDecimal,
        val unit: String?,
        val sign: Char?,
        val keyword: String?,
        val start: Int
    )

    private const val KW_ALT =
        "واریزی|واریز|برداشت|خرید|خرید اینترنتی|خرید از پوز|خرید از پایانه|پایانه فروش|پرداخت|پرداخت قبض|انتقال|انتقال از|انتقال به|انتقال پایا|پایا|انتقال پل|پل|کسر|افزایش|دریافت|بستانکار|بدهکار|هزینه|کارت"

    private val LABEL_AMOUNT = Regex(
        "(?:مبلغ|مقدار)\\s*:?\\s*([+-])?\\s*$NUM([+-])?(?:\\s*$UNIT)?"
    )

    private val TERMINAL_SALE_AMOUNT = Regex(
        "(?:خرید\\s*پایانه\\s*فروش|پایانه\\s*فروش)\\s*:?\\s*([+-])?\\s*$NUM([+-])?(?:\\s*$UNIT)?"
    )

    private val KEYWORD_AMOUNT = Regex(
        "(?<!$FA)($KW_ALT)\\s*:?\\s*([+-])?\\s*$NUM([+-])?(?:\\s*$UNIT)?"
    )

    private val ACTION_AMOUNT = Regex(
        "(?<!$FA)($KW_ALT)(?:[^\\d\\n]{0,32})?\\s*:?\\s*([+-])?\\s*$NUM([+-])?(?:\\s*$UNIT)?"
    )

    // مبلغ با علامت در آخر (مثل `6,080,000-` یا `30,000,000+`)
    private val NUM_WITH_TRAILING_SIGN = Regex("$NUM([+-])(?=\\s|\n|\$)")
    private val SIGNED_LEAD = Regex("(?:^|\\s)([+-])\\s*$NUM(?:\\s*$UNIT)?")
    private val UNIT_AMOUNT = Regex("$NUM\\s*$UNIT")

    private fun mk(num: String, unit: String?, sign: String?, kw: String?, start: Int): AmountHit? {
        val v = parseNumber(num) ?: return null
        if (v.signum() <= 0) return null
        val rialEquivalent = if (unit == UNIT_TOMAN || unit == "irt") v.multiply(BigDecimal.TEN) else v
        if (rialEquivalent > MAX_RIAL) return null
        return AmountHit(v, unit?.ifEmpty { null }, sign?.firstOrNull(), kw, start)
    }

    private fun signOf(lead: String, trail: String): String? = when {
        lead.isNotEmpty() -> lead
        trail.isNotEmpty() -> trail
        else -> null
    }

    private fun findAmount(work: String): AmountHit? {
        for (m in TERMINAL_SALE_AMOUNT.findAll(work)) {
            val g = m.groupValues
            mk(g[2], g[4].ifEmpty { null }, signOf(g[1], g[3]), "خرید", m.range.first)?.let { return it }
        }
        for (m in LABEL_AMOUNT.findAll(work)) {
            val g = m.groupValues
            mk(g[2], g[4].ifEmpty { null }, signOf(g[1], g[3]), null, m.range.first)?.let { return it }
        }
        for (m in KEYWORD_AMOUNT.findAll(work)) {
            val g = m.groupValues
            mk(g[3], g[5].ifEmpty { null }, signOf(g[2], g[4]), g[1], m.range.first)?.let { return it }
        }
        for (m in ACTION_AMOUNT.findAll(work)) {
            val g = m.groupValues
            mk(g[3], g[5].ifEmpty { null }, signOf(g[2], g[4]), g[1], m.range.first)?.let { return it }
        }
        for (m in NUM_WITH_TRAILING_SIGN.findAll(work)) {
            val g = m.groupValues
            mk(g[1], null, g[2], null, m.range.first)?.let { return it }
        }
        for (m in SIGNED_LEAD.findAll(work)) {
            val g = m.groupValues
            mk(g[2], g[3].ifEmpty { null }, g[1], null, m.range.first)?.let { return it }
        }
        for (m in UNIT_AMOUNT.findAll(work)) {
            val g = m.groupValues
            mk(g[1], g[2], null, null, m.range.first)?.let { return it }
        }
        return null
    }

    // =====================================================
    // ۷. نوع تراکنش
    // =====================================================
    private val INCOME_WORDS = nk("واریزی", "واریز", "بستانکار", "افزایش", "دریافتی", "دریافت", "ورود وجه", "deposit", "credit")
    private val EXPENSE_WORDS = nk("برداشت", "بدهکار", "پرداخت", "خرید", "خرید اینترنتی", "خرید از پوز", "خرید از پایانه", "کسر", "کاهش", "پایانه فروش", "هزینه", "پرداخت قبض", "withdraw", "debit")
    private val WEAK_EXPENSE = normalize("انتقال")
    private val WEAK_INCOME = normalize("کارت")

    private fun wordRe(words: List<String>) = Regex(
        "(?<!$FA)(?:" + words.joinToString("|") { Regex.escape(it) } + ")(?!$FA)"
    )

    private val INCOME_RE = wordRe(INCOME_WORDS)
    private val EXPENSE_RE = wordRe(EXPENSE_WORDS)

    private val NOISE_RE = Regex(
        "دریافت\\s*کننده|پرداخت\\s*کننده|واریز\\s*کننده|پرداخت\\s*یار|پیگیری\\s*پرداخت|کد\\s*پیگیری"
    )

    private val FAILED_RE = Regex(
        "ناموفق|نا موفق|عدم موفقیت|موفق نبود|ناکام|لغو شد|declined|failed|خطا در|(?<!$FA)رد شد"
    )
    private val REVERSAL_RE = Regex("برگشت|عودت|مرجوع|بازگشت وجه|استرداد|ابطال|refund|reversal")

    private fun decideType(work: String, hit: AmountHit): Pair<String?, Boolean> {
        hit.sign?.let { return (if (it == '+') TYPE_INCOME else TYPE_EXPENSE) to true }
        hit.keyword?.let { kw ->
            val k = normalize(kw)
            if (k in INCOME_WORDS) return TYPE_INCOME to true
            if (k in EXPENSE_WORDS) return TYPE_EXPENSE to true
        }

        val clean = blankAll(work, NOISE_RE)
        val occ = INCOME_RE.findAll(clean).map { it.range.first to TYPE_INCOME }.toList() +
                EXPENSE_RE.findAll(clean).map { it.range.first to TYPE_EXPENSE }.toList()

        if (occ.isEmpty()) {
            return if (clean.contains(WEAK_EXPENSE)) TYPE_EXPENSE to false else null to false
        }
        val before = occ.filter { it.first <= hit.start }.maxByOrNull { it.first }
        if (before != null && hit.start - before.first <= 80) return before.second to true
        val after = occ.filter { it.first > hit.start }.minByOrNull { it.first }
        if (after != null && after.first - hit.start <= 80) return after.second to true
        return occ.minByOrNull { it.first }!!.second to false
    }

    // =====================================================
    // ۸. کد پیگیری و تاریخ
    // =====================================================
    private val REF_RES = listOf(
        Regex("(?:پیگیری|رهگیری|مرجع)\\s*:?\\s*(\\d{4,})"),
        Regex("(?:ref|rrn|stan)\\s*[:.#]?\\s*(\\d{4,})"),
        Regex("رمز\\s*[:\\s]\\s*(\\d+)"),
        Regex("(\\d+\\.\\d+\\.\\d+\\.\\d+)")
    )

    private fun extractReference(text: String): String? {
        for (p in REF_RES) {
            val m = p.find(text) ?: continue
            return m.groupValues[1]
        }
        return null
    }

    private fun extractDateTime(text: String): String? {
        val d = DATE_RE.find(text)?.value
        val t = TIME_RE.find(text)?.value
        return when {
            d != null && t != null -> "$d $t"
            d != null -> d
            else -> null
        }
    }

    // =====================================================
    // ۹. تشخیص بانک
    // =====================================================
    private class BankDef(
        val name: String,
        aliases: List<String>,
        val needsPrefix: Boolean = false,
        val senderCodes: List<String> = emptyList(),
        val senderLatin: List<String> = emptyList()
    ) {
        val prefixed: List<Regex> = aliases.map {
            Regex("(?<!$FA)بانک\\s*${Regex.escape(it)}(?!$FA)")
        }
        val bare: List<Regex> = aliases.map {
            Regex("(?<!$FA)${Regex.escape(it)}(?!$FA)")
        }
    }

    private val BANKS = listOf(
        BankDef("سپه", nk("سپه"), false, listOf("98300017"), listOf("sepah")),
        BankDef("ملت", nk("ملت"), false, listOf("98300098"), listOf("mellat")),
        BankDef("صادرات", nk("صادرات"), false, listOf("98300012"), listOf("saderat")),
        BankDef("ملی", nk("ملی ایران", "ملی"), true, listOf("98300011"), listOf("melli")),
        BankDef("پاسارگاد", nk("پاسارگاد"), false, listOf("100085"), listOf("pasargad")),
        BankDef("پارسیان", nk("پارسیان"), false, emptyList(), listOf("parsian")),
        BankDef("سامان", nk("سامان"), true, emptyList(), listOf("saman")),
        BankDef("تجارت", nk("تجارت"), false, listOf("98300020"), listOf("tejarat")),
        BankDef("کشاورزی", nk("کشاورزی"), false, listOf("98300016"), listOf("keshavarzi")),
        BankDef("رفاه", nk("رفاه"), true, listOf("98300013"), listOf("refah")),
        BankDef("اقتصاد نوین", nk("اقتصاد نوین"), false, emptyList(), listOf("enbank", "eghtesad")),
        BankDef("پست بانک", nk("پست بانک"), false, emptyList(), listOf("postbank")),
        BankDef("بلوبانک", nk("بلو بانک", "بلوبانک"), false, emptyList(), emptyList()),
        BankDef("مسکن", nk("مسکن"), true),
        BankDef("سینا", nk("سینا"), true),
        BankDef("شهر", nk("شهر"), true),
        BankDef("آینده", nk("آینده"), true),
        BankDef("دی", nk("دی"), true),
        BankDef("کارآفرین", nk("کارآفرین"), false),
        BankDef("قوامین", nk("قوامین"), false),
        BankDef("رسالت", nk("رسالت"), true),
        BankDef("مهر ایران", nk("قرض الحسنه مهر ایران", "مهر ایران"), false),
        BankDef("ایران زمین", nk("ایران زمین"), false),
        BankDef("گردشگری", nk("گردشگری"), false),
        BankDef("توسعه تعاون", nk("توسعه تعاون"), false),
        BankDef("توسعه صادرات", nk("توسعه صادرات"), false),
        BankDef("صنعت و معدن", nk("صنعت و معدن"), false),
        BankDef("سرمایه", nk("سرمایه"), true),
        BankDef("خاورمیانه", nk("خاورمیانه", "خاور میانه"), false),
        BankDef("ملل", nk("ملل"), true),
        BankDef("مهر اقتصاد", nk("مهر اقتصاد"), false),
        BankDef("انصار", nk("انصار"), true),
        BankDef("حکمت ایرانیان", nk("حکمت ایرانیان"), false),
        BankDef("کوثر", nk("کوثر"), true),
        BankDef("نور", nk("نور"), true),
        BankDef("آرین", nk("آرین"), true)
    )

    fun detectBank(sender: String, body: String): String {
        val text = normalize(body)

        for (b in BANKS) if (b.prefixed.any { it.containsMatchIn(text) }) return b.name

        val senderN = normalize(sender)
        val senderDigits = senderN.filter { it.isDigit() }
        for (b in BANKS) {
            if (b.senderCodes.any { senderDigits.contains(it) } ||
                b.senderLatin.any { senderN.contains(it) } ||
                b.bare.any { it.containsMatchIn(senderN) }
            ) return b.name
        }

        for (b in BANKS) if (!b.needsPrefix && b.bare.any { it.containsMatchIn(text) }) return b.name

        return UNKNOWN_BANK
    }

    // =====================================================
    // ۱۰. پارس کامل
    // =====================================================
    fun parse(body: String, sender: String = ""): ParsedSms? {
        if (body.isBlank()) return null
        if (!isBankSms(body, sender)) return null

        val text = normalize(body)
        val bank = detectBank(sender, body)
        val card = extractCard(text)
        val balance = extractBalance(text)
        val reference = extractReference(text)
        val dateTime = extractDateTime(text)

        var work = text
        balance?.let { work = blankRange(work, it.range) }
        work = blankAll(work, DATE_RE)
        work = blankAll(work, TIME_RE)
        work = blankAll(work, SHEBA_RE)
        work = blankAll(work, CARD_16)
        work = MASKED_TOKEN.replace(work) {
            if (maskCount(it.value) >= 2) " ".repeat(it.value.length) else it.value
        }
        work = blankAll(work, ID_LABELED)
        work = blankAll(work, LONG_DIGITS)

        val hit = findAmount(work)
        val failed = FAILED_RE.containsMatchIn(text)
        val reversal = REVERSAL_RE.containsMatchIn(text)

        var type: String? = null
        var typeStrong = false
        if (hit != null && !failed) {
            val (t, strong) = decideType(work, hit)
            type = t
            typeStrong = strong
        }
        if (reversal && type == TYPE_EXPENSE && hit?.sign == null) type = TYPE_INCOME

        var confidence = 0
        if (hit != null) confidence += 40
        if (type != null) confidence += if (typeStrong) 25 else 12
        if (card.last4 != null) confidence += 15
        if (balance != null) confidence += 10
        if (hit?.unit != null) confidence += 5
        if (bank != UNKNOWN_BANK) confidence += 5
        if (failed) confidence = minOf(confidence, 20)
        if (reversal) confidence = minOf(confidence, 60)
        confidence = confidence.coerceIn(0, 100)

        return ParsedSms(
            amount = hit?.let { toToman(it.rial, it.unit) },
            amountUnit = if (hit != null) UNIT_TOMAN else null,
            type = type,
            accountNumber = card.accountNumber,
            cardLast4 = card.last4,
            balance = balance?.toman,
            referenceCode = reference,
            bank = bank,
            raw = body,
            isFailed = failed,
            isReversal = reversal,
            jalaliDateTime = dateTime,
            confidence = confidence
        )
    }
}