package com.hkaboodvand.finacemobile.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hkaboodvand.finacemobile.data.db.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getByAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT 5")
    fun getRecentTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllOnce(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'درآمد'")
    fun getTotalIncome(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'هزینه'")
    fun getTotalExpense(): Flow<Double>

    @Query("""
        SELECT category, SUM(amount) as total
        FROM transactions
        WHERE type = 'هزینه'
        GROUP BY category
        ORDER BY total DESC
        LIMIT 5
    """)
    fun getTopExpenseCategories(): Flow<List<CategorySum>>

    @Query("""
        SELECT category, SUM(amount) as total
        FROM transactions
        WHERE type = 'هزینه'
        AND substr(date, 1, 7) = :yearMonth
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getMonthlyExpensesByCategory(yearMonth: String): Flow<List<CategorySum>>

    @Query("""
        SELECT category, SUM(amount) as total
        FROM transactions
        WHERE type = 'هزینه'
        AND substr(date, 1, 4) = :year
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getYearlyExpensesByCategory(year: String): Flow<List<CategorySum>>
}

data class CategorySum(
    val category: String,
    val total: Double
)