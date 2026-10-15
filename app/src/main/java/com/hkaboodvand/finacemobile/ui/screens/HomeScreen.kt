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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import com.hkaboodvand.finacemobile.ui.navigation.Routes
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale


// ─────────────────────────────────────────
// رنگ‌ها
// ─────────────────────────────────────────
// ─────────────────────────────────────────
// رنگ‌ها — نسخه ملایم (Pastel)
// ─────────────────────────────────────────
val COLOR_PRIMARY_START = Color(0xFF2196F3)
val COLOR_PRIMARY_END = Color(0xFF5B8DD9)

val COLOR_SUCCESS_START = Color(0xFF6CD29A)
val COLOR_SUCCESS_END = Color(0xFF5BB88A)

val COLOR_DANGER_START = Color(0xFFF6655A)
val COLOR_DANGER_END = Color(0xFFD97570)

val COLOR_INFO_START = Color(0xFF9277DC)
val COLOR_INFO_END = Color(0xFF8B75C9)

val COLOR_WARNING_START = Color(0xFFEEC856)
val COLOR_WARNING_END = Color(0xFFD4A356)

val COLOR_PINK_START = Color(0xFFEF6DAA)
val COLOR_PINK_END = Color(0xFFD178A3)

val COLOR_CYAN_START = Color(0xFF68C5E1)
val COLOR_CYAN_END = Color(0xFF5BAEC9)

val COLOR_INDIGO_START = Color(0xFF6E74EE)
val COLOR_INDIGO_END = Color(0xFF6E72CC)

val COLOR_GRAY_START = Color(0xFF647191)
val COLOR_GRAY_END = Color(0xFF7A8899)


// تابع کمکی برای نمایش پول با کاما
fun formatMoney(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
    return "${formatter.format(value.toLong())} تومان"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    var totalIncome by remember { mutableStateOf(0.0) }
    var totalExpense by remember { mutableStateOf(0.0) }
    var accountsCount by remember { mutableStateOf(0) }
    var totalBalance by remember { mutableStateOf(0.0) }
    var initialBalance by remember { mutableStateOf(0.0) }
    var isDarkMode by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        scope.launch {
            repo.getTotalIncome().collect { totalIncome = it }
        }
        scope.launch {
            repo.getTotalExpense().collect { totalExpense = it }
        }
        scope.launch {
            repo.getAllAccounts().collect { accounts ->
                accountsCount = accounts.size
                totalBalance = accounts.sumOf { it.balance }
            }
        }
        scope.launch {
            repo.getTotalInitialBalance().collect { initial ->
                initialBalance = initial
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "💰 مدیریت امور مالی",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111213)
                    )
                },
                actions = {
                    IconButton(onClick = { isDarkMode = !isDarkMode }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "تغییر حالت",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = COLOR_PRIMARY_START
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0x5B74CDD9))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── کارت‌های درآمد و هزینه ───
            // ─── موجودی اولیه کل ───
            BalanceCard(
                icon = "💰",
                title = "موجودی اولیه کل",
                value = initialBalance,
                startColor = COLOR_PRIMARY_START,
                endColor = COLOR_PRIMARY_END,
                isLarge = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─── درآمد کل ───
            BalanceCard(
                icon = "💵",
                title = "درآمد کل",
                value = totalIncome - initialBalance,
                startColor = COLOR_SUCCESS_START,
                endColor = COLOR_SUCCESS_END,
                isLarge = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─── هزینه کل ───
            BalanceCard(
                icon = "💸",
                title = "هزینه کل",
                value = totalExpense,
                startColor = COLOR_DANGER_START,
                endColor = COLOR_DANGER_END,
                isLarge = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─── موجودی کل (فونت بزرگ‌تر) ───
            BalanceCard(
                icon = "💰",
                title = "موجودی کل",
                value = (totalIncome + initialBalance) - totalExpense,
                startColor = COLOR_INFO_START,
                endColor = COLOR_INFO_END,
                isLarge = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ─── عنوان منو ───
            Text(
                text = "━━━ منوی اصلی ━━━",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── ردیف ۱ ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuButton(
                    icon = "➕",
                    title = "ثبت کارت",
                    startColor = COLOR_SUCCESS_START,
                    endColor = COLOR_SUCCESS_END,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.ADD_CARD) }
                )
                MenuButton(
                    icon = "💳",
                    title = "نمایش کارت‌ها",
                    startColor = COLOR_PRIMARY_START,
                    endColor = COLOR_PRIMARY_END,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.CARD_LIST) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── ردیف ۲ ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuButton(
                    icon = "💵",
                    title = "ثبت درآمد",
                    startColor = Color(0xFF22C55E),
                    endColor = Color(0xFF15803D),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.INCOME) }
                )
                MenuButton(
                    icon = "💸",
                    title = "ثبت هزینه",
                    startColor = COLOR_DANGER_START,
                    endColor = COLOR_DANGER_END,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.EXPENSE) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── ردیف ۳ ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuButton(
                    icon = "🔁",
                    title = "انتقال",
                    startColor = COLOR_WARNING_START,
                    endColor = COLOR_WARNING_END,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.TRANSFER) }
                )
                MenuButton(
                    icon = "🏦",
                    title = "پس‌انداز",
                    startColor = COLOR_PINK_START,
                    endColor = COLOR_PINK_END,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.SAVING) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── ردیف ۴ ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuButton(
                    icon = "📊",
                    title = "گزارش‌ها",
                    startColor = COLOR_INFO_START,
                    endColor = COLOR_INFO_END,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.REPORTS) }
                )
                MenuButton(
                    icon = "📩",
                    title = "پیامک‌ها",
                    startColor = COLOR_CYAN_START,
                    endColor = COLOR_CYAN_END,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.SMS) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── ردیف ۵ (پشتیبان‌گیری) ───
            Spacer(modifier = Modifier.height(16.dp))

            // ─── دکمه پشتیبان‌گیری (مستطیلی پهن) ───
            FullWidthBackupButton(
                icon = "💾",
                title = "پشتیبان‌گیری",
                startColor = COLOR_INDIGO_START,
                endColor = COLOR_INDIGO_END,
                onClick = { onNavigate("backup") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────
// کارت آمار
// ─────────────────────────────────────────
@Composable
fun StatCard(
    icon: String,
    title: String,
    value: Double,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(startColor, endColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = icon, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.95f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatMoney(value),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────
// کارت موجودی کل
// ─────────────────────────────────────────
@Composable
fun BalanceCard(
    icon: String,
    title: String,
    value: Double,
    startColor: Color,
    endColor: Color,
    isLarge: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLarge) 80.dp else 65.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLarge) 8.dp else 5.dp
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = icon, fontSize = if (isLarge) 24.sp else 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontSize = if (isLarge) 18.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = formatMoney(value),
                    fontSize = if (isLarge) 20.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────
// دکمه منو — با گرادیان + انیمیشن
// ─────────────────────────────────────────
@Composable
fun MenuButton(
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
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Card(
        modifier = modifier
            .height(100.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(startColor, endColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
@Composable
fun FullWidthBackupButton(
        icon: String,
        title: String,
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
                .height(70.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isPressed) 2.dp else 6.dp
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = icon, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
 }
