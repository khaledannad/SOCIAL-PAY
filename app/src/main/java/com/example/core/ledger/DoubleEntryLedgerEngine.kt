package com.example.core.ledger

import com.example.core.asset.BlockchainNetwork
import java.security.MessageDigest

enum class LedgerEntrySide {
  DEBIT,
  CREDIT
}

enum class LedgerAccountCategory {
  USER_EXTERNAL_SOURCE,
  RECIPIENT_EXTERNAL_DESTINATION,
  PROTOCOL_FEE_REVENUE,
  PARTNER_COMMISSION_PAYABLE,
  SETTLEMENT_CLEARING_TRANSIT
}

enum class ReconciliationStatus(
  val labelAr: String,
  val labelEn: String,
  val labelFr: String
) {
  MATCHED("مطابق محاسبياً ✓", "Reconciled & Matched", "Rapproché ✓"),
  PENDING_PROVIDER_PROOF("بانتظار إثبات المزود", "Awaiting Provider Proof", "En attente de preuve"),
  MISMATCH_BLOCKED("تعارض محاسبي — محجوب للمراجعة", "Mismatch — Blocked for Review", "Écart détecté — Bloqué")
}

/**
 * Immutable Double-Entry Ledger Posting.
 * Every financial event records balanced Debit & Credit lines with zero direct mutability.
 */
data class DoubleEntryPosting(
  val journalId: String,
  val transactionId: String,
  val intentId: String,
  val side: LedgerEntrySide,
  val accountCategory: LedgerAccountCategory,
  val partyScopeId: String, // Internal role-scoped ID (never exposes raw PII across services)
  val stablecoinTicker: String,
  val network: BlockchainNetwork,
  val amount: Double,
  val feeAmountUsd: Double,
  val providerId: String,
  val providerReference: String,
  val transactionState: String,
  val settlementProofHash: String,
  val postedAtMillis: Long = System.currentTimeMillis(),
  val integrityHash: String
)

