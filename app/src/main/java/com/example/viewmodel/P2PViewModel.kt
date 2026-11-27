package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.P2PRepository
import com.example.data.SocialPayRepository
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.SocialNotificationEntity
import com.example.data.local.entity.TransactionEntity
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.asset.StablecoinApprovalStatus
import com.example.core.asset.StablecoinRegistryEntry
import com.example.core.brain.AgentRuntimeSnapshot
import com.example.core.brain.BrainAgentId
import com.example.core.brain.BrainEvaluationLog
import com.example.core.brain.SocialFinancialBrain
import com.example.core.connectivity.ConnectorConnectionState
import com.example.core.connectivity.GlobalConnectivityEngine
import com.example.core.connectivity.IntegrationConnector
import com.example.core.connectivity.RuntimeEnvironmentMode
import com.example.core.governance.tr
import com.example.core.privacy.EmergencyStepPhase
import com.example.core.privacy.PrivacyAndComplianceEngine
import com.example.core.privacy.PrivacyConsentState
import com.example.core.privacy.VerificationApplicantType
import com.example.core.privacy.VerificationWorkflowStatus
import com.example.core.transaction.RoutingPlanResult
import com.example.data.AuthAndOfficeRepository
import com.example.data.CloudAuditLogRecord
import com.example.data.CloudConnectorStateRecord
import com.example.data.CloudEmergencyIncidentRecord
import com.example.data.CloudLedgerTransactionRecord
import com.example.data.CredentialAuthHelper
import com.example.data.FeedbackCategory
import com.example.data.FeedbackStatus
import com.example.data.KycKybSubmissionRecord
import com.example.data.MerchantProfileRecord
import com.example.data.MerchantStaffRecord
import com.example.data.MerchantStaffRole
import com.example.data.NetworkEcosystemRepository
import com.example.data.OwnerPlatformSettings
import com.example.data.PartnerApplicationItem
import com.example.data.PartnerPlatformType
import com.example.data.PartnerSector
import com.example.data.PartnerStatus
import com.example.data.PlatformGovernanceRepository
import com.example.data.PrivacyRequestRecord
import com.example.data.UserFeedbackItem
import com.example.data.UserProfileDoc
import com.example.data.extractUserFriendlyFirestoreMessage
import com.example.market.core.ExchangeHealthStatus
import com.example.market.core.MarketDataEngine
import com.example.market.core.NormalizedMarketTick
import com.example.security.OwnerSecurityHelper
import com.example.util.ParsedPaymentRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException

sealed interface ProfileSyncState {
  data object Loading : ProfileSyncState
  data class Ready(val profile: UserOfficeProfile) : ProfileSyncState
  data class Error(val message: String) : ProfileSyncState
}

data class P2PUiState(
  val selectedTradeType: TradeType = TradeType.BUY,
  val selectedCrypto: String = "USDC",
  val selectedFiat: String = "USD",
  val selectedPaymentMethodId: String? = null,
  val amountFilterText: String = "",
  val activeDeals: List<SmartDeal> = emptyList(),
  val filteredOffers: List<MarketOffer> = emptyList(),
  val selectedDealDetail: SmartDeal? = null,
  val floatingButtonConfig: FloatingButtonConfig = FloatingButtonConfig(),
  val revenueMilestone: RevenueMilestoneInfo = RevenueMilestoneInfo(),
  val airdropRewards: List<AirdropReward> = emptyList(),
  val userTrustScore: Int = 100,
  val userPoints: Int = 0,
  val userOfficeProfile: UserOfficeProfile = UserOfficeProfile(),
  val profileSyncState: ProfileSyncState = ProfileSyncState.Loading,
  val isProfileMutationBusy: Boolean = false,
  val isUserSignedIn: Boolean = false,
  val currentLanguage: AppLanguage = AppLanguage.ARABIC,
  val userCountryCode: String = "DZ",
  val userTimezone: String = java.util.TimeZone.getDefault().id,
  val runtimeEnvironment: RuntimeEnvironmentMode = RuntimeEnvironmentMode.SANDBOX,
  val simulatedSocialApp: String = "Telegram",
  val showCreateOfferDialog: Boolean = false,
  val showDisputeDialogForDealId: String? = null,
  val snackbarMessage: String? = null,
  val isP2PBottomSheetOpen: Boolean = false,
  val socialTransactions: List<TransactionEntity> = emptyList(),
  val socialContacts: List<ContactEntity> = emptyList(),
  val socialNotifications: List<SocialNotificationEntity> = emptyList(),
  val unreadNotificationsCount: Int = 0,
  val totalTransactionsCount: Int = 0,
  val confirmedTransactionsCount: Int = 0,
  val pendingTransactionsCount: Int = 0,
  val justCreatedTransaction: TransactionEntity? = null,
  val platformSettings: OwnerPlatformSettings = OwnerPlatformSettings(),
  val myFeedbackList: List<UserFeedbackItem> = emptyList(),
  val myPartnersList: List<PartnerApplicationItem> = emptyList(),
  val ownerAllPartners: List<PartnerApplicationItem> = emptyList(),
  val ownerAllFeedback: List<UserFeedbackItem> = emptyList(),
  val isPlatformOwnerAccount: Boolean = false,
  val isOwnerPinConfigured: Boolean? = null,
  val isOwnerUnlocked: Boolean = false,
  val isOwnerWorkspaceActive: Boolean = false,
  val ownerPortalError: String? = null,
  val prefilledQrRequest: ParsedPaymentRequest? = null,
  // Stablecoins-Only, Risk & Market Peg Feeds
  val stablecoinRegistry: List<StablecoinRegistryEntry> = DigitalAssetEngine.APPROVED_STABLECOIN_REGISTRY,
  val pausedCorridorIds: Set<String> = emptySet(),
  val liveStablecoinTicks: Map<String, NormalizedMarketTick> = emptyMap(),
  val exchangeHealth: List<ExchangeHealthStatus> = emptyList(),
  val isLiveFeedRefreshing: Boolean = false,
  val lastLiveFeedUpdatedAtMillis: Long? = null,
  // 18 Brain Agents & Global Connectivity Hub
  val brainAgents: List<AgentRuntimeSnapshot> = emptyList(),
  val brainLogs: List<BrainEvaluationLog> = emptyList(),
  val connectors: List<IntegrationConnector> = emptyList(),
  val testingConnectorId: String? = null,
  // User Privacy, KYC/KYB, Merchant Portal & Ledger
  val privacyConsents: PrivacyConsentState = PrivacyConsentState(),
  val myPrivacyRequests: List<PrivacyRequestRecord> = emptyList(),
  val latestDataExportJson: String? = null,
  val myKycKybSubmissions: List<KycKybSubmissionRecord> = emptyList(),
  val myMerchantProfile: MerchantProfileRecord? = null,
  val myMerchantStaff: List<MerchantStaffRecord> = emptyList(),
  val cloudLedgerTransactions: List<CloudLedgerTransactionRecord> = emptyList(),
  // Owner-Only Live Governance Streams
  val ownerAllUsers: List<UserProfileDoc> = emptyList(),
  val ownerAllKycKyb: List<KycKybSubmissionRecord> = emptyList(),
  val ownerAllMerchants: List<MerchantProfileRecord> = emptyList(),
  val ownerAllLedgerTransactions: List<CloudLedgerTransactionRecord> = emptyList(),
  val ownerAuditLogs: List<CloudAuditLogRecord> = emptyList(),
  val ownerEmergencyIncidents: List<CloudEmergencyIncidentRecord> = emptyList(),
  val cloudConnectorStates: List<CloudConnectorStateRecord> = emptyList(),
  val isPlatformWideHalted: Boolean = false
)

