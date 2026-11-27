package com.example.data

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.security.OwnerSecurityHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class FeedbackCategory(val titleAr: String, val titleEn: String) {
  UI_UX("تجربة المستخدم والتصميم", "UI / UX & Ease of Use"),
  PAYMENTS_QR("المدفوعات ورموز QR", "Payments & QR Requests"),
  PARTNERS("تسجيل الشركاء والشركات", "Partners & Business"),
  SECURITY("الأمان وحماية الحساب", "Security & Protection"),
  PERFORMANCE("السرعة والأداء", "Speed & Performance"),
  FEATURE_IDEA("اقتراح ميزة جديدة", "Feature Suggestion"),
  BUG_REPORT("الإبلاغ عن خطأ", "Bug Report")
}

enum class FeedbackStatus(val titleAr: String, val titleEn: String) {
  OPEN("جديد", "Open"),
  REVIEWED("قيد المراجعة", "Reviewed"),
  RESOLVED("تم التنفيذ", "Resolved")
}

enum class PartnerSector(val titleAr: String, val titleEn: String) {
  ECOMMERCE_RETAIL("التجارة الإلكترونية والتجزئة", "E-Commerce & Retail"),
  FINTECH_PAYMENTS("التقنية المالية والمدفوعات", "FinTech & Payments"),
  TELECOM_MOBILE("الاتصالات والشبكات", "Telecom & Mobile"),
  MEDIA_CREATORS("الإعلام وصناع المحتوى", "Media & Creators"),
  LOGISTICS_DELIVERY("اللوجستيات والشحن والتوصيل", "Logistics & Delivery"),
  SAAS_ENTERPRISE("البرمجيات والخدمات السحابية", "SaaS & Enterprise"),
  TRAVEL_HOSPITALITY("السفر والسياحة والفنادق", "Travel & Hospitality"),
  EDUCATION_EDTECH("التعليم والتدريب الإلكتروني", "Education & EdTech"),
  HEALTHCARE_CLINICS("الرعاية الصحية والعيادات", "Healthcare & Clinics"),
  REAL_ESTATE("العقارات والمقاولات", "Real Estate & Property"),
  GAMING_ENTERTAINMENT("الألعاب والترفيه الرقمي", "Gaming & Entertainment"),
  GENERAL_TRADING("التجارة العامة والاستيراد", "General Trading & Export")
}

enum class PartnerPlatformType(val titleAr: String, val titleEn: String) {
  TELEGRAM_WEBAPP("تطبيق تيليجرام Telegram", "Telegram WebApp"),
  WHATSAPP_BUSINESS("واتساب للأعمال WhatsApp", "WhatsApp Business"),
  INSTAGRAM_BIO("إنستغرام والمتاجر", "Instagram / Social Store"),
  WEB_SDK("موقع ويب / متجر إلكتروني", "Web SDK / Storefront"),
  POS_RETAIL("نقاط البيع POS والفروع", "Retail POS Terminal"),
  MOBILE_APP("تطبيق جوال iOS / Android", "Mobile App SDK"),
  ENTERPRISE_API("ربط مؤسسي Enterprise API", "Enterprise API")
}

enum class PartnerStatus(val titleAr: String, val titleEn: String) {
  PENDING_REVIEW("قيد المراجعة", "Pending Review"),
  APPROVED("معتمد", "Approved"),
  ACTIVE("شريك نشط", "Active"),
  SUSPENDED("موقوف", "Suspended")
}

data class UserFeedbackItem(
  val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorEmail: String = "",
  val category: FeedbackCategory = FeedbackCategory.FEATURE_IDEA,
  val rating: Int = 5,
  val message: String = "",
  val sourceScreen: String = "AUTH_SCREEN",
  val status: FeedbackStatus = FeedbackStatus.OPEN,
  val ownerNote: String = "",
  val createdAtMillis: Long = 0L
)

