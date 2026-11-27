package com.example.ui.screens.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.asset.StablecoinRegistryEntry
import com.example.core.brain.BrainAgentId
import com.example.core.connectivity.ConnectorConnectionState
import com.example.core.connectivity.IntegrationConnector
import com.example.core.connectivity.RuntimeEnvironmentMode
import com.example.core.governance.tr
import com.example.core.privacy.EmergencyStepPhase
import com.example.core.privacy.VerificationWorkflowStatus
import com.example.data.FeedbackStatus
import com.example.data.OwnerPlatformSettings
import com.example.data.PartnerStatus
import com.example.model.AppLanguage
import com.example.ui.theme.*
import com.example.viewmodel.P2PUiState
import kotlinx.coroutines.launch

enum class OwnerModule(
  val titleAr: String,
  val titleEn: String,
  val titleFr: String,
  val icon: ImageVector
) {
  OVERVIEW("النظرة العامة", "Overview", "Vue d'ensemble", Icons.Default.Dashboard),
  LIVE_GLOBAL_DASHBOARD("لوحة الشبكة الحية", "Live Global Dashboard", "Tableau Mondial", Icons.Default.Public),
  GLOBAL_CONNECTIVITY_HUB("مركز الربط العالمي", "Global Connectivity Hub", "Hub Connectivité", Icons.Default.Hub),
  USERS_MANAGEMENT("إدارة المستخدمين", "Users Management", "Utilisateurs", Icons.Default.People),
  COMPANIES_MANAGEMENT("الشركات والتجار", "Companies & Partners", "Entreprises", Icons.Default.Business),
  WALLETS_MANAGEMENT("إدارة المحافظ", "Wallets Architecture", "Portefeuilles", Icons.Default.AccountBalanceWallet),
  STABLECOINS_MANAGEMENT("العملات المستقرة", "Stablecoins Control", "Stablecoins", Icons.Default.VerifiedUser),
  NETWORKS_MANAGEMENT("الشبكات المعتمدة", "Blockchain Networks", "Réseaux", Icons.Default.Lan),
  TRANSACTIONS_CONTROL("رقابة الدفتر", "Ledger Control", "Registre", Icons.Default.ReceiptLong),
  FEES_REVENUE("الرسوم والإيرادات", "Fees & Revenue", "Frais & Revenus", Icons.Default.Payments),
  KYC_KYB_CENTER("مركز التوثيق", "KYC / KYB Center", "Vérification", Icons.Default.Badge),
  SECURITY_CENTER("مركز الأمان", "Security Center", "Sécurité", Icons.Default.Security),
  PRIVACY_CENTER("مركز الخصوصية", "Privacy Registry", "Confidentialité", Icons.Default.PrivacyTip),
  AI_AGENTS_CONTROL("الوكلاء الـ 18", "18 AI Agents", "Agents IA", Icons.Default.Psychology),
  API_DEVELOPERS("واجهة المطورين", "API & Developers", "Développeurs API", Icons.Default.Code),
  ANALYTICS_REPORTS("التقارير والتحليلات", "Analytics & Reports", "Analytiques", Icons.Default.Analytics),
  SYSTEM_SETTINGS("إعدادات النظام", "System Settings", "Paramètres", Icons.Default.Tune),
  AUDIT_LOGS("سجل التدقيق", "Immutable Audit Logs", "Journaux d'Audit", Icons.Default.HistoryEdu),
  EMERGENCY_CONTROL("التحكم الطارئ", "Emergency Control", "Urgence", Icons.Default.Emergency)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerCommandCenterScreen(
  uiState: P2PUiState,
  onLanguageSelected: (AppLanguage) -> Unit,
  onRefreshLiveFeeds: () -> Unit,
  onTestConnector: (IntegrationConnector) -> Unit,
  onUpdateConnectorState: (IntegrationConnector, ConnectorConnectionState, String) -> Unit,
  onUpdatePartnerStatus: (String, PartnerStatus, Double, String) -> Unit,
  onToggleStablecoinPause: (StablecoinRegistryEntry) -> Unit,
  onReviewKycKyb: (String, VerificationWorkflowStatus, String) -> Unit,
  onChangeOwnerPin: (String, String) -> Unit,
  onToggleBrainAgent: (BrainAgentId, Boolean) -> Unit,
  onSetRuntimeEnvironment: (RuntimeEnvironmentMode) -> Unit,
  onUpdatePlatformSettings: (OwnerPlatformSettings) -> Unit,
  onUpdateFeedbackStatus: (String, FeedbackStatus, String) -> Unit,
  onTriggerKillSwitch: (String, String, String, Boolean) -> Unit,
  onAdvanceEmergencyPhase: (String, EmergencyStepPhase, Boolean) -> Unit,
  onExitToUserApp: () -> Unit,
  onLockOwnerSession: () -> Unit,
  modifier: Modifier = Modifier
) {
  val lang = uiState.currentLanguage
  var activeModule by remember { mutableStateOf(OwnerModule.OVERVIEW) }
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  BackHandler {
    if (activeModule != OwnerModule.OVERVIEW) {
      activeModule = OwnerModule.OVERVIEW
    } else {
      onExitToUserApp()
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val usePermanentRail = maxWidth >= 720.dp

    val mainWorkspace: @Composable (Boolean) -> Unit = { showMenuToggle ->
      Scaffold(
        containerColor = BrandPrimaryBg,
        topBar = {
          Surface(color = BrandSecondaryBg, border = BorderStroke(1.dp, BrandSurfaceBorder)) {
            Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                  if (showMenuToggle) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.testTag("owner_drawer_toggle_btn")) {
                      Icon(Icons.Default.Menu, contentDescription = "Open Governance Modules", tint = BrandPrimaryAccent)
                    }
                  }
                  Column {
                    Text(
                      text = tr(lang, activeModule.titleAr, activeModule.titleEn, activeModule.titleFr),
                      fontWeight = FontWeight.ExtraBold,
                      color = BrandTextPrimary,
                      fontSize = 15.sp
                    )
                    Text(
                      text = "SOCIAL PAY OWNER COMMAND • ${uiState.runtimeEnvironment.name}",
                      fontSize = 10.sp,
                      color = BrandPrimaryAccent
                    )
                  }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                  TextButton(onClick = onExitToUserApp, modifier = Modifier.testTag("owner_exit_to_user_btn")) {
                    Text(tr(lang, "واجهة المستخدم", "User App", "App Client"), fontSize = 11.sp, color = BrandSecondaryAccent)
                  }
                  IconButton(onClick = onLockOwnerSession, modifier = Modifier.testTag("owner_lock_session_btn")) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock Owner Session", tint = StatusError)
                  }
                }
              }

              // Prominent Global Connectivity Hub Shortcut Strip + Language Switch
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Button(
                  onClick = { activeModule = OwnerModule.GLOBAL_CONNECTIVITY_HUB },
                  colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                  modifier = Modifier.height(36.dp).testTag("owner_top_hub_cta")
                ) {
                  Icon(Icons.Default.Hub, contentDescription = null, tint = BrandPrimaryBg, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("GLOBAL CONNECTIVITY HUB", color = BrandPrimaryBg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }

                var showOwnerLangDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                  onClick = { showOwnerLangDialog = true },
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.5f)),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(32.dp).testTag("owner_lang_btn")
                ) {
                  Text(text = "${lang.flag} ${lang.code.uppercase()}", fontSize = 11.sp, color = BrandTextPrimary, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.width(2.dp))
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(14.dp))
                }

                if (showOwnerLangDialog) {
                  AlertDialog(
                    onDismissRequest = { showOwnerLangDialog = false },
                    containerColor = BrandSecondaryBg,
                    title = {
                      Text(
                        text = tr(lang, "اختر لغة الإدارة", "Select Language", "Choisir la langue", "Выберите язык", "选择语言"),
                        color = BrandTextPrimary,
                        fontWeight = FontWeight.Bold
                      )
                    },
                    text = {
                      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { l ->
                          val isSelected = lang == l
                          Surface(
                            color = if (isSelected) BrandPrimaryAccent.copy(alpha = 0.2f) else BrandPrimaryBg,
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected) BorderStroke(1.5.dp, BrandPrimaryAccent) else BorderStroke(1.dp, BrandSurfaceBorder),
                            modifier = Modifier
                              .fillMaxWidth()
                              .clickable {
                                onLanguageSelected(l)
                                showOwnerLangDialog = false
                              }
                          ) {
                            Row(
                              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                              horizontalArrangement = Arrangement.spacedBy(8.dp),
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Text(l.flag, fontSize = 18.sp)
                              Text(
                                text = l.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BrandPrimaryAccent else BrandTextPrimary
                              )
                            }
                          }
                        }
                      }
                    },
                    confirmButton = {
                      TextButton(onClick = { showOwnerLangDialog = false }) {
                        Text(tr(lang, "إغلاق", "Close", "Fermer", "Закрыть", "关闭"), color = BrandPrimaryAccent)
                      }
                    }
                  )
                }
              }
            }
          }
        }
      ) { padding ->
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 14.dp),
          contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item {
            when (activeModule) {
              OwnerModule.LIVE_GLOBAL_DASHBOARD -> {
                LiveGlobalDashboardView(
                  currentLanguage = lang,
                  liveStablecoinTicks = uiState.liveStablecoinTicks,
                  exchangeHealth = uiState.exchangeHealth,
                  isLiveFeedRefreshing = uiState.isLiveFeedRefreshing,
                  lastLiveFeedUpdatedAtMillis = uiState.lastLiveFeedUpdatedAtMillis,
                  connectors = uiState.connectors,
                  ledgerTransactions = uiState.ownerAllLedgerTransactions,
                  registeredUsers = uiState.ownerAllUsers,
                  merchants = uiState.ownerAllMerchants,
                  partners = uiState.ownerAllPartners,
                  isPlatformWideHalted = uiState.isPlatformWideHalted,
                  onRefreshLiveFeeds = onRefreshLiveFeeds,
                  onOpenConnectivityHub = { activeModule = OwnerModule.GLOBAL_CONNECTIVITY_HUB }
                )
              }

              OwnerModule.GLOBAL_CONNECTIVITY_HUB -> {
                OwnerConnectivityHubView(
                  currentLanguage = lang,
                  connectors = uiState.connectors,
                  testingConnectorId = uiState.testingConnectorId,
                  allPartners = uiState.ownerAllPartners,
                  onTestConnector = onTestConnector,
                  onUpdateConnectorState = onUpdateConnectorState,
                  onUpdatePartnerStatus = onUpdatePartnerStatus
                )
              }

              OwnerModule.OVERVIEW,
              OwnerModule.USERS_MANAGEMENT,
              OwnerModule.COMPANIES_MANAGEMENT,
              OwnerModule.WALLETS_MANAGEMENT,
              OwnerModule.STABLECOINS_MANAGEMENT,
              OwnerModule.NETWORKS_MANAGEMENT,
              OwnerModule.TRANSACTIONS_CONTROL,
              OwnerModule.FEES_REVENUE -> {
                OwnerOperationsModulesView(
                  module = activeModule,
                  uiState = uiState,
                  onSelectModule = { activeModule = it },
                  onToggleStablecoinPause = onToggleStablecoinPause,
                  onUpdatePartnerStatus = onUpdatePartnerStatus,
                  onUpdatePlatformSettings = onUpdatePlatformSettings
                )
              }

              OwnerModule.KYC_KYB_CENTER,
              OwnerModule.SECURITY_CENTER,
              OwnerModule.PRIVACY_CENTER,
              OwnerModule.AI_AGENTS_CONTROL,
              OwnerModule.API_DEVELOPERS,
              OwnerModule.ANALYTICS_REPORTS,
              OwnerModule.SYSTEM_SETTINGS,
              OwnerModule.AUDIT_LOGS,
              OwnerModule.EMERGENCY_CONTROL -> {
                OwnerComplianceAndSecurityView(
                  module = activeModule,
                  uiState = uiState,
                  onReviewKycKyb = onReviewKycKyb,
                  onChangeOwnerPin = onChangeOwnerPin,
                  onToggleBrainAgent = onToggleBrainAgent,
                  onSetRuntimeEnvironment = onSetRuntimeEnvironment,
                  onUpdatePlatformSettings = onUpdatePlatformSettings,
                  onUpdateFeedbackStatus = onUpdateFeedbackStatus,
                  onTriggerKillSwitch = onTriggerKillSwitch,
                  onAdvanceEmergencyPhase = onAdvanceEmergencyPhase
                )
              }
            }
          }
        }
      }
    }

    if (usePermanentRail) {
      Row(modifier = Modifier.fillMaxSize()) {
        Surface(color = BrandSecondaryBg, border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.width(230.dp).fillMaxHeight()) {
          Column(modifier = Modifier.statusBarsPadding().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OwnerModule.entries.forEach { mod ->
              NavigationDrawerItem(
                selected = activeModule == mod,
                onClick = { activeModule = mod },
                icon = { Icon(mod.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text(tr(lang, mod.titleAr, mod.titleEn, mod.titleFr), fontSize = 11.sp, maxLines = 1) },
                modifier = Modifier.height(44.dp)
              )
            }
          }
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          mainWorkspace(false)
        }
      }
    } else {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalDrawerSheet(drawerContainerColor = BrandSecondaryBg, modifier = Modifier.width(280.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("OWNER COMMAND CENTER", fontWeight = FontWeight.ExtraBold, color = BrandPrimaryAccent, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
              HorizontalDivider(color = BrandSurfaceBorder)
              OwnerModule.entries.forEach { mod ->
                NavigationDrawerItem(
                  selected = activeModule == mod,
                  onClick = {
                    activeModule = mod
                    scope.launch { drawerState.close() }
                  },
                  icon = { Icon(mod.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                  label = { Text(tr(lang, mod.titleAr, mod.titleEn, mod.titleFr), fontSize = 12.sp) },
                  modifier = Modifier.height(44.dp).testTag("owner_nav_${mod.name.lowercase()}")
                )
              }
            }
          }
        }
      ) {
        mainWorkspace(true)
      }
    }
  }
}
