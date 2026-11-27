package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.asset.StablecoinApprovalStatus
import com.example.core.asset.StablecoinRegistryEntry
import com.example.core.asset.StablecoinRiskStatus
import com.example.core.governance.tr
import com.example.market.core.ExchangeHealthStatus
import com.example.market.core.NormalizedMarketTick
import com.example.model.AppLanguage
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MarketScreen(
  currentLanguage: AppLanguage,
  stablecoinRegistry: List<StablecoinRegistryEntry>,
  liveStablecoinTicks: Map<String, NormalizedMarketTick>,
  exchangeHealth: List<ExchangeHealthStatus>,
  isLiveFeedRefreshing: Boolean,
  lastLiveFeedUpdatedAtMillis: Long?,
  flatFeeUsd: Double,
  onRefreshLiveFeeds: () -> Unit,
  onRequestStablecoinTransfer: (StablecoinRegistryEntry) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var selectedTickerFilter by remember { mutableStateOf("ALL") }
  val tickers = listOf("ALL", "USDC", "USDT", "EURC", "PYUSD")

  val filteredRegistry = remember(stablecoinRegistry, selectedTickerFilter) {
    if (selectedTickerFilter == "ALL") stablecoinRegistry
    else stablecoinRegistry.filter { it.ticker.equals(selectedTickerFilter, ignoreCase = true) }
  }

  val formattedUpdatedTime = remember(lastLiveFeedUpdatedAtMillis) {
    lastLiveFeedUpdatedAtMillis?.let { ts ->
      SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ts))
    } ?: "N/A"
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    // 1. Stablecoins-Only & Risk Disclosure Header
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
        border = BorderStroke(1.2.dp, BrandPrimaryAccent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandPrimaryAccent)
              Text(
                text = tr(
                  currentLanguage,
                  "سجل العملات المستقرة المعتمدة",
                  "Approved Stablecoins Registry",
                  "Registre des Stablecoins Approuvés"
                ),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
              )
            }
            Surface(
              color = BrandPrimaryBg,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.5f))
            ) {
              Text(
                text = tr(currentLanguage, "غير حافظ", "Non-Custodial", "Non dépositaire"),
                color = BrandSecondaryAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }

          Text(
            text = tr(
              currentLanguage,
              "تقتصر الشبكة حصرياً على العملات المستقرة الموثقة العقود (USDC, USDT, EURC, PYUSD). يُمنع تداول BTC أو العملات المتقلبة أو الميمز. العملات المستقرة ليست خالية من المخاطر وتخضع لجهة الإصدار والاحتياطيات.",
              "SOCIAL PAY strictly accredits contract-verified Stablecoins only (USDC, USDT, EURC, PYUSD). Speculative tokens (BTC, ETH, meme coins) are excluded. Stablecoins carry issuer, reserve, and depeg risks.",
              "SOCIAL PAY prend exclusivement en charge les stablecoins vérifiés (USDC, USDT, EURC, PYUSD). Actifs volatils exclus. Comporte des risques d'émetteur et de dépeg."
            ),
            fontSize = 11.sp,
            color = BrandTextSecondary,
            lineHeight = 16.sp
          )
        }
      }
    }

    // 2. Live Peg & Public Exchange Feeds Monitor (Honest N/A until live refresh)
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BrandSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = tr(
                  currentLanguage,
                  "مراقب ربط السعر الحي (Public Feeds)",
                  "Live Peg & FX Reference Monitor",
                  "Moniteur de Parité en Direct"
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
              Text(
                text = tr(
                  currentLanguage,
                  "آخر تحديث: $formattedUpdatedTime",
                  "Last Updated: $formattedUpdatedTime",
                  "Dernière mise à jour : $formattedUpdatedTime"
                ),
                fontSize = 10.sp,
                color = BrandTextSecondary
              )
            }

            OutlinedButton(
              onClick = onRefreshLiveFeeds,
              enabled = !isLiveFeedRefreshing,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, BrandPrimaryAccent),
              modifier = Modifier.testTag("refresh_live_peg_btn")
            ) {
              if (isLiveFeedRefreshing) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = BrandPrimaryAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
              } else {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
              }
              Text(
                text = tr(currentLanguage, "تحديث الربط", "Refresh Feeds", "Actualiser"),
                fontSize = 11.sp,
                color = BrandPrimaryAccent,
                fontWeight = FontWeight.Bold
              )
            }
          }

          if (liveStablecoinTicks.isEmpty()) {
            Surface(
              color = BrandPrimaryBg,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = tr(
                  currentLanguage,
                  "حالة الربط الحي: N/A — اضغط (تحديث الربط) لجلب أسعار المزودين العموميين الحقيقية.",
                  "Live Peg Status: N/A — Tap Refresh Feeds to query public Coinbase, Binance, Kraken, OKX & Bybit spot endpoints.",
                  "Statut en direct : N/A — Appuyez sur Actualiser pour interroger les flux publics."
                ),
                fontSize = 11.sp,
                color = BrandTextSecondary,
                modifier = Modifier.padding(10.dp)
              )
            }
          } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(liveStablecoinTicks.entries.toList()) { (_, tick) ->
                Surface(
                  color = BrandPrimaryBg,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.35f))
                ) {
                  Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(tick.symbol, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandPrimaryAccent)
                    Text(
                      text = String.format(Locale.US, "%.4f", tick.lastPrice),
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 14.sp,
                      color = BrandTextPrimary
                    )
                    Text(tick.exchangeName, fontSize = 9.sp, color = BrandTextSecondary)
                  }
                }
              }
            }
          }

          if (exchangeHealth.isNotEmpty()) {
            Text(
              text = exchangeHealth.joinToString(" • ") { "${it.exchangeName}: ${it.responseTimeMs}ms" },
              fontSize = 9.sp,
              color = BrandTextMuted
            )
          }
        }
      }
    }

    // 3. Stablecoin Filter Chips
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tickers) { ticker ->
          FilterChip(
            selected = selectedTickerFilter == ticker,
            onClick = { selectedTickerFilter = ticker },
            label = {
              Text(
                text = if (ticker == "ALL") tr(currentLanguage, "الكل", "All", "Tous") else ticker,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            },
            modifier = Modifier.testTag("stablecoin_filter_$ticker")
          )
        }
      }
    }

    // 4. Accredited Stablecoin Registry Cards
    items(filteredRegistry, key = { it.id }) { entry ->
      StablecoinRegistryCard(
        entry = entry,
        currentLanguage = currentLanguage,
        flatFeeUsd = flatFeeUsd,
        onCopyContract = {
          clipboardManager.setText(AnnotatedString(entry.contractAddress))
          Toast.makeText(
            context,
            tr(currentLanguage, "تم نسخ عنوان العقد الموثق", "Contract address copied", "Adresse du contrat copiée"),
            Toast.LENGTH_SHORT
          ).show()
        },
        onStartTransfer = { onRequestStablecoinTransfer(entry) }
      )
    }
  }
}

