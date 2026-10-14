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
import com.hkaboodvand.finacemobile.data.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────
// رنگ‌ها
// ─────────────────────────────────────────
val TR_WARNING_START = Color(0xFFE8B96A)
val TR_WARNING_END = Color(0xFFD4A356)
val TR_PRIMARY_START = Color(0xFF7EA6E0)
val TR_PRIMARY_END = Color(0xFF5B8DD9)
val TR_SUCCESS_START = Color(0xFF7ECBA1)
val TR_SUCCESS_END = Color(0xFF5BB88A)
val TR_DANGER_START = Color(0xFFE8908A)
val TR_DANGER_END = Color(0xFFD97570)
val TR_BG = Color(0xFFF0F4F8)
val TR_TEXT = Color(0xFF1E293B)
val TR_TEXT_LIGHT = Color(0xFF64748B)

// ─────────────────────────────────────────
// توابع فرمت
// ─────────────────────────────────────────
fun formatNumberWithCommaTR(input: String): String {
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

fun parseFormattedNumberTR(input: String): String {
    return input.filter { it.isDigit() }
}

fun formatMoneyTR(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    // لیست کارت‌ها
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    // کارت مبدأ و مقصد
    var sourceAccount by remember { mutableStateOf<Account?>(null) }
    var destAccount by remember { mutableStateOf<Account?>(null) }

    // فیلدها
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // خطاها
    var sourceError by remember { mutableStateOf("") }
    var destError by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf("") }

    // State منوها
    var showSourceMenu by remember { mutableStateOf(false) }
    var showDestMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // بارگذاری کارت‌ها
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list ->
            accounts = list
        }
    }

    // اعتبارسنجی
    fun validateAll(): Boolean {
        var isValid = true

        if (sourceAccount == null) {
            sourceError = "کارت مبدأ را انتخاب کنید"
            isValid = false
        } else sourceError = ""

        if (destAccount == null) {
            destError = "کارت مقصد را انتخاب کنید"
            isValid = false
        } else if (sourceAccount != null && sourceAccount!!.id == destAccount!!.id) {
            destError = "کارت مبدأ و مقصد نمی‌توانند یکسان باشند"
            isValid = false
        } else destError = ""

        val amountValue = parseFormattedNumberTR(amount).toDoubleOrNull()
        if (amount.isBlank()) {
            amountError = "مبلغ را وارد کنید"
            isValid = false
        } else if (amountValue == null || amountValue <= 0) {
            amountError = "عدد معتبر بزرگ‌تر از صفر وارد کنید"
            isValid = false
        } else if (sourceAccount != null && amountValue > sourceAccount!!.balance) {
            amountError = "موجودی کارت مبدأ کافی نیست (موجودی: ${formatMoneyTR(sourceAccount!!.balance)})"
            isValid = false
        } else amountError = ""

        return isValid
    }

    // ثبت انتقال
    fun saveTransfer() {
        if (!validateAll()) return

        scope.launch {
            try {
                val amountValue = parseFormattedNumberTR(amount).toDoubleOrNull() ?: 0.0
                val source = sourceAccount ?: return@launch
                val dest = destAccount ?: return@launch

                val result = repo.transferBetweenAccounts(
                    sourceAccountId = source.id,
                    destinationAccountId = dest.id,
                    amount = amountValue,
                    description = description.trim()
                )

                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("✅ انتقال با موفقیت انجام شد")
                    kotlinx.coroutines.delay(800)
                    onSaved()
                } else {
                    snackbarHostState.showSnackbar(
                        "❌ ${result.exceptionOrNull()?.message ?: "خطا در انتقال"}"
                    )
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ خطا: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🔁 انتقال بین کارت‌ها",
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
                    containerColor = TR_WARNING_START
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TR_BG)
        ) {
            // ─── اگه کمتر از ۲ کارت ───
            if (accounts.size < 2) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "🔁", fontSize = 72.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "شما فقط ${accounts.size} کارت ثبت کرده‌اید",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TR_TEXT,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "برای انتقال، حداقل ۲ کارت لازم است.",
                        fontSize = 14.sp,
                        color = TR_TEXT_LIGHT,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // ─── فرم انتقال ───
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // ─── کارت مبدأ ───
                    Text(
                        text = "از کارت (مبدأ) *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TR_TEXT,
                        modifier = Modifier.padding(bottom = 6.dp),
                        textAlign = TextAlign.Right
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSourceMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sourceAccount?.let {
                                    "💳 ${it.name} (${it.bank})\n💰 ${formatMoneyTR(it.balance)}"
                                } ?: "▼ انتخاب کارت مبدأ",
                                fontSize = 14.sp,
                                color = if (sourceAccount != null) TR_TEXT else TR_TEXT_LIGHT,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Text(text = "▼", fontSize = 12.sp, color = TR_TEXT_LIGHT)
                        }
                    }

                    if (sourceError.isNotEmpty()) {
                        Text(
                            text = "❌ $sourceError",
                            fontSize = 12.sp,
                            color = TR_DANGER_END,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                            textAlign = TextAlign.Right
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── کارت مقصد ───
                    Text(
                        text = "به کارت (مقصد) *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TR_TEXT,
                        modifier = Modifier.padding(bottom = 6.dp),
                        textAlign = TextAlign.Right
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDestMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = destAccount?.let {
                                    "💳 ${it.name} (${it.bank})\n💰 ${formatMoneyTR(it.balance)}"
                                } ?: "▼ انتخاب کارت مقصد",
                                fontSize = 14.sp,
                                color = if (destAccount != null) TR_TEXT else TR_TEXT_LIGHT,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Text(text = "▼", fontSize = 12.sp, color = TR_TEXT_LIGHT)
                        }
                    }

                    if (destError.isNotEmpty()) {
                        Text(
                            text = "❌ $destError",
                            fontSize = 12.sp,
                            color = TR_DANGER_END,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                            textAlign = TextAlign.Right
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── مبلغ ───
                    TransferInputField(
                        label = "مبلغ انتقال *",
                        value = amount,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            amount = if (digitsOnly.isEmpty()) "" else formatNumberWithCommaTR(digitsOnly)
                            if (amountError.isNotEmpty()) amountError = ""
                        },
                        placeholder = "0",
                        error = amountError,
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── توضیحات ───
                    TransferInputField(
                        label = "توضیحات (اختیاری)",
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "مثلاً: انتقال ماهانه",
                        error = "",
                        keyboardType = KeyboardType.Text
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ─── دکمه‌ها ───
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TransferGradientButton(
                            icon = "✅",
                            title = "انتقال",
                            startColor = TR_SUCCESS_START,
                            endColor = TR_SUCCESS_END,
                            modifier = Modifier.weight(1f),
                            onClick = { saveTransfer() }
                        )
                        TransferGradientButton(
                            icon = "❌",
                            title = "انصراف",
                            startColor = TR_DANGER_START,
                            endColor = TR_DANGER_END,
                            modifier = Modifier.weight(1f),
                            onClick = onBack
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // ─── منوی کارت مبدأ ───
    if (showSourceMenu) {
        AlertDialog(
            onDismissRequest = { showSourceMenu = false },
            title = {
                Text("💳 انتخاب کارت مبدأ", fontWeight = FontWeight.Bold, color = TR_TEXT)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    accounts.forEach { acc ->
                        Text(
                            text = "💳 ${acc.name} (${acc.bank})\n💰 ${formatMoneyTR(acc.balance)}",
                            fontSize = 15.sp,
                            color = if (sourceAccount?.id == acc.id) TR_WARNING_END else TR_TEXT,
                            fontWeight = if (sourceAccount?.id == acc.id) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sourceAccount = acc
                                    sourceError = ""
                                    amountError = ""
                                    showSourceMenu = false
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourceMenu = false }) {
                    Text("بستن", color = TR_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }

    // ─── منوی کارت مقصد ───
    if (showDestMenu) {
        AlertDialog(
            onDismissRequest = { showDestMenu = false },
            title = {
                Text("💳 انتخاب کارت مقصد", fontWeight = FontWeight.Bold, color = TR_TEXT)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    accounts.forEach { acc ->
                        Text(
                            text = "💳 ${acc.name} (${acc.bank})\n💰 ${formatMoneyTR(acc.balance)}",
                            fontSize = 15.sp,
                            color = if (destAccount?.id == acc.id) TR_WARNING_END else TR_TEXT,
                            fontWeight = if (destAccount?.id == acc.id) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    destAccount = acc
                                    destError = ""
                                    showDestMenu = false
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDestMenu = false }) {
                    Text("بستن", color = TR_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }
}

// ─────────────────────────────────────────
// فیلد ورودی
// ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TR_TEXT,
            modifier = Modifier.padding(bottom = 6.dp),
            textAlign = TextAlign.Right
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TR_TEXT_LIGHT.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = singleLine,
            isError = error.isNotEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TR_WARNING_START,
                unfocusedBorderColor = Color(0xFFD0D7DE),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorBorderColor = TR_DANGER_START,
                errorContainerColor = Color.White,
                focusedTextColor = TR_TEXT,
                unfocusedTextColor = TR_TEXT,
                focusedPlaceholderColor = TR_TEXT_LIGHT.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = TR_TEXT_LIGHT.copy(alpha = 0.5f),
                cursorColor = TR_WARNING_START,
                errorCursorColor = TR_DANGER_START
            )
        )

        if (error.isNotEmpty()) {
            Text(
                text = "❌ $error",
                fontSize = 12.sp,
                color = TR_DANGER_END,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                textAlign = TextAlign.Right
            )
        }
    }
}

// ─────────────────────────────────────────
// دکمه گرادیانی
// ─────────────────────────────────────────
@Composable
fun TransferGradientButton(
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
            .height(55.dp)
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