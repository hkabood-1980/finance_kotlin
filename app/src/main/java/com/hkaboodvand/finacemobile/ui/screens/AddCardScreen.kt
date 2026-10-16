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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
// رنگ‌های ملایم
// ─────────────────────────────────────────
val AC_PRIMARY_START = Color(0xFF7EA6E0)
val AC_PRIMARY_END = Color(0xFF5B8DD9)
val AC_SUCCESS_START = Color(0xFF7ECBA1)
val AC_SUCCESS_END = Color(0xFF5BB88A)
val AC_DANGER_START = Color(0xFFE8908A)
val AC_DANGER_END = Color(0xFFD97570)
val AC_BG = Color(0xFFF0F4F8)
val AC_TEXT = Color(0xFF1E293B)
val AC_TEXT_LIGHT = Color(0xFF64748B)

// ─────────────────────────────────────────
// توابع کمکی برای فرمت اعداد
// ─────────────────────────────────────────
fun formatNumberWithComma(input: String): String {
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

fun parseFormattedNumber(input: String): String {
    return input.filter { it.isDigit() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    accountId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { FinanceRepository(db) }

    // فیلدهای فرم
    var name by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("جاری") }

    // ─── موجودی با TextFieldValue (رفع باگ کاما) ───
    var balanceTFV by remember { mutableStateOf(TextFieldValue("")) }

    // خطاها
    var nameError by remember { mutableStateOf("") }
    var bankError by remember { mutableStateOf("") }
    var cardError by remember { mutableStateOf("") }
    var balanceError by remember { mutableStateOf("") }

    // اگه حالت ویرایشه، اطلاعات حساب رو لود کن
    LaunchedEffect(accountId) {
        if (accountId != null) {
            val account = repo.getAccountById(accountId)
            if (account != null) {
                name = account.name
                bank = account.bank
                cardNumber = account.cardNumber
                accountType = account.type.ifEmpty { "جاری" }
                val formatted = if (account.balance > 0) {
                    formatNumberWithComma(account.balance.toLong().toString())
                } else ""
                balanceTFV = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                )
            }
        }
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // اعتبارسنجی شماره حساب (۴ تا ۲۴ رقم)
    fun validateCardNumber(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "شماره حساب را وارد کنید"
        if (!trimmed.all { it.isDigit() }) return "فقط عدد وارد کنید"
        if (trimmed.length < 4) return "حداقل ۴ رقم وارد کنید"
        if (trimmed.length > 24) return "حداکثر ۲۴ رقم مجاز است"
        return ""
    }

    // اعتبارسنجی کل فرم
    fun validateAll(): Boolean {
        var isValid = true

        if (name.isBlank()) {
            nameError = "نام حساب را وارد کنید"
            isValid = false
        } else nameError = ""

        if (bank.isBlank()) {
            bankError = "نام بانک را وارد کنید"
            isValid = false
        } else bankError = ""

        val cardErr = validateCardNumber(cardNumber)
        cardError = cardErr
        if (cardErr.isNotEmpty()) isValid = false

        val balanceValue = parseFormattedNumber(balanceTFV.text).toDoubleOrNull()
        if (balanceTFV.text.isBlank()) {
            balanceError = "موجودی اولیه را وارد کنید"
            isValid = false
        } else if (balanceValue == null || balanceValue < 0) {
            balanceError = "عدد معتبر وارد کنید"
            isValid = false
        } else balanceError = ""

        return isValid
    }

    // ذخیره‌ی حساب
    fun saveCard() {
        if (!validateAll()) return

        scope.launch {
            try {
                val balanceValue = parseFormattedNumber(balanceTFV.text).toDoubleOrNull() ?: 0.0

                if (accountId == null) {
                    // ─── حالت ثبت جدید ───
                    val newAccountId = repo.addAccount(
                        Account(
                            name = name.trim(),
                            bank = bank.trim(),
                            cardNumber = cardNumber.trim(),
                            balance = 0.0,
                            type = accountType
                        )
                    )

                    if (balanceValue > 0) {
                        repo.registerTransaction(
                            accountId = newAccountId,
                            type = "درآمد",
                            category = "موجودی اولیه",
                            amount = balanceValue,
                            place = bank.trim(),
                            description = "موجودی اولیه حساب «${name.trim()}»"
                        )
                    }

                    snackbarHostState.showSnackbar("✅ حساب با موفقیت ثبت شد")
                } else {
                    // ─── حالت ویرایش ───
                    val oldAccount = repo.getAccountById(accountId)
                    if (oldAccount == null) {
                        snackbarHostState.showSnackbar("❌ حساب پیدا نشد")
                        return@launch
                    }

                    val oldBalance = oldAccount.balance

                    repo.updateAccount(
                        Account(
                            id = accountId,
                            name = name.trim(),
                            bank = bank.trim(),
                            cardNumber = cardNumber.trim(),
                            balance = oldBalance,
                            type = accountType
                        )
                    )

                    if (balanceValue != oldBalance) {
                        repo.updateInitialBalanceTransaction(
                            accountId = accountId,
                            newBalance = balanceValue,
                            oldBalance = oldBalance,
                            bankName = bank.trim(),
                            cardName = name.trim()
                        )
                    }

                    snackbarHostState.showSnackbar("✅ حساب با موفقیت ویرایش شد")
                }

                kotlinx.coroutines.delay(800)
                onSaved()
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
                        if (accountId == null) "➕ ثبت حساب بانکی" else "✏️ ویرایش حساب",
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
                    containerColor = AC_PRIMARY_START
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AC_BG)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── نام حساب ───
            CardInputField(
                label = "نام حساب *",
                value = name,
                onValueChange = {
                    name = it
                    if (nameError.isNotEmpty()) nameError = ""
                },
                placeholder = "مثلاً: حساب اصلی",
                error = nameError,
                keyboardType = KeyboardType.Text
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── نام بانک ───
            CardInputField(
                label = "نام بانک *",
                value = bank,
                onValueChange = {
                    bank = it
                    if (bankError.isNotEmpty()) bankError = ""
                },
                placeholder = "مثلاً: بانک ملی",
                error = bankError,
                keyboardType = KeyboardType.Text
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── شماره حساب (۴ تا ۲۴ رقم) ───
            CardInputField(
                label = "شماره حساب *",
                value = cardNumber,
                onValueChange = { input ->
                    val filtered = input.filter { ch -> ch.isDigit() }.take(24)
                    cardNumber = filtered
                    if (cardError.isNotEmpty()) cardError = ""
                },
                placeholder = "۴ تا ۲۴ رقم",
                error = cardError,
                keyboardType = KeyboardType.Number,
                helperText = "💡 حداقل ۴ رقم، حداکثر ۲۴ رقم"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ─── نوع حساب ───
            Text(
                text = "نوع حساب *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AC_TEXT,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Right
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountTypeButton(
                    title = "جاری",
                    isSelected = accountType == "جاری",
                    startColor = AC_PRIMARY_START,
                    endColor = AC_PRIMARY_END,
                    modifier = Modifier.weight(1f),
                    onClick = { accountType = "جاری" }
                )
                AccountTypeButton(
                    title = "پس‌انداز",
                    isSelected = accountType == "پس‌انداز",
                    startColor = AC_SUCCESS_START,
                    endColor = AC_SUCCESS_END,
                    modifier = Modifier.weight(1f),
                    onClick = { accountType = "پس‌انداز" }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── موجودی اولیه (با TextFieldValue) ───
            CardInputFieldWithTFV(
                label = "موجودی اولیه *",
                value = balanceTFV,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.text.filter { it.isDigit() }
                    val formatted = if (digitsOnly.isEmpty()) ""
                    else formatNumberWithComma(digitsOnly)
                    balanceTFV = TextFieldValue(
                        text = formatted,
                        selection = TextRange(formatted.length)
                    )
                    if (balanceError.isNotEmpty()) balanceError = ""
                },
                placeholder = "0",
                error = balanceError,
                keyboardType = KeyboardType.Number,
                helperText = "💡 این مبلغ به‌عنوان تراکنش «درآمد - موجودی اولیه» ثبت می‌شود"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── دکمه‌ها ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientButton(
                    icon = "✅",
                    title = "ثبت",
                    startColor = AC_SUCCESS_START,
                    endColor = AC_SUCCESS_END,
                    modifier = Modifier.weight(1f),
                    onClick = { saveCard() }
                )
                GradientButton(
                    icon = "❌",
                    title = "انصراف",
                    startColor = AC_DANGER_START,
                    endColor = AC_DANGER_END,
                    modifier = Modifier.weight(1f),
                    onClick = onBack
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────
// فیلد ورودی معمولی (String)
// ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    helperText: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AC_TEXT,
            modifier = Modifier.padding(bottom = 6.dp),
            textAlign = TextAlign.Right
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = AC_TEXT_LIGHT.copy(alpha = 0.5f))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            isError = error.isNotEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AC_PRIMARY_START,
                unfocusedBorderColor = Color(0xFFD0D7DE),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorBorderColor = AC_DANGER_START,
                errorContainerColor = Color.White,
                focusedTextColor = AC_TEXT,
                unfocusedTextColor = AC_TEXT,
                focusedPlaceholderColor = AC_TEXT_LIGHT.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = AC_TEXT_LIGHT.copy(alpha = 0.5f),
                cursorColor = AC_PRIMARY_START,
                errorCursorColor = AC_DANGER_START
            )
        )

        if (error.isNotEmpty()) {
            Text(
                text = "❌ $error",
                fontSize = 12.sp,
                color = AC_DANGER_START,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                textAlign = TextAlign.Right
            )
        } else if (helperText.isNotEmpty()) {
            Text(
                text = helperText,
                fontSize = 11.sp,
                color = AC_TEXT_LIGHT,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                textAlign = TextAlign.Right
            )
        }
    }
}

// ─────────────────────────────────────────
// فیلد ورودی با TextFieldValue (برای رفع باگ کاما)
// ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardInputFieldWithTFV(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    error: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    helperText: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AC_TEXT,
            modifier = Modifier.padding(bottom = 6.dp),
            textAlign = TextAlign.Right
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = AC_TEXT_LIGHT.copy(alpha = 0.5f))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            isError = error.isNotEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AC_PRIMARY_START,
                unfocusedBorderColor = Color(0xFFD0D7DE),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorBorderColor = AC_DANGER_START,
                errorContainerColor = Color.White,
                focusedTextColor = AC_TEXT,
                unfocusedTextColor = AC_TEXT,
                focusedPlaceholderColor = AC_TEXT_LIGHT.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = AC_TEXT_LIGHT.copy(alpha = 0.5f),
                cursorColor = AC_PRIMARY_START,
                errorCursorColor = AC_DANGER_START
            )
        )

        if (error.isNotEmpty()) {
            Text(
                text = "❌ $error",
                fontSize = 12.sp,
                color = AC_DANGER_START,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                textAlign = TextAlign.Right
            )
        } else if (helperText.isNotEmpty()) {
            Text(
                text = helperText,
                fontSize = 11.sp,
                color = AC_TEXT_LIGHT,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                textAlign = TextAlign.Right
            )
        }
    }
}

// ─────────────────────────────────────────
// دکمه‌ی انتخاب نوع حساب
// ─────────────────────────────────────────
@Composable
fun AccountTypeButton(
    title: String,
    isSelected: Boolean,
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
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSelected) {
                        Brush.linearGradient(colors = listOf(startColor, endColor))
                    } else {
                        Brush.linearGradient(colors = listOf(Color.White, Color.White))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else AC_TEXT
            )
        }
    }
}

// ─────────────────────────────────────────
// دکمه‌ی گرادیانی
// ─────────────────────────────────────────
@Composable
fun GradientButton(
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