package com.hkaboodvand.finacemobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.db.entity.Account
import com.hkaboodvand.finacemobile.data.db.entity.Transaction
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────
// رنگ‌ها
// ─────────────────────────────────────────
val RP_INFO_START = Color(0xFFA692DC)
val RP_INFO_END = Color(0xFF8B75C9)
val RP_SUCCESS_START = Color(0xFF7ECBA1)
val RP_SUCCESS_END = Color(0xFF5BB88A)
val RP_DANGER_START = Color(0xFFE8908A)
val RP_DANGER_END = Color(0xFFD97570)
val RP_PRIMARY_START = Color(0xFF7EA6E0)
val RP_PRIMARY_END = Color(0xFF5B8DD9)
val RP_WARNING_START = Color(0xFFE8B96A)
val RP_WARNING_END = Color(0xFFD4A356)
val RP_PINK_START = Color(0xFFE68FB8)
val RP_PINK_END = Color(0xFFD178A3)
val RP_BG = Color(0xFFF0F4F8)
val RP_TEXT = Color(0xFF1E293B)
val RP_TEXT_LIGHT = Color(0xFF64748B)

val CHART_COLORS = listOf(
    Color(0xFFE8908A),
    Color(0xFF7EA6E0),
    Color(0xFF7ECBA1),
    Color(0xFFE8B96A),
    Color(0xFFE68FB8),
    Color(0xFFA692DC),
    Color(0xFF7EC4D9),
    Color(0xFF8B8FE0),
    Color(0xFF95A3B3),
    Color(0xFFD4A356)
)

// ─────────────────────────────────────────
// توابع کمکی
// ─────────────────────────────────────────
fun formatMoneyRP(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

fun formatShortMoney(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(value.toLong())
}

// تبدیل تاریخ میلادی (yyyy-MM-dd) به شمسی
// ─────────────────────────────────────────
// تبدیل تاریخ میلادی به شمسی (بدون کتابخانه)
// ─────────────────────────────────────────
fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
    var jy = -1595 + (33 * (days / 12053))
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + (days / 31)
        jd = 1 + (days % 31)
    } else {
        jm = 7 + ((days - 186) / 30)
        jd = 1 + ((days - 186) % 30)
    }
    return Triple(jy, jm, jd)
}

fun toJalali(miladiDate: String): String {
    return try {
        val parts = miladiDate.take(10).split("-")
        if (parts.size != 3) return miladiDate
        val gy = parts[0].toInt()
        val gm = parts[1].toInt()
        val gd = parts[2].toInt()
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    } catch (e: Exception) {
        miladiDate
    }
}

// استخراج سال و ماه شمسی از یه تاریخ میلادی
fun getJalaliYearMonth(miladiDate: String): Pair<Int, Int> {
    return try {
        val parts = miladiDate.take(10).split("-")
        if (parts.size != 3) return Pair(0, 0)
        val (jy, jm, _) = gregorianToJalali(
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt()
        )
        Pair(jy, jm)
    } catch (e: Exception) {
        Pair(0, 0)
    }
}

