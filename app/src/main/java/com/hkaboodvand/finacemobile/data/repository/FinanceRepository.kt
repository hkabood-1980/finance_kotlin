package com.hkaboodvand.finacemobile.data.repository

import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.db.dao.CategorySum
import com.hkaboodvand.finacemobile.data.db.entity.Account
import com.hkaboodvand.finacemobile.data.db.entity.Transaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.map
import com.hkaboodvand.finacemobile.data.db.entity.SmsMapping
import com.hkaboodvand.finacemobile.data.db.entity.SmsQueue


class FinanceRepository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val txDao = db.transactionDao()

    // =====================================================
    // Accounts
    // =====================================================

    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAll()

    suspend fun getAccountById(id: Long): Account? = accountDao.getById(id)

    suspend fun addAccount(account: Account): Long = accountDao.insert(account)

    suspend fun updateAccount(account: Account) = accountDao.update(account)

    suspend fun deleteAccount(id: Long) = accountDao.deleteById(id)

    // =====================================================
    // Transactions
    // =====================================================

    fun getAllTransactions(): Flow<List<Transaction>> = txDao.getAll()

    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> =
        txDao.getByAccount(accountId)

    fun getRecentTransactions(): Flow<List<Transaction>> = txDao.getRecentTransactions()

    fun getTotalIncome(): Flow<Double> = txDao.getTotalIncome()

    fun getTotalExpense(): Flow<Double> = txDao.getTotalExpense()

    fun getTopExpenseCategories(): Flow<List<CategorySum>> =
        txDao.getTopExpenseCategories()

    fun getMonthlyExpensesByCategory(yearMonth: String): Flow<List<CategorySum>> =
        txDao.getMonthlyExpensesByCategory(yearMonth)

    fun getYearlyExpensesByCategory(year: String): Flow<List<CategorySum>> =
        txDao.getYearlyExpensesByCategory(year)

    /**
     * ثبت تراکنش (درآمد/هزینه/پس‌انداز) با بررسی موجودی
     */
    suspend fun registerTransaction(
        accountId: Long,
        type: String,
        category: String,
        amount: Double,
        place: String,
        description: String
    ): Result<Long> {
        return try {
            val account = accountDao.getById(accountId)
                ?: return Result.failure(Exception("حساب پیدا نشد"))

            val currentBalance = account.balance
            val newBalance = when (type) {
                "درآمد" -> currentBalance + amount
                "هزینه", "پس‌انداز" -> {
                    if (currentBalance < amount) {
                        return Result.failure(Exception("موجودی کافی نیست"))
                    }
                    currentBalance - amount
                }
                else -> currentBalance
            }

            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date())

            val txId = txDao.insert(
                Transaction(
                    accountId = accountId,
                    date = date,
                    type = type,
                    category = category,
                    amount = amount,
                    place = place,
                    description = description,
                    balanceAfter = newBalance
                )
            )

            accountDao.updateBalance(accountId, newBalance)
            Result.success(txId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * انتقال بین دو حساب
     */
    suspend fun transferBetweenAccounts(
        sourceAccountId: Long,
        destinationAccountId: Long,
        amount: Double,
        description: String = ""
    ): Result<Long> {
        return try {
            if (sourceAccountId == destinationAccountId) {
                return Result.failure(Exception("حساب مبدأ و مقصد یکسان است"))
            }
            if (amount <= 0) {
                return Result.failure(Exception("مبلغ باید بزرگ‌تر از صفر باشد"))
            }

            val source = accountDao.getById(sourceAccountId)
                ?: return Result.failure(Exception("حساب مبدأ پیدا نشد"))
            val dest = accountDao.getById(destinationAccountId)
                ?: return Result.failure(Exception("حساب مقصد پیدا نشد"))

            if (source.balance < amount) {
                return Result.failure(Exception("موجودی کافی نیست"))
            }

            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date())

            val newSourceBalance = source.balance - amount
            val newDestBalance = dest.balance + amount

            // تراکنش مبدأ
            txDao.insert(
                Transaction(
                    accountId = sourceAccountId,
                    date = date,
                    type = "انتقال",
                    category = "انتقال",
                    amount = amount,
                    description = description,
                    balanceAfter = newSourceBalance
                )
            )

            // تراکنش مقصد
            txDao.insert(
                Transaction(
                    accountId = destinationAccountId,
                    date = date,
                    type = "انتقال",
                    category = "انتقال",
                    amount = amount,
                    description = description,
                    balanceAfter = newDestBalance
                )
            )

            accountDao.updateBalance(sourceAccountId, newSourceBalance)
            accountDao.updateBalance(destinationAccountId, newDestBalance)

            Result.success(1L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * آپدیت تراکنش «موجودی اولیه» وقتی کاربر موجودی کارت رو ویرایش می‌کنه
     * - اگه تراکنش موجودی اولیه وجود داشت → آپدیتش کن
     * - اگه وجود نداشت → یکی بساز
     */
    suspend fun updateInitialBalanceTransaction(
        accountId: Long,
        newBalance: Double,
        oldBalance: Double,
        bankName: String,
        cardName: String
    ): Result<Unit> {
        return try {
            val account = accountDao.getById(accountId)
                ?: return Result.failure(Exception("حساب پیدا نشد"))

            // ۱. آپدیت موجودی کارت
            accountDao.updateBalance(accountId, newBalance)

            // ۲. پیدا کردن تراکنش «موجودی اولیه» این کارت
            val allTransactions = txDao.getAllOnce()
            val initialTx = allTransactions.find {
                it.accountId == accountId && it.category == "موجودی اولیه"
            }

            if (initialTx != null) {
                // آپدیت تراکنش موجود
                txDao.update(
                    initialTx.copy(
                        amount = newBalance,
                        balanceAfter = newBalance,
                        place = bankName,
                        description = "موجودی اولیه کارت «$cardName»"
                    )
                )
            } else if (newBalance > 0) {
                // ساخت تراکنش جدید
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date())
                txDao.insert(
                    Transaction(
                        accountId = accountId,
                        date = date,
                        type = "درآمد",
                        category = "موجودی اولیه",
                        amount = newBalance,
                        place = bankName,
                        description = "موجودی اولیه کارت «$cardName»",
                        balanceAfter = newBalance
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * چک می‌کنه که آیا حساب تراکنش داره یا نه
     * برای جلوگیری از حذف کارتی که تراکنش داره
     */
    suspend fun hasTransactions(accountId: Long): Boolean {
        return try {
            val all = txDao.getAllOnce()
            // تراکنش‌های «موجودی اولیه» رو نادیده بگیر
            all.any {
                it.accountId == accountId && it.category != "موجودی اولیه"
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * مجموع موجودی‌های اولیه (تراکنش‌های با category = "موجودی اولیه")
     */
    fun getTotalInitialBalance(): Flow<Double> {
        return txDao.getAll().map { list ->
            list.filter { it.category == "موجودی اولیه" }
                .sumOf { it.amount }
        }
    }

    // =====================================================
    // SMS Queue
    // =====================================================

    fun getPendingSms(): Flow<List<SmsQueue>> = db.smsDao().getPending()

    fun getPendingSmsCount(): Flow<Int> = db.smsDao().getPendingCount()

    suspend fun getSmsById(id: Long): SmsQueue? = db.smsDao().getSmsById(id)

    suspend fun approveSms(smsId: Long, accountId: Long): Result<Long> {
        return try {
            val sms = db.smsDao().getSmsById(smsId)
                ?: return Result.failure(Exception("پیامک پیدا نشد"))

            val amount = sms.parsedAmount
                ?: return Result.failure(Exception("مبلغ پیامک نامشخص است"))
            val type = sms.parsedType
                ?: return Result.failure(Exception("نوع پیامک نامشخص است"))

            // ثبت تراکنش
            val txResult = registerTransaction(
                accountId = accountId,
                type = type,
                category = "پیامک بانکی",
                amount = amount,
                place = sms.sender,
                description = sms.body.take(100)
            )

            if (txResult.isSuccess) {
                // آپدیت وضعیت پیامک
                db.smsDao().updateStatus(smsId, "approved", accountId)
                Result.success(txResult.getOrThrow())
            } else {
                Result.failure(txResult.exceptionOrNull() ?: Exception("خطا در ثبت"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSms(smsId: Long) = db.smsDao().deleteSms(smsId)

    // =====================================================
    // SMS Mapping (یادگیری)
    // =====================================================

    fun getAllSmsMappings(): Flow<List<SmsMapping>> = db.smsDao().getAllMappings()

    suspend fun addSmsMapping(mapping: SmsMapping): Long {
        return db.smsDao().insertMapping(mapping)
    }

    suspend fun deleteSmsMapping(id: Long) = db.smsDao().deleteMapping(id)

    suspend fun deleteAllSmsMappings() = db.smsDao().deleteAllMappings()

    suspend fun getMappingByAccountNumber(accountNumber: String): SmsMapping? {
        return db.smsDao().getMappingByAccountNumber(accountNumber)
    }

    /**
     * وقتی کاربر پیامک رو به یه حساب وصل می‌کنه،
     * یه رکورد توی SmsMapping ذخیره می‌کنه برای یادگیری
     */
    suspend fun learnSmsMapping(
        sender: String,
        cardLast4: String,
        accountId: Long,
        bankName: String = "",
        accountNumber: String = ""
    ): Long {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date())
        return db.smsDao().insertMapping(
            SmsMapping(
                accountNumber = accountNumber.ifEmpty { sender },
                cardLast4 = cardLast4,
                accountId = accountId,
                bankName = bankName,
                autoApprove = true,
                createdAt = now
            )
        )
    }

    // =====================================================
    // ویرایش تراکنش (برای ReportsScreen)
    // =====================================================

    suspend fun getTransactionById(id: Long): Transaction? {
        return txDao.getById(id)
    }

    /**
     * ویرایش تراکنش: نوع، دسته، مبلغ، کارت
     * ⚠️ موجودی کارت‌ها هم آپدیت می‌شه (تفاوت رو اعمال می‌کنه)
     */
    suspend fun updateTransactionFields(
        transactionId: Long,
        newType: String,
        newCategory: String,
        newAmount: Double,
        newAccountId: Long
    ): Result<Unit> {
        return try {
            val oldTx = txDao.getById(transactionId)
                ?: return Result.failure(Exception("تراکنش پیدا نشد"))

            val oldAmount = oldTx.amount
            val oldType = oldTx.type
            val oldAccountId = oldTx.accountId

            // برگردوندن اثر تراکنش قدیمی روی موجودی
            val oldAccount = accountDao.getById(oldAccountId)
            if (oldAccount != null) {
                val revertedBalance = when (oldType) {
                    "درآمد" -> oldAccount.balance - oldAmount
                    "هزینه", "پس‌انداز" -> oldAccount.balance + oldAmount
                    else -> oldAccount.balance
                }
                accountDao.updateBalance(oldAccountId, revertedBalance)
            }

            // اعمال تراکنش جدید
            val newAccount = accountDao.getById(newAccountId)
                ?: return Result.failure(Exception("حساب جدید پیدا نشد"))

            val currentNewBalance = newAccount.balance
            val newBalance = when (newType) {
                "درآمد" -> currentNewBalance + newAmount
                "هزینه", "پس‌انداز" -> {
                    if (currentNewBalance < newAmount) {
                        // برگردون اثر قبلی
                        if (oldAccount != null) {
                            accountDao.updateBalance(oldAccountId, oldAccount.balance)
                        }
                        return Result.failure(Exception("موجودی کافی نیست"))
                    }
                    currentNewBalance - newAmount
                }
                else -> currentNewBalance
            }

            accountDao.updateBalance(newAccountId, newBalance)

            // آپدیت تراکنش
            txDao.update(
                oldTx.copy(
                    accountId = newAccountId,
                    type = newType,
                    category = newCategory,
                    amount = newAmount,
                    balanceAfter = newBalance
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransactionById(id: Long): Result<Unit> {
        return try {
            val tx = txDao.getById(id)
                ?: return Result.failure(Exception("تراکنش پیدا نشد"))

            // برگردوندن اثر روی موجودی
            val account = accountDao.getById(tx.accountId)
            if (account != null) {
                val revertedBalance = when (tx.type) {
                    "درآمد" -> account.balance - tx.amount
                    "هزینه", "پس‌انداز" -> account.balance + tx.amount
                    else -> account.balance
                }
                accountDao.updateBalance(tx.accountId, revertedBalance)
            }

            txDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =====================================================
    // ویرایش پیامک (برای SmsScreen)
    // =====================================================

    /**
     * آپدیت کامل پیامک (مبلغ، نوع، دسته، کارت)
     */
    suspend fun updateSmsFull(
        smsId: Long,
        newAmount: Double?,
        newType: String?,
        newCategory: String?,
        newAccountId: Long?
    ): Result<Unit> {
        return try {
            val sms = db.smsDao().getSmsById(smsId)
                ?: return Result.failure(Exception("پیامک پیدا نشد"))

            db.smsDao().updateSms(
                sms.copy(
                    parsedAmount = newAmount ?: sms.parsedAmount,
                    parsedType = newType ?: sms.parsedType,
                    accountId = newAccountId ?: sms.accountId
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تأیید پیامک با مبلغ/نوع/دسته/کارت **ویرایش‌شده**
     */
    suspend fun approveSmsEdited(
        smsId: Long,
        accountId: Long,
        category: String,
        amount: Double,
        type: String
    ): Result<Long> {
        return try {
            val sms = db.smsDao().getSmsById(smsId)
                ?: return Result.failure(Exception("پیامک پیدا نشد"))

            // ثبت تراکنش با مقادیر ویرایش‌شده
            val txResult = registerTransaction(
                accountId = accountId,
                type = type,
                category = category,
                amount = amount,
                place = sms.sender,
                description = sms.body.take(100)
            )

            if (txResult.isSuccess) {
                db.smsDao().updateStatus(smsId, "approved", accountId)
                Result.success(txResult.getOrThrow())
            } else {
                Result.failure(txResult.exceptionOrNull() ?: Exception("خطا در ثبت"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =====================================================
    // پشتیبان‌گیری (Backup & Restore)
    // =====================================================

    suspend fun getAllAccountsOnce(): List<Account> = accountDao.getAllOnce()

    suspend fun getAllTransactionsOnce(): List<Transaction> = txDao.getAllOnce()

    /**
     * بازیابی از فایل پشتیبان
     * ۱. همه‌ی داده‌های قبلی حذف می‌شن
     * ۲. داده‌های جدید اضافه می‌شن
     */
    suspend fun restoreBackup(
        accounts: List<Account>,
        transactions: List<Transaction>
    ): Result<Unit> {
        return try {
            // ۱. حذف همه
            txDao.deleteAll()
            accountDao.deleteAll()

            // ۲. اضافه کردن حساب‌ها
            accounts.forEach { account ->
                accountDao.insert(account.copy(id = 0))
            }

            // ۳. اضافه کردن تراکنش‌ها
            transactions.forEach { tx ->
                txDao.insert(tx.copy(id = 0))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * پاک کردن همه‌ی داده‌ها
     */
    suspend fun deleteAllData(): Result<Unit> {
        return try {
            txDao.deleteAll()
            accountDao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}