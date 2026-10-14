package com.hkaboodvand.finacemobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import com.hkaboodvand.finacemobile.data.prefs.AppPreferences
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────
// رنگ‌ها
// ─────────────────────────────────────────
val SV_PINK_START = Color(0xFFE68FB8)
val SV_PINK_END = Color(0xFFD178A3)
val SV_PRIMARY_START = Color(0xFF7EA6E0)
val SV_PRIMARY_END = Color(0xFF5B8DD9)
val SV_SUCCESS_START = Color(0xFF7ECBA1)
val SV_SUCCESS_END = Color(0xFF5BB88A)
val SV_DANGER_START = Color(0xFFE8908A)
val SV_DANGER_END = Color(0xFFD97570)
val SV_WARNING_START = Color(0xFFE8B96A)
val SV_WARNING_END = Color(0xFFD4A356)
val SV_BG = Color(0xFFF0F4F8)
val SV_TEXT = Color(0xFF1E293B)
val SV_TEXT_LIGHT = Color(0xFF64748B)

// ─────────────────────────────────────────
// توابع فرمت
// ─────────────────────────────────────────
fun formatNumberWithCommaSV(input: String): String {
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

fun parseFormattedNumberSV(input: String): String {
    return input.filter { it.isDigit() }
}

fun formatMoneySV(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }
    val prefs = remember { AppPreferences(context) }

    // لیست کارت‌ها
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    // ID حساب پس‌انداز
    var savingsAccountId by remember { mutableStateOf<Long?>(null) }

    // State برای دیالوگ‌ها
    var showSetSavingsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // بارگذاری کارت‌ها
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list ->
            accounts = list
        }
    }

    // بارگذاری savingsAccountId
    LaunchedEffect(Unit) {
        prefs.savingsAccountId.collect { id ->
            savingsAccountId = id
        }
    }

    // پیدا کردن حساب پس‌انداز
    val savingsAccount = accounts.find { it.id == savingsAccountId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🏦 پس‌انداز",
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
                    containerColor = SV_PINK_START
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SV_BG)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── بخش حساب پس‌انداز ───
            if (savingsAccount != null) {
                // نمایش حساب پس‌انداز فعلی
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
                                    colors = listOf(SV_SUCCESS_START, SV_SUCCESS_END)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🎯", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "حساب پس‌انداز فعلی",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "💳 ${savingsAccount.name}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🏦 ${savingsAccount.bank}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💰 ${formatMoneySV(savingsAccount.balance)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // دکمه تغییر حساب پس‌انداز
                SmallGradientButtonSV(
                    icon = "⚙️",
                    title = "تغییر حساب پس‌انداز",
                    startColor = SV_WARNING_START,
                    endColor = SV_WARNING_END,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showSetSavingsDialog = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // پیام راهنما
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F4FD)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ℹ️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "برای ثبت پس‌انداز جدید، از منوی «انتقال بین کارت‌ها» استفاده کنید و مبلغ مورد نظر را از کارت دیگر به این حساب انتقال دهید.",
                            fontSize = 13.sp,
                            color = SV_TEXT,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            } else {
                // حالت خالی: هنوز حساب پس‌انداز تعیین نشده
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🏦", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "هنوز حساب پس‌انداز تعیین نکرده‌اید",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SV_TEXT,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "برای شروع، یکی از کارت‌های خود را به‌عنوان حساب پس‌انداز تعیین کنید.",
                        fontSize = 14.sp,
                        color = SV_TEXT_LIGHT,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (accounts.isNotEmpty()) {
                        SmallGradientButtonSV(
                            icon = "⚙️",
                            title = "تعیین حساب پس‌انداز",
                            startColor = SV_PINK_START,
                            endColor = SV_PINK_END,
                            modifier = Modifier.fillMaxWidth(0.8f),
                            onClick = { showSetSavingsDialog = true }
                        )
                    } else {
                        Text(
                            text = "⚠️ ابتدا باید یک کارت ثبت کنید",
                            fontSize = 14.sp,
                            color = SV_DANGER_END,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ─── دیالوگ انتخاب حساب پس‌انداز ───
    if (showSetSavingsDialog) {
        AlertDialog(
            onDismissRequest = { showSetSavingsDialog = false },
            title = {
                Text(
                    "⚙️ تعیین حساب پس‌انداز",
                    fontWeight = FontWeight.Bold,
                    color = SV_TEXT
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "کدام کارت را به‌عنوان حساب پس‌انداز تعیین می‌کنید؟",
                        fontSize = 13.sp,
                        color = SV_TEXT_LIGHT,
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Right
                    )

                    accounts.forEach { acc ->
                        val isSelected = savingsAccountId == acc.id
                        Text(
                            text = "💳 ${acc.name} (${acc.bank})\n💰 ${formatMoneySV(acc.balance)}",
                            fontSize = 15.sp,
                            color = if (isSelected) SV_SUCCESS_END else SV_TEXT,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        prefs.setSavingsAccount(acc.id)
                                        snackbarHostState.showSnackbar(
                                            "✅ «${acc.name}» به‌عنوان حساب پس‌انداز تعیین شد"
                                        )
                                        showSetSavingsDialog = false
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
                TextButton(
                    onClick = { showSetSavingsDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = SV_TEXT_LIGHT)
                ) {
                    Text("بستن")
                }
            },
            containerColor = Color.White
        )
    }
}

// ─────────────────────────────────────────
// دکمه گرادیانی
// ─────────────────────────────────────────
@Composable
fun SmallGradientButtonSV(
    icon: String,
    title: String,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Card(
        modifier = modifier
            .height(52.dp)
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
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}