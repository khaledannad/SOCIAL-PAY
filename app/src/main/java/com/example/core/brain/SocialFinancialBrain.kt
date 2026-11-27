package com.example.core.brain

import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.audit.AuditEngine
import com.example.core.risk.StablecoinRiskIntelligence
import com.example.core.transaction.RoutingPlanResult
import com.example.core.transaction.TransactionEngine
import com.example.model.AppLanguage

enum class BrainAgentId(
  val code: String,
  val titleAr: String,
  val titleEn: String,
  val titleFr: String,
  val missionEn: String,
  val allowedPermissions: Set<String>,
  val allowedDataSources: Set<String>,
  val operationalLimitEn: String
) {
  CENTRAL_FINANCIAL_BRAIN(
    code = "AGENT-01",
    titleAr = "العقل المالي المركزي",
    titleEn = "Central Financial Brain",
    titleFr = "Cerveau Financier Central",
    missionEn = "Orchestrate cross-module routing, enforce policies, and prevent privilege escalation across all 17 specialist agents.",
    allowedPermissions = setOf("COORDINATE_MODULES", "READ_POLICY", "READ_RISK", "BLOCK_UNSAFE_ACTION"),
    allowedDataSources = setOf("StablecoinRegistry", "RiskIntelligence", "RoutingEngine"),
    operationalLimitEn = "Cannot move funds unilaterally or override Owner/User consent."
  ),
  PAYMENT_PROCESSING(
    code = "AGENT-02",
    titleAr = "وكيل معالجة المدفوعات",
    titleEn = "Payment Processing Agent",
    titleFr = "Agent de Traitement des Paiements",
    missionEn = "Prepare payment intents, QR payloads, and itemized invoices under strict idempotency.",
    allowedPermissions = setOf("CREATE_INTENT", "ISSUE_INVOICE", "VERIFY_IDEMPOTENCY"),
    allowedDataSources = setOf("TransactionEngine"),
    operationalLimitEn = "Requires explicit user confirmation before any authorization."
  ),
  STABLECOIN_VERIFICATION(
    code = "AGENT-03",
    titleAr = "وكيل التحقق من العملات المستقرة",
    titleEn = "Stablecoin Verification Agent",
    titleFr = "Agent de Vérification Stablecoins",
    missionEn = "Enforce approved contract addresses, chain matching, and block BTC/volatile/meme tokens.",
    allowedPermissions = setOf("READ_STABLECOIN_REGISTRY", "REJECT_UNAPPROVED_ASSET"),
    allowedDataSources = setOf("ApprovedStablecoinRegistry"),
    operationalLimitEn = "Read-only accreditation gate; cannot self-approve new assets."
  ),
  NETWORK_VALIDATION(
    code = "AGENT-04",
    titleAr = "وكيل التحقق من الشبكات",
    titleEn = "Network Validation Agent",
    titleFr = "Agent de Validation Réseau",
    missionEn = "Validate non-custodial addresses per chain, required confirmations, and network health.",
    allowedPermissions = setOf("VALIDATE_ADDRESS", "READ_NETWORK_STATUS"),
    allowedDataSources = setOf("BlockchainNetworkRegistry"),
    operationalLimitEn = "Cannot sign on-chain transactions."
  ),
  PAYMENT_ROUTING(
    code = "AGENT-05",
    titleAr = "وكيل توجيه المدفوعات",
    titleEn = "Payment Routing Agent",
    titleFr = "Agent de Routage des Paiements",
    missionEn = "Select qualified external providers and compute transparent platform + gas fees.",
    allowedPermissions = setOf("EVALUATE_ROUTING_PLAN", "CALCULATE_FEES"),
    allowedDataSources = setOf("ProviderRegistry", "OwnerPublicPlatformConfig"),
    operationalLimitEn = "Cannot modify fee schedule or hide fees."
  ),
  SECURITY_GUARD(
    code = "AGENT-06",
    titleAr = "وكيل الأمن السيبراني",
    titleEn = "Security Agent",
    titleFr = "Agent de Sécurité",
    missionEn = "Enforce Zero Trust sessions, PBKDF2 owner PIN locks, anti-phishing seals, and HMAC signatures.",
    allowedPermissions = setOf("VERIFY_SIGNATURES", "LOCK_SESSION", "FLAG_ANOMALY"),
    allowedDataSources = setOf("SecurityEngine", "AuthState"),
    operationalLimitEn = "Cannot weaken Firestore rules or reveal PIN salts/hashes."
  ),
  FRAUD_DETECTION(
    code = "AGENT-07",
    titleAr = "وكيل كشف الاحتيال والتثليث",
    titleEn = "Fraud Detection Agent",
    titleFr = "Agent Anti-Fraude",
    missionEn = "Intercept bank triangulation mismatches, phishing links, and duplicate payment attempts.",
    allowedPermissions = setOf("SCAN_TRIANGULATION", "PLACE_UNDER_REVIEW"),
    allowedDataSources = setOf("SecurityEngine", "TransactionIntentMeta"),
    operationalLimitEn = "May flag UNDER_REVIEW only; cannot seize funds."
  ),
  PRIVACY_PROTECTION(
    code = "AGENT-08",
    titleAr = "وكيل حماية الخصوصية",
    titleEn = "Privacy Protection Agent",
    titleFr = "Agent Protection de la Vie Privée",
    missionEn = "Enforce data minimization, strip PII before AI processing, and block unauthorized scraping.",
    allowedPermissions = setOf("REDACT_PII", "ENFORCE_CONSENT", "EXECUTE_DATA_EXPORT"),
    allowedDataSources = setOf("UserPrivacyConsents"),
    operationalLimitEn = "Never transmits private messages, contacts, or secrets externally."
  ),
  IDENTITY_VERIFICATION(
    code = "AGENT-09",
    titleAr = "وكيل التحقق من الهوية (KYC/KYB)",
    titleEn = "Identity Verification Agent",
    titleFr = "Agent Vérification d'Identité",
    missionEn = "Validate KYC/KYB tiers, linked bank/wallet ownership, and market transaction limits.",
    allowedPermissions = setOf("READ_KYC_TIER", "VALIDATE_PAYOUT_RAIL"),
    allowedDataSources = setOf("KycKybRegistry"),
    operationalLimitEn = "Cannot expose raw ID documents outside authorized review."
  ),
  COMPLIANCE_AGENT(
    code = "AGENT-10",
    titleAr = "وكيل الامتثال والرقابة",
    titleEn = "Compliance Agent",
    titleFr = "Agent de Conformité",
    missionEn = "Screen corridors for jurisdictional licensing, AML thresholds, and sanctions compliance.",
    allowedPermissions = setOf("CHECK_CORRIDOR_COMPLIANCE", "BLOCK_RESTRICTED_MARKET"),
    allowedDataSources = setOf("JurisdictionRules"),
    operationalLimitEn = "Cannot approve unverified banking partnerships."
  ),
  BANKING_INTEGRATION(
    code = "AGENT-11",
    titleAr = "وكيل التكامل المصرفي",
    titleEn = "Banking Integration Agent",
    titleFr = "Agent Intégration Bancaire",
    missionEn = "Coordinate accredited Open Banking & instant settlement connectors.",
    allowedPermissions = setOf("TEST_BANK_CONNECTOR", "READ_RAIL_STATUS"),
    allowedDataSources = setOf("GlobalConnectivityHub"),
    operationalLimitEn = "Only invokes authorized gateways with explicit credentials."
  ),
  MERCHANT_SERVICES(
    code = "AGENT-12",
    titleAr = "وكيل خدمات التجار والشركات",
    titleEn = "Merchant Services Agent",
    titleFr = "Agent Services Marchands",
    missionEn = "Manage merchant QR checkout, itemized invoices, staff RBAC scoping, and webhooks.",
    allowedPermissions = setOf("MANAGE_MERCHANT_LINKS", "VERIFY_STAFF_ROLE"),
    allowedDataSources = setOf("MerchantPortal"),
    operationalLimitEn = "Isolated per company; zero cross-tenant access."
  ),
  SOCIAL_INTEGRATION(
    code = "AGENT-13",
    titleAr = "وكيل القنوات الاجتماعية",
    titleEn = "Social Integration Agent",
    titleFr = "Agent Intégration Sociale",
    missionEn = "Format safe payment share links for WhatsApp, Telegram, Instagram, Messenger, TikTok, X.",
    allowedPermissions = setOf("FORMAT_SHARE_LINK", "GENERATE_QR_PAYLOAD"),
    allowedDataSources = setOf("PublicPaymentIntent"),
    operationalLimitEn = "Zero access to private chat history or user contacts."
  ),
  DISPUTE_RESOLUTION(
    code = "AGENT-14",
    titleAr = "وكيل تسوية النزاعات",
    titleEn = "Dispute Resolution Agent",
    titleFr = "Agent Résolution des Litiges",
    missionEn = "Collect objective settlement evidence and route contested cases for human review.",
    allowedPermissions = setOf("OPEN_DISPUTE", "ATTACH_EVIDENCE"),
    allowedDataSources = setOf("TransactionLedger", "AuditEngine"),
    operationalLimitEn = "Cannot alter settled amounts or erase audit logs."
  ),
  FINANCIAL_AUDIT(
    code = "AGENT-15",
    titleAr = "وكيل التدقيق والمحاسبة",
    titleEn = "Financial Audit Agent",
    titleFr = "Agent d'Audit Financier",
    missionEn = "Verify Double-Entry Ledger balance, cryptographic hash chains, and reconciliation parity.",
    allowedPermissions = setOf("VERIFY_LEDGER", "RUN_RECONCILIATION"),
    allowedDataSources = setOf("DoubleEntryLedger", "AuditEngine"),
    operationalLimitEn = "Append-only read/verify; cannot delete audit entries."
  ),
  CUSTOMER_SUPPORT(
    code = "AGENT-16",
    titleAr = "وكيل دعم العملاء متعدد اللغات",
    titleEn = "Customer Support Agent",
    titleFr = "Agent Support Client",
    missionEn = "Provide clear Arabic/French/English guidance and escalate support tickets.",
    allowedPermissions = setOf("CREATE_TICKET", "EXPLAIN_STATUS"),
    allowedDataSources = setOf("MaskedTransactionSummary"),
    operationalLimitEn = "Never sees raw KYC ID documents or owner secrets."
  ),
  SYSTEM_MONITORING(
    code = "AGENT-17",
    titleAr = "وكيل مراقبة الأنظمة والحوادث",
    titleEn = "System Monitoring Agent",
    titleFr = "Agent Surveillance Système",
    missionEn = "Track API response latencies, WebSocket health, and trigger service isolation alerts.",
    allowedPermissions = setOf("CHECK_HEALTH", "EMIT_EMERGENCY_ALERT"),
    allowedDataSources = setOf("MarketDataEngine", "ConnectivityHub"),
    operationalLimitEn = "Cannot shut down full platform on unverified single alert."
  ),
  RISK_INTELLIGENCE(
    code = "AGENT-18",
    titleAr = "وكيل ذكاء مخاطر العملات المستقرة",
    titleEn = "Risk Intelligence Agent",
    titleFr = "Agent Intelligence des Risques",
    missionEn = "Monitor stablecoin peg deviation, issuer reserves, and smart contract pauses.",
    allowedPermissions = setOf("EVALUATE_DEPEG", "PAUSE_CORRIDOR"),
    allowedDataSources = setOf("StablecoinRiskIntelligence"),
    operationalLimitEn = "Cannot alter past user balances or completed records."
  )
}

