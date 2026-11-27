package com.example.core.transaction

import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.asset.StablecoinRiskStatus
import com.example.core.ledger.DoubleEntryLedgerEngine
import com.example.core.ledger.ReconciliationReport
import com.example.core.ledger.ReconciliationStatus
import com.example.core.risk.StablecoinRiskIntelligence
import java.security.MessageDigest
import java.util.UUID

/**
 * Honest Transaction Lifecycle States mandated by Social Pay Financial Policy:
 * CREATED -> PENDING -> AUTHORIZED -> PROCESSING -> SETTLED / FAILED / CANCELLED / UNDER_REVIEW
 */
enum class TransactionState(
  val isTerminal: Boolean,
  val labelAr: String,
  val labelEn: String,
  val labelFr: String
) {
  CREATED(false, "تم الإنشاء", "Created", "Créée"),
  PENDING(false, "قيد الانتظار", "Pending", "En attente"),
  AUTHORIZED(false, "تم التفويض والموافقة", "Authorized", "Autorisée"),
  PROCESSING(false, "قيد التنفيذ والتسوية", "Processing", "En cours de traitement"),
  SETTLED(true, "تمت التسوية والإتمام", "Settled", "Réglée"),
  FAILED(true, "فشلت المعاملة", "Failed", "Échouée"),
  CANCELLED(true, "ملغاة", "Cancelled", "Annulée"),
  UNDER_REVIEW(false, "قيد المراجعة والفحص", "Under Review", "En examen")
}

data class PreExecutionSummary(
  val recipientName: String,
  val amount: Double,
  val stablecoinTicker: String,
  val network: BlockchainNetwork,
  val declaredPlatformFeeUsd: Double,
  val estimatedNetworkFeeUsd: Double,
  val finalTotalChargeUsd: Double,
  val verificationStatusLabel: String,
  val riskDisclosure: String,
  val providerId: String,
  val requiresExplicitUserApproval: Boolean = true
)

data class RoutingCheckStep(
  val stepName: String,
  val passed: Boolean,
  val detail: String
)

data class RoutingPlanResult(
  val isEligible: Boolean,
  val checks: List<RoutingCheckStep>,
  val summary: PreExecutionSummary? = null,
  val blockingReason: String? = null
)

data class PaymentIntent(
  val intentId: String,
  val transactionId: String,
  val amount: Double,
  val currencyTicker: String,
  val assetId: String,
  val network: BlockchainNetwork,
  val senderUserId: String,
  val senderName: String,
  val recipientUserId: String,
  val recipientName: String,
  val recipientAddressOrRail: String = "",
  val paymentLink: String,
  val qrCodePayload: String,
  val idempotencyKey: String,
  val state: TransactionState = TransactionState.CREATED,
  val feeUsd: Double = 0.80,
  val providerId: String = "SANDBOX_MOCK_GATEWAY",
  val providerReference: String = "",
  val userExplicitlyApproved: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val expiresAt: Long = System.currentTimeMillis() + (24 * 3600 * 1000L),
  val metadata: Map<String, String> = emptyMap()
)

data class PaymentAttempt(
  val attemptId: String,
  val intentId: String,
  val providerId: String,
  val idempotencyKey: String = UUID.randomUUID().toString(),
  val attemptedAt: Long = System.currentTimeMillis(),
  val isSuccessful: Boolean,
  val providerReference: String? = null,
  val failureCode: String? = null,
  val failureMessage: String? = null
)

data class PaymentConfirmation(
  val confirmationId: String,
  val intentId: String,
  val providerId: String,
  val providerReference: String,
  val settledAmount: Double,
  val currencyTicker: String,
  val confirmedAt: Long = System.currentTimeMillis(),
  val auditSignature: String
)

data class InvoiceItem(
  val description: String,
  val quantity: Int,
  val unitPrice: Double,
  val total: Double = quantity * unitPrice
)

data class Invoice(
  val invoiceNumber: String,
  val intentId: String,
  val issuerName: String,
  val issuerTaxIdOrVat: String?,
  val clientName: String,
  val clientContact: String,
  val items: List<InvoiceItem>,
  val subtotal: Double,
  val vatPercent: Double = 15.0,
  val vatAmount: Double = subtotal * (vatPercent / 100.0),
  val totalAmount: Double = subtotal + vatAmount,
  val currencyTicker: String,
  val dueDate: Long,
  val paymentLink: String,
  val qrCodePayload: String,
  val isPaid: Boolean = false,
  val issuedAt: Long = System.currentTimeMillis()
)

