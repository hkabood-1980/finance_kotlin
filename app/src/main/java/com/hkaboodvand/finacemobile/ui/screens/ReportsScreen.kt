package com.hkaboodvand.finacemobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.db.entity.Account
import com.hkaboodvand.finacemobile.data.db.entity.Transaction
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

val RP_INFO_START = Color(0xFFA692DC)
val RP_INFO_END = Color(0xFF8B75C9)
val RP_SUCCESS_START = Color(0xFF7ECBA1)
val RP_SUCCESS_END = Color(0xFF5BB88A)
val RP_DANGER_START = Color(0xFFE8908A)
val RP_DANGER_END = Color(0xFFD97570)
val RP_PRIMARY_START = Color(0xFF7EA6E0)
val RP_PRIMARY_END = Color(0xFF5B8DD9)
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

val RP_EXPENSE_CATEGORIES = listOf(
    "غذا", "حمل و نقل", "لباس", "خانه", "درمان",
    "آموزش", "سرگرمی", "قبض و شارژ", "هدیه", "سفر", "سایر"
)

val RP_INCOME_CATEGORIES = listOf(
    "حقوق", "هدیه", "فروش", "سود بانکی", "اجاره", "فریلنس", "یارانه", "سایر"
)

fun formatMoneyRP(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

fun formatShortMoneyRP(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(value.toLong())
}

fun formatNumberWithCommaRP(input: String): String {
    if (input.isEmpty()) return ""
    val digitsOnly = input.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return ""
    return try {
        val number = digitsOnly.toLong()
        NumberFormat.getNumberInstance(Locale.US).format(number)
    } catch (e: Exception) {
        digitsOnly
    }
}

fun parseFormattedNumberRP(input: String): String {
    return input.filter { it.isDigit() }
}

fun gregorianToJalaliRP(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
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

fun toJalaliRP(miladiDate: String): String {
    return try {
        val parts = miladiDate.take(10).split("-")
        if (parts.size != 3) return miladiDate
        val (jy, jm, jd) = gregorianToJalaliRP(
            parts[0].toInt(), parts[1].toInt(), parts[2].toInt()
        )
        String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    } catch (e: Exception) {
        miladiDate
    }
}

fun getJalaliYearMonthRP(miladiDate: String): Pair<Int, Int> {
    return try {
        val parts = miladiDate.take(10).split("-")
        if (parts.size != 3) return Pair(0, 0)
        val (jy, jm, _) = gregorianToJalaliRP(
            parts[0].toInt(), parts[1].toInt(), parts[2].toInt()
        )
        Pair(jy, jm)
    } catch (e: Exception) {
        Pair(0, 0)
    }
}

fun getJalaliYearRP(miladiDate: String): Int {
    return getJalaliYearMonthRP(miladiDate).first
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    var allTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedType by remember { mutableStateOf("هزینه") } // "هزینه" / "درآمد" / "همه"
    var selectedRange by remember { mutableStateOf("ماه") }
    var selectedYear by remember { mutableStateOf(0) }
    var selectedMonth by remember { mutableStateOf(0) }

    var showAccountMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }

    var selectedTxForEdit by remember { mutableStateOf<Transaction?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<Transaction?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        repo.getAllTransactions().collect { list ->
            allTransactions = list
            if (list.isNotEmpty() && selectedYear == 0) {
                val latest = list.maxByOrNull { it.date }?.date ?: ""
                val (y, m) = getJalaliYearMonthRP(latest)
                selectedYear = y
                selectedMonth = m
            }
        }
    }
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list -> accounts = list }
    }

    // ─── فیلتر اصلی ───
    val filteredTransactions = allTransactions.filter { tx ->
        val accountOk = selectedAccountId == null || tx.accountId == selectedAccountId
        val typeOk = selectedType == "همه" || tx.type == selectedType
        val rangeOk = when (selectedRange) {
            "ماه" -> {
                val (y, m) = getJalaliYearMonthRP(tx.date)
                y == selectedYear && m == selectedMonth
            }
            "سال" -> getJalaliYearRP(tx.date) == selectedYear
            else -> true
        }
        accountOk && typeOk && rangeOk
    }

    // ─── برای نمودار میله‌ای (بدون فیلتر نوع و بدون موجودی اولیه) ───
    val barChartIncome = allTransactions.filter { tx ->
        val accountOk = selectedAccountId == null || tx.accountId == selectedAccountId
        val rangeOk = when (selectedRange) {
            "ماه" -> {
                val (y, m) = getJalaliYearMonthRP(tx.date)
                y == selectedYear && m == selectedMonth
            }
            "سال" -> getJalaliYearRP(tx.date) == selectedYear
            else -> true
        }
        accountOk && rangeOk && tx.type == "درآمد" && tx.category != "موجودی اولیه"
    }.sumOf { it.amount }

    val barChartExpense = allTransactions.filter { tx ->
        val accountOk = selectedAccountId == null || tx.accountId == selectedAccountId
        val rangeOk = when (selectedRange) {
            "ماه" -> {
                val (y, m) = getJalaliYearMonthRP(tx.date)
                y == selectedYear && m == selectedMonth
            }
            "سال" -> getJalaliYearRP(tx.date) == selectedYear
            else -> true
        }
        accountOk && rangeOk && tx.type == "هزینه"
    }.sumOf { it.amount }

    val totalFiltered = filteredTransactions.sumOf { it.amount }

    val byCategory = filteredTransactions
        .groupBy { it.category.ifEmpty { "سایر" } }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val prevMonthTotal = if (selectedRange == "ماه") {
        val prevMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
        val prevYear = if (selectedMonth == 1) selectedYear - 1 else selectedYear
        allTransactions.filter { tx ->
            val accountOk = selectedAccountId == null || tx.accountId == selectedAccountId
            val typeOk = selectedType == "همه" || tx.type == selectedType
            val (y, m) = getJalaliYearMonthRP(tx.date)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

            // ─── فیلترها ───
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
                        RPFilterButton(
                            icon = "💳",
                            text = if (selectedAccountId == null) "همه حساب‌ها"
                            else accounts.find { it.id == selectedAccountId }?.name ?: "؟",
                            modifier = Modifier.weight(1f),
                            onClick = { showAccountMenu = true }
                        )
                        RPFilterButton(
                            icon = when (selectedType) {
                                "هزینه" -> "💸"
                                "درآمد" -> "💵"
                                else -> "🌐"
                            },
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
                        RPRangeButton(
                            text = "ماه",
                            isSelected = selectedRange == "ماه",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedRange = "ماه" }
                        )
                        RPRangeButton(
                            text = "سال",
                            isSelected = selectedRange == "سال",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedRange = "سال" }
                        )
                        RPRangeButton(
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
                            RPFilterButton(
                                icon = "📅",
                                text = selectedYear.toString(),
                                modifier = Modifier.weight(1f),
                                onClick = { showYearMenu = true }
                            )
                            if (selectedRange == "ماه") {
                                RPFilterButton(
                                    icon = "📆",
                                    text = if (selectedMonth in 1..12)
                                        monthNames[selectedMonth - 1] else "انتخاب ماه",
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
                                colors = when (selectedType) {
                                    "هزینه" -> listOf(RP_DANGER_START, RP_DANGER_END)
                                    "درآمد" -> listOf(RP_SUCCESS_START, RP_SUCCESS_END)
                                    else -> listOf(RP_INFO_START, RP_INFO_END)
                                }
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (selectedType) {
                                    "هزینه" -> "💸"
                                    "درآمد" -> "💵"
                                    else -> "🌐"
                                },
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (selectedType == "همه") "کل تراکنش‌ها" else "${selectedType} کل",
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

            // ─── نمودار میله‌ای ───
            RPBarChart(
                totalIncome = barChartIncome,
                totalExpense = barChartExpense
            )

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
                                text = when (selectedType) {
                                    "همه" -> "همه تراکنش‌ها بر اساس دسته"
                                    else -> "$selectedType بر اساس دسته"
                                },
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
                            RPDonutChart(
                                data = byCategory.map { it.second.toFloat() },
                                colors = CHART_COLORS.take(byCategory.size),
                                centerText = formatShortMoneyRP(totalFiltered)
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
                                        text = formatShortMoneyRP(amount),
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

            // ─── خلاصه درآمد/هزینه ───
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
                    RPSummaryRow("💵 درآمد", totalIncome, RP_SUCCESS_END)
                    Spacer(modifier = Modifier.height(8.dp))
                    RPSummaryRow("💸 هزینه", totalExpense, RP_DANGER_END)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE2EBF5))
                    Spacer(modifier = Modifier.height(8.dp))
                    RPSummaryRow("💰 مانده", totalIncome - totalExpense, RP_INFO_END)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── تراکنش‌ها ───
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

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 برای ویرایش، روی هر تراکنش بزن",
                        fontSize = 11.sp,
                        color = RP_TEXT_LIGHT,
                        textAlign = TextAlign.Right
                    )

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
                        filteredTransactions.take(50).forEach { tx ->
                            RPTransactionRow(
                                tx = tx,
                                accounts = accounts,
                                onClick = {
                                    selectedTxForEdit = tx
                                    showEditDialog = true
                                }
                            )
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

    // ─── منوی نوع (با گزینه همه) ───
    if (showTypeMenu) {
        AlertDialog(
            onDismissRequest = { showTypeMenu = false },
            title = { Text("🏷️ انتخاب نوع", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("همه", "هزینه", "درآمد").forEach { type ->
                        Text(
                            text = when (type) {
                                "همه" -> "🌐 همه"
                                "هزینه" -> "💸 هزینه"
                                else -> "💵 درآمد"
                            },
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

    // ─── دیالوگ ویرایش ───
    if (showEditDialog && selectedTxForEdit != null) {
        RPEditTransactionDialog(
            tx = selectedTxForEdit!!,
            accounts = accounts,
            onDismiss = {
                showEditDialog = false
                selectedTxForEdit = null
            },
            onSave = { newType, newCategory, newAmount, newAccountId ->
                scope.launch {
                    try {
                        val result = repo.updateTransactionFields(
                            transactionId = selectedTxForEdit!!.id,
                            newType = newType,
                            newCategory = newCategory,
                            newAmount = newAmount,
                            newAccountId = newAccountId
                        )
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("✅ تراکنش ویرایش شد")
                        } else {
                            snackbarHostState.showSnackbar(
                                "❌ ${result.exceptionOrNull()?.message}"
                            )
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("❌ ${e.message}")
                    }
                    showEditDialog = false
                    selectedTxForEdit = null
                }
            },
            onDelete = {
                txToDelete = selectedTxForEdit
                showEditDialog = false
                selectedTxForEdit = null
                showDeleteDialog = true
            }
        )
    }

    // ─── دیالوگ تأیید حذف ───
    if (showDeleteDialog && txToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Text(text = "🗑️", fontSize = 40.sp) },
            title = {
                Text(
                    "حذف تراکنش",
                    fontWeight = FontWeight.Bold,
                    color = RP_DANGER_END
                )
            },
            text = {
                Column {
                    Text(
                        text = "آیا مطمئن هستید که می‌خواهید این تراکنش را حذف کنید؟",
                        color = RP_TEXT,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💰 ${formatMoneyRP(txToDelete!!.amount)}\n🏷️ ${txToDelete!!.category}",
                        fontSize = 13.sp,
                        color = RP_TEXT_LIGHT,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ موجودی کارت هم برگردونده می‌شه",
                        fontSize = 12.sp,
                        color = RP_DANGER_END,
                        textAlign = TextAlign.Right
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = repo.deleteTransactionById(txToDelete!!.id)
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("✅ تراکنش حذف شد")
                                } else {
                                    snackbarHostState.showSnackbar(
                                        "❌ ${result.exceptionOrNull()?.message}"
                                    )
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ ${e.message}")
                            }
                            showDeleteDialog = false
                            txToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RP_DANGER_END)
                ) {
                    Text("🗑️ حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = RP_TEXT_LIGHT)
                ) {
                    Text("❌ انصراف")
                }
            },
            containerColor = Color.White
        )
    }
}

// ─── نمودار میله‌ای ───
@Composable
fun RPBarChart(
    totalIncome: Double,
    totalExpense: Double
) {
    val maxValue = maxOf(totalIncome, totalExpense, 1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📊", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "نمودار درآمد و هزینه",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RP_TEXT
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                RPBarColumn(
                    label = "درآمد",
                    value = totalIncome,
                    maxValue = maxValue,
                    color = RP_SUCCESS_END,
                    icon = "💵"
                )

                RPBarColumn(
                    label = "هزینه",
                    value = totalExpense,
                    maxValue = maxValue,
                    color = RP_DANGER_END,
                    icon = "💸"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE2EBF5))
            Spacer(modifier = Modifier.height(12.dp))

            val balance = totalIncome - totalExpense
            val balanceColor = if (balance >= 0) RP_SUCCESS_END else RP_DANGER_END
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💰 مانده",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RP_TEXT
                )
                Text(
                    text = formatMoneyRP(balance),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
fun RPBarColumn(
    label: String,
    value: Double,
    maxValue: Double,
    color: Color,
    icon: String
) {
    val fraction = (value / maxValue).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 800),
        label = "bar"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = if (value >= 1000000) {
                String.format(Locale.US, "%.1fM", value / 1000000)
            } else if (value >= 1000) {
                String.format(Locale.US, "%.0fK", value / 1000)
            } else {
                value.toLong().toString()
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(180.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(60.dp)
                    .background(Color(0xFFF0F4F8), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight(animatedFraction)
                    .width(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.9f), color)
                        ),
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RP_TEXT
            )
        }
    }
}

// ─── دیالوگ ویرایش تراکنش ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RPEditTransactionDialog(
    tx: Transaction,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (type: String, category: String, amount: Double, accountId: Long) -> Unit,
    onDelete: () -> Unit
) {
    var editType by remember { mutableStateOf(tx.type) }
    var editCategory by remember { mutableStateOf(tx.category) }
    var editAmount by remember {
        mutableStateOf(formatNumberWithCommaRP(tx.amount.toLong().toString()))
    }
    var selectedAccount by remember {
        mutableStateOf(accounts.find { it.id == tx.accountId } ?: accounts.firstOrNull())
    }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "✏️ ویرایش تراکنش",
                fontWeight = FontWeight.Bold,
                color = RP_TEXT
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = "تاریخ: ${toJalaliRP(tx.date)}",
                    fontSize = 11.sp,
                    color = RP_TEXT_LIGHT,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    "نوع:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RP_TEXT,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RPTypeSelector(
                        title = "💸 هزینه",
                        isSelected = editType == "هزینه",
                        startColor = RP_DANGER_START,
                        endColor = RP_DANGER_END,
                        modifier = Modifier.weight(1f),
                        onClick = { editType = "هزینه" }
                    )
                    RPTypeSelector(
                        title = "💵 درآمد",
                        isSelected = editType == "درآمد",
                        startColor = RP_SUCCESS_START,
                        endColor = RP_SUCCESS_END,
                        modifier = Modifier.weight(1f),
                        onClick = { editType = "درآمد" }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "دسته:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RP_TEXT,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryMenu = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = editCategory.ifEmpty { "انتخاب دسته" },
                            fontSize = 14.sp,
                            color = RP_TEXT,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        Text(text = "▼", fontSize = 10.sp, color = RP_TEXT_LIGHT)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "مبلغ (تومان):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RP_TEXT,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        editAmount = if (digitsOnly.isEmpty()) ""
                        else formatNumberWithCommaRP(digitsOnly)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RP_INFO_START,
                        unfocusedBorderColor = Color(0xFFD0D7DE),
                        focusedTextColor = RP_TEXT,
                        unfocusedTextColor = RP_TEXT,
                        cursorColor = RP_INFO_START
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "کارت:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RP_TEXT,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAccountMenu = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedAccount?.let { "💳 ${it.name} (${it.bank})" }
                                ?: "انتخاب کارت",
                            fontSize = 13.sp,
                            color = RP_TEXT,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        Text(text = "▼", fontSize = 10.sp, color = RP_TEXT_LIGHT)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = parseFormattedNumberRP(editAmount).toDoubleOrNull() ?: 0.0
                    val account = selectedAccount ?: return@TextButton
                    if (amount <= 0) return@TextButton
                    onSave(
                        editType,
                        editCategory.ifEmpty { "سایر" },
                        amount,
                        account.id
                    )
                },
                colors = ButtonDefaults.textButtonColors(contentColor = RP_SUCCESS_END)
            ) {
                Text("💾 ذخیره", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = RP_DANGER_END)
                ) {
                    Text("🗑️")
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = RP_TEXT_LIGHT)
                ) {
                    Text("❌")
                }
            }
        },
        containerColor = Color.White
    )

    if (showCategoryMenu) {
        RPCategoryPickerDialog(
            currentCategories = if (editType == "هزینه") RP_EXPENSE_CATEGORIES
            else RP_INCOME_CATEGORIES,
            currentCategory = editCategory,
            onSelect = { cat ->
                editCategory = cat
                showCategoryMenu = false
            },
            onDismiss = { showCategoryMenu = false }
        )
    }

    if (showAccountMenu) {
        AlertDialog(
            onDismissRequest = { showAccountMenu = false },
            title = { Text("💳 انتخاب کارت", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    accounts.forEach { acc ->
                        Text(
                            text = "💳 ${acc.name} (${acc.bank})\n💰 ${formatMoneyRP(acc.balance)}",
                            fontSize = 14.sp,
                            color = if (selectedAccount?.id == acc.id) RP_INFO_END else RP_TEXT,
                            fontWeight = if (selectedAccount?.id == acc.id) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccount = acc
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RPCategoryPickerDialog(
    currentCategories: List<String>,
    currentCategory: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customCategory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🏷️ انتخاب دسته", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = "✍️ نوشتن دسته دلخواه:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RP_TEXT,
                    modifier = Modifier.padding(bottom = 6.dp),
                    textAlign = TextAlign.Right
                )

                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            "مثلاً: تعمیرات، ورزش، ...",
                            color = RP_TEXT_LIGHT.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RP_INFO_START,
                        unfocusedBorderColor = Color(0xFFD0D7DE),
                        focusedTextColor = RP_TEXT,
                        unfocusedTextColor = RP_TEXT,
                        cursorColor = RP_INFO_START
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        if (customCategory.isNotBlank()) {
                            onSelect(customCategory.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = RP_SUCCESS_END
                    )
                ) {
                    Text("✅ تأیید دسته دلخواه", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFE2EBF5))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "یا از لیست انتخاب کن:",
                    fontSize = 12.sp,
                    color = RP_TEXT_LIGHT,
                    modifier = Modifier.padding(bottom = 4.dp),
                    textAlign = TextAlign.Right
                )

                currentCategories.forEach { cat ->
                    Text(
                        text = "• $cat",
                        fontSize = 15.sp,
                        color = if (currentCategory == cat) RP_INFO_END else RP_TEXT,
                        fontWeight = if (currentCategory == cat) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(cat) }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Right
                    )
                    HorizontalDivider(color = Color(0xFFE2EBF5))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = RP_TEXT_LIGHT)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun RPFilterButton(
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
fun RPRangeButton(
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
fun RPTypeSelector(
    title: String,
    isSelected: Boolean,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) startColor else Color(0xFFF0F4F8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else RP_TEXT
            )
        }
    }
}

@Composable
fun RPSummaryRow(
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

@Composable
fun RPDonutChart(
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
                    topLeft = Offset(center.x - radius, center.y - radius),
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RP_TEXT
            )
            Text(text = "تومان", fontSize = 10.sp, color = RP_TEXT_LIGHT)
        }
    }
}

@Composable
fun RPTransactionRow(
    tx: Transaction,
    accounts: List<Account>,
    onClick: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
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
                text = "${account?.name ?: "?"} • ${toJalaliRP(tx.date)}",
                fontSize = 11.sp,
                color = RP_TEXT_LIGHT
            )
        }
        Text(
            text = "${if (tx.type == "هزینه") "-" else "+"}${formatShortMoneyRP(tx.amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "✏️", fontSize = 14.sp, color = RP_TEXT_LIGHT)
    }
}