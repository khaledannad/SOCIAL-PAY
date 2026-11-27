package com.example.data

import android.content.Context
import com.example.R
import com.example.model.KycLevel
import com.example.model.PaymentAccountType
import com.example.model.UserBankAccount
import com.example.model.UserOfficeProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import androidx.annotation.Keep

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
  val userId: String = "",
  val fullName: String = "",
  val email: String = "",
  val phoneNumber: String = "",
  val nationalIdOrPassport: String = "",
  val kycLevel: String = KycLevel.BASIC_LEVEL_1.name,
  val trustScorePercent: Double = 99.8,
  val totalTradesCount: Int = 0,
  val completionRatePercent: Double = 100.0,
  val spotBalanceUsdt: Double = 0.0,
  val lockedEscrowUsdt: Double = 0.0,
  val merchantCollateralUsdt: Double = 0.0,
  val partnerCommissionsEarnedUsd: Double = 0.0,
  val myActiveAdsCount: Int = 0,
  val onlineMerchant: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toUserOfficeProfile(accounts: List<UserBankAccount> = emptyList()): UserOfficeProfile {
    val parsedKyc = enumValues<KycLevel>().firstOrNull { it.name == kycLevel } ?: KycLevel.BASIC_LEVEL_1
    return UserOfficeProfile(
      userId = userId,
      fullName = fullName,
      email = email,
      phoneNumber = phoneNumber,
      nationalIdOrPassport = nationalIdOrPassport,
      kycLevel = parsedKyc,
      trustScorePercent = trustScorePercent,
      totalTradesCount = totalTradesCount,
      completionRatePercent = completionRatePercent,
      spotBalanceUsdt = spotBalanceUsdt,
      lockedEscrowUsdt = lockedEscrowUsdt,
      merchantCollateralUsdt = merchantCollateralUsdt,
      partnerCommissionsEarnedUsd = partnerCommissionsEarnedUsd,
      linkedBankAccounts = accounts,
      myActiveAdsCount = myActiveAdsCount,
      isOnlineMerchant = onlineMerchant
    )
  }
}

@Keep
@IgnoreExtraProperties
data class PaymentAccountDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bankName: String = "",
  val accountNumberOrIban: String = "",
  val accountHolderName: String = "",
  val accountType: String = PaymentAccountType.BANK_IBAN.name,
  val currency: String = "SAR",
  val primaryDefault: Boolean = false,
  val verifiedStatus: String = "VERIFIED",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toUserBankAccount(): UserBankAccount {
    val parsedType = enumValues<PaymentAccountType>().firstOrNull { it.name == accountType }
      ?: PaymentAccountType.BANK_IBAN
    return UserBankAccount(
      id = id,
      bankName = bankName,
      accountNumberOrIban = accountNumberOrIban,
      accountHolderName = accountHolderName,
      isDefault = primaryDefault,
      accountType = parsedType,
      currency = currency,
      verifiedStatus = verifiedStatus
    )
  }
}

class AuthAndOfficeRepository(private val context: Context) {

  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  private val db: FirebaseFirestore by lazy {
    val databaseId = context.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(databaseId)
  }

  private fun requireUserId(): String =
    auth.currentUser?.uid?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("User must be authenticated")

