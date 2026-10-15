package com.hkaboodvand.finacemobile.ui.screens

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────
// رنگ‌ها
// ─────────────────────────────────────────
val BK_INDIGO_START = Color(0xFF8B8FE0)
val BK_INDIGO_END = Color(0xFF6E72CC)
val BK_SUCCESS_START = Color(0xFF7ECBA1)
val BK_SUCCESS_END = Color(0xFF5BB88A)
val BK_WARNING_START = Color(0xFFE8B96A)
val BK_WARNING_END = Color(0xFFD4A356)
val BK_DANGER_START = Color(0xFFE8908A)
val BK_DANGER_END = Color(0xFFD97570)
val BK_CYAN_START = Color(0xFF7EC4D9)
val BK_CYAN_END = Color(0xFF5BAEC9)
val BK_BG = Color(0xFFF0F4F8)
val BK_TEXT = Color(0xFF1E293B)
val BK_TEXT_LIGHT = Color(0xFF64748B)

// تبدیل تاریخ میلادی به شمسی (بدون کتابخانه)
fun gregorianToJalaliBK(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
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

fun toJalaliBK(miladiDate: String): String {
    return try {
        val parts = miladiDate.take(10).split("-")
        if (parts.size != 3) return miladiDate
        val (jy, jm, jd) = gregorianToJalaliBK(
            parts[0].toInt(), parts[1].toInt(), parts[2].toInt()
        )
        String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    } catch (e: Exception) {
        miladiDate
    }
}

fun formatMoneyBK(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    // آمار
    var accountsCount by remember { mutableStateOf(0) }
    var transactionsCount by remember { mutableStateOf(0) }
    var totalBalance by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher برای بازیابی
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonText = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    } ?: return@launch

                    val result = restoreFromJson(jsonText, repo)
                    if (result.isSuccess) {
                        snackbarHostState.showSnackbar("✅ بازیابی انجام شد")
                    } else {
                        snackbarHostState.showSnackbar(
                            "❌ ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("❌ ${e.message}")
                }
            }
        }
    }

    // بارگذاری آمار
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list ->
            accountsCount = list.size
            totalBalance = list.sumOf { it.balance }
        }
    }
    LaunchedEffect(Unit) {
        repo.getAllTransactions().collect { list ->
            transactionsCount = list.size
            isLoading = false
        }
    }

    // ─── توابع ───
    fun createJsonBackup(accounts: List<Account>, transactions: List<Transaction>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))

        // Accounts
        val accountsArr = JSONArray()
        accounts.forEach { acc ->
            accountsArr.put(JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("bank", acc.bank)
                put("cardNumber", acc.cardNumber)
                put("balance", acc.balance)
                put("type", acc.type)
            })
        }
        root.put("accounts", accountsArr)

        // Transactions
        val txArr = JSONArray()
        transactions.forEach { tx ->
            txArr.put(JSONObject().apply {
                put("id", tx.id)
                put("accountId", tx.accountId)
                put("date", tx.date)
                put("type", tx.type)
                put("category", tx.category)
                put("amount", tx.amount)
                put("place", tx.place)
                put("description", tx.description)
                put("balanceAfter", tx.balanceAfter)
            })
        }
        root.put("transactions", txArr)

        return root.toString(2)
    }

    fun createCsvContent(
        accounts: List<Account>,
        transactions: List<Transaction>
    ): String {
        val sb = StringBuilder()

        // BOM برای Excel فارسی
        sb.append("\uFEFF")

        // Header
        sb.append("ردیف,تاریخ,تاریخ میلادی,نوع,دسته,مبلغ,کارت,بانک,محل,توضیحات\n")

        transactions.sortedByDescending { it.date }.forEachIndexed { index, tx ->
            val account = accounts.find { it.id == tx.accountId }
            sb.append("${index + 1},")
            sb.append("${toJalaliBK(tx.date)},")
            sb.append("${tx.date.take(10)},")
            sb.append("${tx.type},")
            sb.append("${tx.category.ifEmpty { "—" }},")
            sb.append("${tx.amount.toLong()},")
            sb.append("${account?.name ?: "?"},")
            sb.append("${account?.bank ?: "?"},")
            sb.append("${tx.place.ifEmpty { "—" }},")
            sb.append("\"${tx.description.ifEmpty { "—" }.replace("\"", "\"\"")}\"\n")
        }

        return sb.toString()
    }

    suspend fun saveToDownloads(
        filename: String,
        mimeType: String,
        content: String
    ): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext null

                resolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                    }
                }

                uri
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "💾 پشتیبان‌گیری",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                    containerColor = BK_INDIGO_START
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BK_BG)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── آمار ───
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
                                colors = listOf(BK_INDIGO_START, BK_INDIGO_END)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📊", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "آمار پروژه",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        StatRowBK("💳 تعداد کارت‌ها", "$accountsCount کارت")
                        Spacer(modifier = Modifier.height(8.dp))
                        StatRowBK("📋 تعداد تراکنش‌ها", "$transactionsCount تراکنش")
                        Spacer(modifier = Modifier.height(8.dp))
                        StatRowBK("💰 موجودی کل", formatMoneyBK(totalBalance))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── دکمه‌های اکشن ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📤", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "پشتیبان‌گیری",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BK_TEXT
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "فایل‌ها در پوشه‌ی Downloads ذخیره می‌شن.",
                        fontSize = 12.sp,
                        color = BK_TEXT_LIGHT,
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─── JSON ───
                    ActionButtonBK(
                        icon = "📥",
                        title = "پشتیبان JSON",
                        subtitle = "برای بازیابی بعدی",
                        startColor = BK_SUCCESS_START,
                        endColor = BK_SUCCESS_END,
                        onClick = {
                            scope.launch {
                                try {
                                    val accounts = repo.getAllAccountsOnce()
                                    val transactions = repo.getAllTransactionsOnce()

                                    if (accounts.isEmpty() && transactions.isEmpty()) {
                                        snackbarHostState.showSnackbar("⚠️ داده‌ای برای پشتیبان نیست")
                                        return@launch
                                    }

                                    val json = createJsonBackup(accounts, transactions)
                                    val timestamp = SimpleDateFormat(
                                        "yyyy-MM-dd_HH-mm-ss",
                                        Locale.US
                                    ).format(Date())
                                    val filename = "finance_backup_$timestamp.json"

                                    val uri = saveToDownloads(
                                        filename,
                                        "application/json",
                                        json
                                    )

                                    if (uri != null) {
                                        snackbarHostState.showSnackbar(
                                            "✅ پشتیبان ذخیره شد:\n$filename"
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar("❌ خطا در ذخیره")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("❌ ${e.message}")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── CSV ───
                    ActionButtonBK(
                        icon = "📊",
                        title = "خروجی Excel (CSV)",
                        subtitle = "برای تحلیل در Excel",
                        startColor = BK_CYAN_START,
                        endColor = BK_CYAN_END,
                        onClick = {
                            scope.launch {
                                try {
                                    val accounts = repo.getAllAccountsOnce()
                                    val transactions = repo.getAllTransactionsOnce()

                                    if (transactions.isEmpty()) {
                                        snackbarHostState.showSnackbar("⚠️ تراکنشی برای خروجی نیست")
                                        return@launch
                                    }

                                    val csv = createCsvContent(accounts, transactions)
                                    val timestamp = SimpleDateFormat(
                                        "yyyy-MM-dd_HH-mm-ss",
                                        Locale.US
                                    ).format(Date())
                                    val filename = "finance_transactions_$timestamp.csv"

                                    val uri = saveToDownloads(
                                        filename,
                                        "text/csv",
                                        csv
                                    )

                                    if (uri != null) {
                                        snackbarHostState.showSnackbar(
                                            "✅ فایل Excel ذخیره شد:\n$filename"
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar("❌ خطا در ذخیره")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("❌ ${e.message}")
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── بازیابی ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔄", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "بازیابی",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BK_TEXT
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "از فایل JSON پشتیبان بازیابی کن. توجه: داده‌های فعلی پاک می‌شن.",
                        fontSize = 12.sp,
                        color = BK_DANGER_END,
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ActionButtonBK(
                        icon = "📂",
                        title = "انتخاب فایل پشتیبان",
                        subtitle = "از Downloads انتخاب کن",
                        startColor = BK_WARNING_START,
                        endColor = BK_WARNING_END,
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "text/*"))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────
// ردیف آمار
// ─────────────────────────────────────────
@Composable
fun StatRowBK(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ─────────────────────────────────────────
// دکمه اکشن
// ─────────────────────────────────────────
@Composable
fun ActionButtonBK(
    icon: String,
    title: String,
    subtitle: String,
    startColor: Color,
    endColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 5.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(colors = listOf(startColor, endColor))
                )
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 26.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Right
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// بازیابی از JSON
// ─────────────────────────────────────────
suspend fun restoreFromJson(
    jsonText: String,
    repo: FinanceRepository
): Result<Unit> {
    return try {
        val root = JSONObject(jsonText)

        // Accounts
        val accountsArr = root.optJSONArray("accounts") ?: JSONArray()
        val accounts = mutableListOf<Account>()
        for (i in 0 until accountsArr.length()) {
            val obj = accountsArr.getJSONObject(i)
            accounts.add(
                Account(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name", ""),
                    bank = obj.optString("bank", ""),
                    cardNumber = obj.optString("cardNumber", ""),
                    balance = obj.optDouble("balance", 0.0),
                    type = obj.optString("type", "")
                )
            )
        }

        // Transactions
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        val transactions = mutableListOf<Transaction>()
        for (i in 0 until txArr.length()) {
            val obj = txArr.getJSONObject(i)
            transactions.add(
                Transaction(
                    id = obj.optLong("id", 0),
                    accountId = obj.optLong("accountId", 0),
                    date = obj.optString("date", ""),
                    type = obj.optString("type", ""),
                    category = obj.optString("category", ""),
                    amount = obj.optDouble("amount", 0.0),
                    place = obj.optString("place", ""),
                    description = obj.optString("description", ""),
                    balanceAfter = obj.optDouble("balanceAfter", 0.0)
                )
            )
        }

        repo.restoreBackup(accounts, transactions)
    } catch (e: Exception) {
        Result.failure(e)
    }
}