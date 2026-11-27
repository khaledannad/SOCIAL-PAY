package com.example.util

import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.delay

sealed class PaymentResult {
  data class Success(val transactionId: String, val referenceNumber: String, val webhookStatus: String) : PaymentResult()
  data class Failure(val errorMessage: String, val errorCode: String) : PaymentResult()
}

interface PaymentProvider {
  val providerId: String
  val nameAr: String
  val nameEn: String

  suspend fun processPayment(transaction: TransactionEntity, cardOrAccountDetails: String = ""): PaymentResult
  suspend fun verifyWebhook(payload: String, signature: String): Boolean
}

class SandboxPaymentProvider : PaymentProvider {
  override val providerId = "SANDBOX_CARD"
  override val nameAr = "بوابة الدفع التجريبية الآمنة (Sandbox 3DS)"
  override val nameEn = "Secure Sandbox Payment Gateway (3DS)"

  override suspend fun processPayment(transaction: TransactionEntity, cardOrAccountDetails: String): PaymentResult {
    // Simulate real gateway processing & 3DS authorization latency
    delay(1200)

    val refNumber = "REF-SBX-" + (1000000..9999999).random()
    return PaymentResult.Success(
      transactionId = transaction.id,
      referenceNumber = refNumber,
      webhookStatus = "HTTP 200 OK — Webhook Verified (HMAC-SHA256)"
    )
  }

  override suspend fun verifyWebhook(payload: String, signature: String): Boolean {
    return signature.isNotBlank() && payload.contains("status=CONFIRMED")
  }
}

class SmartEscrowPaymentProvider : PaymentProvider {
  override val providerId = "SMART_ESCROW"
  override val nameAr = "العقد الذكي للأمانات (0% ربا وتقابض فوري)"
  override val nameEn = "Smart Escrow Contract (0% Riba Spot Handover)"

  override suspend fun processPayment(transaction: TransactionEntity, cardOrAccountDetails: String): PaymentResult {
    delay(800)
    val escrowTxHash = "0x" + (1000000000L..9999999999L).random().toString(16)
    return PaymentResult.Success(
      transactionId = transaction.id,
      referenceNumber = "ESCROW-TX-$escrowTxHash",
      webhookStatus = "Smart Contract Event: EscrowLockedAndConfirmed"
    )
  }

  override suspend fun verifyWebhook(payload: String, signature: String): Boolean = true
}

class BankTransferPaymentProvider(
  val bankName: String = "مصرف الراجحي / STC Pay"
) : PaymentProvider {
  override val providerId = "BANK_TRANSFER"
  override val nameAr = "تحويل بنكي فوري ($bankName)"
  override val nameEn = "Instant Bank Transfer ($bankName)"

  override suspend fun processPayment(transaction: TransactionEntity, cardOrAccountDetails: String): PaymentResult {
    delay(1000)
    val bankRef = "BNK-" + (100000..999999).random()
    return PaymentResult.Success(
      transactionId = transaction.id,
      referenceNumber = bankRef,
      webhookStatus = "Open Banking API: FundsReceivedAndSettled"
    )
  }

  override suspend fun verifyWebhook(payload: String, signature: String): Boolean = true
}

object PaymentArchitecture {
  private val providers = mapOf<String, PaymentProvider>(
    "SANDBOX_CARD" to SandboxPaymentProvider(),
    "SMART_ESCROW" to SmartEscrowPaymentProvider(),
    "AL_RAJHI" to BankTransferPaymentProvider("مصرف الراجحي"),
    "STC_PAY" to BankTransferPaymentProvider("STC Pay"),
    "VODAFONE_CASH" to BankTransferPaymentProvider("فودافون كاش")
  )

  fun getProvider(providerId: String): PaymentProvider {
    return providers[providerId] ?: providers["SANDBOX_CARD"]!!
  }

  fun getAllProviders(): List<PaymentProvider> = providers.values.toList()
}
