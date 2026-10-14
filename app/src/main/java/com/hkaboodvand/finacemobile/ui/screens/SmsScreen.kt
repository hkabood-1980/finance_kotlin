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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.hkaboodvand.finacemobile.data.db.entity.SmsQueue
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
// توابع فرمت
// ─────────────────────────────────────────
fun formatMoneySMS(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

// تبدیل تاریخ میلادی به شمسی (بدون کتابخانه)
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
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt()
        )
        String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    } catch (e: Exception) {
        miladiDate
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    // داده‌ها
    var pendingSms by remember { mutableStateOf<List<SmsQueue>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    // فیلتر
    var filter by remember { mutableStateOf("pending") }  // pending / approved / all

    // دیالوگ‌ها
    var showApproveDialog by remember { mutableStateOf(false) }
    var selectedSms by remember { mutableStateOf<SmsQueue?>(null) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // بارگذاری داده‌ها
    LaunchedEffect(Unit) {
        repo.getPendingSms().collect { list ->
            pendingSms = list
        }
    }
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list ->
            accounts = list
        }
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
            // ─── اطلاعات ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ℹ️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "پیامک‌های بانکی در انتظار تأیید",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SMS_TEXT
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "این پیامک‌ها خودکار توسط اپلیکیشن دریافت شده‌اند. برای ثبت به‌عنوان تراکنش، روی «تأیید» بزنید.",
                        fontSize = 12.sp,
                        color = SMS_TEXT_LIGHT,
                        textAlign = TextAlign.Right
                    )
                }
            }

            // ─── لیست پیامک‌ها ───
            if (pendingSms.isEmpty()) {
                // حالت خالی
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
                        text = "پیامک‌های بانکی که دریافت می‌شوند اینجا نمایش داده می‌شوند.",
                        fontSize = 13.sp,
                        color = SMS_TEXT_LIGHT,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingSms) { sms ->
                        SmsCard(
                            sms = sms,
                            onApprove = {
                                selectedSms = sms
                                if (accounts.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("⚠️ ابتدا یک کارت ثبت کنید")
                                    }
                                } else {
                                    showAccountPicker = true
                                }
                            },
                            onDelete = {
                                selectedSms = sms
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ─── دیالوگ انتخاب حساب ───
    if (showAccountPicker && selectedSms != null) {
        AlertDialog(
            onDismissRequest = { showAccountPicker = false },
            icon = {
                Text(text = "💳", fontSize = 40.sp)
            },
            title = {
                Text(
                    "انتخاب حساب",
                    fontWeight = FontWeight.Bold,
                    color = SMS_TEXT
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "پیامک را به کدام حساب متصل کنم؟",
                        fontSize = 13.sp,
                        color = SMS_TEXT_LIGHT,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Right
                    )

                    accounts.forEach { acc ->
                        Text(
                            text = "💳 ${acc.name} (${acc.bank})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = SMS_TEXT,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        try {
                                            val result = repo.approveSms(
                                                selectedSms!!.id,
                                                acc.id
                                            )
                                            if (result.isSuccess) {
                                                snackbarHostState.showSnackbar(
                                                    "✅ پیامک ثبت شد و به «${acc.name}» متصل گردید"
                                                )
                                                // یادگیری
                                                repo.learnSmsMapping(
                                                    sender = selectedSms!!.sender,
                                                    cardLast4 = selectedSms!!.parsedCard ?: "",
                                                    accountId = acc.id
                                                )
                                            } else {
                                                snackbarHostState.showSnackbar(
                                                    "❌ ${result.exceptionOrNull()?.message}"
                                                )
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("❌ ${e.message}")
                                        }
                                        showAccountPicker = false
                                        selectedSms = null
                                    }
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountPicker = false }) {
                    Text("انصراف", color = SMS_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }

    // ─── دیالوگ تأیید حذف ───
    if (showDeleteDialog && selectedSms != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Text(text = "🗑️", fontSize = 40.sp)
            },
            title = {
                Text(
                    "حذف پیامک",
                    fontWeight = FontWeight.Bold,
                    color = SMS_DANGER_END
                )
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
                                repo.deleteSms(selectedSms!!.id)
                                snackbarHostState.showSnackbar("✅ پیامک حذف شد")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ ${e.message}")
                            }
                            showDeleteDialog = false
                            selectedSms = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SMS_DANGER_END
                    )
                ) {
                    Text("🗑️ حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SMS_TEXT_LIGHT
                    )
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
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ─── هدر ───
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
                // badge نوع
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

            // ─── متن پیامک ───
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

            // ─── مبلغ و کارت ───
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

            // ─── دکمه‌ها ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmsActionButton(
                    icon = "✅",
                    title = "تأیید و اتصال",
                    startColor = SMS_SUCCESS_START,
                    endColor = SMS_SUCCESS_END,
                    modifier = Modifier.weight(2f),
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
// دکمه اکشن
// ─────────────────────────────────────────
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
                Text(text = icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}