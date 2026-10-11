package com.hkaboodvand.finacemobile.data.repository

import com.hkaboodvand.finacemobile.data.db.AppDatabase
import com.hkaboodvand.finacemobile.data.db.dao.CategorySum
import com.hkaboodvand.finacemobile.data.db.entity.Account
import com.hkaboodvand.finacemobile.data.db.entity.Transaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
}