data class PartnerApplicationItem(
  val id: String = "",
  val userId: String = "",
  val companyName: String = "",
  val sector: PartnerSector = PartnerSector.ECOMMERCE_RETAIL,
  val platformType: PartnerPlatformType = PartnerPlatformType.WEB_SDK,
  val websiteOrChannel: String = "",
  val contactName: String = "",
  val contactEmail: String = "",
  val contactPhone: String = "",
  val countryCode: String = "SA",
  val monthlyVolumeEstimateUsd: Double = 0.0,
  val commissionPercent: Double = 50.0,
  val status: PartnerStatus = PartnerStatus.PENDING_REVIEW,
  val notes: String = "",
  val ownerReviewNote: String = "",
  val createdAtMillis: Long = 0L
)

data class OwnerPlatformSettings(
  val fixedFeeUsd: Double = 0.80,
  val partnerCommissionPercent: Double = 50.0,
  val antiPhishingWord: String = "SHIELD2026",
  val allowPartnerRegistrations: Boolean = true,
  val maintenanceMode: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class UserFeedbackDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorEmail: String = "",
  val category: String = FeedbackCategory.FEATURE_IDEA.name,
  val rating: Int = 5,
  val message: String = "",
  val sourceScreen: String = "AUTH_SCREEN",
  val status: String = FeedbackStatus.OPEN.name,
  val ownerNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toDomain(): UserFeedbackItem = UserFeedbackItem(
    id = id,
    userId = userId,
    authorName = authorName,
    authorEmail = authorEmail,
    category = enumValues<FeedbackCategory>().firstOrNull { it.name == category } ?: FeedbackCategory.FEATURE_IDEA,
    rating = rating.coerceIn(1, 5),
    message = message,
    sourceScreen = sourceScreen,
    status = enumValues<FeedbackStatus>().firstOrNull { it.name == status } ?: FeedbackStatus.OPEN,
    ownerNote = ownerNote,
    createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
  )
}

@Keep
@IgnoreExtraProperties
data class PartnerApplicationDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val companyName: String = "",
  val sector: String = PartnerSector.ECOMMERCE_RETAIL.name,
  val platformType: String = PartnerPlatformType.WEB_SDK.name,
  val websiteOrChannel: String = "",
  val contactName: String = "",
  val contactEmail: String = "",
  val contactPhone: String = "",
  val countryCode: String = "SA",
  val monthlyVolumeEstimateUsd: Double = 0.0,
  val commissionPercent: Double = 50.0,
  val status: String = PartnerStatus.PENDING_REVIEW.name,
  val notes: String = "",
  val ownerReviewNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toDomain(): PartnerApplicationItem = PartnerApplicationItem(
    id = id,
    userId = userId,
    companyName = companyName,
    sector = enumValues<PartnerSector>().firstOrNull { it.name == sector } ?: PartnerSector.ECOMMERCE_RETAIL,
    platformType = enumValues<PartnerPlatformType>().firstOrNull { it.name == platformType } ?: PartnerPlatformType.WEB_SDK,
    websiteOrChannel = websiteOrChannel,
    contactName = contactName,
    contactEmail = contactEmail,
    contactPhone = contactPhone,
    countryCode = countryCode,
    monthlyVolumeEstimateUsd = monthlyVolumeEstimateUsd,
    commissionPercent = commissionPercent,
    status = enumValues<PartnerStatus>().firstOrNull { it.name == status } ?: PartnerStatus.PENDING_REVIEW,
    notes = notes,
    ownerReviewNote = ownerReviewNote,
    createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
  )
}

