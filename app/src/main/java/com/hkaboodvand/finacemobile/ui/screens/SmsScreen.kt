package com.hkaboodvand.finacemobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
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
import com.hkaboodvand.finacemobile.data.db.entity.SmsQueue
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import com.hkaboodvand.finacemobile.data.sms.SmsParser
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────
// رنگ‌ها
// ─────────────────────────────────────────
val SMS_WARNING_START = Color(0xFFE8B96A)
val SMS_WARNING_END = Color(0xFFD4A356)
val SMS_SUCCESS_START = Color(0xFF7ECBA1)
val SMS_SUCCESS_END = Color(0xFF5BB88A)
val SMS_DANGER_START = Color(0xFFE8908A)
val SMS_DANGER_END = Color(0xFFD97570)
val SMS_PRIMARY_START = Color(0xFF7EA6E0)
val SMS_PRIMARY_END = Color(0xFF5B8DD9)
val SMS_CYAN_START = Color(0xFF7EC4D9)
val SMS_CYAN_END = Color(0xFF5BAEC9)
val SMS_INFO_START = Color(0xFFA692DC)
val SMS_INFO_END = Color(0xFF8B75C9)
val SMS_BG = Color(0xFFF0F4F8)
val SMS_TEXT = Color(0xFF1E293B)
val SMS_TEXT_LIGHT = Color(0xFF64748B)

// ─────────────────────────────────────────
// دسته‌های پیشنهادی
// ─────────────────────────────────────────
val SMS_EXPENSE_CATEGORIES = listOf(
    "غذا", "حمل و نقل", "لباس", "خانه", "درمان",
    "آموزش", "سرگرمی", "قبض و شارژ", "هدیه", "سفر", "سایر"
)

val SMS_INCOME_CATEGORIES = listOf(
    "حقوق", "هدیه", "فروش", "سود بانکی", "اجاره", "فریلنس", "یارانه", "سایر"
)

