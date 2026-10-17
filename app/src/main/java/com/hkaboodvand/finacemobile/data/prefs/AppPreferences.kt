package com.hkaboodvand.finacemobile.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "finance_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val SAVINGS_ACCOUNT_ID = longPreferencesKey("savings_account_id")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val LAST_SMS_TIMESTAMP = longPreferencesKey("last_sms_timestamp")
    }

    val savingsAccountId: Flow<Long?> = context.dataStore.data
        .map { prefs -> prefs[SAVINGS_ACCOUNT_ID] }

    suspend fun setSavingsAccount(accountId: Long) {
        context.dataStore.edit { prefs ->
            prefs[SAVINGS_ACCOUNT_ID] = accountId
        }
    }

    suspend fun clearSavingsAccount() {
        context.dataStore.edit { prefs ->
            prefs.remove(SAVINGS_ACCOUNT_ID)
        }
    }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[AUTO_BACKUP_ENABLED] ?: true }

    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_BACKUP_ENABLED] = enabled
        }
    }

    val lastBackupTime: Flow<Long?> = context.dataStore.data
        .map { prefs -> prefs[LAST_BACKUP_TIME] }

    suspend fun setLastBackupTime(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_BACKUP_TIME] = time
        }
    }

    // ─── جدید: آخرین timestamp پیامک ───
    val lastSmsTimestamp: Flow<Long?> = context.dataStore.data
        .map { prefs -> prefs[LAST_SMS_TIMESTAMP] }

    suspend fun setLastSmsTimestamp(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SMS_TIMESTAMP] = time
        }
    }
}