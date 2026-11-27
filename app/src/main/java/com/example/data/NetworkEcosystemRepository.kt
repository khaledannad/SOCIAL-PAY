package com.example.data

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.core.connectivity.ConnectorConnectionState
import com.example.core.connectivity.RuntimeEnvironmentMode
import com.example.core.privacy.EmergencyStepPhase
import com.example.core.privacy.VerificationApplicantType
import com.example.core.privacy.VerificationWorkflowStatus
import com.example.security.OwnerSecurityHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

enum class MerchantStaffRole(
  val titleAr: String,
  val titleEn: String,
  val titleFr: String
) {
  MANAGER("مدير العمليات", "Operations Manager", "Responsable Opérations"),
  CASHIER("أمين صندوق / نقطة بيع", "Cashier / POS", "Caissier / POS"),
  FINANCE_VIEWER("مراجع مالي (قراءة فقط)", "Finance Viewer", "Lecteur Finance"),
  INTEGRATION_DEV("مطور التكاملات والـ API", "Integration Developer", "Développeur API")
}

@Keep
@IgnoreExtraProperties
data class PrivacyRequestRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val requestType: String = "DATA_EXPORT",
  val status: String = "SUBMITTED",
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KycKybSubmissionRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val applicantType: String = VerificationApplicantType.INDIVIDUAL_KYC.name,
  val entityOrFullName: String = "",
  val countryCode: String = "DZ",
  val registryOrIdNumber: String = "",
  val documentReferenceHash: String = "",
  val status: String = VerificationWorkflowStatus.SUBMITTED.name,
  val reviewerNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MerchantProfileRecord(
  @DocumentId val id: String = "",
  val ownerUserId: String = "",
  val businessName: String = "",
  val countryCode: String = "DZ",
  val kybStatus: String = VerificationWorkflowStatus.NOT_STARTED.name,
  val webhookUrl: String = "",
  val apiKeyReference: String = "",
  val defaultStablecoin: String = "USDC",
  val defaultNetwork: String = "SOL",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MerchantStaffRecord(
  @DocumentId val id: String = "",
  val merchantOwnerUid: String = "",
  val staffEmail: String = "",
  val staffName: String = "",
  val role: String = MerchantStaffRole.CASHIER.name,
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudLedgerTransactionRecord(
  @DocumentId val id: String = "",
  val senderUserId: String = "",
  val transactionId: String = "",
  val intentId: String = "",
  val stablecoinTicker: String = "USDC",
  val networkCode: String = "SOL",
  val amount: Double = 0.0,
  val declaredFeeUsd: Double = 0.80,
  val recipientName: String = "",
  val recipientAddressOrRail: String = "",
  val providerId: String = "SANDBOX_MOCK_GATEWAY",
  val providerReference: String = "",
  val state: String = "CREATED",
  val reconciliationStatus: String = "PENDING_PROVIDER_PROOF",
  val disputeReason: String = "",
  val idempotencyKey: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudConnectorStateRecord(
  @DocumentId val id: String = "",
  val connectorId: String = "",
  val name: String = "",
  val category: String = "",
  val status: String = ConnectorConnectionState.INTEGRATION_PENDING.name,
  val environment: String = RuntimeEnvironmentMode.SANDBOX.name,
  val latencyMs: Int = 0,
  val statusNote: String = "",
  val updatedByUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAuditLogRecord(
  @DocumentId val id: String = "",
  val actorUid: String = "",
  val targetId: String = "",
  val action: String = "",
  val details: String = "",
  val entryHash: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudEmergencyIncidentRecord(
  @DocumentId val id: String = "",
  val ownerUid: String = "",
  val title: String = "",
  val affectedService: String = "",
  val severity: String = "ELEVATED",
  val phase: String = EmergencyStepPhase.IDENTIFY_SERVICE.name,
  val serviceIsolated: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Cloud Firestore Repository for SOCIAL PAY's Three-System Ecosystem:
 * - Privacy & Data Minimization Requests
 * - KYC / KYB Verification Queue
 * - Merchant Profiles & Staff RBAC
 * - Immutable Ledger Transactions
 * - Global Connectivity Hub Connector States
 * - Append-Only Cryptographic Audit Logs
 * - Emergency Response & Isolation Incidents
 * - Owner-only User Directory Observation
 */
class NetworkEcosystemRepository(private val context: Context) {

  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  private val db: FirebaseFirestore by lazy {
    val databaseId = context.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(databaseId)
  }

  private fun requireVerifiedUserId(): String {
    val user = auth.currentUser ?: throw IllegalStateException("Authentication required")
    return user.uid
  }

  private fun isVerifiedOwnerSession(): Boolean {
    val user = auth.currentUser ?: return false
    return OwnerSecurityHelper.isPlatformOwnerEmail(user.email) && user.isEmailVerified
  }

  private fun computeSha256Hex(raw: String): String {
    return MessageDigest.getInstance("SHA-256")
      .digest(raw.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
  }

  // =========================================================================
  // 1. PRIVACY REQUESTS (users/{userId}/privacy_requests)
  // =========================================================================

  fun observeMyPrivacyRequests(uid: String): Flow<List<PrivacyRequestRecord>> {
    if (uid.isBlank() || auth.currentUser?.uid != uid) return flowOf(emptyList())
    return db.collection("users").document(uid)
      .collection("privacy_requests")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { doc ->
          doc.toObject(PrivacyRequestRecord::class.java)?.copy(
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          )
        }
      }
  }

  suspend fun submitPrivacyRequest(
    requestType: String,
    details: String
  ): Result<String> {
    return try {
      val uid = requireVerifiedUserId()
      val cleanType = requestType.trim().uppercase().let {
        if (it in setOf("DATA_EXPORT", "ACCOUNT_DELETION", "CONSENT_UPDATE", "REVOKE_INTEGRATIONS")) it
        else "CONSENT_UPDATE"
      }
      val ref = db.collection("users").document(uid).collection("privacy_requests").document()
      val payload = hashMapOf(
        "userId" to uid,
        "requestType" to cleanType,
        "status" to "SUBMITTED",
        "details" to details.trim().take(1000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      ref.set(payload).await()
      appendAuditLog(
        targetId = ref.id,
        action = "PRIVACY_$cleanType",
        details = details.trim().take(200)
      )
      Result.success(ref.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/privacy_requests")
      Result.failure(e)
    }
  }

  // =========================================================================
  // 2. KYC & KYB VERIFICATION SUBMISSIONS (kyc_kyb_submissions)
  // =========================================================================

  fun observeMyKycKybSubmissions(uid: String): Flow<List<KycKybSubmissionRecord>> {
    if (uid.isBlank() || auth.currentUser?.uid != uid) return flowOf(emptyList())
    return db.collection("kyc_kyb_submissions")
      .whereEqualTo("userId", uid)
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { it.toObject(KycKybSubmissionRecord::class.java) }
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
  }

  fun observeAllKycKybForOwner(): Flow<List<KycKybSubmissionRecord>> {
    if (!isVerifiedOwnerSession()) return flowOf(emptyList())
    return db.collection("kyc_kyb_submissions")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { it.toObject(KycKybSubmissionRecord::class.java) }
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
  }

  suspend fun submitKycKybVerification(
    applicantType: VerificationApplicantType,
    entityOrFullName: String,
    countryCode: String,
    registryOrIdNumber: String,
    documentReferenceHash: String
  ): Result<String> {
    return try {
      val uid = requireVerifiedUserId()
      require(entityOrFullName.trim().length >= 2) { "Full name or entity name is required" }
      require(registryOrIdNumber.trim().length >= 3) { "Document or registry number is required" }
      val ref = db.collection("kyc_kyb_submissions").document()
      val payload = hashMapOf(
        "userId" to uid,
        "applicantType" to applicantType.name,
        "entityOrFullName" to entityOrFullName.trim().take(140),
        "countryCode" to countryCode.trim().uppercase().take(8).ifBlank { "DZ" },
        "registryOrIdNumber" to registryOrIdNumber.trim().take(80),
        "documentReferenceHash" to documentReferenceHash.trim().take(256),
        "status" to VerificationWorkflowStatus.SUBMITTED.name,
        "reviewerNote" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      ref.set(payload).await()
      appendAuditLog(
        targetId = ref.id,
        action = "KYC_KYB_SUBMITTED",
        details = "${applicantType.name} • ${countryCode.uppercase()}"
      )
      Result.success(ref.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "kyc_kyb_submissions")
      Result.failure(e)
    }
  }

  suspend fun reviewKycKybByOwner(
    submissionId: String,
    newStatus: VerificationWorkflowStatus,
    reviewerNote: String
  ): Result<Unit> {
    return try {
      if (!isVerifiedOwnerSession()) throw SecurityException("Owner access required")
      db.collection("kyc_kyb_submissions").document(submissionId).update(
        mapOf(
          "status" to newStatus.name,
          "reviewerNote" to reviewerNote.trim().take(500),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      appendAuditLog(
        targetId = submissionId,
        action = "OWNER_KYC_REVIEW_${newStatus.name}",
        details = reviewerNote.trim().take(200)
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "kyc_kyb_submissions/$submissionId")
      Result.failure(e)
    }
  }

  // =========================================================================
  // 3. MERCHANT PORTAL & STAFF RBAC (merchant_profiles & staff)
  // =========================================================================

  fun observeMyMerchantProfile(uid: String): Flow<MerchantProfileRecord?> {
    if (uid.isBlank() || auth.currentUser?.uid != uid) return flowOf(null)
    return db.collection("merchant_profiles").document(uid)
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        if (snap.exists()) snap.toObject(MerchantProfileRecord::class.java) else null
      }
  }

  fun observeAllMerchantsForOwner(): Flow<List<MerchantProfileRecord>> {
    if (!isVerifiedOwnerSession()) return flowOf(emptyList())
    return db.collection("merchant_profiles")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap -> snap.documents.mapNotNull { it.toObject(MerchantProfileRecord::class.java) } }
  }

  suspend fun saveMerchantProfile(
    businessName: String,
    countryCode: String,
    webhookUrl: String,
    defaultStablecoin: String,
    defaultNetwork: String
  ): Result<Unit> {
    return try {
      val uid = requireVerifiedUserId()
      require(businessName.trim().length >= 2) { "Business name is required" }
      val safeStablecoin = defaultStablecoin.trim().uppercase().let {
        if (it in setOf("USDC", "USDT", "PYUSD", "EURC", "FDUSD")) it else "USDC"
      }
      val docRef = db.collection("merchant_profiles").document(uid)
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val createMap = hashMapOf(
          "ownerUserId" to uid,
          "businessName" to businessName.trim().take(140),
          "countryCode" to countryCode.trim().uppercase().take(8).ifBlank { "DZ" },
          "kybStatus" to VerificationWorkflowStatus.NOT_STARTED.name,
          "webhookUrl" to webhookUrl.trim().take(300),
          "apiKeyReference" to "MERCH_KEY_${uid.takeLast(6).uppercase()}",
          "defaultStablecoin" to safeStablecoin,
          "defaultNetwork" to defaultNetwork.trim().uppercase().take(24).ifBlank { "SOL" },
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(createMap).await()
      } else {
        docRef.update(
          mapOf(
            "businessName" to businessName.trim().take(140),
            "countryCode" to countryCode.trim().uppercase().take(8).ifBlank { "DZ" },
            "webhookUrl" to webhookUrl.trim().take(300),
            "defaultStablecoin" to safeStablecoin,
            "defaultNetwork" to defaultNetwork.trim().uppercase().take(24).ifBlank { "SOL" },
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "merchant_profiles")
      Result.failure(e)
    }
  }

  fun observeMerchantStaff(merchantUid: String): Flow<List<MerchantStaffRecord>> {
    if (merchantUid.isBlank() || auth.currentUser?.uid != merchantUid) return flowOf(emptyList())
    return db.collection("merchant_profiles").document(merchantUid)
      .collection("staff")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap -> snap.documents.mapNotNull { it.toObject(MerchantStaffRecord::class.java) } }
  }

  suspend fun addMerchantStaff(
    staffName: String,
    staffEmail: String,
    role: MerchantStaffRole
  ): Result<String> {
    return try {
      val uid = requireVerifiedUserId()
      require(staffName.isNotBlank() && staffEmail.contains("@")) { "Valid staff name & email required" }
      val staffRef = db.collection("merchant_profiles").document(uid)
        .collection("staff").document()
      val payload = hashMapOf(
        "merchantOwnerUid" to uid,
        "staffEmail" to staffEmail.trim().take(160),
        "staffName" to staffName.trim().take(120),
        "role" to role.name,
        "active" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      staffRef.set(payload).await()
      appendAuditLog(staffRef.id, "MERCHANT_STAFF_ADDED", "${role.name}: ${staffEmail.trim()}")
      Result.success(staffRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "merchant_profiles/staff")
      Result.failure(e)
    }
  }

  suspend fun removeMerchantStaff(staffId: String): Result<Unit> {
    return try {
      val uid = requireVerifiedUserId()
      db.collection("merchant_profiles").document(uid)
        .collection("staff").document(staffId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "merchant_profiles/staff/$staffId")
      Result.failure(e)
    }
  }

  // =========================================================================
  // 4. CLOUD LEDGER TRANSACTIONS (ledger_transactions - Immutable Amounts)
  // =========================================================================

  fun observeMyLedgerTransactions(uid: String): Flow<List<CloudLedgerTransactionRecord>> {
    if (uid.isBlank() || auth.currentUser?.uid != uid) return flowOf(emptyList())
    return db.collection("ledger_transactions")
      .whereEqualTo("senderUserId", uid)
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { it.toObject(CloudLedgerTransactionRecord::class.java) }
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
  }

  fun observeAllLedgerTransactionsForOwner(): Flow<List<CloudLedgerTransactionRecord>> {
    if (!isVerifiedOwnerSession()) return flowOf(emptyList())
    return db.collection("ledger_transactions")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { it.toObject(CloudLedgerTransactionRecord::class.java) }
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
  }

  suspend fun createCloudLedgerTransaction(
    transactionId: String,
    intentId: String,
    stablecoinTicker: String,
    networkCode: String,
    amount: Double,
    declaredFeeUsd: Double,
    recipientName: String,
    recipientAddressOrRail: String,
    providerId: String,
    idempotencyKey: String,
    initialState: String = "PENDING"
  ): Result<Unit> {
    return try {
      val uid = requireVerifiedUserId()
      val safeState = if (initialState in setOf("CREATED", "PENDING")) initialState else "PENDING"
      val docRef = db.collection("ledger_transactions").document(transactionId)
      val payload = hashMapOf(
        "senderUserId" to uid,
        "transactionId" to transactionId.take(64),
        "intentId" to intentId.take(64),
        "stablecoinTicker" to stablecoinTicker.uppercase().take(10),
        "networkCode" to networkCode.uppercase().take(24),
        "amount" to amount,
        "declaredFeeUsd" to declaredFeeUsd.coerceAtLeast(0.0),
        "recipientName" to recipientName.trim().take(120),
        "recipientAddressOrRail" to recipientAddressOrRail.trim().take(128),
        "providerId" to providerId.trim().take(64).ifBlank { "SANDBOX_MOCK_GATEWAY" },
        "providerReference" to "",
        "state" to safeState,
        "reconciliationStatus" to "PENDING_PROVIDER_PROOF",
        "disputeReason" to "",
        "idempotencyKey" to idempotencyKey.take(128),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      appendAuditLog(
        targetId = transactionId,
        action = "LEDGER_TX_CREATED",
        details = "$amount $stablecoinTicker ($networkCode) -> $recipientName"
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "ledger_transactions/$transactionId")
      Result.failure(e)
    }
  }

  /**
   * Updates status/proof ONLY — never modifies immutable amount, fee, or recipient fields.
   */
  suspend fun updateCloudLedgerTransactionStatus(
    transactionId: String,
    newState: String,
    providerReference: String = "",
    reconciliationStatus: String = "PENDING_PROVIDER_PROOF",
    disputeReason: String = ""
  ): Result<Unit> {
    return try {
      requireVerifiedUserId()
      db.collection("ledger_transactions").document(transactionId).update(
        mapOf(
          "state" to newState,
          "providerReference" to providerReference.take(128),
          "reconciliationStatus" to reconciliationStatus,
          "disputeReason" to disputeReason.take(500),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      appendAuditLog(
        targetId = transactionId,
        action = "LEDGER_TX_STATE_$newState",
        details = if (disputeReason.isNotBlank()) disputeReason else "Reconciliation=$reconciliationStatus Ref=$providerReference"
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "ledger_transactions/$transactionId")
      Result.failure(e)
    }
  }

  // =========================================================================
  // 5. GLOBAL CONNECTIVITY HUB CONNECTORS (connectivity_connectors)
  // =========================================================================

  fun observeConnectivityConnectors(): Flow<List<CloudConnectorStateRecord>> {
    if (auth.currentUser == null) return flowOf(emptyList())
    return db.collection("connectivity_connectors")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap -> snap.documents.mapNotNull { it.toObject(CloudConnectorStateRecord::class.java) } }
  }

  suspend fun saveConnectorStateByOwner(
    connectorId: String,
    name: String,
    category: String,
    status: ConnectorConnectionState,
    environment: RuntimeEnvironmentMode,
    latencyMs: Int,
    statusNote: String
  ): Result<Unit> {
    return try {
      if (!isVerifiedOwnerSession()) throw SecurityException("Owner access required")
      val uid = requireVerifiedUserId()
      val docRef = db.collection("connectivity_connectors").document(connectorId)
      val snap = docRef.get().await()
      if (!snap.exists()) {
        docRef.set(
          hashMapOf(
            "connectorId" to connectorId.take(64),
            "name" to name.take(120),
            "category" to category.take(64),
            "status" to status.name,
            "environment" to environment.name,
            "latencyMs" to latencyMs.coerceAtLeast(0),
            "statusNote" to statusNote.take(400),
            "updatedByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "status" to status.name,
            "environment" to environment.name,
            "latencyMs" to latencyMs.coerceAtLeast(0),
            "statusNote" to statusNote.take(400),
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      appendAuditLog(
        targetId = connectorId,
        action = "CONNECTOR_${status.name}",
        details = "Env=${environment.name}, Latency=${latencyMs}ms: ${statusNote.take(160)}"
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "connectivity_connectors/$connectorId")
      Result.failure(e)
    }
  }

  // =========================================================================
  // 6. IMMUTABLE AUDIT LOGS & EMERGENCY INCIDENTS
  // =========================================================================

  fun observeAuditLogsForOwner(): Flow<List<CloudAuditLogRecord>> {
    if (!isVerifiedOwnerSession()) return flowOf(emptyList())
    return db.collection("system_audit_logs")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(100)
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap -> snap.documents.mapNotNull { it.toObject(CloudAuditLogRecord::class.java) } }
  }

  suspend fun appendAuditLog(
    targetId: String,
    action: String,
    details: String
  ): Result<String> {
    return try {
      val uid = requireVerifiedUserId()
      val safeTarget = targetId.trim().take(128).ifBlank { "SYSTEM" }
      val safeAction = action.trim().take(80).ifBlank { "EVENT" }
      val safeDetails = details.trim().take(600)
      val hash = computeSha256Hex("$uid|$safeTarget|$safeAction|$safeDetails|${System.currentTimeMillis()}")
      val ref = db.collection("system_audit_logs").document()
      ref.set(
        hashMapOf(
          "actorUid" to uid,
          "targetId" to safeTarget,
          "action" to safeAction,
          "details" to safeDetails,
          "entryHash" to hash,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(ref.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "system_audit_logs")
      Result.failure(e)
    }
  }

  fun observeEmergencyIncidentsForOwner(): Flow<List<CloudEmergencyIncidentRecord>> {
    if (!isVerifiedOwnerSession()) return flowOf(emptyList())
    return db.collection("emergency_incidents")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap -> snap.documents.mapNotNull { it.toObject(CloudEmergencyIncidentRecord::class.java) } }
  }

  suspend fun raiseEmergencyIncidentByOwner(
    title: String,
    affectedService: String,
    severity: String,
    phase: EmergencyStepPhase,
    serviceIsolated: Boolean
  ): Result<String> {
    return try {
      if (!isVerifiedOwnerSession()) throw SecurityException("Owner access required")
      val uid = requireVerifiedUserId()
      val safeSeverity = if (severity in setOf("ELEVATED", "HIGH", "CRITICAL")) severity else "HIGH"
      val ref = db.collection("emergency_incidents").document()
      ref.set(
        hashMapOf(
          "ownerUid" to uid,
          "title" to title.trim().take(140),
          "affectedService" to affectedService.trim().take(80),
          "severity" to safeSeverity,
          "phase" to phase.name,
          "serviceIsolated" to serviceIsolated,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      appendAuditLog(ref.id, "EMERGENCY_INCIDENT_${safeSeverity}", "$title | Service=$affectedService | Isolated=$serviceIsolated")
      Result.success(ref.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "emergency_incidents")
      Result.failure(e)
    }
  }

  suspend fun advanceEmergencyPhaseByOwner(
    incidentId: String,
    phase: EmergencyStepPhase,
    serviceIsolated: Boolean
  ): Result<Unit> {
    return try {
      if (!isVerifiedOwnerSession()) throw SecurityException("Owner access required")
      db.collection("emergency_incidents").document(incidentId).update(
        mapOf(
          "phase" to phase.name,
          "serviceIsolated" to serviceIsolated,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      appendAuditLog(incidentId, "EMERGENCY_PHASE_${phase.name}", "serviceIsolated=$serviceIsolated")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "emergency_incidents/$incidentId")
      Result.failure(e)
    }
  }

  // =========================================================================
  // 7. OWNER-ONLY DIRECTORY OBSERVATION
  // =========================================================================

  fun observeAllUsersForOwner(): Flow<List<UserProfileDoc>> {
    if (!isVerifiedOwnerSession()) return flowOf(emptyList())
    return db.collection("users")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap -> snap.documents.mapNotNull { it.toObject(UserProfileDoc::class.java) } }
  }
}
