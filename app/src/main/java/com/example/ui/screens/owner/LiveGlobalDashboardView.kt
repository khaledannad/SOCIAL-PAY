package com.example.ui.screens.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.connectivity.ConnectorConnectionState
import com.example.core.connectivity.IntegrationConnector
import com.example.core.governance.tr
import com.example.data.CloudLedgerTransactionRecord
import com.example.data.MerchantProfileRecord
import com.example.data.PartnerApplicationItem
import com.example.data.UserProfileDoc
import com.example.market.core.ExchangeHealthStatus
import com.example.market.core.NormalizedMarketTick
import com.example.model.AppLanguage
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveGlobalDashboardView(
  currentLanguage: AppLanguage,
  liveStablecoinTicks: Map<String, NormalizedMarketTick>,
  exchangeHealth: List<ExchangeHealthStatus>,
  isLiveFeedRefreshing: Boolean,
  lastLiveFeedUpdatedAtMillis: Long?,
  connectors: List<IntegrationConnector>,
  ledgerTransactions: List<CloudLedgerTransactionRecord>,
  registeredUsers: List<UserProfileDoc>,
  merchants: List<MerchantProfileRecord>,
  partners: List<PartnerApplicationItem>,
  isPlatformWideHalted: Boolean,
  onRefreshLiveFeeds: () -> Unit,
  onOpenConnectivityHub: () -> Unit,
  modifier: Modifier = Modifier
) {
  val lastUpdatedLabel = remember(lastLiveFeedUpdatedAtMillis) {
    lastLiveFeedUpdatedAtMillis?.let {
      SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(it))
    } ?: "N/A"
  }

  val connectedCount = connectors.count { it.status == ConnectorConnectionState.CONNECTED }
  val pendingCount = connectors.count {
    it.status == ConnectorConnectionState.INTEGRATION_PENDING || it.status == ConnectorConnectionState.REQUESTED
  }
  val errorCount = connectors.count { it.status == ConnectorConnectionState.ERROR }
  val declaredRevenueUsd = ledgerTransactions
    .filter { it.state == "SETTLED" || it.state == "AUTHORIZED" }
    .sumOf { it.declaredFeeUsd }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Dark Global Connectivity Map Card (Inspired by Global Command Mockup)
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = BrandPrimaryBg),
      border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                color = if (isPlatformWideHalted) StatusError.copy(alpha = 0.18f) else BrandPrimaryAccent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = if (isPlatformWideHalted) tr(currentLanguage, "حالة الطوارئ — مقفل", "EMERGENCY HALTED", "ARRÊT D'URGENCE")
                  else tr(currentLanguage, "الشبكة متصلة وجاهزة", "SYSTEM ONLINE", "SYSTÈME EN LIGNE"),
                  color = if (isPlatformWideHalted) StatusError else BrandPrimaryAccent,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
              Text(
                text = "${tr(currentLanguage, "آخر تحديث:", "Last Updated:", "Mis à jour :")} $lastUpdatedLabel",
                fontSize = 10.sp,
                color = BrandTextSecondary
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = tr(currentLanguage, "لوحة الشبكة العالمية الحية", "Live Global Connectivity Monitor", "Moniteur Mondial en Direct"),
              fontWeight = FontWeight.ExtraBold,
              color = BrandTextPrimary,
              style = MaterialTheme.typography.titleMedium
            )
          }

          IconButton(
            onClick = onRefreshLiveFeeds,
            enabled = !isLiveFeedRefreshing,
            modifier = Modifier.testTag("owner_refresh_live_feeds_btn")
          ) {
            if (isLiveFeedRefreshing) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BrandPrimaryAccent)
            } else {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh live telemetry", tint = BrandPrimaryAccent)
            }
          }
        }

        // Custom Compose Canvas Node-Arc Global Connectivity Topology
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              Brush.radialGradient(
                colors = listOf(BrandSurfaceVariant, BrandPrimaryBg)
              )
            )
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle coordinate grid
            val gridColor = BrandSurfaceBorder.copy(alpha = 0.35f)
            for (i in 1..5) {
              val y = h * (i / 6f)
              drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            for (j in 1..7) {
              val x = w * (j / 8f)
              drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            }

            // Global Regional Hub Nodes (Americas, Europe, MENA/Algiers, Gulf, Asia, Oceania)
            val nyc = Offset(w * 0.22f, h * 0.38f)
            val sao = Offset(w * 0.30f, h * 0.74f)
            val lon = Offset(w * 0.46f, h * 0.28f)
            val algiers = Offset(w * 0.49f, h * 0.45f)
            val riyadh = Offset(w * 0.59f, h * 0.50f)
            val singapore = Offset(w * 0.76f, h * 0.60f)
            val tokyo = Offset(w * 0.84f, h * 0.36f)

            val arcs = listOf(
              Pair(nyc, lon),
              Pair(lon, algiers),
              Pair(algiers, riyadh),
              Pair(riyadh, singapore),
              Pair(singapore, tokyo),
              Pair(nyc, sao),
              Pair(lon, riyadh),
              Pair(sao, algiers)
            )

            val arcBrush = Brush.linearGradient(
              colors = listOf(BrandPrimaryAccent, BrandSecondaryAccent, BrandIntelligenceAccent)
            )

            arcs.forEach { (start, end) ->
              val control = Offset(
                x = (start.x + end.x) / 2f,
                y = minOf(start.y, end.y) - h * 0.14f
              )
              val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(control.x, control.y, end.x, end.y)
              }
              drawPath(
                path = path,
                brush = arcBrush,
                style = Stroke(width = 2.6f, cap = StrokeCap.Round)
              )
            }

            val nodes = listOf(nyc, sao, lon, algiers, riyadh, singapore, tokyo)
            nodes.forEachIndexed { index, pos ->
              val accent = when {
                index == 3 -> BrandPrimaryAccent // MENA Core
                index % 2 == 0 -> BrandSecondaryAccent
                else -> BrandIntelligenceAccent
              }
              drawCircle(color = accent.copy(alpha = 0.25f), radius = 14f, center = pos)
              drawCircle(color = accent, radius = 6f, center = pos)
              drawCircle(color = Color.White, radius = 2.5f, center = pos)
            }
          }

          Surface(
            color = BrandPrimaryBg.copy(alpha = 0.8f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
          ) {
            Text(
              text = "${tr(currentLanguage, "متصل فعلياً:", "Connected:", "Connectés :")} $connectedCount | ${tr(currentLanguage, "معلق:", "Pending:", "En attente :")} $pendingCount | ${tr(currentLanguage, "أخطاء:", "Errors:", "Erreurs :")} $errorCount",
              fontSize = 10.sp,
              color = BrandPrimaryAccent,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        // Honest 4-Metric Telemetry Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          DashboardMetricTile(
            title = tr(currentLanguage, "المستخدمون", "Users", "Utilisateurs"),
            value = "${registeredUsers.size}",
            accent = BrandPrimaryAccent,
            modifier = Modifier.weight(1f)
          )
          DashboardMetricTile(
            title = tr(currentLanguage, "الشركات والتجار", "Businesses", "Entreprises"),
            value = "${merchants.size + partners.size}",
            accent = BrandSecondaryAccent,
            modifier = Modifier.weight(1f)
          )
          DashboardMetricTile(
            title = tr(currentLanguage, "سجل الدفتر", "Ledger Txns", "Registre"),
            value = "${ledgerTransactions.size}",
            accent = BrandIntelligenceAccent,
            modifier = Modifier.weight(1f)
          )
          DashboardMetricTile(
            title = tr(currentLanguage, "الرسوم المصرحة", "Fee Revenue", "Frais"),
            value = if (ledgerTransactions.isEmpty()) "$0.00" else "$${String.format(Locale.US, "%.2f", declaredRevenueUsd)}",
            accent = StatusSuccess,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // 2. Live Stablecoin Peg Telemetry (Honest N/A when not synced)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
      border = BorderStroke(1.dp, BrandSurfaceBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = tr(currentLanguage, "أسعار الربط الحية للعملات المستقرة", "Live Stablecoin Peg Feeds", "Flux Stablecoins en Direct"),
            fontWeight = FontWeight.Bold,
            color = BrandTextPrimary,
            style = MaterialTheme.typography.titleSmall
          )
          TextButton(onClick = onOpenConnectivityHub) {
            Text(tr(currentLanguage, "مركز التكاملات", "Connectivity Hub →", "Hub Connectivité →"), color = BrandPrimaryAccent, fontSize = 11.sp)
          }
        }

        if (liveStablecoinTicks.isEmpty()) {
          Text(
            text = tr(
              currentLanguage,
              "الحالة: N/A — اضغط تحديث لجلب الأسعار الحية من المصادر العامة المعتمدة فقط.",
              "Status: N/A — Tap Refresh to probe public read-only stablecoin peg feeds.",
              "Statut : N/A — Appuyez sur Actualiser pour interroger les flux publics."
            ),
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
        } else {
          liveStablecoinTicks.values.take(6).forEach { tick ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("${tick.symbol} (${tick.exchangeName})", fontSize = 12.sp, color = BrandTextPrimary, fontWeight = FontWeight.SemiBold)
              Text("$${String.format(Locale.US, "%.4f", tick.lastPrice)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold)
            }
          }
        }

        if (exchangeHealth.isNotEmpty()) {
          HorizontalDivider(color = BrandSurfaceBorder)
          exchangeHealth.take(4).forEach { health ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(health.exchangeName, fontSize = 11.sp, color = BrandTextSecondary)
              Text(
                text = if (health.isOnline) "ONLINE (${health.responseTimeMs}ms)" else "OFFLINE / N/A",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (health.isOnline) BrandPrimaryAccent else StatusWarning
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardMetricTile(
  title: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = BrandSecondaryBg,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = accent, maxLines = 1)
      Text(title, fontSize = 9.sp, color = BrandTextSecondary, maxLines = 1)
    }
  }
}