data class ReconciliationReport(
  val transactionId: String,
  val ledgerBalanced: Boolean,
  val debitTotal: Double,
  val creditTotal: Double,
  val providerReportedAmount: Double?,
  val networkConfirmed: Boolean,
  val status: ReconciliationStatus,
  val discrepancyReason: String? = null,
  val checkedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Double-Entry Accounting & Reconciliation Engine.
 * Guarantees balanced accounting entries (Debits == Credits) and blocks settlement finalization
 * whenever ledger, provider, or network settlement records disagree.
 */
class DoubleEntryLedgerEngine {

  private val postings = mutableListOf<DoubleEntryPosting>()
  private val reconciliationReports = mutableMapOf<String, ReconciliationReport>()

  @Synchronized
  fun recordBalancedSettlement(
    transactionId: String,
    intentId: String,
    payerScopeId: String,
    payeeScopeId: String,
    stablecoinTicker: String,
    network: BlockchainNetwork,
    principalAmount: Double,
    declaredFeeUsd: Double,
    providerId: String,
    providerReference: String,
    settlementProofHash: String,
    transactionState: String
  ): List<DoubleEntryPosting> {
    require(principalAmount > 0.0) { "Principal amount must be positive." }
    require(declaredFeeUsd >= 0.0) { "Fee must be non-negative." }

    val now = System.currentTimeMillis()
    val netTotal = principalAmount + declaredFeeUsd

    val debitPayer = createImmutablePosting(
      journalId = "JRN-DR-$transactionId-${postings.size + 1}",
      transactionId = transactionId,
      intentId = intentId,
      side = LedgerEntrySide.DEBIT,
      accountCategory = LedgerAccountCategory.USER_EXTERNAL_SOURCE,
      partyScopeId = payerScopeId,
      stablecoinTicker = stablecoinTicker,
      network = network,
      amount = netTotal,
      feeAmountUsd = declaredFeeUsd,
      providerId = providerId,
      providerReference = providerReference,
      transactionState = transactionState,
      settlementProofHash = settlementProofHash,
      timestamp = now
    )

    val creditPayee = createImmutablePosting(
      journalId = "JRN-CR-$transactionId-${postings.size + 2}",
      transactionId = transactionId,
      intentId = intentId,
      side = LedgerEntrySide.CREDIT,
      accountCategory = LedgerAccountCategory.RECIPIENT_EXTERNAL_DESTINATION,
      partyScopeId = payeeScopeId,
      stablecoinTicker = stablecoinTicker,
      network = network,
      amount = principalAmount,
      feeAmountUsd = 0.0,
      providerId = providerId,
      providerReference = providerReference,
      transactionState = transactionState,
      settlementProofHash = settlementProofHash,
      timestamp = now
    )

    val batch = mutableListOf(debitPayer, creditPayee)

    if (declaredFeeUsd > 0.0) {
      val creditFeeRevenue = createImmutablePosting(
        journalId = "JRN-FEE-$transactionId-${postings.size + 3}",
        transactionId = transactionId,
        intentId = intentId,
        side = LedgerEntrySide.CREDIT,
        accountCategory = LedgerAccountCategory.PROTOCOL_FEE_REVENUE,
        partyScopeId = "CORP_FEE_REVENUE_TREASURY",
        stablecoinTicker = "USD",
        network = BlockchainNetwork.FIAT_BANKING,
        amount = declaredFeeUsd,
        feeAmountUsd = 0.0,
        providerId = providerId,
        providerReference = providerReference,
        transactionState = transactionState,
        settlementProofHash = settlementProofHash,
        timestamp = now
      )
      batch.add(creditFeeRevenue)
    }

    postings.addAll(batch)
    return batch
  }

  @Synchronized
  fun reconcileTransaction(
    transactionId: String,
    providerReportedAmount: Double?,
    networkConfirmed: Boolean
  ): ReconciliationReport {
    val txPostings = postings.filter { it.transactionId == transactionId }
    val debits = txPostings.filter { it.side == LedgerEntrySide.DEBIT }.sumOf { it.amount }
    val credits = txPostings.filter { it.side == LedgerEntrySide.CREDIT }.sumOf { it.amount }
    val isBalanced = txPostings.isNotEmpty() && kotlin.math.abs(debits - credits) < 0.0001

    val payeeCredit = txPostings
      .filter { it.accountCategory == LedgerAccountCategory.RECIPIENT_EXTERNAL_DESTINATION }
      .sumOf { it.amount }

    val (status, reason) = when {
      !isBalanced -> ReconciliationStatus.MISMATCH_BLOCKED to
        "Unbalanced double-entry postings (Debit=$debits vs Credit=$credits)"
      providerReportedAmount == null -> ReconciliationStatus.PENDING_PROVIDER_PROOF to
        "External provider settlement proof not yet verified"
      kotlin.math.abs(providerReportedAmount - payeeCredit) >= 0.0001 ->
        ReconciliationStatus.MISMATCH_BLOCKED to
          "Provider amount ($providerReportedAmount) disagrees with ledger credit ($payeeCredit)"
      !networkConfirmed -> ReconciliationStatus.PENDING_PROVIDER_PROOF to
        "Waiting for required blockchain / bank finality confirmations"
      else -> ReconciliationStatus.MATCHED to null
    }

    val report = ReconciliationReport(
      transactionId = transactionId,
      ledgerBalanced = isBalanced,
      debitTotal = debits,
      creditTotal = credits,
      providerReportedAmount = providerReportedAmount,
      networkConfirmed = networkConfirmed,
      status = status,
      discrepancyReason = reason
    )
    reconciliationReports[transactionId] = report
    return report
  }

  fun getPostingsForTransaction(transactionId: String): List<DoubleEntryPosting> =
    postings.filter { it.transactionId == transactionId }

  fun getAllPostings(): List<DoubleEntryPosting> = postings.toList()

  fun getAllReconciliations(): List<ReconciliationReport> = reconciliationReports.values.toList()

  private fun createImmutablePosting(
    journalId: String,
    transactionId: String,
    intentId: String,
    side: LedgerEntrySide,
    accountCategory: LedgerAccountCategory,
    partyScopeId: String,
    stablecoinTicker: String,
    network: BlockchainNetwork,
    amount: Double,
    feeAmountUsd: Double,
    providerId: String,
    providerReference: String,
    transactionState: String,
    settlementProofHash: String,
    timestamp: Long
  ): DoubleEntryPosting {
    val canonical = "$journalId|$transactionId|$side|$accountCategory|$partyScopeId|$stablecoinTicker|${network.code}|$amount|$providerReference|$settlementProofHash|$timestamp"
    val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
    val hash = digest.joinToString("") { "%02x".format(it) }
    return DoubleEntryPosting(
      journalId = journalId,
      transactionId = transactionId,
      intentId = intentId,
      side = side,
      accountCategory = accountCategory,
      partyScopeId = partyScopeId,
      stablecoinTicker = stablecoinTicker,
      network = network,
      amount = amount,
      feeAmountUsd = feeAmountUsd,
      providerId = providerId,
      providerReference = providerReference,
      transactionState = transactionState,
      settlementProofHash = settlementProofHash,
      postedAtMillis = timestamp,
      integrityHash = hash
    )
  }
}
