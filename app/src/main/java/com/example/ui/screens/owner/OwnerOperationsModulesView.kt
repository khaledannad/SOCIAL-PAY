package com.example.ui.screens.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.StablecoinApprovalStatus
import com.example.core.asset.StablecoinRegistryEntry
import com.example.core.connectivity.ConnectorConnectionState
import com.example.core.connectivity.IntegrationConnector
import com.example.core.custody.CustodyArchitectureModel
import com.example.core.governance.tr
import com.example.data.*
import com.example.model.AppLanguage
import com.example.ui.theme.*
import com.example.viewmodel.P2PUiState
import java.util.Locale

@Composable
fun OwnerOperationsModulesView(
  module: OwnerModule,
  uiState: P2PUiState,
  onSelectModule: (OwnerModule) -> Unit,
  onToggleStablecoinPause: (StablecoinRegistryEntry) -> Unit,
  onUpdatePartnerStatus: (String, PartnerStatus, Double, String) -> Unit,
  onUpdatePlatformSettings: (OwnerPlatformSettings) -> Unit
) {
  val lang = uiState.currentLanguage
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    when (module) {
      OwnerModule.OVERVIEW -> {
        // Prominent GLOBAL CONNECTIVITY HUB Hero Banner (User Mockup Advice)
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
          border = BorderStroke(1.5.dp, BrandPrimaryAccent.copy(alpha = 0.7f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectModule(OwnerModule.GLOBAL_CONNECTIVITY_HUB) }
            .testTag("overview_connectivity_hub_card")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.linearGradient(
                  listOf(BrandPrimaryAccent.copy(alpha = 0.16f), BrandSecondaryAccent.copy(alpha = 0.14f), BrandIntelligenceAccent.copy(alpha = 0.14f))
                )
              )
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(30.dp))
                Column {
                  Text("GLOBAL CONNECTIVITY HUB", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = BrandPrimaryAccent)
                  Text(
                    text = tr(lang, "بوابة ربط الشبكات الاجتماعية والمحافظ والبنوك ومزودي السيولة", "Social Platforms • Global Wallets • Providers • Banks", "Réseaux Sociaux • Portefeuilles • Banques"),
                    fontSize = 11.sp,
                    color = BrandTextSecondary
                  )
                }
              }
              Icon(Icons.Default.ArrowForward, contentDescription = null, tint = BrandPrimaryAccent)
            }

            val connected = uiState.connectors.count { it.status == ConnectorConnectionState.CONNECTED }
            val pending = uiState.connectors.count { it.status == ConnectorConnectionState.INTEGRATION_PENDING }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              AssistChip(onClick = { onSelectModule(OwnerModule.GLOBAL_CONNECTIVITY_HUB) }, label = { Text("${tr(lang, "متصل:", "Connected:", "Connectés :")} $connected", fontSize = 10.sp) })
              AssistChip(onClick = { onSelectModule(OwnerModule.GLOBAL_CONNECTIVITY_HUB) }, label = { Text("${tr(lang, "قيد الربط:", "Pending:", "En attente :")} $pending", fontSize = 10.sp) })
            }
          }
        }

        // Global Command Quick Grid (6-Card Layout inspired by User Screenshot)
        Text(
          text = tr(lang, "مركز القيادة والتحكم المركزي", "Global Command Grid", "Grille de Commande"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CommandGridCard(tr(lang, "مركز الربط", "Connectivity Hub", "Hub"), Icons.Default.Hub, BrandPrimaryAccent, Modifier.weight(1f)) { onSelectModule(OwnerModule.GLOBAL_CONNECTIVITY_HUB) }
            CommandGridCard(tr(lang, "اللوحة الحية", "Live Dashboard", "Tableau Live"), Icons.Default.Public, BrandSecondaryAccent, Modifier.weight(1f)) { onSelectModule(OwnerModule.LIVE_GLOBAL_DASHBOARD) }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CommandGridCard(tr(lang, "الأمان والخصوصية", "Security & Privacy", "Sécurité"), Icons.Default.Shield, BrandIntelligenceAccent, Modifier.weight(1f)) { onSelectModule(OwnerModule.SECURITY_CENTER) }
            CommandGridCard(tr(lang, "المستخدمون والشركات", "Users & Businesses", "Utilisateurs"), Icons.Default.Groups, BrandPrimaryAccent, Modifier.weight(1f)) { onSelectModule(OwnerModule.USERS_MANAGEMENT) }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CommandGridCard(tr(lang, "إعدادات النظام", "System Settings", "Paramètres"), Icons.Default.Tune, BrandSecondaryAccent, Modifier.weight(1f)) { onSelectModule(OwnerModule.SYSTEM_SETTINGS) }
            CommandGridCard(tr(lang, "التوثيق والتحقق", "KYC / KYB Queue", "Vérification"), Icons.Default.VerifiedUser, StatusSuccess, Modifier.weight(1f)) { onSelectModule(OwnerModule.KYC_KYB_CENTER) }
          }
        }

        // SOCIAL FINANCIAL BRAIN Architecture Hub Diagram (Inspired by Mockup Screenshot)
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
          border = BorderStroke(1.dp, BrandIntelligenceAccent.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
              color = BrandPrimaryBg,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, BrandIntelligenceAccent.copy(alpha = 0.65f)),
              modifier = Modifier.fillMaxWidth().clickable { onSelectModule(OwnerModule.AI_AGENTS_CONTROL) }
            ) {
              Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOCIAL FINANCIAL BRAIN (18 Agents)", fontWeight = FontWeight.ExtraBold, color = BrandIntelligenceAccent, fontSize = 14.sp)
                Text(
                  text = tr(lang, "العقل المركزي والتنسيق المالي مع عزل تام للصلاحيات", "Central deterministic coordination & strict agent permission boundaries", "Coordination centrale avec isolation stricte des permissions"),
                  fontSize = 10.sp,
                  color = BrandTextSecondary
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              BrainPillarTile(tr(lang, "المستخدمون", "Users", "Utilisateurs"), "${uiState.ownerAllUsers.size}", Icons.Default.Person, Modifier.weight(1f)) { onSelectModule(OwnerModule.USERS_MANAGEMENT) }
              BrainPillarTile(tr(lang, "التكاملات", "Integrations", "Intégrations"), "${uiState.connectors.size}", Icons.Default.Share, Modifier.weight(1f)) { onSelectModule(OwnerModule.GLOBAL_CONNECTIVITY_HUB) }
              BrainPillarTile(tr(lang, "الإدارة والأمان", "Governance", "Sécurité"), "${uiState.brainAgents.size}", Icons.Default.VerifiedUser, Modifier.weight(1f)) { onSelectModule(OwnerModule.AI_AGENTS_CONTROL) }
            }
          }
        }
      }

      OwnerModule.USERS_MANAGEMENT -> {
        var kycFilter by remember { mutableStateOf("ALL") }
        Text(
          text = tr(lang, "إدارة المستخدمين (${uiState.ownerAllUsers.size})", "Users Directory (${uiState.ownerAllUsers.size})", "Annuaire Utilisateurs (${uiState.ownerAllUsers.size})"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("ALL", "BASIC_LEVEL_1", "VERIFIED_LEVEL_2", "VIP_MERCHANT").forEach { tier ->
            FilterChip(selected = kycFilter == tier, onClick = { kycFilter = tier }, label = { Text(tier.take(10), fontSize = 10.sp) })
          }
        }
        val filtered = uiState.ownerAllUsers.filter { kycFilter == "ALL" || it.kycLevel == kycFilter }
        if (filtered.isEmpty()) {
          EmptyOwnerNotice(tr(lang, "لا توجد حسابات مطابقة في Firestore بعد.", "No matching user records loaded yet.", "Aucun utilisateur correspondant."))
        } else {
          filtered.forEach { user ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(user.fullName.ifBlank { user.email }, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandTextPrimary)
                  Text(user.kycLevel, fontSize = 10.sp, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold)
                }
                Text("${user.email} • ID: ${user.userId.takeLast(8)}", fontSize = 11.sp, color = BrandTextSecondary)
                Text("Verified Trades: ${user.totalTradesCount} • Trust: ${user.trustScorePercent}%", fontSize = 10.sp, color = BrandTextMuted)
              }
            }
          }
        }
      }

      OwnerModule.COMPANIES_MANAGEMENT -> {
        Text(
          text = tr(lang, "الشركات والتجار المعتمدون (${uiState.ownerAllMerchants.size})", "Registered Merchants (${uiState.ownerAllMerchants.size})", "Marchands Enregistrés (${uiState.ownerAllMerchants.size})"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        if (uiState.ownerAllMerchants.isEmpty()) {
          EmptyOwnerNotice(tr(lang, "لا توجد متاجر مسجلة بعد.", "No merchants registered yet.", "Aucun marchand enregistré."))
        } else {
          uiState.ownerAllMerchants.forEach { merch ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(merch.businessName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandTextPrimary)
                Text("${merch.countryCode} • ${merch.defaultStablecoin}/${merch.defaultNetwork} • KYB: ${merch.kybStatus}", fontSize = 11.sp, color = BrandSecondaryAccent)
                Text("KeyRef: ${merch.apiKeyReference}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = BrandTextMuted)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = tr(lang, "شركاء المنصة والربط التجاري (${uiState.ownerAllPartners.size})", "Platform Partners (${uiState.ownerAllPartners.size})", "Partenaires (${uiState.ownerAllPartners.size})"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        uiState.ownerAllPartners.forEach { p ->
          Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(p.companyName, fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 13.sp)
                Text(p.status.name, fontSize = 10.sp, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold)
              }
              Text("${p.sector.titleEn} • ${p.countryCode}", fontSize = 11.sp, color = BrandTextSecondary)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onUpdatePartnerStatus(p.id, PartnerStatus.ACTIVE, p.commissionPercent, "Approved by Owner") }, modifier = Modifier.weight(1f)) {
                  Text(tr(lang, "اعتماد", "Approve", "Approuver"), fontSize = 10.sp, color = BrandPrimaryAccent)
                }
                OutlinedButton(onClick = { onUpdatePartnerStatus(p.id, PartnerStatus.SUSPENDED, p.commissionPercent, "Suspended by Owner") }, modifier = Modifier.weight(1f)) {
                  Text(tr(lang, "تعليق", "Suspend", "Suspendre"), fontSize = 10.sp, color = StatusError)
                }
              }
            }
          }
        }
      }

      OwnerModule.WALLETS_MANAGEMENT -> {
        Text(
          text = tr(lang, "تصنيف المحافظ (غير وصائية مقابل حفظ مرخص)", "Wallet Architecture Governance", "Gouvernance des Portefeuilles"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        CustodyArchitectureModel.COMPARISON_PROFILES.forEach { cp ->
          Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(tr(lang, cp.titleAr, cp.titleEn, cp.titleFr), fontWeight = FontWeight.Bold, color = BrandPrimaryAccent, fontSize = 12.sp)
              Text(tr(lang, cp.keyControlAr, cp.keyControlEn, cp.keyControlFr), fontSize = 11.sp, color = BrandTextSecondary)
            }
          }
        }
        val walletConnectors = uiState.connectors.filter { it.custodyClassification.name.contains("WALLET") || it.custodyClassification.name.contains("CUSTODIAN") }
        walletConnectors.forEach { wc ->
          Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSurfaceVariant), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("${wc.name} • [${wc.custodyClassification.name}]", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
              Text("Status: ${wc.status.name} • Chains: ${wc.supportedNetworks.joinToString { it.code }}", fontSize = 10.sp, color = BrandTextSecondary)
            }
          }
        }
      }

      OwnerModule.STABLECOINS_MANAGEMENT -> {
        Text(
          text = tr(lang, "حوكمة العملات المستقرة المعتمدة (إيقاف/استئناف المسار)", "Approved Stablecoins Registry & Circuit Breaker", "Registre Stablecoins & Coupe-circuit"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        uiState.stablecoinRegistry.forEach { entry ->
          val isPaused = entry.approvalStatus == StablecoinApprovalStatus.PAUSED
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
            border = BorderStroke(1.dp, if (isPaused) StatusError.copy(alpha = 0.5f) else BrandSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${entry.ticker} (${entry.network.code}) — ${entry.issuer}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
                Text("Contract: ${entry.contractAddress.take(18)}... • ${entry.approvalStatus.name}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = BrandTextSecondary)
              }
              Switch(checked = !isPaused, onCheckedChange = { onToggleStablecoinPause(entry) }, modifier = Modifier.testTag("owner_toggle_coin_${entry.id}"))
            }
          }
        }
      }

      OwnerModule.NETWORKS_MANAGEMENT -> {
        Text(
          text = tr(lang, "إعدادات شبكات البلوكشين والتأكيدات", "Blockchain Networks & Finality Config", "Réseaux Blockchain & Confirmations"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        BlockchainNetwork.entries.forEach { net ->
          Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${net.displayNameEn} (${net.code})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
                Text(if (net.isOperational) "OPERATIONAL" else "OFFLINE", fontSize = 10.sp, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold)
              }
              Text("Fee Asset: ${net.nativeFeeAsset} • Required Confirmations: ${net.requiredConfirmations}", fontSize = 11.sp, color = BrandTextSecondary)
            }
          }
        }
      }

      OwnerModule.TRANSACTIONS_CONTROL -> {
        Text(
          text = tr(lang, "رقابة الدفتر المالي (سجلات غير قابلة للتعديل)", "Immutable Cloud Ledger Inspection (Read-Only)", "Inspection Registre Immuable (Lecture Seule)"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        Text(
          text = tr(lang, "لا يمكن للمالك تعديل مبالغ أو مستفيدي المعاملات المسجلة لضمان النزاهة المحاسبية.", "Owner governance strictly enforces ledger immutability: amounts & recipients cannot be altered.", "L'immuabilité interdit toute modification des montants ou bénéficiaires."),
          fontSize = 11.sp,
          color = BrandSecondaryAccent
        )
        if (uiState.ownerAllLedgerTransactions.isEmpty()) {
          EmptyOwnerNotice(tr(lang, "لا توجد قيود معاملات في الدفتر السحابي بعد.", "No cloud ledger records yet.", "Aucune écriture dans le registre."))
        } else {
          uiState.ownerAllLedgerTransactions.forEach { tx ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("${tx.transactionId} • ${tx.amount} ${tx.stablecoinTicker} (${tx.networkCode})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
                  Text(tx.state, fontSize = 10.sp, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold)
                }
                Text("Recipient: ${tx.recipientName} • Fee: $${String.format(Locale.US, "%.2f", tx.declaredFeeUsd)}", fontSize = 11.sp, color = BrandTextSecondary)
                Text("Reconciliation: ${tx.reconciliationStatus} • Proof: ${tx.providerReference.ifBlank { "PENDING" }}", fontSize = 10.sp, color = BrandTextMuted)
              }
            }
          }
        }
      }

      OwnerModule.FEES_REVENUE -> {
        val settledFees = uiState.ownerAllLedgerTransactions
          .filter { it.state == "SETTLED" || it.state == "AUTHORIZED" }
          .sumOf { it.declaredFeeUsd }
        var feeInput by remember(uiState.platformSettings.fixedFeeUsd) {
          mutableStateOf(uiState.platformSettings.fixedFeeUsd.toString())
        }
        var commInput by remember(uiState.platformSettings.partnerCommissionPercent) {
          mutableStateOf(uiState.platformSettings.partnerCommissionPercent.toString())
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr(lang, "إيرادات الرسوم الفعلية من الدفتر", "Verified Ledger Fee Revenue", "Revenus de Frais Vérifiés"), fontWeight = FontWeight.Bold, color = BrandTextPrimary)
            Text(
              text = "$${String.format(Locale.US, "%,.2f", settledFees)} USD",
              fontSize = 24.sp,
              fontWeight = FontWeight.ExtraBold,
              color = BrandPrimaryAccent
            )
            Text(
              text = tr(lang, "تُحسب حصرياً من المعاملات الموثقة في Firestore (${uiState.ownerAllLedgerTransactions.size} قيد).", "Computed strictly from recorded Firestore ledger entries (${uiState.ownerAllLedgerTransactions.size} entries).", "Calculé uniquement à partir des écritures Firestore."),
              fontSize = 11.sp,
              color = BrandTextSecondary
            )
            HorizontalDivider(color = BrandSurfaceBorder)
            OutlinedTextField(value = feeInput, onValueChange = { feeInput = it }, label = { Text(tr(lang, "الرسوم الثابتة (USD)", "Flat Platform Fee (USD)", "Frais fixes (USD)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = commInput, onValueChange = { commInput = it }, label = { Text(tr(lang, "نسبة عمولة الشركاء (%)", "Partner Commission (%)", "Commission partenaire (%)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
              onClick = {
                val f = feeInput.toDoubleOrNull() ?: uiState.platformSettings.fixedFeeUsd
                val c = commInput.toDoubleOrNull() ?: uiState.platformSettings.partnerCommissionPercent
                onUpdatePlatformSettings(uiState.platformSettings.copy(fixedFeeUsd = f, partnerCommissionPercent = c))
              },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(tr(lang, "حفظ جدول الرسوم الشفاف", "Save Fee Schedule", "Enregistrer les frais"), color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      else -> Unit
    }
  }
}

@Composable
private fun CommandGridCard(
  title: String,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    modifier = modifier.clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
      Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
    }
  }
}

@Composable
private fun BrainPillarTile(
  label: String,
  count: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    color = BrandPrimaryBg,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, BrandSurfaceBorder),
    modifier = modifier.clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Icon(icon, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(18.dp))
      Text(count, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = BrandTextPrimary)
      Text(label, fontSize = 10.sp, color = BrandTextSecondary)
    }
  }
}

@Composable
internal fun EmptyOwnerNotice(text: String) {
  Surface(
    color = BrandSecondaryBg,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, BrandSurfaceBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(text, fontSize = 11.sp, color = BrandTextSecondary, modifier = Modifier.padding(14.dp))
  }
}