fun getJalaliYear(miladiDate: String): Int {
    return getJalaliYearMonth(miladiDate).first
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    // داده‌های خام
    var allTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    // فیلترها
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedType by remember { mutableStateOf("هزینه") }
    var selectedRange by remember { mutableStateOf("ماه") }

    // برای بازه‌ی ماهانه
    var selectedYear by remember { mutableStateOf(0) }
    var selectedMonth by remember { mutableStateOf(0) }

    // منوها
    var showAccountMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }

    // بارگذاری داده‌ها
    LaunchedEffect(Unit) {
        repo.getAllTransactions().collect { list ->
            allTransactions = list
            if (list.isNotEmpty() && selectedYear == 0) {
                val latest = list.maxByOrNull { it.date }?.date ?: ""
                val (y, m) = getJalaliYearMonth(latest)
                selectedYear = y
                selectedMonth = m
            }
        }
    }
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list ->
            accounts = list
        }
    }

    // ─── فیلتر تراکنش‌ها ───
    val filteredTransactions = allTransactions.filter { tx ->
        val accountOk = selectedAccountId == null || tx.accountId == selectedAccountId
        val typeOk = tx.type == selectedType
        val rangeOk = when (selectedRange) {
            "ماه" -> {
                val (y, m) = getJalaliYearMonth(tx.date)
                y == selectedYear && m == selectedMonth
            }
            "سال" -> {
                val y = getJalaliYear(tx.date)
                y == selectedYear
            }
            else -> true
        }
        accountOk && typeOk && rangeOk
    }

    val totalFiltered = filteredTransactions.sumOf { it.amount }

    val byCategory = filteredTransactions
        .groupBy { it.category.ifEmpty { "سایر" } }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // ماه قبل
    val prevMonthTotal = if (selectedRange == "ماه") {
        val prevMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
        val prevYear = if (selectedMonth == 1) selectedYear - 1 else selectedYear

        allTransactions.filter { tx ->
            val accountOk = selectedAccountId == null || tx.accountId == selectedAccountId
            val typeOk = tx.type == selectedType
            val (y, m) = getJalaliYearMonth(tx.date)
            accountOk && typeOk && y == prevYear && m == prevMonth
        }.sumOf { it.amount }
    } else 0.0

    val totalIncome = filteredTransactions.filter { it.type == "درآمد" }.sumOf { it.amount }
    val totalExpense = filteredTransactions.filter { it.type == "هزینه" }.sumOf { it.amount }

    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📊 گزارش‌ها",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RP_INFO_START
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(RP_BG)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── بخش فیلترها ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔍", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فیلترها",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RP_TEXT
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportFilterButton(
                            icon = "💳",
                            text = if (selectedAccountId == null) "همه حساب‌ها"
                            else accounts.find { it.id == selectedAccountId }?.name ?: "؟",
                            modifier = Modifier.weight(1f),
                            onClick = { showAccountMenu = true }
                        )
                        ReportFilterButton(
                            icon = if (selectedType == "هزینه") "💸" else "💵",
                            text = selectedType,
                            modifier = Modifier.weight(1f),
                            onClick = { showTypeMenu = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RangeButton(
                            text = "ماه",
                            isSelected = selectedRange == "ماه",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedRange = "ماه" }
                        )
                        RangeButton(
                            text = "سال",
                            isSelected = selectedRange == "سال",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedRange = "سال" }
                        )
                        RangeButton(
                            text = "همه",
                            isSelected = selectedRange == "همه",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedRange = "همه" }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedRange != "همه") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportFilterButton(
                                icon = "📅",
                                text = selectedYear.toString(),
                                modifier = Modifier.weight(1f),
                                onClick = { showYearMenu = true }
                            )

                            if (selectedRange == "ماه") {
                                ReportFilterButton(
                                    icon = "📆",
                                    text = if (selectedMonth in 1..12)
                                        monthNames[selectedMonth - 1]
                                    else "انتخاب ماه",
                                    modifier = Modifier.weight(1f),
                                    onClick = { showMonthMenu = true }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── خلاصه ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = if (selectedType == "هزینه")
                                    listOf(RP_DANGER_START, RP_DANGER_END)
                                else
                                    listOf(RP_SUCCESS_START, RP_SUCCESS_END)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (selectedType == "هزینه") "💸" else "💵",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${selectedType} کل",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatMoneyRP(totalFiltered),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (selectedRange == "ماه" && prevMonthTotal > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val diff = totalFiltered - prevMonthTotal
                            val percent = (diff / prevMonthTotal) * 100
                            val arrow = if (diff > 0) "▲" else "▼"
                            val sign = if (diff > 0) "+" else ""

                            Text(
                                text = "$arrow نسبت به ماه قبل: $sign${String.format(Locale.US, "%.1f", percent)}%",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── نمودار دایره‌ای ───
            if (byCategory.isNotEmpty() && totalFiltered > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🥧", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$selectedType بر اساس دسته",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RP_TEXT
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                data = byCategory.map { it.second.toFloat() },
                                colors = CHART_COLORS.take(byCategory.size),
                                centerText = formatShortMoney(totalFiltered)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        byCategory.forEachIndexed { index, pair ->
                            val category = pair.first
                            val amount = pair.second
                            val percent = (amount / totalFiltered) * 100

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            CHART_COLORS[index % CHART_COLORS.size],
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = category,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RP_TEXT,
                                    modifier = Modifier.weight(1f)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", percent)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RP_TEXT
                                    )
                                    Text(
                                        text = formatShortMoney(amount),
                                        fontSize = 11.sp,
                                        color = RP_TEXT_LIGHT
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2EBF5))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ─── خلاصه درآمد و هزینه ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💰", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "خلاصه",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RP_TEXT
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SummaryRow("💵 درآمد", totalIncome, RP_SUCCESS_END)
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("💸 هزینه", totalExpense, RP_DANGER_END)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE2EBF5))
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("💰 مانده", totalIncome - totalExpense, RP_INFO_END)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── لیست تراکنش‌ها ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📋", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تراکنش‌ها (${filteredTransactions.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RP_TEXT
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredTransactions.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📋", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "تراکنشی در این بازه نیست",
                                fontSize = 14.sp,
                                color = RP_TEXT_LIGHT
                            )
                        }
                    } else {
                        filteredTransactions.take(20).forEach { tx ->
                            ReportTransactionRow(tx, accounts)
                            HorizontalDivider(
                                color = Color(0xFFE2EBF5),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ─── منوی حساب ───
    if (showAccountMenu) {
        AlertDialog(
            onDismissRequest = { showAccountMenu = false },
            title = { Text("💳 انتخاب حساب", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "🌐 همه حساب‌ها",
                        fontSize = 15.sp,
                        fontWeight = if (selectedAccountId == null) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedAccountId == null) RP_INFO_END else RP_TEXT,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAccountId = null
                                showAccountMenu = false
                            }
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Right
                    )
                    HorizontalDivider(color = Color(0xFFE2EBF5))

                    accounts.forEach { acc ->
                        Text(
                            text = "💳 ${acc.name} (${acc.bank})",
                            fontSize = 15.sp,
                            fontWeight = if (selectedAccountId == acc.id) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedAccountId == acc.id) RP_INFO_END else RP_TEXT,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccountId = acc.id
                                    showAccountMenu = false
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountMenu = false }) {
                    Text("بستن", color = RP_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }

    // ─── منوی نوع ───
    if (showTypeMenu) {
        AlertDialog(
            onDismissRequest = { showTypeMenu = false },
            title = { Text("🏷️ انتخاب نوع", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("هزینه", "درآمد").forEach { type ->
                        Text(
                            text = if (type == "هزینه") "💸 هزینه" else "💵 درآمد",
                            fontSize = 15.sp,
                            fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedType == type) RP_INFO_END else RP_TEXT,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedType = type
                                    showTypeMenu = false
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypeMenu = false }) {
                    Text("بستن", color = RP_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }

    // ─── منوی سال ───
    if (showYearMenu) {
        val years = (selectedYear - 5..selectedYear + 1).toList().reversed()
        AlertDialog(
            onDismissRequest = { showYearMenu = false },
            title = { Text("📅 انتخاب سال", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    years.forEach { year ->
                        Text(
                            text = year.toString(),
                            fontSize = 15.sp,
                            fontWeight = if (selectedYear == year) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedYear == year) RP_INFO_END else RP_TEXT,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedYear = year
                                    showYearMenu = false
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearMenu = false }) {
                    Text("بستن", color = RP_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }

    // ─── منوی ماه ───
    if (showMonthMenu) {
        AlertDialog(
            onDismissRequest = { showMonthMenu = false },
            title = { Text("📆 انتخاب ماه", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    monthNames.forEachIndexed { index, name ->
                        val monthNum = index + 1
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            fontWeight = if (selectedMonth == monthNum) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedMonth == monthNum) RP_INFO_END else RP_TEXT,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMonth = monthNum
                                    showMonthMenu = false
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthMenu = false }) {
                    Text("بستن", color = RP_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }
}

// ─────────────────────────────────────────
// دکمه فیلتر
// ─────────────────────────────────────────
@Composable
fun ReportFilterButton(
    icon: String,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RP_TEXT,
                modifier = Modifier.weight(1f)
            )
            Text(text = "▼", fontSize = 10.sp, color = RP_TEXT_LIGHT)
        }
    }
}

@Composable
fun RangeButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RP_INFO_START else Color(0xFFF0F4F8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else RP_TEXT
            )
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = RP_TEXT)
        Text(
            text = formatMoneyRP(value),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─────────────────────────────────────────
// نمودار دایره‌ای (Donut)
// ─────────────────────────────────────────
@Composable
fun DonutChart(
    data: List<Float>,
    colors: List<Color>,
    centerText: String
) {
    val total = data.sum()
    if (total <= 0) return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f * 0.85f
            val center = Offset(size.width / 2f, size.height / 2f)
            var startAngle = -90f

            data.forEachIndexed { index, value ->
                val sweep = (value / total) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweep
            }

            drawCircle(
                color = Color.White,
                radius = radius * 0.55f,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = centerText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RP_TEXT
            )
            Text(
                text = "تومان",
                fontSize = 10.sp,
                color = RP_TEXT_LIGHT
            )
        }
    }
}

// ─────────────────────────────────────────
// ردیف تراکنش
// ─────────────────────────────────────────
@Composable
fun ReportTransactionRow(
    tx: Transaction,
    accounts: List<Account>
) {
    val account = accounts.find { it.id == tx.accountId }
    val icon = when (tx.type) {
        "درآمد" -> "💵"
        "هزینه" -> "💸"
        "انتقال" -> "🔁"
        "پس‌انداز" -> "🏦"
        else -> "📝"
    }
    val color = when (tx.type) {
        "درآمد" -> RP_SUCCESS_END
        "هزینه" -> RP_DANGER_END
        "انتقال" -> RP_PRIMARY_END
        "پس‌انداز" -> RP_PINK_END
        else -> RP_TEXT_LIGHT
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.category.ifEmpty { tx.type },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RP_TEXT
            )
            Text(
                text = "${account?.name ?: "?"} • ${toJalali(tx.date)}",
                fontSize = 11.sp,
                color = RP_TEXT_LIGHT
            )
        }
        Text(
            text = "${if (tx.type == "هزینه") "-" else "+"}${formatShortMoney(tx.amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}