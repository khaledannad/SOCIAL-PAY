package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.asset.StablecoinRegistryEntry
import com.example.data.CredentialAuthHelper
import com.example.model.AppLanguage
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.screens.owner.OwnerCommandCenterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.P2PViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.util.configureAppCheck(this, intent)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val firebaseAuth = remember { FirebaseAuth.getInstance() }
        val currentUser by remember(firebaseAuth) {
          CredentialAuthHelper.authStateFlow(firebaseAuth)
        }.collectAsStateWithLifecycle(initialValue = firebaseAuth.currentUser)

        var selectedLanguage by remember { mutableStateOf(AppLanguage.ARABIC) }
        var openPartnerAfterAuth by remember { mutableStateOf(false) }
        var openOwnerAfterAuth by remember { mutableStateOf(false) }

        CompositionLocalProvider(
          LocalLayoutDirection provides if (selectedLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
          val authenticatedUser = currentUser
          if (authenticatedUser == null) {
            AuthScreen(
              currentLanguage = selectedLanguage,
              onLanguageSelected = { selectedLanguage = it },
              onRequestPartnerAfterAuth = {
                openPartnerAfterAuth = true
                openOwnerAfterAuth = false
              },
              onRequestOwnerAfterAuth = {
                openOwnerAfterAuth = true
                openPartnerAfterAuth = false
              }
            )
          } else {
            key(authenticatedUser.uid) {
              val viewModel: P2PViewModel = viewModel(key = authenticatedUser.uid)
              val uiState by viewModel.uiState.collectAsStateWithLifecycle()

              LaunchedEffect(selectedLanguage) {
                if (uiState.currentLanguage != selectedLanguage) {
                  viewModel.setAppLanguage(selectedLanguage)
                }
              }

              LaunchedEffect(uiState.currentLanguage) {
                if (uiState.currentLanguage != selectedLanguage) {
                  selectedLanguage = uiState.currentLanguage
                }
              }

              P2PAppContent(
                viewModel = viewModel,
                initialOpenPartner = openPartnerAfterAuth,
                initialOpenOwner = openOwnerAfterAuth,
                onConsumeAuthRedirects = {
                  openPartnerAfterAuth = false
                  openOwnerAfterAuth = false
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun P2PAppContent(
  viewModel: P2PViewModel,
  initialOpenPartner: Boolean = false,
  initialOpenOwner: Boolean = false,
  onConsumeAuthRedirects: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var currentTab by remember { mutableStateOf(P2PNavigationItem.HOME) }
  var showAiChatbotSheet by remember { mutableStateOf(false) }
  var showContactsScreen by remember { mutableStateOf(false) }
  var showNotificationsScreen by remember { mutableStateOf(false) }
  var showOfficeScreen by remember { mutableStateOf(false) }
  var showPartnerScreen by remember { mutableStateOf(initialOpenPartner) }
  var showInvoicesScreen by remember { mutableStateOf(false) }
  var preselectedStablecoinEntry by remember { mutableStateOf<StablecoinRegistryEntry?>(null) }
  var showWalletQrSheet by remember { mutableStateOf(false) }
  var showFeedbackDialog by remember { mutableStateOf(false) }
  var showOwnerPortalDialog by remember { mutableStateOf(initialOpenOwner) }
  val snackbarHostState = remember { SnackbarHostState() }

  val isRtl = uiState.currentLanguage.isRtl
  val context = LocalContext.current
  val firebaseAuth = remember { FirebaseAuth.getInstance() }
  var fabPrefs by remember { mutableStateOf(FloatingButtonPreferences.load(context)) }
  var showFabSettings by remember { mutableStateOf(false) }

  LaunchedEffect(initialOpenPartner, initialOpenOwner) {
    if (initialOpenPartner || initialOpenOwner) {
      if (initialOpenOwner && uiState.isPlatformOwnerAccount) {
        viewModel.refreshOwnerSecurityState()
      }
      onConsumeAuthRedirects()
    }
  }

  LaunchedEffect(uiState.isOwnerUnlocked, uiState.isOwnerWorkspaceActive) {
    if (showOwnerPortalDialog && uiState.isOwnerUnlocked && uiState.isPlatformOwnerAccount) {
      if (!uiState.isOwnerWorkspaceActive) {
        viewModel.openOwnerWorkspaceIfUnlocked()
      }
      showOwnerPortalDialog = false
    }
  }

  // Trigger snackbar messages across both User App & Owner Workspace
  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  // STRICT ISOLATION: Owner Command Center renders as a standalone workspace
  if (uiState.isOwnerWorkspaceActive && uiState.isPlatformOwnerAccount && uiState.isOwnerUnlocked) {
    Box(modifier = modifier.fillMaxSize()) {
      OwnerCommandCenterScreen(
        uiState = uiState,
        onLanguageSelected = { viewModel.setAppLanguage(it) },
        onRefreshLiveFeeds = { viewModel.refreshLiveStablecoinFeeds() },
        onTestConnector = { connector -> viewModel.testConnectorHealth(connector) },
        onUpdateConnectorState = { connector, state, note -> viewModel.updateConnectorStateByOwner(connector, state, note) },
        onUpdatePartnerStatus = { id, status, share, note -> viewModel.updatePartnerStatus(id, status, share, note) },
        onToggleStablecoinPause = { entry -> viewModel.toggleStablecoinPauseByOwner(entry) },
        onReviewKycKyb = { id, status, note -> viewModel.reviewKycKybByOwner(id, status, note) },
        onChangeOwnerPin = { cur, next -> viewModel.changeOwnerPin(cur, next) },
        onToggleBrainAgent = { agentId, enabled -> viewModel.toggleBrainAgentByOwner(agentId, enabled) },
        onSetRuntimeEnvironment = { mode -> viewModel.setRuntimeEnvironmentByOwner(mode) },
        onUpdatePlatformSettings = { settings -> viewModel.updatePlatformSettings(settings) },
        onUpdateFeedbackStatus = { id, status, note -> viewModel.updateFeedbackStatus(id, status, note) },
        onTriggerKillSwitch = { title, service, severity, haltAll ->
          viewModel.triggerEmergencyKillSwitchByOwner(title, service, severity, haltAll)
        },
        onAdvanceEmergencyPhase = { incId, nextPhase, isolated ->
          viewModel.advanceEmergencyIncidentPhaseByOwner(incId, nextPhase, isolated)
        },
        onExitToUserApp = { viewModel.exitOwnerWorkspaceToUserView() },
        onLockOwnerSession = { viewModel.lockOwnerPortal() }
      )
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 12.dp)
      )
    }
    return
  }

  // Handle back button when deal detail or sub-screens are open in User App
  BackHandler(
    enabled = uiState.selectedDealDetail != null ||
      showContactsScreen ||
      showNotificationsScreen ||
      showOfficeScreen ||
      showPartnerScreen ||
      showInvoicesScreen ||
      currentTab != P2PNavigationItem.HOME
  ) {
    if (uiState.selectedDealDetail != null) {
      viewModel.selectDealForDetail(null)
    } else if (showContactsScreen) {
      showContactsScreen = false
    } else if (showNotificationsScreen) {
      showNotificationsScreen = false
    } else if (showOfficeScreen) {
      showOfficeScreen = false
    } else if (showPartnerScreen) {
      showPartnerScreen = false
    } else if (showInvoicesScreen) {
      showInvoicesScreen = false
    } else if (currentTab != P2PNavigationItem.HOME) {
      currentTab = P2PNavigationItem.HOME
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      if (uiState.selectedDealDetail == null) {
        TopNavBar(
          currentLanguage = uiState.currentLanguage,
          currentFeeUsd = uiState.platformSettings.fixedFeeUsd,
          unreadNotificationsCount = uiState.unreadNotificationsCount,
          showOwnerWorkspaceSwitch = uiState.isPlatformOwnerAccount && uiState.isOwnerUnlocked,
          onOpenOwnerWorkspace = { viewModel.openOwnerWorkspaceIfUnlocked() },
          onLanguageSelected = { viewModel.setAppLanguage(it) },
          onOpenNotifications = {
            showNotificationsScreen = true
            showContactsScreen = false
            showOfficeScreen = false
            showPartnerScreen = false
            showInvoicesScreen = false
          },
          onOpenContacts = {
            showContactsScreen = true
            showNotificationsScreen = false
            showOfficeScreen = false
            showPartnerScreen = false
            showInvoicesScreen = false
          },
          onOpenOffice = {
            showOfficeScreen = true
            showNotificationsScreen = false
            showContactsScreen = false
            showPartnerScreen = false
            showInvoicesScreen = false
          },
          onOpenAiChatbot = { showAiChatbotSheet = true },
          onOpenWalletQr = { showWalletQrSheet = true },
          onBrandClick = {
            currentTab = P2PNavigationItem.HOME
            showContactsScreen = false
            showNotificationsScreen = false
            showOfficeScreen = false
            showPartnerScreen = false
            showInvoicesScreen = false
          }
        )
      }
    },
    bottomBar = {
      if (uiState.selectedDealDetail == null &&
        !showContactsScreen &&
        !showNotificationsScreen &&
        !showOfficeScreen &&
        !showPartnerScreen &&
        !showInvoicesScreen
      ) {
        BottomNavBar(
          currentLanguage = uiState.currentLanguage,
          selectedItem = currentTab,
          onItemSelected = {
            currentTab = it
            showContactsScreen = false
            showNotificationsScreen = false
            showOfficeScreen = false
            showPartnerScreen = false
            showInvoicesScreen = false
          },
          activeTransactionsCount = uiState.pendingTransactionsCount
        )
      }
    },
    floatingActionButton = {
      if (uiState.selectedDealDetail == null && fabPrefs.isVisible) {
        SmartFloatingButton(
          preferences = fabPrefs,
          onClick = { showAiChatbotSheet = true },
          onCustomizeClick = { showFabSettings = true }
        )
      }
    },
    floatingActionButtonPosition = when (fabPrefs.position) {
      FloatingButtonPosition.BOTTOM_END -> FabPosition.End
      FloatingButtonPosition.BOTTOM_START -> FabPosition.Start
      FloatingButtonPosition.BOTTOM_CENTER -> FabPosition.Center
      FloatingButtonPosition.CENTER_END -> FabPosition.End
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      if (uiState.selectedDealDetail != null) {
        SmartDealDetailScreen(
          deal = uiState.selectedDealDetail!!,
          isRtl = isRtl,
          onBack = { viewModel.selectDealForDetail(null) },
          onMarkPaid = { dealId -> viewModel.markDealPaid(dealId) },
          onReleaseEscrow = { dealId -> viewModel.releaseEscrow(dealId) },
          onCancelDeal = { dealId -> viewModel.cancelDeal(dealId) },
          onOpenDispute = { dealId -> viewModel.setDisputeDialogDealId(dealId) },
          onSendMessage = { dealId, text -> viewModel.sendChatMessage(dealId, text) }
        )
      } else if (showContactsScreen) {
        SocialContactsScreen(
          contacts = uiState.socialContacts,
          isRtl = isRtl,
          onAddContact = { name, handle, channel -> viewModel.addSocialContact(name, handle, channel) },
          onInitiatePaymentToContact = { _ ->
            showContactsScreen = false
            currentTab = P2PNavigationItem.CREATE
          }
        )
      } else if (showNotificationsScreen) {
        SocialNotificationsScreen(
          notifications = uiState.socialNotifications,
          isRtl = isRtl,
          onMarkAsRead = { notifId -> viewModel.markSocialNotificationAsRead(notifId) },
          onMarkAllAsRead = { viewModel.markAllSocialNotificationsAsRead() },
          onClearAll = { viewModel.clearAllSocialNotifications() }
        )
      } else if (showOfficeScreen) {
        PrivateOfficeScreen(
          profile = uiState.userOfficeProfile,
          currentLanguage = uiState.currentLanguage,
          userCountryCode = uiState.userCountryCode,
          profileSyncState = uiState.profileSyncState,
          isBusy = uiState.isProfileMutationBusy,
          isUserSignedIn = uiState.isUserSignedIn,
          privacyConsents = uiState.privacyConsents,
          myPrivacyRequests = uiState.myPrivacyRequests,
          latestDataExportJson = uiState.latestDataExportJson,
          myKycKybSubmissions = uiState.myKycKybSubmissions,
          onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
          onSignOut = { viewModel.signOut(context) },
          onUpdateKyc = { name, phone, id -> viewModel.updateKyc(name, phone, id) },
          onSubmitKycKyb = { type, name, country, docNum, docHash ->
            viewModel.submitKycOrKyb(type, name, country, docNum, docHash)
          },
          onUpdatePrivacyConsents = { viewModel.updatePrivacyConsents(it) },
          onGeneratePrivacyExport = { viewModel.generateAndRecordPrivacyExport() },
          onRequestAccountDeletion = { reason -> viewModel.requestAccountDeletion(reason) },
          onAddBankAccount = { bank, iban, holder, type, currency, isDefault ->
            viewModel.addBankAccount(bank, iban, holder, type, currency, isDefault)
          },
          onSetDefaultBankAccount = { accountId -> viewModel.setDefaultBankAccount(accountId) },
          onRemoveBankAccount = { id -> viewModel.removeBankAccount(id) },
          onDepositCollateral = { amount -> viewModel.depositCollateral(amount) },
          onDepositSpot = { amount -> viewModel.recordSpotDeposit(amount) },
          onWithdrawSpot = { amount, label -> viewModel.recordSpotWithdrawal(amount, label) },
          onClaimPartnerCommissions = { viewModel.claimPartnerCommissions() },
          onOpenWalletQr = { showWalletQrSheet = true },
          onToggleOnline = { isOnline -> viewModel.toggleMerchantOnline(isOnline) },
          onOpenCreatePaymentLink = {
            showOfficeScreen = false
            currentTab = P2PNavigationItem.CREATE
          }
        )
      } else if (showPartnerScreen) {
        PartnerAndSettingsScreen(
          currentLanguage = uiState.currentLanguage,
          selectedFiat = uiState.selectedFiat,
          userCountryCode = uiState.userCountryCode,
          userTimezone = uiState.userTimezone,
          supportedFiat = viewModel.supportedFiat,
          defaultContactName = uiState.userOfficeProfile.fullName,
          defaultContactEmail = uiState.userOfficeProfile.email,
          myPartners = uiState.myPartnersList,
          platformSettings = uiState.platformSettings,
          isBusy = uiState.isProfileMutationBusy,
          onLanguageSelected = { viewModel.setAppLanguage(it) },
          onCountryTimezoneSelected = { cc, tz -> viewModel.setUserCountryAndTimezone(cc, tz) },
          onFiatSelected = { viewModel.setSelectedFiat(it) },
          onSubmitPartnerApplication = { comp, sector, plat, web, cname, cemail, cphone, country, vol, notes ->
            viewModel.submitPartnerApplication(comp, sector, plat, web, cname, cemail, cphone, country, vol, notes)
          },
          onOpenDirectFeedback = { showFeedbackDialog = true },
          onOpenWalletQr = { showWalletQrSheet = true }
        )
      } else if (showInvoicesScreen) {
        SocialInvoicesScreen(
          transactions = uiState.socialTransactions,
          currentLanguage = uiState.currentLanguage,
          flatFeeUsd = uiState.platformSettings.fixedFeeUsd,
          onNavigateToCreateInvoice = {
            showInvoicesScreen = false
            currentTab = P2PNavigationItem.CREATE
          },
          onSelectInvoice = {
            showInvoicesScreen = false
            currentTab = P2PNavigationItem.TRANSACTIONS
          }
        )
      } else {
        when (currentTab) {
          P2PNavigationItem.HOME -> {
            SocialHomeScreen(
              recentTransactions = uiState.socialTransactions,
              totalCount = uiState.totalTransactionsCount,
              confirmedCount = uiState.confirmedTransactionsCount,
              pendingCount = uiState.pendingTransactionsCount,
              currentFeeUsd = uiState.platformSettings.fixedFeeUsd,
              currentLanguage = uiState.currentLanguage,
              onNavigateToCreate = { currentTab = P2PNavigationItem.CREATE },
              onNavigateToStablecoins = { currentTab = P2PNavigationItem.STABLECOINS },
              onNavigateToTransactions = { currentTab = P2PNavigationItem.TRANSACTIONS },
              onNavigateToInvoices = { showInvoicesScreen = true },
              onNavigateToMerchant = { currentTab = P2PNavigationItem.MERCHANT },
              onOpenWalletQr = { showWalletQrSheet = true },
              onOpenPartners = { showPartnerScreen = true },
              onOpenFeedback = { showFeedbackDialog = true },
              onSelectTransaction = { currentTab = P2PNavigationItem.TRANSACTIONS }
            )
          }

          P2PNavigationItem.STABLECOINS -> {
            MarketScreen(
              currentLanguage = uiState.currentLanguage,
              stablecoinRegistry = uiState.stablecoinRegistry,
              liveStablecoinTicks = uiState.liveStablecoinTicks,
              exchangeHealth = uiState.exchangeHealth,
              isLiveFeedRefreshing = uiState.isLiveFeedRefreshing,
              lastLiveFeedUpdatedAtMillis = uiState.lastLiveFeedUpdatedAtMillis,
              flatFeeUsd = uiState.platformSettings.fixedFeeUsd,
              onRefreshLiveFeeds = { viewModel.refreshLiveStablecoinFeeds() },
              onRequestStablecoinTransfer = { entry ->
                preselectedStablecoinEntry = entry
                currentTab = P2PNavigationItem.CREATE
              }
            )
          }

          P2PNavigationItem.CREATE -> {
            SocialCreateTransactionScreen(
              contacts = uiState.socialContacts,
              currentLanguage = uiState.currentLanguage,
              flatFeeUsd = uiState.platformSettings.fixedFeeUsd,
              prefilledQrRequest = uiState.prefilledQrRequest,
              preselectedStablecoinEntry = preselectedStablecoinEntry,
              onConsumePrefilledQr = { viewModel.setPrefilledQrRequest(null) },
              onConsumePreselectedStablecoin = { preselectedStablecoinEntry = null },
              onEvaluatePlan = { recName, recRail, ticker, net, amt, prov ->
                viewModel.evaluatePaymentPlan(recName, recRail, ticker, net, amt, prov)
              },
              onAuthorizeAndCreate = { type, title, desc, amt, ticker, net, recName, recRail, ch, prov ->
                viewModel.authorizeAndCreateStablecoinRequest(type, title, desc, amt, ticker, net, recName, recRail, ch, prov)
              },
              createdTransaction = uiState.justCreatedTransaction,
              onDismissCreatedPreview = { viewModel.dismissCreatedTransactionPreview() }
            )
          }

          P2PNavigationItem.TRANSACTIONS -> {
            SocialTransactionsHistoryScreen(
              transactions = uiState.socialTransactions,
              cloudLedgerTransactions = uiState.cloudLedgerTransactions,
              currentLanguage = uiState.currentLanguage,
              onConfirmPayment = { id, provider -> viewModel.confirmSocialPayment(id, provider) },
              onCancelTransaction = { id -> viewModel.cancelSocialTransaction(id) }
            )
          }

          P2PNavigationItem.MERCHANT -> {
            MerchantPortalScreen(
              currentLanguage = uiState.currentLanguage,
              merchantProfile = uiState.myMerchantProfile,
              merchantStaff = uiState.myMerchantStaff,
              isBusy = uiState.isProfileMutationBusy,
              onSaveMerchantProfile = { biz, cc, webhook, coin, net ->
                viewModel.saveMerchantProfile(biz, cc, webhook, coin, net)
              },
              onAddStaff = { name, email, role -> viewModel.addMerchantStaffMember(name, email, role) },
              onRemoveStaff = { id -> viewModel.removeMerchantStaffMember(id) },
              onOpenInvoices = { showInvoicesScreen = true }
            )
          }
        }
      }
    }

    // ZXing Wallet QR Code & P2P Request Bottom Sheet
    if (showWalletQrSheet) {
      WalletQrRequestSheet(
        profile = uiState.userOfficeProfile,
        isRtl = isRtl,
        onDismiss = { showWalletQrSheet = false },
        onSavePaymentRequest = { asset, net, amt, cur, addr, memo, uri ->
          viewModel.registerWalletQrPaymentRequest(asset, net, amt, cur, addr, memo, uri)
          showOfficeScreen = false
          showPartnerScreen = false
          showInvoicesScreen = false
          currentTab = P2PNavigationItem.CREATE
        },
        onPayFromDecodedQr = { parsed ->
          viewModel.setPrefilledQrRequest(parsed)
          showOfficeScreen = false
          showPartnerScreen = false
          showInvoicesScreen = false
          currentTab = P2PNavigationItem.CREATE
        }
      )
    }

    // Direct App Improvement Feedback Dialog
    if (showFeedbackDialog) {
      DirectFeedbackDialog(
        isRtl = isRtl,
        isUserSignedIn = true,
        defaultName = uiState.userOfficeProfile.fullName,
        defaultEmail = uiState.userOfficeProfile.email,
        myFeedbackList = uiState.myFeedbackList,
        isSubmitting = uiState.isProfileMutationBusy,
        onDismiss = { showFeedbackDialog = false },
        onSubmitFeedback = { name, email, category, rating, msg ->
          viewModel.submitFeedback(name, email, category, rating, msg, "IN_APP")
        }
      )
    }

    // Owner Secret PIN Unlock Gate (only triggered via AuthScreen Owner route or Owner PIN flow)
    if (showOwnerPortalDialog) {
      val currentAuthUser = firebaseAuth.currentUser
      OwnerPortalDialog(
        isRtl = isRtl,
        isUserSignedIn = uiState.isUserSignedIn,
        signedInEmail = currentAuthUser?.email.orEmpty(),
        isEmailVerified = currentAuthUser?.isEmailVerified == true,
        isPinConfigured = uiState.isOwnerPinConfigured,
        isOwnerUnlocked = uiState.isOwnerUnlocked,
        isBusy = uiState.isProfileMutationBusy,
        ownerError = uiState.ownerPortalError,
        platformSettings = uiState.platformSettings,
        allPartners = uiState.ownerAllPartners,
        allFeedback = uiState.ownerAllFeedback,
        onSignInAsOwner = { viewModel.signInWithGoogle(context) },
        onSetupInitialPin = { pin -> viewModel.setupInitialOwnerPin(pin) },
        onVerifyOwnerPin = { pin -> viewModel.verifyOwnerPin(pin) },
        onChangeOwnerPin = { cur, next -> viewModel.changeOwnerPin(cur, next) },
        onUpdatePlatformSettings = { settings -> viewModel.updatePlatformSettings(settings) },
        onUpdatePartnerStatus = { id, status, share, note ->
          viewModel.updatePartnerStatus(id, status, share, note)
        },
        onUpdateFeedbackStatus = { id, status, note ->
          viewModel.updateFeedbackStatus(id, status, note)
        },
        onLockAndClose = {
          viewModel.lockOwnerPortal()
          showOwnerPortalDialog = false
        }
      )
    }

    if (uiState.isP2PBottomSheetOpen) {
      QuickSwapBottomSheet(
        supportedCrypto = viewModel.supportedCrypto,
        supportedFiat = viewModel.supportedFiat,
        paymentMethods = viewModel.paymentMethods,
        currentFeeUsd = uiState.platformSettings.fixedFeeUsd,
        isRtl = isRtl,
        onDismiss = { viewModel.toggleP2PBottomSheet(false) },
        onInitiateQuickDeal = { asset, _, _, cryptoAmount, fiatAmount ->
          val matchingOffer = uiState.filteredOffers.firstOrNull { it.asset.symbol == asset.symbol }
            ?: viewModel.repository.marketOffers.value.first()
          viewModel.createDealFromOffer(matchingOffer, cryptoAmount, fiatAmount)
        }
      )
    }

    if (uiState.showCreateOfferDialog) {
      CreateOfferDialog(
        supportedCrypto = viewModel.supportedCrypto,
        supportedFiat = viewModel.supportedFiat,
        isRtl = isRtl,
        onDismiss = { viewModel.showCreateOffer(false) },
        onPublish = { type, crypto, fiat, price, amount, minL, maxL, terms ->
          viewModel.publishNewOffer(type, crypto, fiat, price, amount, minL, maxL, terms)
        }
      )
    }

    uiState.showDisputeDialogForDealId?.let { dealId ->
      DisputeDialog(
        dealId = dealId,
        isRtl = isRtl,
        onDismiss = { viewModel.setDisputeDialogDealId(null) },
        onSubmitDispute = { reason ->
          viewModel.submitDispute(dealId, reason)
        }
      )
    }

    if (showAiChatbotSheet) {
      com.example.ai.DialectAiChatbotSheet(
        isRtl = isRtl,
        onDismiss = { showAiChatbotSheet = false }
      )
    }

    if (showFabSettings) {
      SmartFloatingButtonSettingsDialog(
        preferences = fabPrefs,
        isRtl = isRtl,
        onSave = { updated ->
          fabPrefs = updated
          updated.save(context)
          showFabSettings = false
        },
        onDismiss = { showFabSettings = false }
      )
    }
  }
}
