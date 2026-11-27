package com.example.ui.screens.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.connectivity.ConnectorCategory
import com.example.core.connectivity.ConnectorConnectionState
import com.example.core.connectivity.IntegrationConnector
import com.example.core.governance.tr
import com.example.data.PartnerApplicationItem
import com.example.data.PartnerStatus
import com.example.model.AppLanguage
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ConnectivityHubSubTab(val labelAr: String, val labelEn: String, val labelFr: String) {
  SOCIAL("المنصات الاجتماعية", "Social Platforms", "Social"),
  WALLETS("المحافظ العالمية", "Global Wallets", "Portefeuilles"),
  STABLECOINS("مزودو المستقرة", "Stablecoin Providers", "Fournisseurs"),
  BANKING_PARTNERS("البنوك والشركاء", "Banking & Partners", "Banques & Partenaires"),
  API_MANAGER("مدير مفاتيح API", "API Connection Mgr", "Gestion API"),
  MARKETPLACE("سوق التكاملات", "Marketplace", "Marketplace")
}

@Composable
fun OwnerConnectivityHubView(
  currentLanguage: AppLanguage,
  connectors: List<IntegrationConnector>,
  testingConnectorId: String?,
  allPartners: List<PartnerApplicationItem>,
  onTestConnector: (IntegrationConnector) -> Unit,
  onUpdateConnectorState: (IntegrationConnector, ConnectorConnectionState, String) -> Unit,
  onUpdatePartnerStatus: (String, PartnerStatus, Double, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedSubTab by remember { mutableStateOf(ConnectivityHubSubTab.MARKETPLACE) }
  var marketplaceStateFilter by remember { mutableStateOf<ConnectorConnectionState?>(null) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Hub Title Banner
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
      border = BorderStroke(1.5.dp, BrandPrimaryAccent.copy(alpha = 0.6f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Hub, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(24.dp))
          Text(
            text = "GLOBAL CONNECTIVITY HUB",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = BrandPrimaryAccent
          )
        }
        Text(
          text = tr(
            currentLanguage,
            "مركز الربط الموحد: تفريق صارم بين CONNECTED و INTEGRATION_PENDING و AVAILABLE دون أي شراكات أو مفاتيح وهمية.",
            "Unified Integration Hub: Strict differentiation between CONNECTED, INTEGRATION_PENDING, and AVAILABLE. Zero plain-text secrets.",
            "Hub Unifié : Distinction stricte des états de connexion sans clés en clair."
          ),
          fontSize = 11.sp,
          color = BrandTextSecondary
        )
      }
    }

    // 6 Hub Sub-Modules Selector
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(ConnectivityHubSubTab.entries) { tab ->
        FilterChip(
          selected = selectedSubTab == tab,
          onClick = { selectedSubTab = tab },
          label = { Text(tr(currentLanguage, tab.labelAr, tab.labelEn, tab.labelFr), fontSize = 11.sp) },
          modifier = Modifier.testTag("hub_subtab_${tab.name.lowercase()}")
        )
      }
    }

    val filteredConnectors = when (selectedSubTab) {
      ConnectivityHubSubTab.SOCIAL -> connectors.filter { it.category == ConnectorCategory.SOCIAL_PLATFORM }
      ConnectivityHubSubTab.WALLETS -> connectors.filter { it.category == ConnectorCategory.GLOBAL_WALLET }
      ConnectivityHubSubTab.STABLECOINS -> connectors.filter { it.category == ConnectorCategory.STABLECOIN_PROVIDER }
      ConnectivityHubSubTab.BANKING_PARTNERS -> connectors.filter { it.category == ConnectorCategory.BANKING_FINANCIAL }
      ConnectivityHubSubTab.API_MANAGER -> connectors
      ConnectivityHubSubTab.MARKETPLACE -> connectors.filter {
        marketplaceStateFilter == null || it.status == marketplaceStateFilter
      }
    }

    if (selectedSubTab == ConnectivityHubSubTab.MARKETPLACE) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
          FilterChip(
            selected = marketplaceStateFilter == null,
            onClick = { marketplaceStateFilter = null },
            label = { Text(tr(currentLanguage, "الكل", "All", "Tout"), fontSize = 10.sp) }
          )
        }
        items(ConnectorConnectionState.entries) { st ->
          FilterChip(
            selected = marketplaceStateFilter == st,
            onClick = { marketplaceStateFilter = st },
            label = { Text(st.name, fontSize = 10.sp) }
          )
        }
      }
    }

    // Connector Cards
    filteredConnectors.forEach { conn ->
      ConnectorCard(
        currentLanguage = currentLanguage,
        connector = conn,
        isApiManagerMode = selectedSubTab == ConnectivityHubSubTab.API_MANAGER,
        isTesting = testingConnectorId == conn.id,
        onTest = { onTestConnector(conn) },
        onSetStatus = { nextStatus -> onUpdateConnectorState(conn, nextStatus, conn.missingRequirementsNote) }
      )
    }

    // Partner Application Moderation Queue inside Banking & Partners tab
    if (selectedSubTab == ConnectivityHubSubTab.BANKING_PARTNERS) {
      Text(
        text = tr(currentLanguage, "طابور اعتماد الشركاء والشركات (${allPartners.size})", "Partner Onboarding Moderation Queue (${allPartners.size})", "File d'approbation Partenaires (${allPartners.size})"),
        fontWeight = FontWeight.Bold,
        color = BrandTextPrimary,
        style = MaterialTheme.typography.titleSmall
      )

      if (allPartners.isEmpty()) {
        Surface(color = BrandSecondaryBg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
          Text(
            text = tr(currentLanguage, "لا توجد طلبات شراكة مسجلة بعد.", "No partner applications submitted yet.", "Aucune demande partenaire."),
            fontSize = 11.sp,
            color = BrandTextSecondary,
            modifier = Modifier.padding(14.dp)
          )
        }
      } else {
        allPartners.forEach { partner ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
            border = BorderStroke(1.dp, BrandSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(partner.companyName, fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 13.sp)
                Surface(color = BrandSecondaryAccent.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                  Text(partner.status.name, color = BrandSecondaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
              }
              Text("${partner.sector.titleEn} • ${partner.countryCode} • ${partner.contactEmail}", fontSize = 11.sp, color = BrandTextSecondary)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { onUpdatePartnerStatus(partner.id, PartnerStatus.ACTIVE, partner.commissionPercent, "Verified by Owner") },
                  modifier = Modifier.weight(1f)
                ) {
                  Text(tr(currentLanguage, "تفعيل", "Activate", "Activer"), fontSize = 10.sp, color = BrandPrimaryAccent)
                }
                OutlinedButton(
                  onClick = { onUpdatePartnerStatus(partner.id, PartnerStatus.SUSPENDED, partner.commissionPercent, "Suspended by Owner") },
                  modifier = Modifier.weight(1f)
                ) {
                  Text(tr(currentLanguage, "إيقاف", "Suspend", "Suspendre"), fontSize = 10.sp, color = StatusError)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ConnectorCard(
  currentLanguage: AppLanguage,
  connector: IntegrationConnector,
  isApiManagerMode: Boolean,
  isTesting: Boolean,
  onTest: () -> Unit,
  onSetStatus: (ConnectorConnectionState) -> Unit
) {
  val statusColor = when (connector.status) {
    ConnectorConnectionState.CONNECTED -> BrandPrimaryAccent
    ConnectorConnectionState.AVAILABLE -> BrandSecondaryAccent
    ConnectorConnectionState.REQUESTED -> BrandIntelligenceAccent
    ConnectorConnectionState.INTEGRATION_PENDING -> StatusWarning
    ConnectorConnectionState.DISABLED -> BrandTextMuted
    ConnectorConnectionState.ERROR -> StatusError
  }

  val lastCheckedText = connector.lastCheckedAtMillis?.let {
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(it))
  } ?: "N/A"

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth().testTag("connector_card_${connector.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(connector.name, fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 13.sp)
          Text("${connector.custodyClassification.name} • ${connector.apiType}", fontSize = 10.sp, color = BrandTextSecondary)
        }
        Surface(color = statusColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
          Text(
            text = tr(currentLanguage, connector.status.labelAr, connector.status.labelEn, connector.status.labelFr),
            color = statusColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      if (isApiManagerMode) {
        Surface(color = BrandPrimaryBg, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, BrandSurfaceBorder)) {
          Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Secret Ref (Masked): ${connector.secretReferenceKey}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = BrandPrimaryAccent)
            Text("Endpoint: ${connector.endpointUrl}", fontSize = 10.sp, color = BrandTextSecondary)
            Text("Auth Configured: ${connector.authConfigured} | Rate Limit: ${connector.rateLimitPerMin}/min | Env: ${connector.environment.name}", fontSize = 10.sp, color = BrandTextMuted)
          }
        }
      } else {
        Text(
          text = "Stablecoins: ${connector.supportedStablecoins.joinToString()} • Networks: ${connector.supportedNetworks.joinToString { it.code }}",
          fontSize = 10.sp,
          color = BrandSecondaryAccent
        )
        Text(connector.lastErrorNote ?: connector.missingRequirementsNote, fontSize = 10.sp, color = BrandTextSecondary)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Probe: $lastCheckedText • Latency: ${connector.latencyMs?.let { "${it}ms" } ?: "N/A"}",
          fontSize = 10.sp,
          color = BrandTextMuted
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = {
              val next = if (connector.status == ConnectorConnectionState.DISABLED) ConnectorConnectionState.AVAILABLE else ConnectorConnectionState.DISABLED
              onSetStatus(next)
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Text(
              text = if (connector.status == ConnectorConnectionState.DISABLED) tr(currentLanguage, "تفعيل", "Enable", "Activer")
              else tr(currentLanguage, "تعطيل", "Disable", "Désactiver"),
              fontSize = 10.sp
            )
          }

          Button(
            onClick = onTest,
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(34.dp).testTag("test_connector_${connector.id}")
          ) {
            if (isTesting) {
              CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BrandPrimaryBg)
            } else {
              Text(tr(currentLanguage, "فحص الاتصال", "Test Connection", "Tester"), color = BrandPrimaryBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
