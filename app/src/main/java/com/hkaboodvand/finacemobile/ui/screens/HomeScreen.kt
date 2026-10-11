package com.hkaboodvand.finacemobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.launch

// رنگ‌های برنامه
val COLOR_PRIMARY = Color(0xFF3B82F6)
val COLOR_SUCCESS = Color(0xFF10B981)
val COLOR_DANGER = Color(0xFFEF4444)
val COLOR_INFO = Color(0xFF8B5CF6)
val COLOR_WARNING = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // دیتابیس
    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    // آمار
    var totalIncome by remember { mutableStateOf(0.0) }
    var totalExpense by remember { mutableStateOf(0.0) }
    var accountsCount by remember { mutableStateOf(0) }
    var totalBalance by remember { mutableStateOf(0.0) }

    // بارگذاری داده‌ها
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "💰 مدیریت مالی",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = COLOR_PRIMARY
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF0F4F8))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // کارت‌های آمار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "درآمد",
                    value = totalIncome,
                    color = COLOR_SUCCESS,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "هزینه",
                    value = totalExpense,
                    color = COLOR_DANGER,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "مانده",
                    value = totalBalance,
                    color = COLOR_INFO,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "کارت‌ها",
                    value = accountsCount.toDouble(),
                    color = COLOR_WARNING,
                    isCount = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // عنوان منو
            Text(
                text = "━━━ منوی اصلی ━━━",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // دکمه‌های منو
            MenuButton(
                icon = "💳",
                title = "نمایش کارت‌ها",
                color = COLOR_PRIMARY,
                onClick = { /* بعداً */ }
            )

            MenuButton(
                icon = "➕",
                title = "ثبت کارت جدید",
                color = COLOR_SUCCESS,
                onClick = { /* بعداً */ }
            )

            MenuButton(
                icon = "💵",
                title = "ثبت درآمد",
                color = COLOR_SUCCESS,
                onClick = { /* بعداً */ }
            )

            MenuButton(
                icon = "💸",
                title = "ثبت هزینه",
                color = COLOR_DANGER,
                onClick = { /* بعداً */ }
            )

            MenuButton(
                icon = "🔁",
                title = "انتقال بین کارت‌ها",
                color = COLOR_WARNING,
                onClick = { /* بعداً */ }
            )

            MenuButton(
                icon = "🐷",
                title = "پس‌انداز",
                color = Color(0xFFEC4899),
                onClick = { /* بعداً */ }
            )

            MenuButton(
                icon = "📊",
                title = "گزارش‌ها",
                color = COLOR_INFO,
                onClick = { /* بعداً */ }
            )

            MenuButton(
                icon = "📩",
                title = "پیامک‌های بانکی",
                color = COLOR_WARNING,
                onClick = { /* بعداً */ }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
    isCount: Boolean = false
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isCount) value.toInt().toString()
                else "${value.toLong() / 1000}K",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun MenuButton(
    icon: String,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}