// ─────────────────────────────────────────
// توابع کمکی
// ─────────────────────────────────────────
fun formatMoneySMS(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

fun formatNumberWithCommaSMS(input: String): String {
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

fun parseFormattedNumberSMS(input: String): String {
    return input.filter { it.isDigit() }
}

fun gregorianToJalaliSMS(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
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

fun toJalaliSMS(miladiDate: String): String {
    return try {
        val parts = miladiDate.take(10).split("-")
        if (parts.size != 3) return miladiDate
        val (jy, jm, jd) = gregorianToJalaliSMS(
            parts[0].toInt(), parts[1].toInt(), parts[2].toInt()
        )
        String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    } catch (e: Exception) {
        miladiDate
    }
}

// ─────────────────────────────────────────
// صفحه اصلی
// ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    var pendingSms by remember { mutableStateOf<List<SmsQueue>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    var showNewSmsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedSmsForEdit by remember { mutableStateOf<SmsQueue?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var smsToDelete by remember { mutableStateOf<SmsQueue?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        repo.getPendingSms().collect { list -> pendingSms = list }
    }
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list -> accounts = list }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📩 پیامک‌های بانکی",
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
                    containerColor = SMS_CYAN_START
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SMS_BG)
        ) {
            // ─── دکمه پیامک جدید ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SMS_CYAN_START, SMS_CYAN_END)
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNewSmsDialog = true }
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "➕", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "پیامک جدید (کپی-پیست)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "اگه پیامکی از بانک اومده و برنامه نشناخته ، متن پیامک رو Paste کن، برنامه تحلیل می‌کنه ",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }

            // ─── لیست پیامک‌ها ───
            if (pendingSms.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "📭", fontSize = 72.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "هیچ پیامکی در انتظار تأیید نیست",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SMS_TEXT
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "برای ثبت پیامک جدید، روی دکمه بالا بزن",
                        fontSize = 13.sp,
                        color = SMS_TEXT_LIGHT,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingSms) { sms ->
                        SmsCard(
                            sms = sms,
                            onApprove = {
                                if (accounts.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("⚠️ ابتدا یک حساب ثبت کنید")
                                    }
                                } else {
                                    selectedSmsForEdit = sms
                                    showEditDialog = true
                                }
                            },
                            onEdit = {
                                selectedSmsForEdit = sms
                                showEditDialog = true
                            },
                            onDelete = {
                                smsToDelete = sms
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ─── دیالوگ پیامک جدید ───
    if (showNewSmsDialog) {
        NewSmsDialog(
            accounts = accounts,
            db = db,
            onDismiss = { showNewSmsDialog = false },
            onSaved = {
                scope.launch {
                    snackbarHostState.showSnackbar("✅ پیامک ذخیره شد")
                }
                showNewSmsDialog = false
            }
        )
    }

    // ─── دیالوگ ویرایش + تأیید ───
    if (showEditDialog && selectedSmsForEdit != null) {
        SmsEditDialog(
            sms = selectedSmsForEdit!!,
            accounts = accounts,
            onDismiss = {
                showEditDialog = false
                selectedSmsForEdit = null
            },
            onSaveAndApprove = { accountId, category, amount, type ->
                scope.launch {
                    try {
                        val result = repo.approveSmsEdited(
                            smsId = selectedSmsForEdit!!.id,
                            accountId = accountId,
                            category = category,
                            amount = amount,
                            type = type
                        )
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("✅ پیامک ثبت شد")

                            val bankName = SmsParser.detectBank(
                                selectedSmsForEdit!!.sender,
                                selectedSmsForEdit!!.body
                            )
                            val parsed = SmsParser.parse(
                                selectedSmsForEdit!!.body,
                                selectedSmsForEdit!!.sender
                            )
                            repo.learnSmsMapping(
                                sender = selectedSmsForEdit!!.sender,
                                cardLast4 = parsed?.cardLast4 ?: "",
                                accountId = accountId,
                                bankName = bankName,
                                accountNumber = parsed?.accountNumber ?: ""
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                "❌ ${result.exceptionOrNull()?.message}"
                            )
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("❌ ${e.message}")
                    }
                    showEditDialog = false
                    selectedSmsForEdit = null
                }
            },
            onSaveOnly = { accountId, category, amount, type ->
                scope.launch {
                    try {
                        repo.updateSmsFull(
                            smsId = selectedSmsForEdit!!.id,
                            newAmount = amount,
                            newType = type,
                            newCategory = category,
                            newAccountId = accountId
                        )
                        snackbarHostState.showSnackbar("✅ پیامک ذخیره شد (تأیید نشد)")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("❌ ${e.message}")
                    }
                    showEditDialog = false
                    selectedSmsForEdit = null
                }
            }
        )
    }

    // ─── دیالوگ تأیید حذف ───
    if (showDeleteDialog && smsToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Text(text = "🗑️", fontSize = 40.sp) },
            title = {
                Text("حذف پیامک", fontWeight = FontWeight.Bold, color = SMS_DANGER_END)
            },
            text = {
                Text(
                    "آیا مطمئن هستید که می‌خواهید این پیامک را حذف کنید؟",
                    color = SMS_TEXT,
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                repo.deleteSms(smsToDelete!!.id)
                                snackbarHostState.showSnackbar("✅ پیامک حذف شد")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ ${e.message}")
                            }
                            showDeleteDialog = false
                            smsToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SMS_DANGER_END)
                ) {
                    Text("🗑️ حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = SMS_TEXT_LIGHT)
                ) {
                    Text("❌ انصراف")
                }
            },
            containerColor = Color.White
        )
    }
}