data class AgentRuntimeSnapshot(
  val agent: BrainAgentId,
  val isEnabled: Boolean = true,
  val evaluationScore: Int = 99,
  val tasksHandled: Int = 0,
  val violationsBlocked: Int = 0,
  val lastActionSummary: String = "Standing by under deterministic rules"
)

data class BrainEvaluationLog(
  val timestampMillis: Long = System.currentTimeMillis(),
  val agentCode: String,
  val summary: String,
  val outcome: String
)

/**
 * SOCIAL FINANCIAL BRAIN:
 * Deterministic Central Coordinator binding User Application, Connectivity Hub,
 * and Owner Command Center with strict permission boundaries.
 */
class SocialFinancialBrain(
  val transactionEngine: TransactionEngine = TransactionEngine(),
  val auditEngine: AuditEngine = AuditEngine(),
  val riskIntelligence: StablecoinRiskIntelligence = transactionEngine.riskIntelligence
) {

  private val agentStates = BrainAgentId.entries.associateWith {
    MutableAgentState(isEnabled = true, tasksHandled = 0, violationsBlocked = 0, lastAction = "Ready")
  }.toMutableMap()

  private val evaluationLogs = mutableListOf<BrainEvaluationLog>()

  private data class MutableAgentState(
    var isEnabled: Boolean,
    var tasksHandled: Int,
    var violationsBlocked: Int,
    var lastAction: String
  )

  fun setAgentEnabled(agentId: BrainAgentId, enabled: Boolean, actorId: String): Boolean {
    // Non-negotiable invariant: Security, Privacy, Audit, and Central Brain cannot be disabled!
    if (!enabled && (agentId == BrainAgentId.CENTRAL_FINANCIAL_BRAIN ||
        agentId == BrainAgentId.SECURITY_GUARD ||
        agentId == BrainAgentId.PRIVACY_PROTECTION ||
        agentId == BrainAgentId.FINANCIAL_AUDIT ||
        agentId == BrainAgentId.STABLECOIN_VERIFICATION)
    ) {
      recordAgentActivity(agentId, "Blocked attempt to disable mandatory core guardrail by $actorId", "BLOCKED", isViolation = true)
      return false
    }
    agentStates[agentId]?.let {
      it.isEnabled = enabled
      it.lastAction = if (enabled) "Enabled by $actorId" else "Paused by $actorId"
    }
    auditEngine.record("SYS-BRAIN", "AGENT_STATE_CHANGE", actorId, "${agentId.code} enabled=$enabled")
    return true
  }

  /**
   * Coordinates a full deterministic pre-execution review across all specialist agents.
   */
  fun coordinatePaymentRequest(
    senderUserId: String,
    senderVerified: Boolean,
    recipientName: String,
    recipientAddressOrRail: String,
    ticker: String,
    network: BlockchainNetwork,
    amount: Double,
    declaredFeeUsd: Double,
    contractAddress: String? = null,
    providerOnline: Boolean = true,
    providerId: String = "SANDBOX_MOCK_GATEWAY",
    externalBalanceSufficient: Boolean = true
  ): RoutingPlanResult {
    recordAgentActivity(BrainAgentId.CENTRAL_FINANCIAL_BRAIN, "Coordinating payment check for $amount $ticker", "OK")

    // Check Stablecoin Policy first via AGENT-03
    val assetCheck = DigitalAssetEngine.validateStablecoinEligibility(ticker, network, contractAddress)
    if (assetCheck.isFailure) {
      recordAgentActivity(
        BrainAgentId.STABLECOIN_VERIFICATION,
        assetCheck.exceptionOrNull()?.message ?: "Asset rejected",
        "DENIED",
        isViolation = true
      )
    } else {
      recordAgentActivity(BrainAgentId.STABLECOIN_VERIFICATION, "Verified ${assetCheck.getOrThrow().assetName}", "PASS")
    }

    val plan = transactionEngine.evaluateRoutingPlan(
      senderUserId = senderUserId,
      senderVerified = senderVerified,
      recipientName = recipientName,
      recipientAddressOrRail = recipientAddressOrRail,
      ticker = ticker,
      network = network,
      amount = amount,
      declaredFeeUsd = declaredFeeUsd,
      contractAddress = contractAddress,
      providerOnline = providerOnline,
      providerId = providerId,
      externalBalanceSufficient = externalBalanceSufficient
    )

    recordAgentActivity(
      BrainAgentId.PAYMENT_ROUTING,
      if (plan.isEligible) "Eligible route via $providerId" else "Blocked: ${plan.blockingReason}",
      if (plan.isEligible) "ELIGIBLE" else "BLOCKED",
      isViolation = !plan.isEligible
    )

    auditEngine.record(
      transactionId = "PRE-CHECK-${System.currentTimeMillis()}",
      action = if (plan.isEligible) "BRAIN_ROUTE_APPROVED" else "BRAIN_ROUTE_BLOCKED",
      actorId = "USR_${senderUserId.takeLast(6)}",
      details = plan.blockingReason ?: "Eligible $amount $ticker on ${network.code}"
    )

    return plan
  }

  /**
   * Deterministic permission boundary enforcement: prevents any AI agent from modifying
   * security rules, privacy rules, or bypassing user approval to transfer money.
   */
  fun authorizeAgentPrivilegeRequest(agentId: BrainAgentId, requestedPermission: String): Result<Boolean> {
    if (requestedPermission in setOf("MOVE_FUNDS_DIRECTLY", "MODIFY_SECURITY_RULES", "READ_PRIVATE_CHATS", "DELETE_AUDIT_LOG")) {
      recordAgentActivity(agentId, "Privilege escalation blocked: $requestedPermission", "DENIED", isViolation = true)
      return Result.failure(SecurityException("Non-Negotiable Violation: ${agentId.code} cannot hold $requestedPermission"))
    }
    val allowed = requestedPermission in agentId.allowedPermissions
    if (!allowed) {
      recordAgentActivity(agentId, "Out-of-scope permission rejected: $requestedPermission", "DENIED", isViolation = true)
      return Result.failure(SecurityException("Scope boundary enforced for ${agentId.code}"))
    }
    recordAgentActivity(agentId, "Executed scoped permission: $requestedPermission", "GRANTED")
    return Result.success(true)
  }

  fun summarizeForUser(plan: RoutingPlanResult, lang: AppLanguage): String {
    val summary = plan.summary
    return if (plan.isEligible && summary != null) {
      when (lang) {
        AppLanguage.ARABIC ->
          "تم فحص الطلب عبر العقل المالي المركزي: المستفيد ${summary.recipientName}، المبلغ ${summary.amount} ${summary.stablecoinTicker} على ${summary.network.displayNameAr}، الرسوم الشفافة $${String.format("%.2f", summary.declaredPlatformFeeUsd + summary.estimatedNetworkFeeUsd)}. بانتظار موافقتك الصريحة."
        AppLanguage.FRENCH ->
          "Vérifié par Social Financial Brain : Bénéficiaire ${summary.recipientName}, ${summary.amount} ${summary.stablecoinTicker} sur ${summary.network.displayNameFr}. Approbation explicite requise."
        AppLanguage.ENGLISH ->
          "Verified by Social Financial Brain: Recipient ${summary.recipientName}, ${summary.amount} ${summary.stablecoinTicker} on ${summary.network.displayNameEn}. Explicit approval required."
      }
    } else {
      when (lang) {
        AppLanguage.ARABIC -> "تعذر تمرير الطلب: ${plan.blockingReason}"
        AppLanguage.FRENCH -> "Demande bloquée : ${plan.blockingReason}"
        AppLanguage.ENGLISH -> "Request blocked: ${plan.blockingReason}"
      }
    }
  }

  fun getAllAgentSnapshots(): List<AgentRuntimeSnapshot> {
    return BrainAgentId.entries.map { agent ->
      val state = agentStates[agent]!!
      AgentRuntimeSnapshot(
        agent = agent,
        isEnabled = state.isEnabled,
        evaluationScore = (100 - (state.violationsBlocked * 2)).coerceIn(60, 100),
        tasksHandled = state.tasksHandled,
        violationsBlocked = state.violationsBlocked,
        lastActionSummary = state.lastAction
      )
    }
  }

  fun getRecentEvaluationLogs(): List<BrainEvaluationLog> = evaluationLogs.take(40)

  private fun recordAgentActivity(
    agentId: BrainAgentId,
    summary: String,
    outcome: String,
    isViolation: Boolean = false
  ) {
    agentStates[agentId]?.let {
      it.tasksHandled += 1
      if (isViolation) it.violationsBlocked += 1
      it.lastAction = summary
    }
    evaluationLogs.add(0, BrainEvaluationLog(agentCode = agentId.code, summary = summary, outcome = outcome))
    if (evaluationLogs.size > 100) evaluationLogs.removeAt(evaluationLogs.lastIndex)
  }
}
