package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a social transaction request as specified in the
 * SOCIAL PAY — Smart Transaction Engine specification.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
  @PrimaryKey
  val id: String, // Unique ID e.g. "SP-REQ-849201"
  val type: String, // "BUY", "SELL", "INVOICE", "PAYMENT_REQUEST"
  val title: String,
  val description: String,
  val amount: Double,
  val currency: String, // "SAR", "USD", "USDT", "AED", "EGP", "EUR"
  val senderName: String,
  val senderContact: String,
  val recipientName: String,
  val recipientContact: String,
  val shareUrl: String, // Secure sharing link: "https://pay.socialpay.io/req/SP-REQ-849201"
  val qrCodeData: String,
  val status: String, // "PENDING", "ESCROW_LOCKED", "CONFIRMED", "COMPLETED", "CANCELLED", "DISPUTED"
  val channel: String, // "WHATSAPP", "TELEGRAM", "INSTAGRAM", "FACEBOOK", "DIRECT_LINK", "WEB_EMBED"
  val paymentProvider: String, // "SMART_ESCROW", "SANDBOX_CARD", "STC_PAY", "VODAFONE_CASH", "AL_RAJHI"
  val feeUsd: Double = 0.80, // Flat fee $0.80
  val idempotencyKey: String, // Prevents duplicate execution
  val createdAt: Long = System.currentTimeMillis(),
  val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L), // 24 hours expiry
  val auditLog: String = "تم إنشاء الطلب بنجاح | حجز الأمانة في العقد الذكي"
)

@Entity(tableName = "contacts")
data class ContactEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  val phoneOrHandle: String,
  val preferredChannel: String, // "WHATSAPP", "TELEGRAM", "INSTAGRAM"
  val trustScore: Int = 98,
  val totalTransactionsCount: Int = 12,
  val avatarInitials: String = "SP",
  val isVerified: Boolean = true
)

@Entity(tableName = "social_notifications")
data class SocialNotificationEntity(
  @PrimaryKey
  val id: String,
  val title: String,
  val message: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isRead: Boolean = false,
  val transactionId: String? = null,
  val type: String = "PAYMENT_CONFIRMED" // "PAYMENT_CONFIRMED", "ESCROW_LOCKED", "INVOICE_SENT", "SECURITY_ALERT"
)
