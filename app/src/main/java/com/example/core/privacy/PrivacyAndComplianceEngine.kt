package com.example.core.privacy

import com.example.core.governance.tr
import com.example.model.AppLanguage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Identity Verification States mandated for both Individual KYC and Business KYB.
 */
enum class VerificationWorkflowStatus(
  val titleAr: String,
  val titleEn: String,
  val titleFr: String
) {
  NOT_STARTED("لم يبدأ بعد", "Not Started", "Non commencé"),
  SUBMITTED("تم الإرسال بنجاح", "Submitted", "Soumis"),
  UNDER_REVIEW("قيد المراجعة التنظيمية", "Under Review", "En cours d'examen"),
  VERIFIED("موثّق ومعتمد ✓", "Verified ✓", "Vérifié ✓"),
  REJECTED("مرفوض — يرجى مراجعة السبب", "Rejected", "Rejeté"),
  ACTION_REQUIRED("مطلوب استكمال إجراء", "Action Required", "Action requise"),
  EXPIRED("منتهي الصلاحية", "Expired", "Expiré")
}

enum class VerificationApplicantType {
  INDIVIDUAL_KYC,
  BUSINESS_KYB
}

data class DataFieldPurposeRecord(
  val fieldName: String,
  val domainIsolationGroup: String, // Identity, Account, Payment, Business, Verification, SecurityLogs, Ledger
  val legalBasis: String,
  val technicalPurpose: String,
  val retentionPolicy: String
)

data class PrivacyConsentState(
  val essentialPaymentRouting: Boolean = true, // mandatory for core operation
  val securityAlertsAndSessions: Boolean = true,
  val aiAssistantContextMemory: Boolean = false, // strict opt-in
  val externalPartnerSharing: Boolean = false,
  val dataSaleOptOutLocked: Boolean = true // NON-NEGOTIABLE: Data sale strictly forbidden
)

enum class EmergencyStepPhase(val stepNumber: Int, val labelAr: String, val labelEn: String, val labelFr: String) {
  IDENTIFY_SERVICE(1, "تحديد الخدمة المتضررة", "Identify Affected Service", "Identifier le service affecté"),
  ISOLATE_SERVICE(2, "عزل الخدمة المتضررة", "Isolate Service", "Isoler le service"),
  PAUSE_OPERATIONS(3, "إيقاف العمليات المتأثرة", "Pause Impacted Flows", "Suspendre les opérations"),
  PROTECT_KEYS_ASSETS(4, "حماية المفاتيح والأصول", "Protect Keys & Assets", "Protéger clés et actifs"),
  PRESERVE_EVIDENCE(5, "حفظ الأدلة الجنائية والسجلات", "Preserve Forensic Evidence", "Conserver les preuves"),
  NOTIFY_RESPONSIBLES(6, "إخطار المسؤولين والمستخدمين", "Notify Stakeholders", "Notifier les responsables"),
  INVESTIGATE(7, "التحقيق في السبب الجذري", "Investigate Root Cause", "Enquêter sur la cause"),
  REMEDIATE(8, "معالجة الخلل وإصلاحه", "Remediate Defect", "Corriger l'anomalie"),
  RETEST(9, "إعادة الاختبار والتحقق", "Re-test & Verify", "Re-tester et valider"),
  AUTHORIZED_RECOVERY(10, "استعادة الخدمة بموافقة مخولة", "Authorized Recovery", "Reprise autorisée")
}