// ─────────────────────────────────────────
// کارت پیامک
// ─────────────────────────────────────────
@Composable
fun SmsCard(
    sms: SmsQueue,
    onApprove: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🏦", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sms.sender,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SMS_TEXT
                    )
                    Text(
                        text = toJalaliSMS(sms.receivedAt),
                        fontSize = 11.sp,
                        color = SMS_TEXT_LIGHT
                    )
                }
                if (sms.parsedType != null) {
                    val (bg, icon) = if (sms.parsedType == "هزینه") {
                        SMS_DANGER_START to "💸"
                    } else {
                        SMS_SUCCESS_START to "💵"
                    }
                    Box(
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$icon ${sms.parsedType}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
            ) {
                Text(
                    text = sms.body,
                    fontSize = 12.sp,
                    color = SMS_TEXT,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Right
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sms.parsedAmount != null) {
                    Text(
                        text = "💰 ${formatMoneySMS(sms.parsedAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SMS_INFO_END
                    )
                } else {
                    Text(
                        text = "⚠️ مبلغ نامشخص",
                        fontSize = 12.sp,
                        color = SMS_DANGER_END
                    )
                }

                if (!sms.parsedCard.isNullOrEmpty()) {
                    Text(
                        text = "🔢 **** ${sms.parsedCard}",
                        fontSize = 12.sp,
                        color = SMS_TEXT_LIGHT
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmsActionButton(
                    icon = "✏️",
                    title = "ویرایش",
                    startColor = SMS_WARNING_START,
                    endColor = SMS_WARNING_END,
                    modifier = Modifier.weight(1f),
                    enabled = true,
                    onClick = onEdit
                )
                SmsActionButton(
                    icon = "✅",
                    title = "تأیید",
                    startColor = SMS_SUCCESS_START,
                    endColor = SMS_SUCCESS_END,
                    modifier = Modifier.weight(1f),
                    enabled = sms.parsedAmount != null && sms.parsedType != null,
                    onClick = onApprove
                )
                SmsActionButton(
                    icon = "🗑️",
                    title = "حذف",
                    startColor = SMS_DANGER_START,
                    endColor = SMS_DANGER_END,
                    modifier = Modifier.weight(1f),
                    enabled = true,
                    onClick = onDelete
                )
            }
        }
    }
}

