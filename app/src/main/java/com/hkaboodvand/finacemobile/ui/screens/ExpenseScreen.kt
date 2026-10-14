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
val EX_DANGER_START = Color(0xFFE8908A)
val EX_DANGER_END = Color(0xFFD97570)
val EX_PRIMARY_START = Color(0xFF7EA6E0)
val EX_PRIMARY_END = Color(0xFF5B8DD9)
val EX_SUCCESS_START = Color(0xFF7ECBA1)
val EX_SUCCESS_END = Color(0xFF5BB88A)
val EX_BG = Color(0xFFF0F4F8)
val EX_TEXT = Color(0xFF1E293B)
val EX_TEXT_LIGHT = Color(0xFF64748B)

// لیست پیشنهادی هزینه‌ها
val EXPENSE_CATEGORIES = listOf(
    "غذا",
    "حمل و نقل",
    "لباس",
    "خانه",
    "درمان",
    "آموزش",
    "سرگرمی",
    "قبض و شارژ",
    "هدیه",
    "سفر",
    "سایر"
)

// ─────────────────────────────────────────
// توابع فرمت اعداد
// ─────────────────────────────────────────
fun formatNumberWithCommaEX(input: String): String {
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

fun parseFormattedNumberEX(input: String): String {
    return input.filter { it.isDigit() }
}

fun formatMoneyEX(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(value.toLong())} تومان"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    // لیست کارت‌ها
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }

    // فیلدهای فرم
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // خطاها
    var accountError by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf("") }

    // State منوها
    var showAccountMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // بارگذاری کارت‌ها
    LaunchedEffect(Unit) {
        repo.getAllAccounts().collect { list ->
            accounts = list
            if (accounts.size == 1) {
                selectedAccount = accounts.first()
            }
        }
    }

    // اعتبارسنجی
    fun validateAll(): Boolean {
        var isValid = true

        if (selectedAccount == null) {
            accountError = "یک کارت انتخاب کنید"
            isValid = false
        } else accountError = ""

        val amountValue = parseFormattedNumberEX(amount).toDoubleOrNull()
        if (amount.isBlank()) {
            amountError = "مبلغ را وارد کنید"
            isValid = false
        } else if (amountValue == null || amountValue <= 0) {
            amountError = "عدد معتبر بزرگ‌تر از صفر وارد کنید"
            isValid = false
        } else if (selectedAccount != null && amountValue > selectedAccount!!.balance) {
            amountError = "موجودی کارت کافی نیست (موجودی: ${formatMoneyEX(selectedAccount!!.balance)})"
            isValid = false
        } else amountError = ""

        if (category.isBlank()) {
            categoryError = "نوع هزینه را وارد کنید"
            isValid = false
        } else categoryError = ""

        return isValid
    }

    // ثبت هزینه
    fun saveExpense() {
        if (!validateAll()) return

        scope.launch {
            try {
                val amountValue = parseFormattedNumberEX(amount).toDoubleOrNull() ?: 0.0
                val account = selectedAccount ?: return@launch

                val result = repo.registerTransaction(
                    accountId = account.id,
                    type = "هزینه",
                    category = category.trim(),
                    amount = amountValue,
                    place = place.trim(),
                    description = description.trim()
                )

                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("✅ هزینه با موفقیت ثبت شد")
                    kotlinx.coroutines.delay(800)
                    onSaved()
                } else {
                    snackbarHostState.showSnackbar(
                        "❌ ${result.exceptionOrNull()?.message ?: "خطا در ثبت"}"
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
                        "💸 ثبت هزینه",
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
                    containerColor = EX_DANGER_START
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(EX_BG)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            if (accounts.isEmpty()) {
                Text(
                    text = "⚠️ هنوز کارتی ثبت نکردی!\nاول از منوی «ثبت کارت» یه کارت اضافه کن.",
                    fontSize = 14.sp,
                    color = EX_DANGER_END,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                return@Column
            }

            // ─── انتخاب کارت ───
            Text(
                text = "انتخاب کارت *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EX_TEXT,
                modifier = Modifier.padding(bottom = 6.dp),
                textAlign = TextAlign.Right
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAccountMenu = true },
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
                        text = selectedAccount?.let {
                            "💳 ${it.name} (${it.bank})\n💰 ${formatMoneyEX(it.balance)}"
                        } ?: "▼ انتخاب کارت",
                        fontSize = 14.sp,
                        color = if (selectedAccount != null) EX_TEXT else EX_TEXT_LIGHT,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Right
                    )
                    Text(text = "▼", fontSize = 12.sp, color = EX_TEXT_LIGHT)
                }
            }

            if (accountError.isNotEmpty()) {
                Text(
                    text = "❌ $accountError",
                    fontSize = 12.sp,
                    color = EX_DANGER_END,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    textAlign = TextAlign.Right
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── مبلغ ───
            ExpenseInputField(
                label = "مبلغ هزینه *",
                value = amount,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    amount = if (digitsOnly.isEmpty()) "" else formatNumberWithCommaEX(digitsOnly)
                    if (amountError.isNotEmpty()) amountError = ""
                },
                placeholder = "0",
                error = amountError,
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── نوع هزینه ───
            Text(
                text = "نوع هزینه *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EX_TEXT,
                modifier = Modifier.padding(bottom = 6.dp),
                textAlign = TextAlign.Right
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCategoryMenu = true },
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
                        text = category.ifEmpty { "▼ انتخاب یا وارد کردن" },
                        fontSize = 14.sp,
                        color = if (category.isNotEmpty()) EX_TEXT else EX_TEXT_LIGHT,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Right
                    )
                    Text(text = "▼", fontSize = 12.sp, color = EX_TEXT_LIGHT)
                }
            }

            if (categoryError.isNotEmpty()) {
                Text(
                    text = "❌ $categoryError",
                    fontSize = 12.sp,
                    color = EX_DANGER_END,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    textAlign = TextAlign.Right
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── محل ───
            ExpenseInputField(
                label = "محل (اختیاری)",
                value = place,
                onValueChange = { place = it },
                placeholder = "مثلاً: رستوران",
                error = "",
                keyboardType = KeyboardType.Text
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── توضیحات ───
            ExpenseInputField(
                label = "توضیحات (اختیاری)",
                value = description,
                onValueChange = { description = it },
                placeholder = "هر توضیحی...",
                error = "",
                keyboardType = KeyboardType.Text,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── دکمه‌ها ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpenseGradientButton(
                    icon = "✅",
                    title = "ثبت",
                    startColor = EX_SUCCESS_START,
                    endColor = EX_SUCCESS_END,
                    modifier = Modifier.weight(1f),
                    onClick = { saveExpense() }
                )
                ExpenseGradientButton(
                    icon = "❌",
                    title = "انصراف",
                    startColor = EX_PRIMARY_START,
                    endColor = EX_PRIMARY_END,
                    modifier = Modifier.weight(1f),
                    onClick = onBack
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ─── منوی انتخاب کارت ───
    if (showAccountMenu) {
        AlertDialog(
            onDismissRequest = { showAccountMenu = false },
            title = {
                Text("💳 انتخاب کارت", fontWeight = FontWeight.Bold, color = EX_TEXT)
            },
            text = {
                Column {
                    accounts.forEach { acc ->
                        Text(
                            text = "💳 ${acc.name} (${acc.bank})\n💰 ${formatMoneyEX(acc.balance)}",
                            fontSize = 15.sp,
                            color = if (selectedAccount?.id == acc.id) EX_DANGER_END else EX_TEXT,
                            fontWeight = if (selectedAccount?.id == acc.id) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccount = acc
                                    accountError = ""
                                    amountError = ""
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
                    Text("بستن", color = EX_TEXT_LIGHT)
                }
            },
            containerColor = Color.White
        )
    }

    // ─── منوی انتخاب/ورود دسته ───
    if (showCategoryMenu) {
        var customCategory by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCategoryMenu = false },
            title = {
                Text("🏷️ نوع هزینه", fontWeight = FontWeight.Bold, color = EX_TEXT)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    EXPENSE_CATEGORIES.forEach { cat ->
                        Text(
                            text = "• $cat",
                            fontSize = 15.sp,
                            color = if (category == cat) EX_DANGER_END else EX_TEXT,
                            fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    category = cat
                                    categoryError = ""
                                    showCategoryMenu = false
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        HorizontalDivider(color = Color(0xFFE2EBF5))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "✍️ یا خودت بنویس:",
                        fontSize = 13.sp,
                        color = EX_TEXT_LIGHT,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Right
                    )

                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        placeholder = { Text("مثلاً: تعمیرات", color = EX_TEXT_LIGHT.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EX_DANGER_START,
                            unfocusedBorderColor = Color(0xFFD0D7DE),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = EX_TEXT,
                            unfocusedTextColor = EX_TEXT,
                            cursorColor = EX_DANGER_START
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customCategory.isNotBlank()) {
                            category = customCategory.trim()
                            categoryError = ""
                        }
                        showCategoryMenu = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EX_DANGER_END)
                ) {
                    Text("✅ تأیید", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCategoryMenu = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = EX_TEXT_LIGHT)
                ) {
                    Text("❌ انصراف")
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
fun ExpenseInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    helperText: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EX_TEXT,
            modifier = Modifier.padding(bottom = 6.dp),
            textAlign = TextAlign.Right
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = EX_TEXT_LIGHT.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = singleLine,
            isError = error.isNotEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EX_DANGER_START,
                unfocusedBorderColor = Color(0xFFD0D7DE),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorBorderColor = EX_DANGER_START,
                errorContainerColor = Color.White,
                focusedTextColor = EX_TEXT,
                unfocusedTextColor = EX_TEXT,
                focusedPlaceholderColor = EX_TEXT_LIGHT.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = EX_TEXT_LIGHT.copy(alpha = 0.5f),
                cursorColor = EX_DANGER_START,
                errorCursorColor = EX_DANGER_START
            )
        )

        if (error.isNotEmpty()) {
            Text(
                text = "❌ $error",
                fontSize = 12.sp,
                color = EX_DANGER_END,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                textAlign = TextAlign.Right
            )
        } else if (helperText.isNotEmpty()) {
            Text(
                text = helperText,
                fontSize = 11.sp,
                color = EX_TEXT_LIGHT,
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
fun ExpenseGradientButton(
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