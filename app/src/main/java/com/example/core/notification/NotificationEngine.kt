package com.example.core.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SocialPayEvent {
  data class PaymentCreated(val transactionId: String, val amount: Double, val currency: String, val channel: String) : SocialPayEvent()
  data class EscrowLocked(val transactionId: String, val amount: Double, val currency: String) : SocialPayEvent()
  data class PaymentConfirmed(val transactionId: String, val referenceNumber: String, val amount: Double, val currency: String) : SocialPayEvent()
  data class ReceiptIssued(val receiptId: String, val transactionId: String) : SocialPayEvent()
  data class SecurityAlert(val title: String, val description: String, val severity: String = "HIGH") : SocialPayEvent()
  data class TransactionCancelled(val transactionId: String, val reason: String) : SocialPayEvent()
}

data class SystemNotification(
  val id: String,
  val title: String,
  val body: String,
  val eventType: String,
  val transactionId: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val isRead: Boolean = false
)

object NotificationEngine {

  private val _notifications = MutableStateFlow<List<SystemNotification>>(emptyList())
  val notifications: StateFlow<List<SystemNotification>> = _notifications.asStateFlow()

  fun dispatch(event: SocialPayEvent): SystemNotification {
    val id = "NOTIF-" + (10000..99999).random()
    val notif = when (event) {
      is SocialPayEvent.PaymentCreated -> SystemNotification(
        id = id,
        title = "طلب دفع جديد 📋",
        body = "تم إنشاء الطلب #${event.transactionId} بقيمة ${event.amount} ${event.currency} ومشاركته عبر ${event.channel}.",
        eventType = "PAYMENT_CREATED",
        transactionId = event.transactionId
      )
      is SocialPayEvent.EscrowLocked -> SystemNotification(
        id = id,
        title = "تم تفويض وحجز الطلب 🔒",
        body = "تم تفعيل حماية المعاملة #${event.transactionId} بمبلغ ${event.amount} ${event.currency} بانتظار إثبات التسوية.",
        eventType = "AUTHORIZATION_HELD",
        transactionId = event.transactionId
      )
      is SocialPayEvent.PaymentConfirmed -> SystemNotification(
        id = id,
        title = "تم تأكيد السداد بنجاح! 💸",
        body = "تم تأكيد سداد ${event.amount} ${event.currency} برقم مرجعي: ${event.referenceNumber}.",
        eventType = "PAYMENT_CONFIRMED",
        transactionId = event.transactionId
      )
      is SocialPayEvent.ReceiptIssued -> SystemNotification(
        id = id,
        title = "تم إصدار الإيصال الرقمي 🧾",
        body = "الإيصال #${event.receiptId} متاح الآن للتحميل والمشاركة مع التوقيع المشفر.",
        eventType = "RECEIPT_ISSUED",
        transactionId = event.transactionId
      )
      is SocialPayEvent.SecurityAlert -> SystemNotification(
        id = id,
        title = "⚠️ تنبيه أمني: ${event.title}",
        body = event.description,
        eventType = "SECURITY_ALERT"
      )
      is SocialPayEvent.TransactionCancelled -> SystemNotification(
        id = id,
        title = "تم إلغاء المعاملة 🛑",
        body = "تم إلغاء المعاملة #${event.transactionId}: ${event.reason}",
        eventType = "TRANSACTION_CANCELLED",
        transactionId = event.transactionId
      )
    }

    _notifications.value = listOf(notif) + _notifications.value
    return notif
  }

  fun markAsRead(id: String) {
    _notifications.value = _notifications.value.map {
      if (it.id == id) it.copy(isRead = true) else it
    }
  }

  fun clearAll() {
    _notifications.value = emptyList()
  }
}