// ─────────────────────────────────────────
// دیالوگ پیامک جدید (کپی-پیست)
// ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSmsDialog(
    accounts: List<Account>,
    db: AppDatabase,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var smsText by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<SmsParser.ParsedSms?>(null) }
    var detectedAccount by remember { mutableStateOf<Account?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "➕ پیامک جدید",
                fontWeight = FontWeight.Bold,
                color = SMS_TEXT
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = "متن پیامک را اینجا Paste کن:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SMS_TEXT,
                    modifier = Modifier.padding(bottom = 6.dp),
                    textAlign = TextAlign.Right
                )

                OutlinedTextField(
                    value = smsText,
                    onValueChange = {
                        smsText = it
                        parsedResult = null
                        detectedAccount = null
                        errorMsg = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = {
                        Text(
                            "متن پیامک بانکی...",
                            color = SMS_TEXT_LIGHT.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SMS_CYAN_START,
                        unfocusedBorderColor = Color(0xFFD0D7DE),
                        focusedTextColor = SMS_TEXT,
                        unfocusedTextColor = SMS_TEXT,
                        cursorColor = SMS_CYAN_START
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        if (smsText.isBlank()) {
                            errorMsg = "متن پیامک را Paste کنید"
                            return@TextButton
                        }
                        val parsed = SmsParser.parse(smsText, "")
                        if (parsed == null || parsed.amount == null) {
                            errorMsg = "نتونستم پیامک رو تحلیل کنم. لطفاً بررسی کن."
                            parsedResult = null
                            return@TextButton
                        }
                        parsedResult = parsed
                        errorMsg = ""

                        detectedAccount = null
                        if (!parsed.cardLast4.isNullOrEmpty()) {
                            detectedAccount = accounts.find {
                                it.cardNumber.endsWith(parsed.cardLast4!!)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SMS_CYAN_END
                    )
                ) {
                    Text("🔍 تحلیل پیامک", fontWeight = FontWeight.Bold)
                }

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "❌ $errorMsg",
                        fontSize = 12.sp,
                        color = SMS_DANGER_END,
                        textAlign = TextAlign.Right
                    )
                }

                if (parsedResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "📋 نتیجه تحلیل:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SMS_TEXT,
                                modifier = Modifier.padding(bottom = 8.dp),
                                textAlign = TextAlign.Right
                            )

                            val result = parsedResult!!
                            InfoRow("🏦 بانک", result.bank)
                            InfoRow("💰 مبلغ", formatMoneySMS(result.amount ?: 0.0))
                            InfoRow("💸 نوع", result.type ?: "نامشخص")
                            InfoRow("🔢 کارت", "**** ${result.cardLast4 ?: "?"}")

                            if (detectedAccount != null) {
                                InfoRow("💳 حساب متصل", detectedAccount!!.name)
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            SMS_SUCCESS_START.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "✅ حساب خودکار تشخیص داده شد",
                                        fontSize = 12.sp,
                                        color = SMS_SUCCESS_END,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            SMS_WARNING_START.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ حساب پیدا نشد. بعد از ذخیره، دستی متصل کن.",
                                        fontSize = 12.sp,
                                        color = SMS_WARNING_END,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (parsedResult != null) {
                TextButton(
                    onClick = {
                        val result = parsedResult!!
                        val accountId = detectedAccount?.id

                        scope.launch {
                            try {
                                val dateFormat = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                )
                                val now = dateFormat.format(Date())

                                db.smsDao().insertSms(
                                    SmsQueue(
                                        sender = "دستی",
                                        body = smsText,
                                        receivedAt = now,
                                        status = "pending",
                                        parsedAmount = result.amount,
                                        parsedType = result.type,
                                        parsedCard = result.cardLast4,
                                        accountId = accountId,
                                        rawParsed = ""
                                    )
                                )
                                onSaved()
                            } catch (e: Exception) {
                                errorMsg = "خطا: ${e.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SMS_SUCCESS_END
                    )
                ) {
                    Text("✅ ذخیره در لیست", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = SMS_TEXT_LIGHT)
            ) {
                Text("❌ انصراف")
            }
        },
        containerColor = Color.White
    )
}

// ─────────────────────────────────────────
// ردیف اطلاعات
// ─────────────────────────────────────────
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = SMS_TEXT_LIGHT
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SMS_TEXT
        )
    }
}

