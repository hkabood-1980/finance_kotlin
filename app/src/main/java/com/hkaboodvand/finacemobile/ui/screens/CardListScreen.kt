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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────
// رنگ‌ها
// ─────────────────────────────────────────
val CL_PRIMARY_START = Color(0xFF7EA6E0)
val CL_PRIMARY_END = Color(0xFF5B8DD9)
val CL_CARD_BG_START = Color(0xFFF0F4F8)
val CL_CARD_BG_END = Color(0xFFE2EBF5)
val CL_DANGER_START = Color(0xFFE8908A)
val CL_DANGER_END = Color(0xFFD97570)
val CL_WARNING_START = Color(0xFFE8B96A)
val CL_WARNING_END = Color(0xFFD4A356)
val CL_BG = Color(0xFFF0F4F8)
val CL_TEXT = Color(0xFF1E293B)
val CL_TEXT_LIGHT = Color(0xFF64748B)

// فرمت پول
fun formatMoneyCL(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    onBack: () -> Unit,
    onAddCard: () -> Unit,
    onEditCard: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }

    // State برای دیالوگ تأیید حذف
    var showDeleteDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    // بارگذاری خودکار حساب‌ها
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list ->
            accounts = list
            isLoading = false
        }
    }

    // درخواست حذف — اول چک می‌کنه، بعد دیالوگ نشون می‌ده
    fun requestDelete(account: Account) {
        scope.launch {
            val hasTransactions = repo.hasTransactions(account.id)

            if (hasTransactions) {
                snackbarHostState.showSnackbar(
                    "❌ این حساب تراکنش دارد و قابل حذف نیست"
                )
                return@launch
            }

            accountToDelete = account
            showDeleteDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "💳 نمایش حساب ها",
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
                actions = {
                    IconButton(onClick = onAddCard) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "ثبت حساب جدید",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CL_PRIMARY_START
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CL_BG)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CL_PRIMARY_START
                    )
                }

                accounts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "💳", fontSize = 72.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "هنوز حسابی ثبت نشده",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CL_TEXT
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "برای شروع، روی دکمه‌ی + بالای صفحه بزن",
                            fontSize = 13.sp,
                            color = CL_TEXT_LIGHT,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        EmptyStateButton(
                            icon = "➕",
                            title = "ثبت حساب جدید",
                            onClick = onAddCard
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "📋 ${accounts.size} حساب ثبت شده",
                            fontSize = 13.sp,
                            color = CL_TEXT_LIGHT,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        accounts.forEach { account ->
                            AccountCardItem(
                                account = account,
                                onEdit = { onEditCard(account.id) },
                                onDelete = { requestDelete(account) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // ─── دیالوگ تأیید حذف ───
    if (showDeleteDialog && accountToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                accountToDelete = null
            },
            title = {
                Text(
                    "🗑️ حذف حساب",
                    fontWeight = FontWeight.Bold,
                    color = CL_TEXT
                )
            },
            text = {
                Text(
                    "آیا مطمئن هستید که می‌خواهید حساب «${accountToDelete?.name}» را حذف کنید؟\n\nاین عملیات قابل بازگشت نیست.",
                    color = CL_TEXT
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val acc = accountToDelete
                        showDeleteDialog = false
                        accountToDelete = null

                        if (acc != null) {
                            scope.launch {
                                try {
                                    repo.deleteAccount(acc.id)
                                    snackbarHostState.showSnackbar("✅ حساب حذف شد")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("❌ خطا: ${e.message}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = CL_DANGER_END
                    )
                ) {
                    Text("🗑️ حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = CL_TEXT_LIGHT
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
// آیتم حساب توی لیست
// ─────────────────────────────────────────
@Composable
fun AccountCardItem(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(CL_CARD_BG_START, CL_CARD_BG_END)
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💳", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = account.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CL_TEXT,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏦", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = account.bank,
                        fontSize = 13.sp,
                        color = CL_TEXT_LIGHT
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔢", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "**** ${account.cardNumber.takeLast(4)}",
                        fontSize = 13.sp,
                        color = CL_TEXT_LIGHT
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = Color(0xFFD0D7DE))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💰", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatMoneyCL(account.balance),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CL_PRIMARY_END
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallGradientButton(
                        icon = Icons.Default.Edit,
                        title = "ویرایش",
                        startColor = CL_WARNING_START,
                        endColor = CL_WARNING_END,
                        modifier = Modifier.weight(1f),
                        onClick = onEdit
                    )
                    SmallGradientButton(
                        icon = Icons.Default.Delete,
                        title = "حذف",
                        startColor = CL_DANGER_START,
                        endColor = CL_DANGER_END,
                        modifier = Modifier.weight(1f),
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// دکمه‌ی کوچک با گرادیان
// ─────────────────────────────────────────
@Composable
fun SmallGradientButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            .height(42.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 1.dp else 3.dp
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
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
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

// ─────────────────────────────────────────
// دکمه‌ی گرادیانی بزرگ (برای حالت خالی)
// ─────────────────────────────────────────
@Composable
fun EmptyStateButton(
    icon: String,
    title: String,
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
        modifier = Modifier
            .width(220.dp)
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
                    Brush.linearGradient(
                        colors = listOf(CL_PRIMARY_START, CL_PRIMARY_END)
                    )
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