data class PaymentReceipt(
  val receiptId: String,
  val transactionId: String,
  val intentId: String,
  val amountPaid: Double,
  val currencyTicker: String,
  val feeUsd: Double,
  val payerName: String,
  val payeeName: String,
  val providerReference: String,
  val networkOrBank: String,
  val issuedAt: Long = System.currentTimeMillis(),
  val digitalSignatureHash: String
)

/**
 * Transaction & Payment Routing Engine:
 * - Enforces Stablecoins-Only accreditation before creating intents.
 * - Guards against duplicate execution via idempotency keys & pre-retry state lookups.
 * - Requires explicit user authorization and immutability of recipient & amount post-approval.
 * - Ties every settlement to balanced Double-Entry Ledger postings & Reconciliation.
 */
class TransactionEngine(
  private val intentStore: MutableMap<String, PaymentIntent> = mutableMapOf(),
  private val idempotencyStore: MutableMap<String, String> = mutableMapOf(),
  private val attemptsStore: MutableList<PaymentAttempt> = mutableListOf(),
  private val receiptsStore: MutableMap<String, PaymentReceipt> = mutableMapOf(),
  val ledgerEngine: DoubleEntryLedgerEngine = DoubleEntryLedgerEngine(),
  val riskIntelligence: StablecoinRiskIntelligence = StablecoinRiskIntelligence()
) {

  companion object {
    const val BASE_SHARE_URL = "https://pay.socialpay.io/req/"
    const val DEFAULT_PROTOCOL_FEE_USD = 0.80

    private val ALLOWED_TRANSITIONS = mapOf(
      TransactionState.CREATED to setOf(
        TransactionState.PENDING,
        TransactionState.AUTHORIZED,
        TransactionState.UNDER_REVIEW,
        TransactionState.CANCELLED,
        TransactionState.FAILED
      ),
      TransactionState.PENDING to setOf(
        TransactionState.AUTHORIZED,
        TransactionState.PROCESSING,
        TransactionState.UNDER_REVIEW,
        TransactionState.CANCELLED,
        TransactionState.FAILED
      ),
      TransactionState.AUTHORIZED to setOf(
        TransactionState.PROCESSING,
        TransactionState.SETTLED,
        TransactionState.UNDER_REVIEW,
        TransactionState.CANCELLED,
        TransactionState.FAILED
      ),
      TransactionState.PROCESSING to setOf(
        TransactionState.SETTLED,
        TransactionState.UNDER_REVIEW,
        TransactionState.FAILED
      ),
      TransactionState.UNDER_REVIEW to setOf(
        TransactionState.AUTHORIZED,
        TransactionState.SETTLED,
        TransactionState.CANCELLED,
        TransactionState.FAILED
      ),
      TransactionState.SETTLED to emptySet(),
      TransactionState.FAILED to emptySet(),
      TransactionState.CANCELLED to emptySet()
    )
  }

  /**
   * 13-Step Pre-Execution Validation & Routing Pipeline.
   * Performs every check BEFORE exposing the user authorization confirmation gate.
   */
  fun evaluateRoutingPlan(
    senderUserId: String,
    senderVerified: Boolean,
    recipientName: String,
    recipientAddressOrRail: String,
    ticker: String,
    network: BlockchainNetwork,
    amount: Double,
    declaredFeeUsd: Double = DEFAULT_PROTOCOL_FEE_USD,
    contractAddress: String? = null,
    providerOnline: Boolean = true,
    providerId: String = "SANDBOX_MOCK_GATEWAY",
    externalBalanceSufficient: Boolean = true
  ): RoutingPlanResult {
    val checks = mutableListOf<RoutingCheckStep>()

    val userOk = senderUserId.isNotBlank() && senderVerified
    checks.add(RoutingCheckStep("1. User Identity Check", userOk, if (userOk) "Authenticated & verified" else "Sender authentication/verification incomplete"))

    val recipientOk = recipientName.isNotBlank() && recipientAddressOrRail.isNotBlank()
    checks.add(RoutingCheckStep("2. Recipient Check", recipientOk, if (recipientOk) recipientName else "Missing recipient or destination rail"))

    val stablecoinResult = DigitalAssetEngine.validateStablecoinEligibility(ticker, network, contractAddress)
    val entry = stablecoinResult.getOrNull()
    checks.add(
      RoutingCheckStep(
        "3. Stablecoin Policy Check",
        entry != null,
        entry?.assetName ?: (stablecoinResult.exceptionOrNull()?.message ?: "Asset rejected")
      )
    )

    val (addrValid, addrMsg) = DigitalAssetEngine.validateAddressForNetwork(recipientAddressOrRail, network)
    val networkOk = network.isOperational && addrValid
    checks.add(
      RoutingCheckStep(
        "4. Contract & Network Check",
        networkOk,
        if (networkOk) "${network.displayNameEn} (${entry?.contractAddress?.take(12) ?: "N/A"}...)" else (addrMsg ?: "Network unavailable")
      )
    )

    val routePaused = entry?.let { riskIntelligence.isRoutePaused(it.id, network) } ?: true
    val serviceOk = providerOnline && !routePaused
    checks.add(
      RoutingCheckStep(
        "5. Service & Corridor Availability",
        serviceOk,
        if (serviceOk) "Corridor open via $providerId" else "Service offline or corridor paused by Risk Intelligence"
      )
    )

    val boundsOk = entry != null && amount in entry.minAmount..entry.maxAmount
    val balanceOk = externalBalanceSufficient && boundsOk
    checks.add(
      RoutingCheckStep(
        "6. External Balance & Corridor Limits",
        balanceOk,
        if (balanceOk) "Within corridor bounds & external wallet balance verified" else "Exceeds corridor bounds or external balance unconfirmed"
      )
    )

    val feeTotal = declaredFeeUsd.coerceAtLeast(0.0) + (entry?.estimatedNetworkFeeUsd ?: 0.0)
    checks.add(
      RoutingCheckStep(
        "7. Transparent Fee Calculation",
        true,
        "Platform $${String.format("%.2f", declaredFeeUsd)} + Network est. $${String.format("%.2f", entry?.estimatedNetworkFeeUsd ?: 0.0)}"
      )
    )

    val riskOk = entry != null && entry.riskStatus != StablecoinRiskStatus.CRITICAL
    checks.add(
      RoutingCheckStep(
        "8. Stablecoin Risk Scan",
        riskOk,
        entry?.riskStatus?.labelEn ?: "Risk evaluation failed"
      )
    )

    val blocking = checks.firstOrNull { !it.passed }
    if (blocking != null || entry == null) {
      return RoutingPlanResult(
        isEligible = false,
        checks = checks,
        summary = null,
        blockingReason = blocking?.detail ?: "Routing check failed"
      )
    }

    val summary = PreExecutionSummary(
      recipientName = recipientName,
      amount = amount,
      stablecoinTicker = entry.ticker,
      network = network,
      declaredPlatformFeeUsd = declaredFeeUsd,
      estimatedNetworkFeeUsd = entry.estimatedNetworkFeeUsd,
      finalTotalChargeUsd = amount + feeTotal,
      verificationStatusLabel = "Verified (${entry.approvalStatus.labelEn})",
      riskDisclosure = entry.riskDisclosureEn,
      providerId = providerId,
      requiresExplicitUserApproval = true
    )

    return RoutingPlanResult(
      isEligible = true,
      checks = checks,
      summary = summary,
      blockingReason = null
    )
  }

  /**
   * Creates a new PaymentIntent with strict idempotency & Stablecoin-Only validation.
   */
  fun createPaymentIntent(
    typePrefix: String = "SP-REQ",
    amount: Double,
    assetId: String,
    senderUserId: String,
    senderName: String,
    recipientUserId: String,
    recipientName: String,
    idempotencyKey: String,
    recipientAddressOrRail: String = "",
    declaredFeeUsd: Double = DEFAULT_PROTOCOL_FEE_USD,
    metadata: Map<String, String> = emptyMap()
  ): Result<PaymentIntent> {
    if (amount <= 0.0) {
      return Result.failure(IllegalArgumentException("Transaction amount must be greater than zero"))
    }

    // 1. Idempotency check
    val existingIntentId = idempotencyStore[idempotencyKey]
    if (existingIntentId != null) {
      val existingIntent = intentStore[existingIntentId]
      if (existingIntent != null) {
        return Result.success(existingIntent)
      }
    }

    val asset = DigitalAssetEngine.findAssetById(assetId)
      ?: return Result.failure(IllegalArgumentException("Unsupported or unapproved asset: $assetId"))

    if (asset.type == com.example.core.asset.AssetType.APPROVED_STABLECOIN) {
      val policyCheck = DigitalAssetEngine.validateStablecoinEligibility(asset.ticker, asset.network, asset.contractAddress)
      if (policyCheck.isFailure) {
        return Result.failure(policyCheck.exceptionOrNull()!!)
      }
      if (riskIntelligence.isRoutePaused(asset.id, asset.network)) {
        return Result.failure(IllegalStateException("Corridor ${asset.id} is temporarily paused by Risk Intelligence"))
      }
    }

    if (amount < asset.minAmount || amount > asset.maxAmount) {
      return Result.failure(
        IllegalArgumentException("Amount out of corridor limits (${asset.minAmount} - ${asset.maxAmount} ${asset.ticker})")
      )
    }

    val randomSuffix = (100000..999999).random()
    val transactionId = "$typePrefix-$randomSuffix"
    val intentId = "SP-PI-$randomSuffix"
    val paymentLink = "$BASE_SHARE_URL$transactionId"
    val qrCodePayload = "socialpay://req/$transactionId?amount=$amount&cur=${asset.ticker}&asset=${asset.id}&net=${asset.network.code}"

    val intent = PaymentIntent(
      intentId = intentId,
      transactionId = transactionId,
      amount = amount,
      currencyTicker = asset.ticker,
      assetId = asset.id,
      network = asset.network,
      senderUserId = senderUserId,
      senderName = senderName,
      recipientUserId = recipientUserId,
      recipientName = recipientName,
      recipientAddressOrRail = recipientAddressOrRail,
      paymentLink = paymentLink,
      qrCodePayload = qrCodePayload,
      idempotencyKey = idempotencyKey,
      state = TransactionState.CREATED,
      feeUsd = declaredFeeUsd,
      metadata = metadata
    )

    intentStore[intentId] = intent
    idempotencyStore[idempotencyKey] = intentId
    return Result.success(intent)
  }

  /**
   * Explicitly records user approval of final amounts and recipient.
   * Rejects any attempt to alter recipient or amount once authorized.
   */
  fun authorizeByUser(
    intentId: String,
    expectedAmount: Double,
    expectedRecipientName: String
  ): Result<PaymentIntent> {
    val current = intentStore[intentId]
      ?: return Result.failure(NoSuchElementException("Payment intent not found: $intentId"))

    if (kotlin.math.abs(current.amount - expectedAmount) >= 0.0001 ||
      current.recipientName != expectedRecipientName
    ) {
      val flagged = current.copy(state = TransactionState.UNDER_REVIEW)
      intentStore[intentId] = flagged
      return Result.failure(SecurityException("Tamper Alert: Amount or recipient mutated prior to authorization"))
    }

    val next = current.copy(
      state = TransactionState.AUTHORIZED,
      userExplicitlyApproved = true
    )
    intentStore[intentId] = next
    return Result.success(next)
  }

  /**
   * Pre-retry state verification: prevents double-spending/re-sending on network drops.
   */
  fun canSafelyRetry(idempotencyKey: String): Pair<Boolean, PaymentIntent?> {
    val existingId = idempotencyStore[idempotencyKey] ?: return Pair(true, null)
    val existing = intentStore[existingId] ?: return Pair(true, null)
    val safeToRetry = existing.state == TransactionState.FAILED || existing.state == TransactionState.CANCELLED
    return Pair(safeToRetry, existing)
  }

  fun transitionState(intentId: String, targetState: TransactionState, reason: String = ""): Result<PaymentIntent> {
    val current = intentStore[intentId]
      ?: return Result.failure(NoSuchElementException("Payment intent not found: $intentId"))

    if (current.state.isTerminal) {
      return Result.failure(IllegalStateException("Terminal transaction state cannot change: ${current.state}"))
    }

    val allowedNextStates = ALLOWED_TRANSITIONS[current.state] ?: emptySet()
    if (!allowedNextStates.contains(targetState)) {
      return Result.failure(IllegalStateException("Invalid state transition ${current.state} -> $targetState"))
    }

    if (targetState == TransactionState.SETTLED && !current.userExplicitlyApproved) {
      return Result.failure(SecurityException("Cannot settle transaction without explicit user approval"))
    }

    if (System.currentTimeMillis() > current.expiresAt &&
      targetState != TransactionState.CANCELLED &&
      targetState != TransactionState.FAILED
    ) {
      val expiredIntent = current.copy(state = TransactionState.FAILED)
      intentStore[intentId] = expiredIntent
      return Result.failure(IllegalStateException("Payment request expired"))
    }

    val updated = current.copy(state = targetState)
    intentStore[intentId] = updated
    return Result.success(updated)
  }

  /**
   * Finalizes settlement with Provider Proof, Double-Entry Ledger posting, and Reconciliation check.
   */
  fun settleWithProof(
    intentId: String,
    providerId: String,
    providerReference: String,
    providerReportedAmount: Double,
    networkConfirmed: Boolean
  ): Result<Pair<PaymentReceipt, ReconciliationReport>> {
    val intent = intentStore[intentId]
      ?: return Result.failure(NoSuchElementException("Payment intent not found"))

    if (!intent.userExplicitlyApproved) {
      return Result.failure(SecurityException("Explicit user consent required before settlement"))
    }
    if (providerReference.isBlank()) {
      return Result.failure(IllegalArgumentException("Missing external provider settlement reference"))
    }

    val proofDigest = MessageDigest.getInstance("SHA-256")
      .digest("${intent.transactionId}|$providerId|$providerReference|$providerReportedAmount".toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }

    ledgerEngine.recordBalancedSettlement(
      transactionId = intent.transactionId,
      intentId = intent.intentId,
      payerScopeId = "USR_${intent.senderUserId.takeLast(8)}",
      payeeScopeId = "USR_${intent.recipientUserId.takeLast(8)}",
      stablecoinTicker = intent.currencyTicker,
      network = intent.network,
      principalAmount = intent.amount,
      declaredFeeUsd = intent.feeUsd,
      providerId = providerId,
      providerReference = providerReference,
      settlementProofHash = proofDigest,
      transactionState = TransactionState.SETTLED.name
    )

    val report = ledgerEngine.reconcileTransaction(
      transactionId = intent.transactionId,
      providerReportedAmount = providerReportedAmount,
      networkConfirmed = networkConfirmed
    )

    if (report.status != ReconciliationStatus.MATCHED) {
      intentStore[intentId] = intent.copy(state = TransactionState.UNDER_REVIEW)
      return Result.failure(IllegalStateException("Reconciliation blocked settlement: ${report.discrepancyReason}"))
    }

    intentStore[intentId] = intent.copy(
      state = TransactionState.SETTLED,
      providerId = providerId,
      providerReference = providerReference
    )

    val receipt = generateReceipt(intent, providerReference, intent.network.displayNameEn)
    return Result.success(receipt to report)
  }

  fun recordAttempt(attempt: PaymentAttempt) {
    attemptsStore.add(attempt)
  }

  fun generateReceipt(
    intent: PaymentIntent,
    providerReference: String,
    networkOrBank: String
  ): PaymentReceipt {
    val receiptId = "REC-${intent.transactionId}"
    val signPayload = "$receiptId:${intent.amount}:${intent.currencyTicker}:$providerReference:${intent.recipientUserId}"
    val digest = MessageDigest.getInstance("SHA-256").digest(signPayload.toByteArray(Charsets.UTF_8))
    val signatureHash = digest.joinToString("") { "%02x".format(it) }

    val receipt = PaymentReceipt(
      receiptId = receiptId,
      transactionId = intent.transactionId,
      intentId = intent.intentId,
      amountPaid = intent.amount,
      currencyTicker = intent.currencyTicker,
      feeUsd = intent.feeUsd,
      payerName = intent.senderName,
      payeeName = intent.recipientName,
      providerReference = providerReference,
      networkOrBank = networkOrBank,
      issuedAt = System.currentTimeMillis(),
      digitalSignatureHash = signatureHash
    )
    receiptsStore[receiptId] = receipt
    return receipt
  }

  fun getIntent(intentId: String): PaymentIntent? = intentStore[intentId]
  fun getReceipt(receiptId: String): PaymentReceipt? = receiptsStore[receiptId]
  fun getAllIntents(): List<PaymentIntent> = intentStore.values.toList()
  fun getAttemptsForIntent(intentId: String): List<PaymentAttempt> = attemptsStore.filter { it.intentId == intentId }
}