data class EmergencyIncidentRecord(
  val incidentId: String,
  val title: String,
  val affectedService: String,
  val severity: String, // "ELEVATED", "HIGH", "CRITICAL"
  val currentPhase: EmergencyStepPhase,
  val isServiceIsolated: Boolean,
  val isPlatformWideHalted: Boolean = false,
  val triggeredBy: String,
  val evidenceHash: String,
  val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * Privacy by Design, Identity Compliance & Multi-Tier Emergency Response Engine.
 */
object PrivacyAndComplianceEngine {

  /**
   * Data Minimization & Isolation Registry.
   * Documents legal + technical purpose before storing any field and enforces domain separation.
   */
  val DATA_MINIMIZATION_REGISTRY: List<DataFieldPurposeRecord> = listOf(
    DataFieldPurposeRecord(
      fieldName = "userId / internalScopeId",
      domainIsolationGroup = "Account",
      legalBasis = "Contract Execution & Multi-Tenant Isolation",
      technicalPurpose = "Route user data without exposing PII across sub-services.",
      retentionPolicy = "Duration of active account + mandatory statutory ledger lock"
    ),
    DataFieldPurposeRecord(
      fieldName = "fullName, countryCode, timezone",
      domainIsolationGroup = "Identity",
      legalBasis = "Service Provision & Anti-Triangulation Check",
      technicalPurpose = "Match beneficiary names on payout rails.",
      retentionPolicy = "Deleted upon account deletion except legally immutable settlement receipts"
    ),
    DataFieldPurposeRecord(
      fieldName = "bankName, accountNumberOrIban / nonCustodialWalletAddress",
      domainIsolationGroup = "Payment",
      legalBasis = "Explicit User Payment Routing Instruction",
      technicalPurpose = "Non-custodial settlement destination validation.",
      retentionPolicy = "Removable immediately by user"
    ),
    DataFieldPurposeRecord(
      fieldName = "kycDocumentRef / kybRegistryNumber",
      domainIsolationGroup = "Verification (Isolated)",
      legalBasis = "AML / KYB Regulatory Compliance",
      technicalPurpose = "Tiered verification; hidden from customer support screens.",
      retentionPolicy = "Statutory AML retention window (5 years post-closure, non-editable)"
    ),
    DataFieldPurposeRecord(
      fieldName = "system_audit_logs",
      domainIsolationGroup = "SecurityLogs (Append-Only)",
      legalBasis = "Legal & Security Accountability",
      technicalPurpose = "Cryptographic chain proving non-tampering; cannot be deleted by owner/staff.",
      retentionPolicy = "Immutable append-only archive"
    )
  )

  fun privacyPolicySummary(lang: AppLanguage): String = tr(
    lang,
    ar = "سياسة الخصوصية الصارمة: لا بيع للبيانات، لا قراءة للمحادثات الخاصة أو جهات الاتصال، فصل تام بين بيانات الهوية والدفع والسجلات، واستخدام معرفات داخلية معزولة مع حقوق التصدير والحذف.",
    en = "Strict Privacy by Design: Zero data sales, zero private message or contact scraping, isolated identity/payment/ledger domains, and full user rights to export or delete eligible data.",
    fr = "Confidentialité stricte : aucune vente de données, aucune lecture de messages privés ni de contacts, séparation stricte des domaines et droits complets d'export et de suppression."
  )

  /**
   * Builds a portable JSON export of the authenticated user's personal & operational data.
   */
  fun buildUserDataExportJson(
    userId: String,
    fullName: String,
    email: String,
    languageCode: String,
    countryCode: String,
    timezone: String,
    kycLevel: String,
    consents: PrivacyConsentState,
    linkedAccountLabels: List<String>,
    transactionIds: List<String>
  ): String {
    return JSONObject().apply {
      put("exportVersion", "SOCIAL_PAY_PRIVACY_EXPORT_V1")
      put("generatedAtEpochMs", System.currentTimeMillis())
      put("policyGuarantee", "Zero private chat contents collected; zero third-party ad tracking.")
      put("identity", JSONObject().apply {
        put("userId", userId)
        put("fullName", fullName)
        put("email", email)
        put("language", languageCode)
        put("countryCode", countryCode)
        put("timezone", timezone)
        put("kycLevel", kycLevel)
      })
      put("consents", JSONObject().apply {
        put("essentialPaymentRouting", consents.essentialPaymentRouting)
        put("securityAlertsAndSessions", consents.securityAlertsAndSessions)
        put("aiAssistantContextMemory", consents.aiAssistantContextMemory)
        put("externalPartnerSharing", consents.externalPartnerSharing)
        put("dataSaleProhibited", consents.dataSaleOptOutLocked)
      })
      put("linkedSettlementRails", JSONArray(linkedAccountLabels))
      put("ledgerTransactionIds", JSONArray(transactionIds))
    }.toString(2)
  }
}