@Composable
private fun StablecoinRegistryCard(
  entry: StablecoinRegistryEntry,
  currentLanguage: AppLanguage,
  flatFeeUsd: Double,
  onCopyContract: () -> Unit,
  onStartTransfer: () -> Unit
) {
  val isApproved = entry.approvalStatus == StablecoinApprovalStatus.APPROVED &&
    entry.riskStatus != StablecoinRiskStatus.CRITICAL
  val approvalColor = when (entry.approvalStatus) {
    StablecoinApprovalStatus.APPROVED -> StatusSuccess
    StablecoinApprovalStatus.UNDER_REVIEW -> StatusWarning
    StablecoinApprovalStatus.PAUSED, StablecoinApprovalStatus.REJECTED -> StatusError
  }
  val riskColor = when (entry.riskStatus) {
    StablecoinRiskStatus.LOW -> BrandPrimaryAccent
    StablecoinRiskStatus.MEDIUM -> StatusWarning
    StablecoinRiskStatus.ELEVATED, StablecoinRiskStatus.CRITICAL -> StatusError
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, approvalColor.copy(alpha = 0.4f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("stablecoin_card_${entry.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(entry.assetName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
          Text(
            text = "${entry.issuer} • ${tr(currentLanguage, entry.network.displayNameAr, entry.network.displayNameEn, entry.network.displayNameFr)}",
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
        }
        Surface(color = approvalColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
          Text(
            text = tr(currentLanguage, entry.approvalStatus.labelAr, entry.approvalStatus.labelEn, entry.approvalStatus.labelFr),
            color = approvalColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
          )
        }
      }

      // Contract Address Row
      Surface(color = BrandPrimaryBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = tr(currentLanguage, "عنوان العقد الموثق:", "Verified Contract:", "Contrat vérifié :"),
              fontSize = 9.sp,
              color = BrandTextSecondary
            )
            Text(
              text = entry.contractAddress,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              color = BrandPrimaryAccent,
              maxLines = 1
            )
          }
          IconButton(onClick = onCopyContract, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy contract", tint = BrandPrimaryAccent, modifier = Modifier.size(14.dp))
          }
        }
      }

      // Corridor Limits, Fees & Risk Status
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = tr(
            currentLanguage,
            "الحدود: ${entry.minAmount}–${entry.maxAmount.toInt()} ${entry.ticker}",
            "Limits: ${entry.minAmount}–${entry.maxAmount.toInt()} ${entry.ticker}",
            "Limites : ${entry.minAmount}–${entry.maxAmount.toInt()} ${entry.ticker}"
          ),
          fontSize = 10.sp,
          color = BrandTextSecondary
        )
        Text(
          text = tr(
            currentLanguage,
            "الرسوم: $${String.format(Locale.US, "%.2f", flatFeeUsd)} + شبكة ~$${String.format(Locale.US, "%.2f", entry.estimatedNetworkFeeUsd)}",
            "Fee: $${String.format(Locale.US, "%.2f", flatFeeUsd)} + gas ~$${String.format(Locale.US, "%.2f", entry.estimatedNetworkFeeUsd)}",
            "Frais : $${String.format(Locale.US, "%.2f", flatFeeUsd)} + réseau ~$${String.format(Locale.US, "%.2f", entry.estimatedNetworkFeeUsd)}"
          ),
          fontSize = 10.sp,
          color = BrandSecondaryAccent,
          fontWeight = FontWeight.SemiBold
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = riskColor.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
          Text(
            text = tr(currentLanguage, entry.riskStatus.labelAr, entry.riskStatus.labelEn, entry.riskStatus.labelFr),
            color = riskColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
        Text(
          text = "${entry.verificationSource} (${entry.lastReviewDate})",
          fontSize = 9.sp,
          color = BrandTextMuted,
          maxLines = 1
        )
      }

      Text(
        text = tr(currentLanguage, entry.riskDisclosureAr, entry.riskDisclosureEn, entry.riskDisclosureFr),
        fontSize = 10.sp,
        color = BrandTextSecondary,
        lineHeight = 14.sp
      )

      Button(
        onClick = onStartTransfer,
        enabled = isApproved,
        colors = ButtonDefaults.buttonColors(
          containerColor = BrandPrimaryAccent,
          contentColor = BrandPrimaryBg
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("start_transfer_${entry.id}")
      ) {
        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isApproved) {
            tr(
              currentLanguage,
              "إنشاء طلب دفع ${entry.ticker} (${entry.network.code})",
              "Create ${entry.ticker} (${entry.network.code}) Request",
              "Créer une demande ${entry.ticker} (${entry.network.code})"
            )
          } else {
            tr(currentLanguage, "غير متاح للتحويل حالياً", "Unavailable for Transfers", "Indisponible")
          },
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
      }
    }
  }
}