// ─────────────────────────────────────────
// دیالوگ ویرایش پیامک
// ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsEditDialog(
    sms: SmsQueue,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSaveAndApprove: (accountId: Long, category: String, amount: Double, type: String) -> Unit,
    onSaveOnly: (accountId: Long, category: String, amount: Double, type: String) -> Unit
) {
    var editType by remember { mutableStateOf(sms.parsedType ?: "هزینه") }
    var editCategory by remember { mutableStateOf("پیامک بانکی") }
    var editAmount by remember {
        mutableStateOf(
            if (sms.parsedAmount != null)
                formatNumberWithCommaSMS(sms.parsedAmount.toLong().toString())
            else ""
        )
    }
    var selectedAccount by remember {
        mutableStateOf(
            accounts.find { it.id == sms.accountId } ?: accounts.firstOrNull()
        )
    }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val currentCategories = if (editType == "هزینه") SMS_EXPENSE_CATEGORIES
    else SMS_INCOME_CATEGORIES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("✏️ ویرایش و تأیید پیامک", fontWeight = FontWeight.Bold, color = SMS_TEXT)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    "نوع:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SMS_TEXT,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeSelectorButtonSms(
                        title = "💸 هزینه",
                        isSelected = editType == "هزینه",
                        startColor = SMS_DANGER_START,
                        endColor = SMS_DANGER_END,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            editType = "هزینه"
                            editCategory = "پیامک بانکی"
                        }
                    )
                    TypeSelectorButtonSms(
                        title = "💵 درآمد",
                        isSelected = editType == "درآمد",
                        startColor = SMS_SUCCESS_START,
                        endColor = SMS_SUCCESS_END,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            editType = "درآمد"
                            editCategory = "پیامک بانکی"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "دسته:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SMS_TEXT,
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
                            color = SMS_TEXT,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        Text(text = "▼", fontSize = 10.sp, color = SMS_TEXT_LIGHT)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "مبلغ (تومان):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SMS_TEXT,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        editAmount = if (digitsOnly.isEmpty()) ""
                        else formatNumberWithCommaSMS(digitsOnly)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SMS_CYAN_START,
                        unfocusedBorderColor = Color(0xFFD0D7DE),
                        focusedTextColor = SMS_TEXT,
                        unfocusedTextColor = SMS_TEXT,
                        cursorColor = SMS_CYAN_START
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "کارت:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SMS_TEXT,
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
                            color = SMS_TEXT,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        Text(text = "▼", fontSize = 10.sp, color = SMS_TEXT_LIGHT)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = parseFormattedNumberSMS(editAmount).toDoubleOrNull() ?: 0.0
                    val account = selectedAccount ?: return@TextButton
                    if (amount <= 0) return@TextButton
                    onSaveAndApprove(
                        account.id,
                        editCategory.ifEmpty { "پیامک بانکی" },
                        amount,
                        editType
                    )
                },
                colors = ButtonDefaults.textButtonColors(contentColor = SMS_SUCCESS_END)
            ) {
                Text("✅ تأیید و ثبت", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        val amount = parseFormattedNumberSMS(editAmount).toDoubleOrNull() ?: 0.0
                        val account = selectedAccount ?: return@TextButton
                        if (amount <= 0) return@TextButton
                        onSaveOnly(
                            account.id,
                            editCategory.ifEmpty { "پیامک بانکی" },
                            amount,
                            editType
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SMS_WARNING_END)
                ) {
                    Text("💾 فقط ذخیره")
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = SMS_TEXT_LIGHT)
                ) {
                    Text("❌")
                }
            }
        },
        containerColor = Color.White
    )

    if (showCategoryMenu) {
        var customCategory by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCategoryMenu = false },
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
                        color = SMS_TEXT,
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
                                color = SMS_TEXT_LIGHT.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SMS_CYAN_START,
                            unfocusedBorderColor = Color(0xFFD0D7DE),
                            focusedTextColor = SMS_TEXT,
                            unfocusedTextColor = SMS_TEXT,
                            cursorColor = SMS_CYAN_START
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            if (customCategory.isNotBlank()) {
                                editCategory = customCategory.trim()
                                showCategoryMenu = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = SMS_SUCCESS_END
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
                        color = SMS_TEXT_LIGHT,
                        modifier = Modifier.padding(bottom = 4.dp),
                        textAlign = TextAlign.Right
                    )

                    currentCategories.forEach { cat ->
                        Text(
                            text = "• $cat",
                            fontSize = 15.sp,
                            color = if (editCategory == cat) SMS_INFO_END else SMS_TEXT,
                            fontWeight = if (editCategory == cat) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editCategory = cat
                                    showCategoryMenu = false
                                }
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryMenu = false }) {
                    Text("بستن", color = SMS_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
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
                            text = "💳 ${acc.name} (${acc.bank})",
                            fontSize = 15.sp,
                            color = if (selectedAccount?.id == acc.id) SMS_INFO_END else SMS_TEXT,
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
                    Text("بستن", color = SMS_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }
}

// ─────────────────────────────────────────
// دکمه‌های کمکی
// ─────────────────────────────────────────
@Composable
fun TypeSelectorButtonSms(
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
                color = if (isSelected) Color.White else SMS_TEXT
            )
        }
    }
}

@Composable
fun SmsActionButton(
    icon: String,
    title: String,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    val alpha = if (enabled) 1f else 0.5f

    Card(
        modifier = modifier
            .height(44.dp)
            .scale(scale)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 1.dp else 3.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            startColor.copy(alpha = alpha),
                            endColor.copy(alpha = alpha)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}