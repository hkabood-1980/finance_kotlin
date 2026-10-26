package com.hkaboodvand.finacemobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
// رنگ‌های برنامه (نسخه اختصاصی شما)
// ─────────────────────────────────────────
val COLOR_PRIMARY_START = Color(0xFF3B82F6)
val COLOR_PRIMARY_END = Color(0xFF2563EB)

val COLOR_SUCCESS_START = Color(0xFF16BB26)
val COLOR_SUCCESS_END = Color(0xFF138A04)

val COLOR_DANGER_START = Color(0xFFEF4444)
val COLOR_DANGER_END = Color(0xFFDC2626)

val COLOR_INFO_START = Color(0xFF8B5CF6)
val COLOR_INFO_END = Color(0xFF7C3AED)

val COLOR_WARNING_START = Color(0xFFF59E0B)
val COLOR_WARNING_END = Color(0xFFD97706)

val COLOR_PINK_START = Color(0xFFEC4899)
val COLOR_PINK_END = Color(0xFFBE185D)

val COLOR_CYAN_START = Color(0xFF08A485)
val COLOR_CYAN_END = Color(0xFF0E7490)

val COLOR_CYAN_START_card = Color(0xFF61850F)
val COLOR_CYAN_END_card = Color(0xFF364D02)

val COLOR_INDIGO_START = Color(0xFF6366F1)
val COLOR_INDIGO_END = Color(0xFF4338CA)

val BG_COLOR = Color(0xFFDFEEEE)
val TEXT_DARK = Color(0xFF1E293B)
val TEXT_LIGHT = Color(0xFF64748B)

fun formatMoneyHome(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
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
    var totalBalance by remember { mutableStateOf(0.0) }
    var initialBalance by remember { mutableStateOf(0.0) }
    var isDarkMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch { repo.getTotalIncome().collect { totalIncome = it } }
        scope.launch { repo.getTotalExpense().collect { totalExpense = it } }
        scope.launch {
            repo.getAllAccounts().collect { accounts ->
                totalBalance = accounts.sumOf { it.balance }
            }
        }
        scope.launch { repo.getTotalInitialBalance().collect { initialBalance = it } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "💰 مدیریت امور مالی",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                .background(BG_COLOR)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── کارت درآمد و هزینه ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    icon = Icons.Default.ArrowUpward,
                    title = "درآمد کل",
                    value = totalIncome - initialBalance,
                    startColor = COLOR_SUCCESS_START,
                    endColor = COLOR_SUCCESS_END,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.ArrowDownward,
                    title = "هزینه کل",
                    value = totalExpense,
                    startColor = COLOR_DANGER_START,
                    endColor = COLOR_DANGER_END,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── کارت موجودی کل ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(colors = listOf(COLOR_INFO_START, COLOR_INFO_END))
                        )
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(text = "💰 موجودی کل", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "موجودی اولیه: ${formatMoneyHome(initialBalance)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                        Text(text = formatMoneyHome(totalBalance), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── دکمه‌های منو (با رنگ‌های اختصاصی شما) ───
            val menuItems = listOf(
                MenuItem("💳 نمایش کارت‌ها", Icons.Default.CreditCard, COLOR_PRIMARY_START, COLOR_PRIMARY_END, Routes.CARD_LIST),
                MenuItem("➕ ثبت حساب", Icons.Default.AddCircle, COLOR_CYAN_START_card, COLOR_CYAN_END_card, Routes.ADD_CARD), // رنگ اختصاصی شما
                MenuItem("💵 ثبت درآمد", Icons.Default.Money, COLOR_SUCCESS_START, COLOR_SUCCESS_END, Routes.INCOME),
                MenuItem("💸 ثبت هزینه", Icons.Default.ShoppingCart, COLOR_DANGER_START, COLOR_DANGER_END, Routes.EXPENSE),
                MenuItem("🔁 انتقال", Icons.Default.SwapHoriz, COLOR_WARNING_START, COLOR_WARNING_END, Routes.TRANSFER),
                MenuItem("🏦 پس‌انداز", Icons.Default.Savings, COLOR_PINK_START, COLOR_PINK_END, Routes.SAVING),
                MenuItem("📊 گزارش‌ها", Icons.Default.BarChart, COLOR_INFO_START, COLOR_INFO_END, Routes.REPORTS),
                MenuItem("📩 پیامک‌ها", Icons.Default.Message, COLOR_CYAN_START, COLOR_CYAN_END, Routes.SMS)
            )

            for (i in menuItems.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (i < menuItems.size) {
                        val item = menuItems[i]
                        GradientMenuCard(
                            title = item.title,
                            icon = item.icon,
                            startColor = item.startColor,
                            endColor = item.endColor,
                            modifier = Modifier.weight(1f),
                            onClick = { if (item.route.isNotEmpty()) onNavigate(item.route) }
                        )
                    }
                    if (i + 1 < menuItems.size) {
                        val item = menuItems[i + 1]
                        GradientMenuCard(
                            title = item.title,
                            icon = item.icon,
                            startColor = item.startColor,
                            endColor = item.endColor,
                            modifier = Modifier.weight(1f),
                            onClick = { if (item.route.isNotEmpty()) onNavigate(item.route) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── دکمه پشتیبان‌گیری ───
            Card(
                onClick = { onNavigate(Routes.BACKUP) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(colors = listOf(COLOR_INDIGO_START, COLOR_INDIGO_END))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "پشتیبان", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "💾 پشتیبان‌گیری", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val startColor: Color,
    val endColor: Color,
    val route: String
)

@Composable
fun StatCard(
    icon: ImageVector,
    title: String,
    value: Double,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = listOf(startColor, endColor))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = title, fontSize = 13.sp, color = Color.White.copy(alpha = 0.95f))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatMoneyHome(value),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GradientMenuCard(
    title: String,
    icon: ImageVector,
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
            .height(90.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = listOf(startColor, endColor))),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}