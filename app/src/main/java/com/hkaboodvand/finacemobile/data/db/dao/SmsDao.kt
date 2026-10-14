package com.hkaboodvand.finacemobile.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hkaboodvand.finacemobile.data.db.entity.SmsMapping
import com.hkaboodvand.finacemobile.data.db.entity.SmsQueue
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {

    // =====================================================
    // SMS Queue
    // =====================================================

    @Query("SELECT * FROM sms_queue WHERE status = 'pending' ORDER BY receivedAt DESC")
    fun getPending(): Flow<List<SmsQueue>>

    @Query("SELECT COUNT(*) FROM sms_queue WHERE status = 'pending'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM sms_queue WHERE id = :id")
    suspend fun getSmsById(id: Long): SmsQueue?

    @Insert
    suspend fun insertSms(sms: SmsQueue): Long

    @Query("UPDATE sms_queue SET status = :status, accountId = :accountId WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, accountId: Long?)

    @Update
    suspend fun updateSms(sms: SmsQueue)

    @Query("DELETE FROM sms_queue WHERE id = :id")
    suspend fun deleteSms(id: Long)

    // =====================================================
    // SMS Mapping
    // =====================================================

    @Query("SELECT * FROM sms_account_mapping WHERE accountNumber = :accountNumber AND autoApprove = 1 LIMIT 1")
    suspend fun getMappingByAccountNumber(accountNumber: String): SmsMapping?

    @Query("SELECT * FROM sms_account_mapping ORDER BY createdAt DESC")
    fun getAllMappings(): Flow<List<SmsMapping>>

    @Query("SELECT * FROM sms_account_mapping")
    suspend fun getAllMappingsOnce(): List<SmsMapping>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: SmsMapping): Long

    @Query("DELETE FROM sms_account_mapping WHERE id = :id")
    suspend fun deleteMapping(id: Long)

    @Query("DELETE FROM sms_account_mapping")
    suspend fun deleteAllMappings()

    @Query("UPDATE sms_account_mapping SET autoApprove = :autoApprove WHERE id = :id")
    suspend fun updateMappingAutoApprove(id: Long, autoApprove: Boolean)
}