  fun observeUserProfile(uid: String): Flow<UserOfficeProfile?> {
    if (uid.isBlank() || auth.currentUser?.uid != uid) {
      return flowOf(null)
    }
    val docRef = db.collection("users").document(uid)
    return docRef.snapshots(MetadataChanges.EXCLUDE).map { snapshot ->
      if (!snapshot.exists()) {
        null
      } else {
        val doc = snapshot.toObject(
          UserProfileDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        doc?.toUserOfficeProfile()
      }
    }
  }

  fun observePaymentAccounts(uid: String): Flow<List<UserBankAccount>> {
    if (uid.isBlank() || auth.currentUser?.uid != uid) {
      return flowOf(emptyList())
    }
    val collRef = db.collection("users").document(uid).collection("payment_accounts")
    return collRef.snapshots(MetadataChanges.EXCLUDE).map { querySnapshot ->
      querySnapshot.documents.mapNotNull { docSnapshot ->
        docSnapshot.toObject(
          PaymentAccountDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )?.toUserBankAccount()
      }.sortedWith(
        compareByDescending<UserBankAccount> { it.isDefault }
          .thenBy { it.bankName.lowercase() }
      )
    }
  }

  suspend fun syncProfileOnSignIn(user: FirebaseUser): Result<Unit> {
    val uid = user.uid
    val docPath = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val existing = docRef.get().await()
      val safeDisplayName = (user.displayName?.trim()?.take(120)).takeIf { !it.isNullOrBlank() }
        ?: user.email?.substringBefore("@")?.take(120)?.ifBlank { "Social Pay User" }
        ?: "Social Pay User"
      val safeEmail = user.email?.trim()?.take(160).orEmpty()
      val safePhone = user.phoneNumber?.trim()?.take(32).orEmpty()

      if (!existing.exists()) {
        val initialPayload = mapOf(
          "userId" to uid,
          "fullName" to safeDisplayName,
          "email" to safeEmail,
          "phoneNumber" to safePhone,
          "nationalIdOrPassport" to "",
          "kycLevel" to KycLevel.BASIC_LEVEL_1.name,
          "trustScorePercent" to 99.8,
          "totalTradesCount" to 0,
          "completionRatePercent" to 100.0,
          "spotBalanceUsdt" to 0.0,
          "lockedEscrowUsdt" to 0.0,
          "merchantCollateralUsdt" to 0.0,
          "partnerCommissionsEarnedUsd" to 0.0,
          "myActiveAdsCount" to 0,
          "onlineMerchant" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(initialPayload).await()
      } else if (safeEmail.isNotBlank()) {
        docRef.update(
          mapOf(
            "email" to safeEmail,
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

  suspend fun updateProfileAndKyc(
    fullName: String,
    phoneNumber: String,
    nationalIdOrPassport: String,
    upgradeToVerifiedPro: Boolean = true
  ): Result<Unit> {
    var docPath = "users/unauthenticated"
    return try {
      val uid = requireUserId()
      docPath = "users/$uid"

      val cleanName = fullName.trim()
      val cleanPhone = phoneNumber.trim()
      val cleanId = nationalIdOrPassport.trim()

      require(cleanName.length in 1..120) {
        "Full name must be between 1 and 120 characters."
      }
      require(cleanPhone.length <= 32) {
        "Phone number must not exceed 32 characters."
      }
      require(cleanId.length <= 64) {
        "National ID or Passport must not exceed 64 characters."
      }

      val targetKyc = when {
        upgradeToVerifiedPro && cleanId.length >= 4 && cleanPhone.length >= 5 -> KycLevel.VERIFIED_PRO_LEVEL_2
        cleanName.isNotBlank() -> KycLevel.BASIC_LEVEL_1
        else -> KycLevel.UNVERIFIED
      }

      db.collection("users").document(uid)
        .update(
          mapOf(
            "fullName" to cleanName,
            "phoneNumber" to cleanPhone,
            "nationalIdOrPassport" to cleanId,
            "kycLevel" to targetKyc.name,
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

  suspend fun toggleMerchantOnlineStatus(isOnline: Boolean): Result<Unit> {
    var docPath = "users/unauthenticated"
    return try {
      val uid = requireUserId()
      docPath = "users/$uid"
      db.collection("users").document(uid)
        .update(
          mapOf(
            "onlineMerchant" to isOnline,
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

  suspend fun depositCollateral(amountUsdt: Double): Result<Unit> {
    var docPath = "users/unauthenticated"
    return try {
      val uid = requireUserId()
      docPath = "users/$uid"
      require(amountUsdt > 0.0 && amountUsdt.isFinite()) {
        "Collateral amount must be greater than zero."
      }
      val docRef = db.collection("users").document(uid)
      db.runTransaction { txn ->
        val snap = txn.get(docRef)
        val currentCollateral = snap.getDouble("merchantCollateralUsdt") ?: 0.0
        val currentSpot = snap.getDouble("spotBalanceUsdt") ?: 0.0
        val updatedSpot = (currentSpot - amountUsdt).coerceAtLeast(0.0)
        txn.update(
          docRef,
          mapOf(
            "merchantCollateralUsdt" to (currentCollateral + amountUsdt),
            "spotBalanceUsdt" to updatedSpot,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun recordSpotDeposit(amountUsdt: Double): Result<Unit> {
    var docPath = "users/unauthenticated"
    return try {
      val uid = requireUserId()
      docPath = "users/$uid"
      require(amountUsdt > 0.0 && amountUsdt.isFinite()) {
        "Deposit amount must be greater than zero."
      }
      val docRef = db.collection("users").document(uid)
      db.runTransaction { txn ->
        val snap = txn.get(docRef)
        val currentSpot = snap.getDouble("spotBalanceUsdt") ?: 0.0
        txn.update(
          docRef,
          mapOf(
            "spotBalanceUsdt" to (currentSpot + amountUsdt),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun recordSpotWithdrawal(amountUsdt: Double): Result<Double> {
    var docPath = "users/unauthenticated"
    return try {
      val uid = requireUserId()
      docPath = "users/$uid"
      require(amountUsdt > 0.0 && amountUsdt.isFinite()) {
        "Withdrawal amount must be greater than zero."
      }
      val docRef = db.collection("users").document(uid)
      val withdrawn = db.runTransaction { txn ->
        val snap = txn.get(docRef)
        val currentSpot = snap.getDouble("spotBalanceUsdt") ?: 0.0
        require(currentSpot >= amountUsdt) {
          "Insufficient Spot balance ($currentSpot USDT)."
        }
        val nextSpot = (currentSpot - amountUsdt).coerceAtLeast(0.0)
        txn.update(
          docRef,
          mapOf(
            "spotBalanceUsdt" to nextSpot,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        nextSpot
      }.await()
      Result.success(withdrawn)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun claimPartnerCommissions(): Result<Double> {
    var docPath = "users/unauthenticated"
    return try {
      val uid = requireUserId()
      docPath = "users/$uid"
      val docRef = db.collection("users").document(uid)
      val claimedAmount = db.runTransaction { txn ->
        val snap = txn.get(docRef)
        val commissions = snap.getDouble("partnerCommissionsEarnedUsd") ?: 0.0
        require(commissions > 0.0) {
          "No pending partner commissions available to claim."
        }
        val currentSpot = snap.getDouble("spotBalanceUsdt") ?: 0.0
        txn.update(
          docRef,
          mapOf(
            "spotBalanceUsdt" to (currentSpot + commissions),
            "partnerCommissionsEarnedUsd" to 0.0,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        commissions
      }.await()
      Result.success(claimedAmount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun linkPaymentAccount(
    bankName: String,
    ibanOrAccount: String,
    holderName: String,
    accountType: PaymentAccountType = PaymentAccountType.BANK_IBAN,
    currency: String = "SAR",
    setAsDefault: Boolean = false
  ): Result<String> {
    var docPath = "users/unauthenticated/payment_accounts"
    return try {
      val uid = requireUserId()
      val cleanBank = bankName.trim()
      val cleanIban = ibanOrAccount.trim()
      val cleanHolder = holderName.trim()
      val cleanCurrency = currency.trim().uppercase().ifBlank { "SAR" }

      require(cleanBank.length in 2..80) {
        "Provider / bank name must be 2-80 characters."
      }
      require(cleanIban.length in 4..64) {
        "Account number / IBAN must be 4-64 characters."
      }
      require(cleanHolder.length in 2..120) {
        "Account holder name must be 2-120 characters."
      }
      require(cleanCurrency.length in 2..10) {
        "Currency code must be 2-10 characters."
      }

      val collRef = db.collection("users").document(uid).collection("payment_accounts")
      val existingDocs = collRef.get().await()
      val makePrimary = setAsDefault || existingDocs.isEmpty

      val newDocRef = collRef.document()
      docPath = newDocRef.path

      val batch = db.batch()
      if (makePrimary) {
        existingDocs.documents.forEach { sibling ->
          if (sibling.getBoolean("primaryDefault") == true) {
            batch.update(
              sibling.reference,
              mapOf(
                "primaryDefault" to false,
                "updatedAt" to FieldValue.serverTimestamp()
              )
            )
          }
        }
      }

      val payload = mapOf(
        "userId" to uid,
        "bankName" to cleanBank,
        "accountNumberOrIban" to cleanIban,
        "accountHolderName" to cleanHolder,
        "accountType" to accountType.name,
        "currency" to cleanCurrency,
        "primaryDefault" to makePrimary,
        "verifiedStatus" to "VERIFIED",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      batch.set(newDocRef, payload)
      batch.commit().await()

      Result.success(newDocRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun setDefaultPaymentAccount(accountId: String): Result<Unit> {
    var docPath = "users/unauthenticated/payment_accounts/$accountId"
    return try {
      val uid = requireUserId()
      val collRef = db.collection("users").document(uid).collection("payment_accounts")
      docPath = "${collRef.path}/$accountId"
      val existingDocs = collRef.get().await()
      val batch = db.batch()
      existingDocs.documents.forEach { doc ->
        val shouldBePrimary = doc.id == accountId
        val currentlyPrimary = doc.getBoolean("primaryDefault") ?: false
        if (shouldBePrimary != currentlyPrimary) {
          batch.update(
            doc.reference,
            mapOf(
              "primaryDefault" to shouldBePrimary,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun removePaymentAccount(accountId: String): Result<Unit> {
    var docPath = "users/unauthenticated/payment_accounts/$accountId"
    return try {
      val uid = requireUserId()
      val targetRef = db.collection("users").document(uid)
        .collection("payment_accounts").document(accountId)
      docPath = targetRef.path
      targetRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun signOut(onBeforeSignOut: suspend () -> Unit = {}) {
    CredentialAuthHelper.signOutWithCredentialClear(context, auth, onBeforeSignOut)
  }
}
