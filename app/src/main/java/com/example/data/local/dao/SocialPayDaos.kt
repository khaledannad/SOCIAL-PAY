package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.SocialNotificationEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
  fun getAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE status = :status ORDER BY createdAt DESC")
  fun getTransactionsByStatus(status: String): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
  fun getTransactionById(id: String): Flow<TransactionEntity?>

  @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
  suspend fun getTransactionByIdDirect(id: String): TransactionEntity?

  @Query("SELECT * FROM transactions WHERE idempotencyKey = :key LIMIT 1")
  suspend fun getByIdempotencyKey(key: String): TransactionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(transaction: TransactionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(transactions: List<TransactionEntity>)

  @Update
  suspend fun updateTransaction(transaction: TransactionEntity)

  @Query("UPDATE transactions SET status = :status, auditLog = :auditLog WHERE id = :id")
  suspend fun updateStatus(id: String, status: String, auditLog: String)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteTransaction(id: String)

  @Query("SELECT COUNT(*) FROM transactions")
  fun getCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM transactions WHERE status IN ('SETTLED', 'CONFIRMED', 'COMPLETED')")
  fun getConfirmedCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM transactions WHERE status IN ('CREATED', 'PENDING', 'AUTHORIZED', 'PROCESSING', 'UNDER_REVIEW', 'ESCROW_LOCKED')")
  fun getPendingCount(): Flow<Int>
}

@Dao
interface ContactDao {
  @Query("SELECT * FROM contacts ORDER BY trustScore DESC")
  fun getAllContacts(): Flow<List<ContactEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContact(contact: ContactEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(contacts: List<ContactEntity>)

  @Query("DELETE FROM contacts WHERE id = :id")
  suspend fun deleteContact(id: String)
}

@Dao
interface NotificationDao {
  @Query("SELECT * FROM social_notifications ORDER BY timestamp DESC")
  fun getAllNotifications(): Flow<List<SocialNotificationEntity>>

  @Query("SELECT COUNT(*) FROM social_notifications WHERE isRead = 0")
  fun getUnreadCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(notification: SocialNotificationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(notifications: List<SocialNotificationEntity>)

  @Query("UPDATE social_notifications SET isRead = 1 WHERE id = :id")
  suspend fun markAsRead(id: String)

  @Query("UPDATE social_notifications SET isRead = 1")
  suspend fun markAllAsRead()

  @Query("DELETE FROM social_notifications")
  suspend fun clearAll()
}