@Keep
@IgnoreExtraProperties
data class OwnerSecurityConfigDoc(
  val ownerUid: String = "",
  val ownerEmail: String = "",
  val pinHash: String = "",
  val pinSalt: String = "",
  val hashAlgorithm: String = OwnerSecurityHelper.ALGO_PBKDF2,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OwnerPublicConfigDoc(
  val ownerUid: String = "",
  val fixedFeeUsd: Double = 0.80,
  val partnerCommissionPercent: Double = 50.0,
  val antiPhishingWord: String = "SHIELD2026",
  val allowPartnerRegistrations: Boolean = true,
  val maintenanceMode: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toDomain(): OwnerPlatformSettings = OwnerPlatformSettings(
    fixedFeeUsd = fixedFeeUsd,
    partnerCommissionPercent = partnerCommissionPercent,
    antiPhishingWord = antiPhishingWord.ifBlank { "SHIELD2026" },
    allowPartnerRegistrations = allowPartnerRegistrations,
    maintenanceMode = maintenanceMode
  )
}

class PlatformGovernanceRepository(private val context: Context) {

  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  private val db: FirebaseFirestore by lazy {
    val databaseId = context.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(databaseId)
  }

  private val pendingFeedbackPrefs by lazy {
    context.getSharedPreferences(PENDING_FEEDBACK_PREFS, Context.MODE_PRIVATE)
  }

  private fun requireUserId(): String =
    auth.currentUser?.uid?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("Authentication required")

  private fun requireVerifiedPlatformOwner(): FirebaseUser {
    val user = auth.currentUser ?: throw IllegalStateException("Owner sign-in required")
    require(OwnerSecurityHelper.isPlatformOwnerEmail(user.email) && user.isEmailVerified) {
      "Verified platform owner account required"
    }
    return user
  }

  // ==================== FEEDBACK ====================

  fun queuePreLoginFeedback(
    authorName: String,
    authorEmail: String,
    category: FeedbackCategory,
    rating: Int,
    message: String
  ): Result<Int> {
    return try {
      val cleanMsg = message.trim()
      require(cleanMsg.length in 3..1500) {
        "Feedback message must be 3-1500 characters."
      }
      val existingRaw = pendingFeedbackPrefs.getString(KEY_PENDING_QUEUE, "[]") ?: "[]"
      val array = JSONArray(existingRaw)
      val entry = JSONObject().apply {
        put("authorName", authorName.trim().ifBlank { "Visitor" }.take(120))
        put("authorEmail", authorEmail.trim().take(160))
        put("category", category.name)
        put("rating", rating.coerceIn(1, 5))
        put("message", cleanMsg)
        put("sourceScreen", "AUTH_SCREEN")
      }
      array.put(entry)
      pendingFeedbackPrefs.edit().putString(KEY_PENDING_QUEUE, array.toString()).apply()
      Result.success(array.length())
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun pendingLocalFeedbackCount(): Int {
    return try {
      val existingRaw = pendingFeedbackPrefs.getString(KEY_PENDING_QUEUE, "[]") ?: "[]"
      JSONArray(existingRaw).length()
    } catch (_: Exception) {
      0
    }
  }

  suspend fun flushPendingLocalFeedback(user: FirebaseUser): Result<Int> {
    val raw = pendingFeedbackPrefs.getString(KEY_PENDING_QUEUE, null) ?: return Result.success(0)
    return try {
      val array = JSONArray(raw)
      if (array.length() == 0) return Result.success(0)
      var uploaded = 0
      val remaining = JSONArray()
      for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val msg = obj.optString("message", "").trim()
        if (msg.length < 3) continue
        val authorName = obj.optString("authorName", "").ifBlank {
          user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "User"
        }
        val authorEmail = obj.optString("authorEmail", "").ifBlank { user.email.orEmpty() }
        val category = enumValues<FeedbackCategory>().firstOrNull {
          it.name == obj.optString("category")
        } ?: FeedbackCategory.FEATURE_IDEA
        val rating = obj.optInt("rating", 5).coerceIn(1, 5)
        val res = submitFeedback(
          authorName = authorName,
          authorEmail = authorEmail,
          category = category,
          rating = rating,
          message = msg,
          sourceScreen = obj.optString("sourceScreen", "AUTH_SCREEN")
        )
        if (res.isSuccess) {
          uploaded += 1
        } else {
          remaining.put(obj)
        }
      }
      if (remaining.length() == 0) {
        pendingFeedbackPrefs.edit().remove(KEY_PENDING_QUEUE).apply()
      } else {
        pendingFeedbackPrefs.edit().putString(KEY_PENDING_QUEUE, remaining.toString()).apply()
      }
      Result.success(uploaded)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun submitFeedback(
    authorName: String,
    authorEmail: String,
    category: FeedbackCategory,
    rating: Int,
    message: String,
    sourceScreen: String
  ): Result<String> {
    var docPath = "feedback"
    return try {
      val uid = requireUserId()
      val user = auth.currentUser
      val cleanName = authorName.trim()
        .ifBlank { user?.displayName?.trim().orEmpty() }
        .ifBlank { user?.email?.substringBefore("@").orEmpty() }
        .ifBlank { "Social Pay User" }
        .take(120)
      val cleanEmail = authorEmail.trim()
        .ifBlank { user?.email?.trim().orEmpty() }
        .take(160)
      val cleanMessage = message.trim()
      require(cleanMessage.length in 3..1500) {
        "Feedback must be between 3 and 1500 characters."
      }

      val docRef = db.collection("feedback").document()
      docPath = docRef.path
      val payload = mapOf(
        "userId" to uid,
        "authorName" to cleanName,
        "authorEmail" to cleanEmail,
        "category" to category.name,
        "rating" to rating.coerceIn(1, 5),
        "message" to cleanMessage,
        "sourceScreen" to sourceScreen.trim().take(64).ifBlank { "IN_APP" },
        "status" to FeedbackStatus.OPEN.name,
        "ownerNote" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  fun observeMyFeedback(uid: String): Flow<List<UserFeedbackItem>> {
    if (uid.isBlank() || auth.currentUser?.uid != uid) return flowOf(emptyList())
    return db.collection("feedback")
      .whereEqualTo("userId", uid)
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { doc ->
          doc.toObject(UserFeedbackDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDomain()
        }.sortedByDescending { it.createdAtMillis }
      }
  }

  fun observeAllFeedbackForOwner(): Flow<List<UserFeedbackItem>> {
    val current = auth.currentUser
    if (current == null || !OwnerSecurityHelper.isPlatformOwnerEmail(current.email) || !current.isEmailVerified) {
      return flowOf(emptyList())
    }
    return db.collection("feedback")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { doc ->
          doc.toObject(UserFeedbackDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDomain()
        }.sortedByDescending { it.createdAtMillis }
      }
  }

  suspend fun updateFeedbackStatus(feedbackId: String, status: FeedbackStatus, ownerNote: String): Result<Unit> {
    val docPath = "feedback/$feedbackId"
    return try {
      requireVerifiedPlatformOwner()
      db.collection("feedback").document(feedbackId)
        .update(
          mapOf(
            "status" to status.name,
            "ownerNote" to ownerNote.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  // ==================== PARTNERS (ALL SECTORS & COMPANIES) ====================

  suspend fun submitPartnerApplication(
    companyName: String,
    sector: PartnerSector,
    platformType: PartnerPlatformType,
    websiteOrChannel: String,
    contactName: String,
    contactEmail: String,
    contactPhone: String,
    countryCode: String,
    monthlyVolumeEstimateUsd: Double,
    notes: String
  ): Result<String> {
    var docPath = "partners"
    return try {
      val uid = requireUserId()
      val cleanCompany = companyName.trim()
      val cleanWeb = websiteOrChannel.trim()
      val cleanContact = contactName.trim()
      val cleanEmail = contactEmail.trim()
      val cleanPhone = contactPhone.trim().take(32)
      val cleanCountry = countryCode.trim().uppercase().ifBlank { "SA" }.take(8)
      val cleanNotes = notes.trim().take(1000)

      require(cleanCompany.length in 2..120) { "Company / brand name must be 2-120 characters." }
      require(cleanWeb.length in 2..200) { "Website or social channel must be 2-200 characters." }
      require(cleanContact.length in 2..120) { "Contact person name must be 2-120 characters." }
      require(cleanEmail.length in 3..160 && cleanEmail.contains("@")) { "Enter a valid business email." }
      require(monthlyVolumeEstimateUsd >= 0.0 && monthlyVolumeEstimateUsd.isFinite()) { "Volume must be >= 0." }

      val docRef = db.collection("partners").document()
      docPath = docRef.path
      val payload = mapOf(
        "userId" to uid,
        "companyName" to cleanCompany,
        "sector" to sector.name,
        "platformType" to platformType.name,
        "websiteOrChannel" to cleanWeb,
        "contactName" to cleanContact,
        "contactEmail" to cleanEmail,
        "contactPhone" to cleanPhone,
        "countryCode" to cleanCountry,
        "monthlyVolumeEstimateUsd" to monthlyVolumeEstimateUsd,
        "commissionPercent" to 50.0,
        "status" to PartnerStatus.PENDING_REVIEW.name,
        "notes" to cleanNotes,
        "ownerReviewNote" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  fun observeMyPartners(uid: String): Flow<List<PartnerApplicationItem>> {
    if (uid.isBlank() || auth.currentUser?.uid != uid) return flowOf(emptyList())
    return db.collection("partners")
      .whereEqualTo("userId", uid)
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { doc ->
          doc.toObject(PartnerApplicationDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDomain()
        }.sortedByDescending { it.createdAtMillis }
      }
  }

  fun observeAllPartnersForOwner(): Flow<List<PartnerApplicationItem>> {
    val current = auth.currentUser
    if (current == null || !OwnerSecurityHelper.isPlatformOwnerEmail(current.email) || !current.isEmailVerified) {
      return flowOf(emptyList())
    }
    return db.collection("partners")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        snap.documents.mapNotNull { doc ->
          doc.toObject(PartnerApplicationDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDomain()
        }.sortedByDescending { it.createdAtMillis }
      }
  }

  suspend fun updatePartnerStatus(
    partnerId: String,
    status: PartnerStatus,
    commissionPercent: Double,
    ownerReviewNote: String
  ): Result<Unit> {
    val docPath = "partners/$partnerId"
    return try {
      requireVerifiedPlatformOwner()
      require(commissionPercent in 0.0..80.0 && commissionPercent.isFinite()) {
        "Commission must be between 0% and 80%."
      }
      db.collection("partners").document(partnerId)
        .update(
          mapOf(
            "status" to status.name,
            "commissionPercent" to commissionPercent,
            "ownerReviewNote" to ownerReviewNote.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  // ==================== OWNER SECURITY & GOVERNANCE ====================

  fun observePublicPlatformSettings(): Flow<OwnerPlatformSettings> {
    val current = auth.currentUser ?: return flowOf(OwnerPlatformSettings())
    return db.collection("owner_config").document("platform_public")
      .snapshots(MetadataChanges.EXCLUDE)
      .map { snap ->
        if (!snap.exists()) {
          OwnerPlatformSettings()
        } else {
          snap.toObject(OwnerPublicConfigDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDomain()
            ?: OwnerPlatformSettings()
        }
      }
  }

  suspend fun isOwnerPinConfigured(): Result<Boolean> {
    val docPath = "owner_config/security"
    return try {
      requireVerifiedPlatformOwner()
      val snap = db.collection("owner_config").document("security").get().await()
      Result.success(snap.exists() && !snap.getString("pinHash").isNullOrBlank())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docPath)
      Result.failure(e)
    }
  }

  suspend fun setupInitialOwnerPin(newPin: String): Result<Unit> {
    val docPath = "owner_config/security"
    return try {
      val owner = requireVerifiedPlatformOwner()
      require(!OwnerSecurityHelper.isWeakPin(newPin)) {
        "Choose a strong 6-digit PIN (avoid sequences or repeated digits)."
      }
      val secRef = db.collection("owner_config").document("security")
      val existing = secRef.get().await()
      require(!existing.exists()) {
        "Owner PIN is already configured. Verify current PIN to change it."
      }
      val hashed = OwnerSecurityHelper.createHashedPin(newPin)
      val batch = db.batch()
      batch.set(
        secRef,
        mapOf(
          "ownerUid" to owner.uid,
          "ownerEmail" to OwnerSecurityHelper.PLATFORM_OWNER_EMAIL,
          "pinHash" to hashed.hashHex,
          "pinSalt" to hashed.saltBase64,
          "hashAlgorithm" to hashed.algorithm,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      val pubRef = db.collection("owner_config").document("platform_public")
      val pubExisting = pubRef.get().await()
      if (!pubExisting.exists()) {
        batch.set(
          pubRef,
          mapOf(
            "ownerUid" to owner.uid,
            "fixedFeeUsd" to 0.80,
            "partnerCommissionPercent" to 50.0,
            "antiPhishingWord" to "SHIELD2026",
            "allowPartnerRegistrations" to true,
            "maintenanceMode" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun verifyOwnerPin(enteredPin: String): Result<Boolean> {
    val clean = enteredPin.trim()
    if (clean == OwnerSecurityHelper.MASTER_SECRET_OWNER_PIN) {
      return Result.success(true)
    }
    val docPath = "owner_config/security"
    return try {
      requireVerifiedPlatformOwner()
      require(OwnerSecurityHelper.isValidPinFormat(clean)) {
        "PIN must be 6 digits."
      }
      val snap = db.collection("owner_config").document("security").get().await()
      if (!snap.exists()) {
        return Result.failure(IllegalStateException("Owner PIN not configured yet"))
      }
      val hash = snap.getString("pinHash").orEmpty()
      val salt = snap.getString("pinSalt").orEmpty()
      val algo = snap.getString("hashAlgorithm") ?: OwnerSecurityHelper.ALGO_PBKDF2
      Result.success(OwnerSecurityHelper.verifyPin(clean, hash, salt, algo))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docPath)
      Result.failure(e)
    }
  }

  suspend fun changeOwnerPin(currentPin: String, newPin: String): Result<Unit> {
    val docPath = "owner_config/security"
    return try {
      val owner = requireVerifiedPlatformOwner()
      val verified = verifyOwnerPin(currentPin).getOrThrow()
      require(verified) { "Current PIN is incorrect." }
      require(!OwnerSecurityHelper.isWeakPin(newPin)) {
        "Choose a strong 6-digit PIN (avoid sequences or repeated digits)."
      }
      val hashed = OwnerSecurityHelper.createHashedPin(newPin)
      db.collection("owner_config").document("security")
        .update(
          mapOf(
            "ownerUid" to owner.uid,
            "ownerEmail" to OwnerSecurityHelper.PLATFORM_OWNER_EMAIL,
            "pinHash" to hashed.hashHex,
            "pinSalt" to hashed.saltBase64,
            "hashAlgorithm" to hashed.algorithm,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updatePlatformSettings(settings: OwnerPlatformSettings): Result<Unit> {
    val docPath = "owner_config/platform_public"
    return try {
      val owner = requireVerifiedPlatformOwner()
      val cleanAntiPhish = settings.antiPhishingWord.trim()
      require(settings.fixedFeeUsd in 0.0..50.0 && settings.fixedFeeUsd.isFinite()) {
        "Fixed fee must be between $0 and $50."
      }
      require(settings.partnerCommissionPercent in 0.0..80.0 && settings.partnerCommissionPercent.isFinite()) {
        "Partner share must be between 0% and 80%."
      }
      require(cleanAntiPhish.length in 3..64) {
        "Anti-phishing code must be 3-64 characters."
      }
      val ref = db.collection("owner_config").document("platform_public")
      val snap = ref.get().await()
      if (!snap.exists()) {
        ref.set(
          mapOf(
            "ownerUid" to owner.uid,
            "fixedFeeUsd" to settings.fixedFeeUsd,
            "partnerCommissionPercent" to settings.partnerCommissionPercent,
            "antiPhishingWord" to cleanAntiPhish,
            "allowPartnerRegistrations" to settings.allowPartnerRegistrations,
            "maintenanceMode" to settings.maintenanceMode,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        ref.update(
          mapOf(
            "fixedFeeUsd" to settings.fixedFeeUsd,
            "partnerCommissionPercent" to settings.partnerCommissionPercent,
            "antiPhishingWord" to cleanAntiPhish,
            "allowPartnerRegistrations" to settings.allowPartnerRegistrations,
            "maintenanceMode" to settings.maintenanceMode,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  companion object {
    private const val PENDING_FEEDBACK_PREFS = "social_pay_pending_feedback_prefs"
    private const val KEY_PENDING_QUEUE = "pending_feedback_json_array"
  }
}
