package com.example

import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.audit.AuditEngine
import com.example.core.identity.IdentityEngine
import com.example.core.identity.KycTier
import com.example.core.identity.PayoutDestination
import com.example.core.identity.PayoutType
import com.example.core.identity.UserIdentity
import com.example.core.notification.NotificationEngine
import com.example.core.notification.SocialPayEvent
import com.example.core.payment.PaymentAdapterRegistry
import com.example.core.payment.PaymentExecutionResult
import com.example.core.payment.SandboxPaymentAdapter
import com.example.core.payment.WebhookVerificationResult
import com.example.core.security.SecurityEngine
import com.example.core.social.SocialIntegrationEngine
import com.example.core.transaction.PaymentAttempt
import com.example.core.transaction.TransactionEngine
import com.example.core.transaction.TransactionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SocialPayCoreTest {

  private lateinit var transactionEngine: TransactionEngine
  private lateinit var sandboxAdapter: SandboxPaymentAdapter
  private lateinit var auditEngine: AuditEngine

  @Before
  fun setUp() {
    transactionEngine = TransactionEngine()
    sandboxAdapter = SandboxPaymentAdapter()
    auditEngine = AuditEngine()
  }

  // =========================================================================
  // 1. HAPPY PATH: End-to-End lifecycle from creation to receipt
  // =========================================================================
  @Test
  fun testCompleteTransactionLifecycle() = runTest {
    // 1. Create Payment Intent
    val intentResult = transactionEngine.createPaymentIntent(
      typePrefix = "SP-REQ",
      amount = 1875.0,
      assetId = "SAR_FIAT",
      senderUserId = "USR-001",
      senderName = "خالد العناد",
      recipientUserId = "USR-002",
      recipientName = "عبدالرحمن الشمري",
      idempotencyKey = "IDEM-" + UUID.randomUUID()
    )
    assertTrue("Intent creation must succeed", intentResult.isSuccess)
    val intent = intentResult.getOrThrow()
    assertEquals(TransactionState.CREATED, intent.state)
    assertEquals(0.80, intent.feeUsd, 0.001)

    // Audit initial creation
    auditEngine.record(intent.transactionId, "INTENT_CREATED", intent.senderUserId, "تم إنشاء الطلب بنجاح")

    // 2. Transition to PENDING
    val pendingResult = transactionEngine.transitionState(intent.intentId, TransactionState.PENDING)
    assertTrue(pendingResult.isSuccess)
    assertEquals(TransactionState.PENDING, pendingResult.getOrThrow().state)
    auditEngine.record(intent.transactionId, "INTENT_PENDING", "SYSTEM", "طلب الدفع قيد الانتظار للمصادقة")

    // 3. Execute Payment through Sandbox Adapter
    val attempt = PaymentAttempt(
      attemptId = "ATT-1",
      intentId = intent.intentId,
      providerId = sandboxAdapter.adapterId,
      isSuccessful = true
    )
    val payResult = sandboxAdapter.executePayment(intent, attempt)
    assertTrue("Payment must succeed in default sandbox mode", payResult is PaymentExecutionResult.Success)
    val successResult = payResult as PaymentExecutionResult.Success

    // 4. Explicit User Authorization
    val authResult = transactionEngine.authorizeByUser(intent.intentId, intent.amount, intent.recipientName)
    assertTrue(authResult.isSuccess)
    assertEquals(TransactionState.AUTHORIZED, authResult.getOrThrow().state)
    auditEngine.record(intent.transactionId, "PAYMENT_AUTHORIZED", "USER", "تفويض صريح من المستخدم")

    // 5. Complete Settlement & Generate Verifiable Receipt
    val completeResult = transactionEngine.transitionState(intent.intentId, TransactionState.SETTLED)
    assertTrue(completeResult.isSuccess)
    assertEquals(TransactionState.SETTLED, completeResult.getOrThrow().state)

    val receipt = transactionEngine.generateReceipt(intent, successResult.providerReference, "مصرف الراجحي")
    assertNotNull(receipt)
    assertEquals(intent.amount, receipt.amountPaid, 0.001)
    assertTrue("Receipt signature must be 64-char SHA256", receipt.digitalSignatureHash.length == 64)

    // Verify Audit Chain Integrity
    val (isChainValid, chainError) = auditEngine.verifyChainIntegrity()
    assertTrue(chainError ?: "Audit chain should be valid", isChainValid)
  }

  // =========================================================================
  // SCENARIO 1: Connection Failure & Retry
  // =========================================================================
  @Test
  fun testConnectionFailureHandling() = runTest {
    sandboxAdapter.simulateConnectionFailure = true

    val intent = transactionEngine.createPaymentIntent(
      amount = 100.0,
      assetId = "USDT_TRC20",
      senderUserId = "USR-001",
      senderName = "سالم",
      recipientUserId = "USR-002",
      recipientName = "أحمد",
      idempotencyKey = "IDEM-FAIL-1"
    ).getOrThrow()

    val attempt = PaymentAttempt("ATT-FAIL", intent.intentId, sandboxAdapter.adapterId, isSuccessful = false)
    val result = sandboxAdapter.executePayment(intent, attempt)

    assertTrue("Result must be failure on connection timeout", result is PaymentExecutionResult.Failure)
    val failure = result as PaymentExecutionResult.Failure
    assertEquals("CONNECTION_TIMEOUT", failure.errorCode)
    assertTrue("Should flag that retry is allowed", failure.canRetry)

    // Now restore connection and retry
    sandboxAdapter.simulateConnectionFailure = false
    val retryResult = sandboxAdapter.executePayment(intent, attempt)
    assertTrue("Retry after reconnect must succeed", retryResult is PaymentExecutionResult.Success)
  }

  // =========================================================================
  // SCENARIO 2: Duplicate Payment Request / Idempotency Replay Prevention
  // =========================================================================
  @Test
  fun testDuplicatePaymentIdempotencyPrevention() {
    val sharedIdempotencyKey = "IDEM-REPLAY-TOKEN-998877"

    val intent1 = transactionEngine.createPaymentIntent(
      amount = 250.0,
      assetId = "USD_FIAT",
      senderUserId = "USR-10",
      senderName = "علي",
      recipientUserId = "USR-20",
      recipientName = "خالد",
      idempotencyKey = sharedIdempotencyKey
    ).getOrThrow()

    // Second call with identical idempotencyKey
    val intent2 = transactionEngine.createPaymentIntent(
      amount = 250.0,
      assetId = "USD_FIAT",
      senderUserId = "USR-10",
      senderName = "علي",
      recipientUserId = "USR-20",
      recipientName = "خالد",
      idempotencyKey = sharedIdempotencyKey
    ).getOrThrow()

    assertEquals("Both calls must return the identical intent ID", intent1.intentId, intent2.intentId)
    assertEquals("Must not create duplicate transaction IDs", intent1.transactionId, intent2.transactionId)
  }

  // =========================================================================
  // SCENARIO 3: Delayed Webhook Arrival (Out-of-order handling)
  // =========================================================================
  @Test
  fun testDelayedWebhookArrival() = runTest {
    val intent = transactionEngine.createPaymentIntent(
      amount = 500.0,
      assetId = "SAR_FIAT",
      senderUserId = "USR-A",
      senderName = "سارة",
      recipientUserId = "USR-B",
      recipientName = "منى",
      idempotencyKey = "IDEM-DELAY-1"
    ).getOrThrow()

    val webhookSecret = "secret_key_prod_8899"
    val webhookPayload = "txId=${intent.transactionId}&amount=500.0&status=CONFIRMED"
    val validSignature = SandboxPaymentAdapter.computeHmacSha256(webhookPayload, webhookSecret)

    // Simulate webhook arriving while transaction is in PENDING state
    transactionEngine.transitionState(intent.intentId, TransactionState.PENDING)

    val verification = sandboxAdapter.verifyWebhook(webhookPayload, validSignature, webhookSecret)
    assertTrue("Delayed webhook verification must succeed", verification is WebhookVerificationResult.Valid)

    // Explicit user approval then settle smoothly
    transactionEngine.authorizeByUser(intent.intentId, intent.amount, intent.recipientName)
    val transitioned = transactionEngine.transitionState(intent.intentId, TransactionState.SETTLED)
    assertTrue(transitioned.isSuccess)
    assertEquals(TransactionState.SETTLED, transitioned.getOrThrow().state)
  }

  // =========================================================================
  // SCENARIO 4: Amount Mismatch Detection
  // =========================================================================
  @Test
  fun testAmountMismatchDetection() = runTest {
    val webhookSecret = "whsec_secure_key"
    val payload = "txId=SP-REQ-999&amount=50.0&status=CONFIRMED"
    val signature = SandboxPaymentAdapter.computeHmacSha256(payload, webhookSecret)

    sandboxAdapter.simulateAmountMismatch = true
    val result = sandboxAdapter.verifyWebhook(payload, signature, webhookSecret)

    assertTrue("Mismatch must be rejected", result is WebhookVerificationResult.Invalid)
    val invalid = result as WebhookVerificationResult.Invalid
    assertTrue("Error should mention amount mismatch", invalid.reason.contains("عدم تطابق في المبلغ"))
  }

  // =========================================================================
  // SCENARIO 5: Expired Link Rejection
  // =========================================================================
  @Test
  fun testExpiredLinkHandling() = runTest {
    // Intent with expiresAt in the past
    val expiredIntent = transactionEngine.createPaymentIntent(
      amount = 75.0,
      assetId = "USD_FIAT",
      senderUserId = "U1",
      senderName = "بدر",
      recipientUserId = "U2",
      recipientName = "طارق",
      idempotencyKey = "IDEM-EXP-1"
    ).getOrThrow().copy(expiresAt = System.currentTimeMillis() - 1000L)

    val attempt = PaymentAttempt("ATT-EXP", expiredIntent.intentId, sandboxAdapter.adapterId, isSuccessful = false)
    val result = sandboxAdapter.executePayment(expiredIntent, attempt)

    assertTrue("Expired link payment execution must fail", result is PaymentExecutionResult.Failure)
    val failure = result as PaymentExecutionResult.Failure
    assertEquals("LINK_EXPIRED", failure.errorCode)
  }

  // =========================================================================
  // SCENARIO 6: Pending Escrow / Asynchronous Confirmation
  // =========================================================================
  @Test
  fun testPendingPaymentTransition() = runTest {
    sandboxAdapter.simulatePendingDelay = true

    val intent = transactionEngine.createPaymentIntent(
      amount = 300.0,
      assetId = "SAR_FIAT",
      senderUserId = "U1",
      senderName = "ناصر",
      recipientUserId = "U2",
      recipientName = "فهد",
      idempotencyKey = "IDEM-PENDING-1"
    ).getOrThrow()

    val attempt = PaymentAttempt("ATT-PND", intent.intentId, sandboxAdapter.adapterId, isSuccessful = true)
    val result = sandboxAdapter.executePayment(intent, attempt)

    assertTrue("Result must be Pending", result is PaymentExecutionResult.Pending)
    val pending = result as PaymentExecutionResult.Pending
    assertTrue(pending.providerReference.startsWith("REF-PENDING-"))
  }

  // =========================================================================
  // SCENARIO 7: Safe Transaction Cancellation
  // =========================================================================
  @Test
  fun testTransactionCancellation() {
    val intent = transactionEngine.createPaymentIntent(
      amount = 150.0,
      assetId = "AED_FIAT",
      senderUserId = "U1",
      senderName = "زايد",
      recipientUserId = "U2",
      recipientName = "محمد",
      idempotencyKey = "IDEM-CNCL-1"
    ).getOrThrow()

    // Transition to CANCELLED
    val cancelResult = transactionEngine.transitionState(intent.intentId, TransactionState.CANCELLED, "طلب الإلغاء من المستخدم")
    assertTrue(cancelResult.isSuccess)
    assertEquals(TransactionState.CANCELLED, cancelResult.getOrThrow().state)

    // Cannot transition further from terminal state CANCELLED
    val illegalTransition = transactionEngine.transitionState(intent.intentId, TransactionState.SETTLED)
    assertFalse("Transition from terminal state must be rejected", illegalTransition.isSuccess)
  }

  // =========================================================================
  // SCENARIO 8: Verification & Signature Failure (Tampering Detection)
  // =========================================================================
  @Test
  fun testSignatureAndVerificationFailure() = runTest {
    val webhookSecret = "whsec_genuine_secret"
    val genuinePayload = "txId=SP-REQ-123456&amount=1000.0&status=PAID"
    val fakeSignature = "bad_signature_0000000000000000000000000000000000000000000000000000"

    val verification = sandboxAdapter.verifyWebhook(genuinePayload, fakeSignature, webhookSecret)
    assertTrue("Fake signature must fail verification", verification is WebhookVerificationResult.Invalid)

    // Also test constant-time SecurityEngine signature verification
    val isSigValid = SecurityEngine.verifySignature(genuinePayload, fakeSignature, webhookSecret)
    assertFalse("SecurityEngine must reject forged signature", isSigValid)
  }

  // =========================================================================
  // MULTI-ASSET & NON-CUSTODIAL SAFETY CHECKS
  // =========================================================================
  @Test
  fun testDigitalAssetNetworkDistinction() {
    val usdtTron = DigitalAssetEngine.findAssetById("USDT_TRC20")
    val usdtEth = DigitalAssetEngine.findAssetById("USDT_ERC20")
    val usdtSol = DigitalAssetEngine.findAssetById("USDT_SOL")

    assertNotNull(usdtTron)
    assertNotNull(usdtEth)
    assertNotNull(usdtSol)

    assertEquals(BlockchainNetwork.TRON_TRC20, usdtTron?.network)
    assertEquals(BlockchainNetwork.ETHEREUM_ERC20, usdtEth?.network)
    assertEquals(BlockchainNetwork.SOLANA, usdtSol?.network)

    // Address format validations
    val (isTronValid, _) = DigitalAssetEngine.validateAddressForNetwork("TYDzsYUEpvnYmQk4zGP9sWWcTEd2MiAtW6", BlockchainNetwork.TRON_TRC20)
    assertTrue("Valid Tron address must pass", isTronValid)

    val (isBadTron, _) = DigitalAssetEngine.validateAddressForNetwork("0x1234567890abcdef1234567890abcdef12345678", BlockchainNetwork.TRON_TRC20)
    assertFalse("EVM address must be rejected on TRON network", isBadTron)
  }

  // =========================================================================
  // ANTI-TRIANGULATION & ANTI-PHISHING DEFENSE CHECKS
  // =========================================================================
  @Test
  fun testAntiTriangulationAndAntiPhishing() {
    // 1. Phishing scan
    val cleanText = "السلام عليكم، سأقوم بسداد الفاتورة الآن."
    val phishingText = "تواصل معي على تيليجرام خاص برة المنصة واضغط على رابط التفعيل bit.ly/hack"

    val cleanScan = SecurityEngine.scanMessageContent(cleanText)
    assertTrue(cleanScan.isSafe)

    val badScan = SecurityEngine.scanMessageContent(phishingText)
    assertFalse(badScan.isSafe)
    assertNotNull(badScan.warningAr)

    // 2. Anti-triangulation check
    val matched = SecurityEngine.verifyBeneficiaryTriangulation("خالد محمد العناد", "خالد محمد العناد")
    assertTrue(matched.isMatch)
    assertEquals("NONE", matched.riskLevel)

    val mismatch = SecurityEngine.verifyBeneficiaryTriangulation("أحمد عبدالله السارق", "خالد محمد العناد")
    assertFalse(mismatch.isMatch)
    assertEquals("CRITICAL", mismatch.riskLevel)
  }

  // =========================================================================
  // AUDIT ENGINE TAMPER-EVIDENT HASH CHAIN
  // =========================================================================
  @Test
  fun testAuditEngineIntegrityAndTamperEvidence() {
    auditEngine.record("TX-1", "CREATED", "USR-1", "Initial request")
    auditEngine.record("TX-1", "LOCKED", "SYSTEM", "Escrow deposit confirmed")
    auditEngine.record("TX-1", "CONFIRMED", "BANK", "Bank wire arrived")

    val (isValidInitial, _) = auditEngine.verifyChainIntegrity()
    assertTrue("Fresh chain must be intact", isValidInitial)

    // Verify Notification Dispatch
    val notif = NotificationEngine.dispatch(SocialPayEvent.PaymentConfirmed("TX-1", "REF-8899", 500.0, "SAR"))
    assertNotNull(notif)
    assertEquals("PAYMENT_CONFIRMED", notif.eventType)
  }
}
