package com.example.data

import android.content.Context
import com.example.data.local.SocialPayDatabase
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.SocialNotificationEntity
import com.example.data.local.entity.TransactionEntity
import com.example.util.PaymentArchitecture
import com.example.util.PaymentResult
import com.example.util.SocialIntegrationEngine
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SocialPayRepository(
  context: Context,
  val database: SocialPayDatabase = SocialPayDatabase.getDatabase(context)
) {

  private val transactionDao = database.transactionDao()
  private val contactDao = database.contactDao()
  private val notificationDao = database.notificationDao()

  val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
  val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
  val allNotifications: Flow<List<SocialNotificationEntity>> = notificationDao.getAllNotifications()
  val unreadNotificationsCount: Flow<Int> = notificationDao.getUnreadCount()

  val totalTransactionsCount: Flow<Int> = transactionDao.getCount()
  val confirmedTransactionsCount: Flow<Int> = transactionDao.getConfirmedCount()
  val pendingTransactionsCount: Flow<Int> = transactionDao.getPendingCount()

  suspend fun createTransaction(
    type: String, // "BUY", "SELL", "INVOICE", "PAYMENT_REQUEST"
    title: String,
    description: String,
    amount: Double,
    currency: String,
    senderName: String,
    senderContact: String,
    recipientName: String,
    recipientContact: String,
    channel: String, // "WHATSAPP", "TELEGRAM", "INSTAGRAM", "FACEBOOK", "DIRECT_LINK"
    paymentProvider: String = "SMART_ESCROW",
    idempotencyKey: String = UUID.randomUUID().toString()
  ): TransactionEntity {
    // 1. Check idempotency to prevent duplicate transaction creation
    val existing = transactionDao.getByIdempotencyKey(idempotencyKey)
    if (existing != null) {
      return existing
    }

    val id = SocialIntegrationEngine.generateTransactionId(type)
    val shareUrl = SocialIntegrationEngine.BASE_SHARE_URL + id
    val qrCodeData = "socialpay://req/$id?amount=$amount&cur=$currency&channel=$channel"

    val newTxn = TransactionEntity(
      id = id,
      type = type,
      title = title,
      description = description,
      amount = amount,
      currency = currency,
      senderName = senderName,
      senderContact = senderContact,
      recipientName = recipientName,
      recipientContact = recipientContact,
      shareUrl = shareUrl,
      qrCodeData = qrCodeData,
      status = "PENDING",
      channel = channel,
      paymentProvider = paymentProvider,
      feeUsd = 0.80,
      idempotencyKey = idempotencyKey,
      createdAt = System.currentTimeMillis(),
      expiresAt = System.currentTimeMillis() + (48 * 3600 * 1000L),
      auditLog = "تم إنشاء الطلب بأمان عبر قناة $channel | تم توليد رابط المشاركة الآمن ورمز QR"
    )

    transactionDao.insertTransaction(newTxn)

    // Also add notification
    val notification = SocialNotificationEntity(
      id = "NOTIF-" + (10000..99999).random(),
      title = "تم إنشاء طلب دفع جديد 📋",
      message = "تم إنشاء الطلب $id بمبلغ $amount $currency ومشاركته عبر $channel.",
      timestamp = System.currentTimeMillis(),
      transactionId = id,
      type = if (type == "INVOICE") "INVOICE_SENT" else "PAYMENT_REQUEST_CREATED"
    )
    notificationDao.insertNotification(notification)

    return newTxn
  }

  suspend fun processAndConfirmPayment(
    transactionId: String,
    providerId: String = "SANDBOX_CARD"
  ): PaymentResult {
    val txn = transactionDao.getTransactionByIdDirect(transactionId)
      ?: return PaymentResult.Failure("المعاملة غير موجودة في النظام", "NOT_FOUND")

    val provider = PaymentArchitecture.getProvider(providerId)
    val result = provider.processPayment(txn)

    if (result is PaymentResult.Success) {
      val updatedAudit = "${txn.auditLog} | تم تأكيد التسوية عبر ${provider.nameAr} | رقم المرجع: ${result.referenceNumber} | ${result.webhookStatus}"
      transactionDao.updateStatus(txn.id, "SETTLED", updatedAudit)

      val notification = SocialNotificationEntity(
        id = "NOTIF-" + (10000..99999).random(),
        title = "تمت التسوية والتأكيد بنجاح! 💸",
        message = "اكتملت تسوية العملية ${txn.id} بمبلغ ${txn.amount} ${txn.currency}. ${result.webhookStatus}",
        timestamp = System.currentTimeMillis(),
        transactionId = txn.id,
        type = "PAYMENT_SETTLED"
      )
      notificationDao.insertNotification(notification)
    }

    return result
  }

  suspend fun cancelTransaction(transactionId: String) {
    val txn = transactionDao.getTransactionByIdDirect(transactionId) ?: return
    val updatedAudit = "${txn.auditLog} | تم إلغاء المعاملة بأمان وإعادة الرصيد"
    transactionDao.updateStatus(txn.id, "CANCELLED", updatedAudit)

    val notification = SocialNotificationEntity(
      id = "NOTIF-" + (10000..99999).random(),
      title = "تم إلغاء المعاملة ⚠️",
      message = "تم إلغاء الطلب ${txn.id} بناءً على رغبة المستخدم.",
      timestamp = System.currentTimeMillis(),
      transactionId = txn.id,
      type = "SECURITY_ALERT"
    )
    notificationDao.insertNotification(notification)
  }

  suspend fun addContact(name: String, handleOrPhone: String, channel: String) {
    val initials = name.split(" ")
      .mapNotNull { it.firstOrNull() }
      .take(2)
      .joinToString(".")
      .ifEmpty { "SP" }

    val newContact = ContactEntity(
      id = "CNT-" + (1000..9999).random(),
      name = name,
      phoneOrHandle = handleOrPhone,
      preferredChannel = channel,
      trustScore = 98,
      totalTransactionsCount = 1,
      avatarInitials = initials,
      isVerified = true
    )
    contactDao.insertContact(newContact)
  }

  suspend fun markNotificationAsRead(id: String) {
    notificationDao.markAsRead(id)
  }

  suspend fun markAllNotificationsAsRead() {
    notificationDao.markAllAsRead()
  }

  suspend fun clearAllNotifications() {
    notificationDao.clearAll()
  }
}
