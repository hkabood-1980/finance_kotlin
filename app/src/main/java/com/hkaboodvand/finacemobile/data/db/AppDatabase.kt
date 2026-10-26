package com.hkaboodvand.finacemobile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hkaboodvand.finacemobile.data.db.dao.AccountDao
import com.hkaboodvand.finacemobile.data.db.dao.SmsDao
import com.hkaboodvand.finacemobile.data.db.dao.TransactionDao
import com.hkaboodvand.finacemobile.data.db.entity.Account
import com.hkaboodvand.finacemobile.data.db.entity.SmsMapping
import com.hkaboodvand.finacemobile.data.db.entity.SmsQueue
import com.hkaboodvand.finacemobile.data.db.entity.Transaction

@Database(
    entities = [
        Account::class,
        Transaction::class,
        SmsQueue::class,
        SmsMapping::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun smsDao(): SmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}