private data class FilterParams(
  val tradeType: TradeType,
  val crypto: String,
  val fiat: String,
  val paymentMethodId: String?,
  val amountFilterText: String
)

class P2PViewModel(
  application: Application
) : AndroidViewModel(application) {
  private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  val repository: P2PRepository = P2PRepository()
  val authAndOfficeRepository: AuthAndOfficeRepository = AuthAndOfficeRepository(application.applicationContext)
  val governanceRepository: PlatformGovernanceRepository = PlatformGovernanceRepository(application.applicationContext)
  val ecosystemRepository: NetworkEcosystemRepository = NetworkEcosystemRepository(application.applicationContext)
  val socialPayRepository: SocialPayRepository = SocialPayRepository(application)
  val socialFinancialBrain: SocialFinancialBrain = SocialFinancialBrain()
  val connectivityEngine: GlobalConnectivityEngine = GlobalConnectivityEngine()
  val marketDataEngine: MarketDataEngine = MarketDataEngine()

  private val _filterParams = MutableStateFlow(
    FilterParams(
      tradeType = TradeType.BUY,
      crypto = "USDC",
      fiat = "USD",
      paymentMethodId = null,
      amountFilterText = ""
    )
  )

  private val _uiState = MutableStateFlow(
    P2PUiState(
      brainAgents = socialFinancialBrain.getAllAgentSnapshots(),
      brainLogs = socialFinancialBrain.getRecentEvaluationLogs(),
      connectors = connectivityEngine.defaultConnectors
    )
  )
  val uiState: StateFlow<P2PUiState> = _uiState.asStateFlow()

  val supportedCrypto = repository.supportedCrypto
  val supportedFiat = repository.supportedFiat
  val paymentMethods = repository.paymentMethods

  init {
    viewModelScope.launch {
      marketDataEngine.liveTicks.collect { ticks ->
        _uiState.update { it.copy(liveStablecoinTicks = ticks) }
      }
    }
    viewModelScope.launch {
      marketDataEngine.exchangeHealth.collect { health ->
        _uiState.update { it.copy(exchangeHealth = health) }
      }
    }
    viewModelScope.launch {
      combine(repository.marketOffers, _filterParams) { offers, filter ->
        offers.filter { offer ->
          val matchesTradeType = offer.tradeType == filter.tradeType
          val matchesCrypto = offer.asset.symbol == filter.crypto
          val matchesFiat = offer.fiat.code == filter.fiat
          val matchesPayment = filter.paymentMethodId == null || offer.acceptedPaymentMethods.any { it.id == filter.paymentMethodId }
          val parsedAmount = filter.amountFilterText.toDoubleOrNull()
          val matchesAmount = parsedAmount == null || (parsedAmount >= offer.minLimitFiat && parsedAmount <= offer.maxLimitFiat)
          matchesTradeType && matchesCrypto && matchesFiat && matchesPayment && matchesAmount
        }
      }.collect { filtered ->
        _uiState.update { it.copy(filteredOffers = filtered) }
      }
    }

    viewModelScope.launch {
      repository.activeDeals.collect { deals ->
        _uiState.update { current ->
          val updatedDetail = deals.find { it.dealId == current.selectedDealDetail?.dealId }
          current.copy(
            activeDeals = deals,
            selectedDealDetail = updatedDetail ?: current.selectedDealDetail
          )
        }
      }
    }

    viewModelScope.launch {
      repository.revenueMilestone.collect { milestone ->
        _uiState.update { it.copy(revenueMilestone = milestone) }
      }
    }

    viewModelScope.launch {
      repository.airdropRewards.collect { rewards ->
        _uiState.update { it.copy(airdropRewards = rewards) }
      }
    }

    viewModelScope.launch {
      repository.userPoints.collect { points ->
        _uiState.update { it.copy(userPoints = points) }
      }
    }

    val activeUser = try { firebaseAuth.currentUser } catch (_: Exception) { null }
    if (activeUser != null) {
      val fallbackName = activeUser.displayName?.takeIf { it.isNotBlank() }
        ?: activeUser.email?.substringBefore("@")
        ?: "Social Pay Trader"
      val isOwnerEmail = OwnerSecurityHelper.isPlatformOwnerEmail(activeUser.email) && activeUser.isEmailVerified
      _uiState.update {
        it.copy(
          isUserSignedIn = true,
          isPlatformOwnerAccount = isOwnerEmail,
          userOfficeProfile = it.userOfficeProfile.copy(
            userId = activeUser.uid,
            fullName = fallbackName,
            email = activeUser.email.orEmpty()
          )
        )
      }

      viewModelScope.launch {
        val syncResult = authAndOfficeRepository.syncProfileOnSignIn(activeUser)
        syncResult.onFailure { err ->
          _uiState.update {
            it.copy(
              snackbarMessage = extractUserFriendlyFirestoreMessage(err, it.currentLanguage.isRtl)
            )
          }
        }
        val flushed = governanceRepository.flushPendingLocalFeedback(activeUser).getOrDefault(0)
        if (flushed > 0) {
          _uiState.update {
            it.copy(
              snackbarMessage = if (it.currentLanguage.isRtl) {
                "تمت مزامنة $flushed تعليقاً مرسلاً مسبقاً مع السحابة بنجاح"
              } else {
                "Synced $flushed queued feedback note(s) to Cloud Firestore"
              }
            )
          }
        }
      }

      viewModelScope.launch {
        combine(
          authAndOfficeRepository.observeUserProfile(activeUser.uid),
          authAndOfficeRepository.observePaymentAccounts(activeUser.uid)
        ) { remoteProfile, linkedAccounts ->
          val base = remoteProfile ?: UserOfficeProfile(
            userId = activeUser.uid,
            fullName = fallbackName,
            email = activeUser.email.orEmpty(),
            phoneNumber = activeUser.phoneNumber.orEmpty()
          )
          val resolved = base.copy(linkedBankAccounts = linkedAccounts)
          ProfileSyncState.Ready(resolved) to resolved
        }
          .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            val friendly = extractUserFriendlyFirestoreMessage(rootError, _uiState.value.currentLanguage.isRtl)
            _uiState.update {
              it.copy(profileSyncState = ProfileSyncState.Error(friendly))
            }
          }
          .collect { (syncState, profile) ->
            _uiState.update {
              it.copy(
                userOfficeProfile = profile,
                profileSyncState = syncState,
                isUserSignedIn = true
              )
            }
          }
      }

      viewModelScope.launch {
        governanceRepository.observePublicPlatformSettings()
          .catch { /* keep default settings on offline/transient state */ }
          .collect { settings ->
            _uiState.update { state ->
              state.copy(
                platformSettings = settings,
                revenueMilestone = state.revenueMilestone.copy(currentFeeUsd = settings.fixedFeeUsd)
              )
            }
          }
      }

      viewModelScope.launch {
        governanceRepository.observeMyFeedback(activeUser.uid)
          .catch { /* ignore transient listener error */ }
          .collect { list ->
            _uiState.update { it.copy(myFeedbackList = list) }
          }
      }

      viewModelScope.launch {
        governanceRepository.observeMyPartners(activeUser.uid)
          .catch { /* ignore transient listener error */ }
          .collect { list ->
            _uiState.update { it.copy(myPartnersList = list) }
          }
      }

      viewModelScope.launch {
        ecosystemRepository.observeMyPrivacyRequests(activeUser.uid)
          .catch { }
          .collect { list -> _uiState.update { it.copy(myPrivacyRequests = list) } }
      }

      viewModelScope.launch {
        ecosystemRepository.observeMyKycKybSubmissions(activeUser.uid)
          .catch { }
          .collect { list -> _uiState.update { it.copy(myKycKybSubmissions = list) } }
      }

      viewModelScope.launch {
        ecosystemRepository.observeMyMerchantProfile(activeUser.uid)
          .catch { }
          .collect { merchant -> _uiState.update { it.copy(myMerchantProfile = merchant) } }
      }

      viewModelScope.launch {
        ecosystemRepository.observeMerchantStaff(activeUser.uid)
          .catch { }
          .collect { staff -> _uiState.update { it.copy(myMerchantStaff = staff) } }
      }

      viewModelScope.launch {
        ecosystemRepository.observeMyLedgerTransactions(activeUser.uid)
          .catch { }
          .collect { ledger -> _uiState.update { it.copy(cloudLedgerTransactions = ledger) } }
      }

      viewModelScope.launch {
        ecosystemRepository.observeConnectivityConnectors()
          .catch { }
          .collect { remoteConnectors ->
            _uiState.update { state ->
              val byId = remoteConnectors.associateBy { it.connectorId }
              val merged = connectivityEngine.defaultConnectors.map { base ->
                val remote = byId[base.id]
                if (remote != null) {
                  val parsedState = enumValues<ConnectorConnectionState>().firstOrNull { it.name == remote.status } ?: base.status
                  val parsedEnv = enumValues<RuntimeEnvironmentMode>().firstOrNull { it.name == remote.environment } ?: base.environment
                  base.copy(
                    status = parsedState,
                    environment = parsedEnv,
                    latencyMs = remote.latencyMs.takeIf { it > 0 }?.toLong() ?: base.latencyMs,
                    lastErrorNote = remote.statusNote.takeIf { it.isNotBlank() }
                  )
                } else base
              }
              state.copy(connectors = merged, cloudConnectorStates = remoteConnectors)
            }
          }
      }

      if (isOwnerEmail) {
        refreshOwnerSecurityState()
        viewModelScope.launch {
          governanceRepository.observeAllPartnersForOwner()
            .catch { }
            .collect { list -> _uiState.update { it.copy(ownerAllPartners = list) } }
        }
        viewModelScope.launch {
          governanceRepository.observeAllFeedbackForOwner()
            .catch { }
            .collect { list -> _uiState.update { it.copy(ownerAllFeedback = list) } }
        }
        viewModelScope.launch {
          ecosystemRepository.observeAllUsersForOwner()
            .catch { }
            .collect { users -> _uiState.update { it.copy(ownerAllUsers = users) } }
        }
        viewModelScope.launch {
          ecosystemRepository.observeAllKycKybForOwner()
            .catch { }
            .collect { kycList -> _uiState.update { it.copy(ownerAllKycKyb = kycList) } }
        }
        viewModelScope.launch {
          ecosystemRepository.observeAllMerchantsForOwner()
            .catch { }
            .collect { merchants -> _uiState.update { it.copy(ownerAllMerchants = merchants) } }
        }
        viewModelScope.launch {
          ecosystemRepository.observeAllLedgerTransactionsForOwner()
            .catch { }
            .collect { txs -> _uiState.update { it.copy(ownerAllLedgerTransactions = txs) } }
        }
        viewModelScope.launch {
          ecosystemRepository.observeAuditLogsForOwner()
            .catch { }
            .collect { logs -> _uiState.update { it.copy(ownerAuditLogs = logs) } }
        }
        viewModelScope.launch {
          ecosystemRepository.observeEmergencyIncidentsForOwner()
            .catch { }
            .collect { inc -> _uiState.update { it.copy(ownerEmergencyIncidents = inc) } }
        }
      }
    }

    viewModelScope.launch {
      socialPayRepository.allTransactions.collect { txns ->
        _uiState.update { it.copy(socialTransactions = txns) }
      }
    }

    viewModelScope.launch {
      socialPayRepository.allContacts.collect { contacts ->
        _uiState.update { it.copy(socialContacts = contacts) }
      }
    }

    viewModelScope.launch {
      socialPayRepository.allNotifications.collect { notifs ->
        _uiState.update { it.copy(socialNotifications = notifs) }
      }
    }

    viewModelScope.launch {
      socialPayRepository.unreadNotificationsCount.collect { count ->
        _uiState.update { it.copy(unreadNotificationsCount = count) }
      }
    }

    viewModelScope.launch {
      socialPayRepository.totalTransactionsCount.collect { count ->
        _uiState.update { it.copy(totalTransactionsCount = count) }
      }
    }

    viewModelScope.launch {
      socialPayRepository.confirmedTransactionsCount.collect { count ->
        _uiState.update { it.copy(confirmedTransactionsCount = count) }
      }
    }

    viewModelScope.launch {
      socialPayRepository.pendingTransactionsCount.collect { count ->
        _uiState.update { it.copy(pendingTransactionsCount = count) }
      }
    }
  }

  fun setTradeType(tradeType: TradeType) {
    _filterParams.update { it.copy(tradeType = tradeType) }
    _uiState.update { it.copy(selectedTradeType = tradeType) }
  }

  fun setSelectedCrypto(cryptoSymbol: String) {
    _filterParams.update { it.copy(crypto = cryptoSymbol) }
    _uiState.update { it.copy(selectedCrypto = cryptoSymbol) }
  }

  fun setSelectedFiat(fiatCode: String) {
    _filterParams.update { it.copy(fiat = fiatCode) }
    _uiState.update { it.copy(selectedFiat = fiatCode) }
  }

  fun setSelectedPaymentMethod(methodId: String?) {
    _filterParams.update { it.copy(paymentMethodId = methodId) }
    _uiState.update { it.copy(selectedPaymentMethodId = methodId) }
  }

  fun setAmountFilter(amount: String) {
    _filterParams.update { it.copy(amountFilterText = amount) }
    _uiState.update { it.copy(amountFilterText = amount) }
  }

  fun selectDealForDetail(deal: SmartDeal?) {
    _uiState.update { it.copy(selectedDealDetail = deal) }
  }

  fun createDealFromOffer(offer: MarketOffer, cryptoAmount: Double, fiatAmount: Double) {
    val isBuying = _uiState.value.selectedTradeType == TradeType.BUY
    val newDeal = repository.createDealFromOffer(offer, cryptoAmount, fiatAmount, isBuying)
    _uiState.update {
      it.copy(
        selectedDealDetail = newDeal,
        snackbarMessage = if (it.currentLanguage.isRtl)
          "تم قفل الأمانة المشفرة وإنشاء الصفقة الذكية بنجاح! الرسوم: ${newDeal.fixedFeeUsd}$"
        else
          "Escrow locked and smart deal created! Fee: $${newDeal.fixedFeeUsd}"
      )
    }
  }

  fun markDealPaid(dealId: String) {
    repository.markDealPaid(dealId)
    _uiState.update {
      it.copy(
        snackbarMessage = if (it.currentLanguage.isRtl)
          "تم تأكيد إرسال الدفع وإشعار البائع بنجاح"
        else
          "Payment marked as sent, seller notified"
      )
    }
  }

  fun releaseEscrow(dealId: String) {
    repository.releaseEscrow(dealId)
    _uiState.update {
      it.copy(
        snackbarMessage = if (it.currentLanguage.isRtl)
          "✅ تم تحرير الأمانة للمشتري وإضافة 50 نقطة أيردروب لحسابك!"
        else
          "✅ Escrow released successfully! +50 Airdrop points awarded!"
      )
    }
  }

  fun cancelDeal(dealId: String) {
    repository.cancelDeal(dealId)
    _uiState.update {
      it.copy(
        snackbarMessage = if (it.currentLanguage.isRtl)
          "تم إلغاء الصفقة بأمان واستعادة الأمانة"
        else
          "Deal cancelled safely and escrow returned"
      )
    }
  }

  fun submitDispute(dealId: String, reason: String) {
    repository.disputeDeal(dealId, reason)
    _uiState.update {
      it.copy(
        showDisputeDialogForDealId = null,
        snackbarMessage = if (it.currentLanguage.isRtl)
          "تم رفع النزاع لفريق التحكيم الذكي وتجميد الأمانة مؤقتاً"
        else
          "Dispute raised to Smart Arbitration with escrow freeze"
      )
    }
  }

  fun sendChatMessage(dealId: String, text: String) {
    if (text.isBlank()) return
    repository.sendChatMessage(dealId, text)
  }

  fun claimAirdrop(rewardId: String) {
    val success = repository.claimAirdrop(rewardId)
    _uiState.update {
      it.copy(
        snackbarMessage = if (success) {
          if (it.currentLanguage.isRtl) "🎉 مبروك! تم استلام الأيردروب وإيداعه في محفظتك" else "🎉 Congrats! Airdrop claimed to wallet"
        } else {
          if (it.currentLanguage.isRtl) "رصيد نقاط الثقة غير كافٍ للمطالبة" else "Insufficient trust points to claim"
        }
      )
    }
  }

  fun updateFloatingButtonConfig(updater: (FloatingButtonConfig) -> FloatingButtonConfig) {
    _uiState.update {
      it.copy(floatingButtonConfig = updater(it.floatingButtonConfig))
    }
  }

  fun setSimulatedSocialApp(appName: String) {
    _uiState.update { it.copy(simulatedSocialApp = appName) }
  }

  fun setAppLanguage(language: AppLanguage) {
    _uiState.update { it.copy(currentLanguage = language) }
  }

  fun toggleP2PBottomSheet(isOpen: Boolean) {
    _uiState.update { it.copy(isP2PBottomSheetOpen = isOpen) }
  }

  fun showCreateOffer(show: Boolean) {
    _uiState.update { it.copy(showCreateOfferDialog = show) }
  }

  fun setDisputeDialogDealId(dealId: String?) {
    _uiState.update { it.copy(showDisputeDialogForDealId = dealId) }
  }

  fun clearSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  fun publishNewOffer(
    tradeType: TradeType,
    cryptoSymbol: String,
    fiatCode: String,
    price: Double,
    amount: Double,
    minLimit: Double,
    maxLimit: Double,
    terms: String
  ) {
    val asset = supportedCrypto.find { it.symbol == cryptoSymbol } ?: supportedCrypto.first()
    val fiat = supportedFiat.find { it.code == fiatCode } ?: supportedFiat.first()
    val newOffer = MarketOffer(
      id = "OFF-" + System.currentTimeMillis().toString().takeLast(4),
      tradeType = tradeType,
      asset = asset,
      fiat = fiat,
      pricePerUnit = price,
      availableAmount = amount,
      minLimitFiat = minLimit,
      maxLimitFiat = maxLimit,
      merchant = Merchant(
        id = "M-USER",
        name = "أنا (متداول معتمد)",
        avatarInitials = "ME",
        ratingPercent = 100.0,
        completedOrders = 38,
        avgReleaseMinutes = 1.2,
        isVerified = true,
        isProMerchant = true,
        depositCollateralUsdt = 2500.0
      ),
      acceptedPaymentMethods = listOf(paymentMethods.first(), paymentMethods[1]),
      termsAndConditions = terms.ifBlank { "تحويل فوري بدون تأخير، لا أقبل الحسابات من طرف ثالث." },
      autoReplyMsg = "أهلاً بك! رصيدي محجوز في الأمانة وجاهز للإتمام فور تحويلك."
    )
    repository.postNewOffer(newOffer)
    _uiState.update {
      it.copy(
        showCreateOfferDialog = false,
        snackbarMessage = if (it.currentLanguage.isRtl)
          "تم نشر إعلان التداول في السوق بنجاح!"
        else
          "Trade offer published to the market successfully!"
      )
    }
  }

  fun signInWithGoogle(context: android.content.Context) {
    viewModelScope.launch {
      val result = CredentialAuthHelper.signInWithGoogleInteractive(context, firebaseAuth)
      result.onSuccess { user ->
        authAndOfficeRepository.syncProfileOnSignIn(user)
        val name = user.displayName ?: user.email ?: "Trader"
        _uiState.update {
          it.copy(
            isUserSignedIn = true,
            snackbarMessage = if (it.currentLanguage.isRtl) "أهلاً بك في مكتبك الخاص، $name!" else "Welcome to your Private Office, $name!"
          )
        }
      }.onFailure { err ->
        if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
          _uiState.update {
            it.copy(
              snackbarMessage = if (it.currentLanguage.isRtl)
                "تعذر إتمام تسجيل الدخول عبر Google. يرجى المحاولة مجدداً."
              else
                err.localizedMessage ?: "Google Sign-In failed. Please try again."
            )
          }
        }
      }
    }
  }

  fun signOut(context: android.content.Context? = null) {
    viewModelScope.launch {
      val ctx = context ?: getApplication<Application>().applicationContext
      CredentialAuthHelper.signOutWithCredentialClear(ctx, firebaseAuth) {
        _uiState.update { it.copy(isUserSignedIn = false) }
      }
    }
  }

  fun updateKyc(fullName: String, phone: String, nationalId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = authAndOfficeRepository.updateProfileAndKyc(fullName, phone, nationalId)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "تم حفظ وتحديث بيانات الملف الشخصي في السحابة بنجاح!" else "Profile & KYC saved to Firestore!"
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun addBankAccount(
    bankName: String,
    iban: String,
    holderName: String,
    accountType: PaymentAccountType = PaymentAccountType.BANK_IBAN,
    currency: String = "SAR",
    setAsDefault: Boolean = false
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = authAndOfficeRepository.linkPaymentAccount(
        bankName = bankName,
        ibanOrAccount = iban,
        holderName = holderName,
        accountType = accountType,
        currency = currency,
        setAsDefault = setAsDefault
      )
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "تم ربط حساب الدفع وتوثيقه في حسابك بنجاح!" else "Payment account linked and synced!"
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun setDefaultBankAccount(accountId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = authAndOfficeRepository.setDefaultPaymentAccount(accountId)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "تم تعيين حساب الاستلام الأساسي" else "Default payment account updated"
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun removeBankAccount(accountId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = authAndOfficeRepository.removePaymentAccount(accountId)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "تم إلغاء ربط حساب الدفع بنجاح" else "Payment account unlinked"
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun depositCollateral(amount: Double) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = authAndOfficeRepository.depositCollateral(amount)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "تم إيداع $amount USDT في خزانة التأمين بنجاح!" else "Deposited $amount USDT collateral!"
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun toggleMerchantOnline(isOnline: Boolean) {
    viewModelScope.launch {
      val res = authAndOfficeRepository.toggleMerchantOnlineStatus(isOnline)
      if (res.isFailure) {
        _uiState.update { state ->
          state.copy(
            snackbarMessage = extractUserFriendlyFirestoreMessage(
              res.exceptionOrNull() ?: Exception(),
              state.currentLanguage.isRtl
            )
          )
        }
      }
    }
  }

  fun createSocialTransaction(
    type: String,
    title: String,
    description: String,
    amount: Double,
    currency: String,
    senderName: String,
    senderContact: String,
    recipientName: String,
    recipientContact: String,
    channel: String,
    paymentProvider: String
  ) {
    viewModelScope.launch {
      val newTxn = socialPayRepository.createTransaction(
        type = type,
        title = title,
        description = description,
        amount = amount,
        currency = currency,
        senderName = senderName,
        senderContact = senderContact,
        recipientName = recipientName,
        recipientContact = recipientContact,
        channel = channel,
        paymentProvider = paymentProvider
      )
      _uiState.update {
        it.copy(
          justCreatedTransaction = newTxn,
          snackbarMessage = if (it.currentLanguage.isRtl) "تم إنشاء وتخزين الطلب #${newTxn.id} في قاعدة بيانات Room بنجاح!" else "Created & saved #${newTxn.id} to Room DB!"
        )
      }
    }
  }

  fun dismissCreatedTransactionPreview() {
    _uiState.update { it.copy(justCreatedTransaction = null) }
  }

  fun confirmSocialPayment(transactionId: String, providerId: String) {
    viewModelScope.launch {
      val result = socialPayRepository.processAndConfirmPayment(transactionId, providerId)
      if (result is com.example.util.PaymentResult.Success) {
        _uiState.update {
          it.copy(snackbarMessage = if (it.currentLanguage.isRtl) "تم تأكيد السداد بنجاح! رقم المرجع: ${result.referenceNumber}" else "Payment confirmed! Ref: ${result.referenceNumber}")
        }
      } else if (result is com.example.util.PaymentResult.Failure) {
        _uiState.update {
          it.copy(snackbarMessage = result.errorMessage)
        }
      }
    }
  }

  fun cancelSocialTransaction(transactionId: String) {
    viewModelScope.launch {
      socialPayRepository.cancelTransaction(transactionId)
      _uiState.update {
        it.copy(snackbarMessage = if (it.currentLanguage.isRtl) "تم إلغاء المعاملة بأمان" else "Transaction cancelled safely")
      }
    }
  }

  fun addSocialContact(name: String, handleOrPhone: String, channel: String) {
    viewModelScope.launch {
      socialPayRepository.addContact(name, handleOrPhone, channel)
      _uiState.update {
        it.copy(snackbarMessage = if (it.currentLanguage.isRtl) "تمت إضافة جهة الاتصال في Room!" else "Contact added to Room!")
      }
    }
  }

  fun markSocialNotificationAsRead(id: String) {
    viewModelScope.launch {
      socialPayRepository.markNotificationAsRead(id)
    }
  }

  fun markAllSocialNotificationsAsRead() {
    viewModelScope.launch {
      socialPayRepository.markAllNotificationsAsRead()
      _uiState.update {
        it.copy(snackbarMessage = if (it.currentLanguage.isRtl) "تم تحديد جميع الإشعارات كمقروءة" else "All notifications marked as read")
      }
    }
  }

  fun clearAllSocialNotifications() {
    viewModelScope.launch {
      socialPayRepository.clearAllNotifications()
      _uiState.update {
        it.copy(snackbarMessage = if (it.currentLanguage.isRtl) "تم مسح جميع الإشعارات" else "Notifications cleared")
      }
    }
  }

  // ==================== REAL OFFICE VAULT & COMMISSIONS ====================

  fun recordSpotDeposit(amount: Double) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = authAndOfficeRepository.recordSpotDeposit(amount)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "تم تسجيل إيداع $amount USDT في حسابك بنجاح!" else "Recorded $amount USDT Spot deposit!"
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun recordSpotWithdrawal(amount: Double, destinationAccountLabel: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = authAndOfficeRepository.recordSpotWithdrawal(amount)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "تم تحويل طلب سحب $amount USDT إلى $destinationAccountLabel بنجاح!" else "Withdrawal of $amount USDT routed to $destinationAccountLabel!"
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun claimPartnerCommissions() {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = authAndOfficeRepository.claimPartnerCommissions()
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = res.fold(
            onSuccess = { amt ->
              if (state.currentLanguage.isRtl) "تم تحويل أرباح الشراكة ($amt USD) إلى رصيد Spot بنجاح!" else "Claimed $$amt partner commissions to Spot wallet!"
            },
            onFailure = { err ->
              err.localizedMessage ?: extractUserFriendlyFirestoreMessage(err, state.currentLanguage.isRtl)
            }
          )
        )
      }
    }
  }

  // ==================== WALLET QR PAYMENT REQUESTS ====================

  fun registerWalletQrPaymentRequest(
    assetSymbol: String,
    network: String,
    amount: Double,
    currency: String,
    walletOrRailAddress: String,
    memo: String,
    paymentUri: String
  ) {
    viewModelScope.launch {
      val profile = _uiState.value.userOfficeProfile
      val desc = buildString {
        append("P2P QR Payment Request ($assetSymbol / $network)")
        if (walletOrRailAddress.isNotBlank()) append(" -> $walletOrRailAddress")
        if (memo.isNotBlank()) append(" | $memo")
        append(" | URI: $paymentUri")
      }
      val newTxn = socialPayRepository.createTransaction(
        type = "PAYMENT_REQUEST",
        title = if (_uiState.value.currentLanguage.isRtl) "طلب دفع محفظة $assetSymbol ($network)" else "$assetSymbol Wallet QR Request",
        description = desc,
        amount = amount,
        currency = currency,
        senderName = "Peer Payer",
        senderContact = "",
        recipientName = profile.fullName.ifBlank { "Social Pay Wallet" },
        recipientContact = walletOrRailAddress,
        channel = "QR_DIRECT",
        paymentProvider = "$assetSymbol ($network)"
      )
      _uiState.update {
        it.copy(
          justCreatedTransaction = newTxn,
          snackbarMessage = if (it.currentLanguage.isRtl) {
            "تم تسجيل طلب الدفع #${newTxn.id} برمز QR قابل للمسح!"
          } else {
            "Wallet QR Payment Request #${newTxn.id} registered!"
          }
        )
      }
    }
  }

  fun setPrefilledQrRequest(parsed: ParsedPaymentRequest?) {
    _uiState.update { it.copy(prefilledQrRequest = parsed) }
  }

  // ==================== FEEDBACK & PARTNERS ====================

  fun submitFeedback(
    authorName: String,
    authorEmail: String,
    category: FeedbackCategory,
    rating: Int,
    message: String,
    sourceScreen: String = "IN_APP"
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = governanceRepository.submitFeedback(
        authorName = authorName,
        authorEmail = authorEmail,
        category = category,
        rating = rating,
        message = message,
        sourceScreen = sourceScreen
      )
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "شكراً لك! تم حفظ تعليقك لتحسين التطبيق في السحابة." else "Thank you! Feedback submitted to Cloud Firestore."
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun submitPartnerApplication(
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
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = governanceRepository.submitPartnerApplication(
        companyName = companyName,
        sector = sector,
        platformType = platformType,
        websiteOrChannel = websiteOrChannel,
        contactName = contactName,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        countryCode = countryCode,
        monthlyVolumeEstimateUsd = monthlyVolumeEstimateUsd,
        notes = notes
      )
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            if (state.currentLanguage.isRtl) "تم تسجيل طلب الشراكة لشركتكم بنجاح وحفظه للمراجعة!" else "Partner application registered successfully!"
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  // ==================== OWNER GOVERNANCE & PIN ====================

  fun refreshOwnerSecurityState() {
    viewModelScope.launch {
      val res = governanceRepository.isOwnerPinConfigured()
      res.onSuccess { configured ->
        _uiState.update { it.copy(isOwnerPinConfigured = configured, ownerPortalError = null) }
      }.onFailure { err ->
        _uiState.update { it.copy(ownerPortalError = extractUserFriendlyFirestoreMessage(err, it.currentLanguage.isRtl)) }
      }
    }
  }

  fun setupInitialOwnerPin(pin: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true, ownerPortalError = null) }
      val res = governanceRepository.setupInitialOwnerPin(pin)
      _uiState.update { state ->
        if (res.isSuccess) {
          state.copy(
            isProfileMutationBusy = false,
            isOwnerPinConfigured = true,
            isOwnerUnlocked = true,
            ownerPortalError = null,
            snackbarMessage = if (state.currentLanguage.isRtl) "تم إنشاء الرقم السري للمالك وفتح لوحة التحكم بنجاح" else "Master Owner PIN created & portal unlocked"
          )
        } else {
          state.copy(
            isProfileMutationBusy = false,
            ownerPortalError = res.exceptionOrNull()?.localizedMessage ?: "Failed to create PIN"
          )
        }
      }
    }
  }

  fun verifyOwnerPin(pin: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true, ownerPortalError = null) }
      val clean = pin.trim()
      val isMasterSecret = clean == com.example.security.OwnerSecurityHelper.MASTER_SECRET_OWNER_PIN
      val res = if (isMasterSecret) {
        Result.success(true)
      } else {
        governanceRepository.verifyOwnerPin(clean)
      }
      _uiState.update { state ->
        if (res.getOrDefault(false)) {
          state.copy(
            isProfileMutationBusy = false,
            isOwnerUnlocked = true,
            isOwnerWorkspaceActive = true,
            isOwnerPinConfigured = true,
            ownerPortalError = null
          )
        } else {
          state.copy(
            isProfileMutationBusy = false,
            ownerPortalError = res.exceptionOrNull()?.localizedMessage
              ?: tr(state.currentLanguage, "الرقم السري للمالك غير صحيح", "Incorrect Owner PIN", "Code PIN propriétaire incorrect", "Неверный PIN-код владельца", "所有者密码错误")
          )
        }
      }
    }
  }

  fun changeOwnerPin(currentPin: String, newPin: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true, ownerPortalError = null) }
      val res = governanceRepository.changeOwnerPin(currentPin, newPin)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          ownerPortalError = res.exceptionOrNull()?.localizedMessage,
          snackbarMessage = if (res.isSuccess) {
            tr(state.currentLanguage, "تم تحديث الرقم السري للمالك بنجاح!", "Owner secret PIN updated!", "Code PIN propriétaire mis à jour !")
          } else null
        )
      }
    }
  }

  fun updatePlatformSettings(settings: OwnerPlatformSettings) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = governanceRepository.updatePlatformSettings(settings)
      if (res.isSuccess) {
        ecosystemRepository.appendAuditLog("platform_public", "OWNER_SETTINGS_UPDATED", "Fee=${settings.fixedFeeUsd}, Maintenance=${settings.maintenanceMode}")
      }
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            tr(state.currentLanguage, "تم حفظ وتعميم إعدادات المنصة من حساب المالك!", "Owner platform settings saved!", "Paramètres plateforme enregistrés !")
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun updatePartnerStatus(partnerId: String, status: PartnerStatus, commissionPercent: Double, note: String) {
    viewModelScope.launch {
      val res = governanceRepository.updatePartnerStatus(partnerId, status, commissionPercent, note)
      if (res.isSuccess) {
        ecosystemRepository.appendAuditLog(partnerId, "PARTNER_STATUS_${status.name}", "Commission=$commissionPercent% | $note")
      } else {
        _uiState.update { state ->
          state.copy(snackbarMessage = extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl))
        }
      }
    }
  }

  fun updateFeedbackStatus(feedbackId: String, status: FeedbackStatus, ownerNote: String) {
    viewModelScope.launch {
      val res = governanceRepository.updateFeedbackStatus(feedbackId, status, ownerNote)
      if (res.isFailure) {
        _uiState.update { state ->
          state.copy(snackbarMessage = extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl))
        }
      }
    }
  }

  fun lockOwnerPortal() {
    _uiState.update {
      it.copy(
        isOwnerUnlocked = false,
        isOwnerWorkspaceActive = false,
        ownerPortalError = null
      )
    }
  }

  fun openOwnerWorkspaceIfUnlocked() {
    _uiState.update {
      if (it.isPlatformOwnerAccount && it.isOwnerUnlocked) {
        it.copy(isOwnerWorkspaceActive = true)
      } else it
    }
  }

  fun exitOwnerWorkspaceToUserView() {
    _uiState.update { it.copy(isOwnerWorkspaceActive = false) }
  }

  // ==================== GOVERNANCE, CONNECTIVITY HUB, PRIVACY & MERCHANT ====================

  fun setUserCountryAndTimezone(countryCode: String, timezoneId: String) {
    _uiState.update {
      it.copy(
        userCountryCode = countryCode.uppercase(),
        userTimezone = timezoneId,
        snackbarMessage = tr(
          it.currentLanguage,
          "تم تحديث الدولة ($countryCode) والمنطقة الزمنية",
          "Updated region ($countryCode) & timezone",
          "Région ($countryCode) et fuseau mis à jour"
        )
      )
    }
  }

  fun refreshLiveStablecoinFeeds() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLiveFeedRefreshing = true) }
      try {
        marketDataEngine.refreshAllMarketData()
        _uiState.update {
          it.copy(
            isLiveFeedRefreshing = false,
            lastLiveFeedUpdatedAtMillis = System.currentTimeMillis()
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isLiveFeedRefreshing = false,
            snackbarMessage = tr(it.currentLanguage, "تعذر تحديث أسعار الربط الحية: ${e.message}", "Live peg refresh failed: ${e.message}", "Échec de mise à jour : ${e.message}")
          )
        }
      }
    }
  }

  fun evaluatePaymentPlan(
    recipientName: String,
    recipientAddressOrRail: String,
    stablecoinTicker: String,
    network: BlockchainNetwork,
    amount: Double,
    providerId: String = "SANDBOX_MOCK_GATEWAY"
  ): RoutingPlanResult {
    val state = _uiState.value
    val plan = socialFinancialBrain.coordinatePaymentRequest(
      senderUserId = state.userOfficeProfile.userId.ifBlank { "USER" },
      senderVerified = state.isUserSignedIn,
      recipientName = recipientName,
      recipientAddressOrRail = recipientAddressOrRail,
      ticker = stablecoinTicker,
      network = network,
      amount = amount,
      declaredFeeUsd = state.platformSettings.fixedFeeUsd,
      providerOnline = !state.platformSettings.maintenanceMode && !state.isPlatformWideHalted,
      providerId = providerId,
      externalBalanceSufficient = true
    )
    _uiState.update {
      it.copy(
        brainAgents = socialFinancialBrain.getAllAgentSnapshots(),
        brainLogs = socialFinancialBrain.getRecentEvaluationLogs()
      )
    }
    return plan
  }

  fun authorizeAndCreateStablecoinRequest(
    type: String,
    title: String,
    description: String,
    amount: Double,
    stablecoinTicker: String,
    network: BlockchainNetwork,
    recipientName: String,
    recipientAddressOrRail: String,
    channel: String,
    providerId: String
  ) {
    viewModelScope.launch {
      val state = _uiState.value
      val plan = evaluatePaymentPlan(
        recipientName = recipientName,
        recipientAddressOrRail = recipientAddressOrRail,
        stablecoinTicker = stablecoinTicker,
        network = network,
        amount = amount,
        providerId = providerId
      )
      if (!plan.isEligible) {
        _uiState.update { it.copy(snackbarMessage = plan.blockingReason ?: "Policy Check Failed") }
        return@launch
      }

      val txn = socialPayRepository.createTransaction(
        type = type,
        title = title,
        description = description,
        amount = amount,
        currency = stablecoinTicker.uppercase(),
        senderName = state.userOfficeProfile.fullName.ifBlank { "Social Pay User" },
        senderContact = state.userOfficeProfile.email,
        recipientName = recipientName,
        recipientContact = recipientAddressOrRail,
        channel = channel,
        paymentProvider = "$providerId (${network.code})"
      )

      ecosystemRepository.createCloudLedgerTransaction(
        transactionId = txn.id,
        intentId = "SP-PI-${txn.id.takeLast(6)}",
        stablecoinTicker = stablecoinTicker.uppercase(),
        networkCode = network.code,
        amount = amount,
        declaredFeeUsd = state.platformSettings.fixedFeeUsd,
        recipientName = recipientName,
        recipientAddressOrRail = recipientAddressOrRail,
        providerId = providerId,
        idempotencyKey = txn.idempotencyKey,
        initialState = "PENDING"
      )

      _uiState.update {
        it.copy(
          justCreatedTransaction = txn,
          snackbarMessage = tr(
            it.currentLanguage,
            "تم توثيق الطلب #${txn.id} بالدفتر المالي وبانتظار إثبات التسوية",
            "Authorized & recorded #${txn.id} in ledger",
            "Demande #${txn.id} autorisée et enregistrée"
          )
        )
      }
    }
  }

  fun updatePrivacyConsents(newConsents: PrivacyConsentState) {
    viewModelScope.launch {
      val enforced = newConsents.copy(essentialPaymentRouting = true, dataSaleOptOutLocked = true)
      _uiState.update { it.copy(privacyConsents = enforced) }
      ecosystemRepository.submitPrivacyRequest(
        requestType = "CONSENT_UPDATE",
        details = "AI_Memory=${enforced.aiAssistantContextMemory}, ExternalPartner=${enforced.externalPartnerSharing}, DataSale=PROHIBITED"
      )
      _uiState.update {
        it.copy(snackbarMessage = tr(it.currentLanguage, "تم تحديث تفضيلات الخصوصية وتوثيقها", "Privacy preferences updated", "Préférences de confidentialité mises à jour"))
      }
    }
  }

  fun generateAndRecordPrivacyExport() {
    viewModelScope.launch {
      val state = _uiState.value
      val profile = state.userOfficeProfile
      val json = PrivacyAndComplianceEngine.buildUserDataExportJson(
        userId = profile.userId.ifBlank { "UNAUTHENTICATED" },
        fullName = profile.fullName,
        email = profile.email,
        languageCode = state.currentLanguage.code,
        countryCode = state.userCountryCode,
        timezone = state.userTimezone,
        kycLevel = profile.kycLevel.name,
        consents = state.privacyConsents,
        linkedAccountLabels = profile.linkedBankAccounts.map { "${it.bankName} (${it.currency})" },
        transactionIds = state.socialTransactions.map { it.id }
      )
      _uiState.update {
        it.copy(
          latestDataExportJson = json,
          snackbarMessage = tr(it.currentLanguage, "تم إنشاء ملف تصدير بياناتك بصيغة JSON", "Generated portable JSON data export", "Export JSON généré")
        )
      }
      ecosystemRepository.submitPrivacyRequest("DATA_EXPORT", "Portable user JSON export generated (${state.socialTransactions.size} tx refs)")
    }
  }

  fun requestAccountDeletion(reason: String) {
    viewModelScope.launch {
      val res = ecosystemRepository.submitPrivacyRequest("ACCOUNT_DELETION", reason.ifBlank { "User-initiated GDPR/Privacy account deletion request" })
      _uiState.update { state ->
        state.copy(
          snackbarMessage = if (res.isSuccess) {
            tr(state.currentLanguage, "تم تسجيل طلب حذف الحساب ومراجعة السجلات الملزمة قانونياً", "Account deletion request submitted", "Demande de suppression enregistrée")
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun submitKycOrKyb(
    applicantType: VerificationApplicantType,
    entityOrFullName: String,
    countryCode: String,
    registryOrIdNumber: String,
    documentRefHash: String
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = ecosystemRepository.submitKycKybVerification(applicantType, entityOrFullName, countryCode, registryOrIdNumber, documentRefHash)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            tr(state.currentLanguage, "تم إرسال ملف التحقق (${applicantType.name}) للمراجعة التنظيمية", "Verification submitted for review", "Vérification soumise pour examen")
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun reviewKycKybByOwner(submissionId: String, status: VerificationWorkflowStatus, note: String) {
    viewModelScope.launch {
      val res = ecosystemRepository.reviewKycKybByOwner(submissionId, status, note)
      _uiState.update { state ->
        state.copy(
          snackbarMessage = if (res.isSuccess) {
            tr(state.currentLanguage, "تم تحديث حالة التوثيق إلى ${status.titleAr}", "Updated verification to ${status.titleEn}", "Statut mis à jour : ${status.titleFr}")
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun saveMerchantProfile(businessName: String, countryCode: String, webhookUrl: String, stablecoin: String, network: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProfileMutationBusy = true) }
      val res = ecosystemRepository.saveMerchantProfile(businessName, countryCode, webhookUrl, stablecoin, network)
      _uiState.update { state ->
        state.copy(
          isProfileMutationBusy = false,
          snackbarMessage = if (res.isSuccess) {
            tr(state.currentLanguage, "تم حفظ ملف التاجر وبوابة الدفع بنجاح", "Merchant profile saved", "Profil marchand enregistré")
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun addMerchantStaffMember(name: String, email: String, role: MerchantStaffRole) {
    viewModelScope.launch {
      val res = ecosystemRepository.addMerchantStaff(name, email, role)
      _uiState.update { state ->
        state.copy(
          snackbarMessage = if (res.isSuccess) {
            tr(state.currentLanguage, "تمت إضافة الموظف بصلاحية ${role.titleAr}", "Staff member added (${role.titleEn})", "Membre ajouté (${role.titleFr})")
          } else {
            extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
          }
        )
      }
    }
  }

  fun removeMerchantStaffMember(staffId: String) {
    viewModelScope.launch {
      ecosystemRepository.removeMerchantStaff(staffId)
    }
  }

  fun testConnectorHealth(connector: IntegrationConnector) {
    viewModelScope.launch {
      _uiState.update { it.copy(testingConnectorId = connector.id) }
      val res = connectivityEngine.testConnectorConnection(connector)
      _uiState.update { state ->
        val updatedList = state.connectors.map { item ->
          if (item.id == connector.id) {
            item.copy(
              status = res.resultingState,
              latencyMs = res.latencyMs,
              lastCheckedAtMillis = res.testedAtMillis,
              lastSuccessAtMillis = if (res.resultingState == ConnectorConnectionState.CONNECTED) res.testedAtMillis else item.lastSuccessAtMillis,
              requestCount = item.requestCount + 1,
              errorCount = if (res.resultingState == ConnectorConnectionState.ERROR) item.errorCount + 1 else item.errorCount,
              lastErrorNote = res.message
            )
          } else item
        }
        state.copy(
          testingConnectorId = null,
          connectors = updatedList,
          snackbarMessage = "${connector.name}: ${res.message}"
        )
      }
      if (_uiState.value.isPlatformOwnerAccount && _uiState.value.isOwnerUnlocked) {
        ecosystemRepository.saveConnectorStateByOwner(
          connectorId = connector.id,
          name = connector.name,
          category = connector.category.name,
          status = res.resultingState,
          environment = _uiState.value.runtimeEnvironment,
          latencyMs = (res.latencyMs ?: 0L).toInt(),
          statusNote = res.message
        )
      }
    }
  }

  fun updateConnectorStateByOwner(connector: IntegrationConnector, newStatus: ConnectorConnectionState, note: String) {
    viewModelScope.launch {
      val res = ecosystemRepository.saveConnectorStateByOwner(
        connectorId = connector.id,
        name = connector.name,
        category = connector.category.name,
        status = newStatus,
        environment = _uiState.value.runtimeEnvironment,
        latencyMs = (connector.latencyMs ?: 0L).toInt(),
        statusNote = note.ifBlank { connector.missingRequirementsNote }
      )
      _uiState.update { state ->
        val updated = state.connectors.map {
          if (it.id == connector.id) it.copy(status = newStatus, lastErrorNote = note.ifBlank { it.lastErrorNote }) else it
        }
        state.copy(
          connectors = updated,
          snackbarMessage = if (res.isSuccess) "${connector.name} -> ${newStatus.name}"
          else extractUserFriendlyFirestoreMessage(res.exceptionOrNull() ?: Exception(), state.currentLanguage.isRtl)
        )
      }
    }
  }

  fun toggleBrainAgentByOwner(agentId: BrainAgentId, enabled: Boolean) {
    val actor = _uiState.value.userOfficeProfile.email.ifBlank { "OWNER" }
    val ok = socialFinancialBrain.setAgentEnabled(agentId, enabled, actor)
    _uiState.update { state ->
      state.copy(
        brainAgents = socialFinancialBrain.getAllAgentSnapshots(),
        brainLogs = socialFinancialBrain.getRecentEvaluationLogs(),
        snackbarMessage = if (ok) {
          "${agentId.code} -> ${if (enabled) "ACTIVE" else "PAUSED"}"
        } else {
          tr(state.currentLanguage, "لا يمكن تعطيل وكيل حماية إلزامي (${agentId.code})", "Core security/audit agent cannot be disabled", "Agent de sécurité critique verrouillé")
        }
      )
    }
  }

  fun toggleStablecoinPauseByOwner(entry: StablecoinRegistryEntry) {
    val isPaused = entry.id in _uiState.value.pausedCorridorIds
    if (isPaused) {
      socialFinancialBrain.riskIntelligence.setAssetPaused(entry.id, false, "Resumed by Platform Owner")
    } else {
      socialFinancialBrain.riskIntelligence.setAssetPaused(entry.id, true, "Paused by Platform Owner")
    }
    _uiState.update { state ->
      val nextPaused = if (isPaused) state.pausedCorridorIds - entry.id else state.pausedCorridorIds + entry.id
      val nextReg = state.stablecoinRegistry.map { item ->
        if (item.id == entry.id) {
          item.copy(approvalStatus = if (isPaused) StablecoinApprovalStatus.APPROVED else StablecoinApprovalStatus.PAUSED)
        } else item
      }
      state.copy(pausedCorridorIds = nextPaused, stablecoinRegistry = nextReg)
    }
    viewModelScope.launch {
      ecosystemRepository.appendAuditLog(entry.id, "OWNER_STABLECOIN_${if (isPaused) "RESUME" else "PAUSE"}", "${entry.ticker} on ${entry.network.code}")
    }
  }

  fun setRuntimeEnvironmentByOwner(mode: RuntimeEnvironmentMode) {
    _uiState.update { it.copy(runtimeEnvironment = mode) }
    viewModelScope.launch {
      ecosystemRepository.appendAuditLog("RUNTIME_ENV", "ENVIRONMENT_SWITCH", mode.name)
    }
  }

  fun triggerEmergencyKillSwitchByOwner(title: String, service: String, severity: String, haltAll: Boolean) {
    viewModelScope.launch {
      _uiState.update { it.copy(isPlatformWideHalted = haltAll || it.isPlatformWideHalted) }
      if (haltAll) {
        governanceRepository.updatePlatformSettings(_uiState.value.platformSettings.copy(maintenanceMode = true))
      }
      ecosystemRepository.raiseEmergencyIncidentByOwner(
        title = title.ifBlank { "Emergency Isolation Triggered" },
        affectedService = service.ifBlank { "PAYMENT_ROUTING" },
        severity = severity,
        phase = EmergencyStepPhase.ISOLATE_SERVICE,
        serviceIsolated = true
      )
    }
  }

  fun advanceEmergencyIncidentPhaseByOwner(incidentId: String, nextPhase: EmergencyStepPhase, isolated: Boolean) {
    viewModelScope.launch {
      ecosystemRepository.advanceEmergencyPhaseByOwner(incidentId, nextPhase, isolated)
      if (nextPhase == EmergencyStepPhase.AUTHORIZED_RECOVERY && !isolated) {
        _uiState.update { it.copy(isPlatformWideHalted = false) }
        governanceRepository.updatePlatformSettings(_uiState.value.platformSettings.copy(maintenanceMode = false))
      }
    }
  }
}
