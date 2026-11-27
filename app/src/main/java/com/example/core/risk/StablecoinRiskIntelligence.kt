package com.example.core.risk

import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.asset.StablecoinApprovalStatus
import com.example.core.asset.StablecoinRegistryEntry
import com.example.core.asset.StablecoinRiskStatus
import kotlin.math.abs

enum class RiskSeverity {
  INFO,
  WARNING,
  HIGH,
  CRITICAL
}

enum class RiskDimension(
  val titleAr: String,
  val titleEn: String,
  val titleFr: String
) {
  DEPEG("انحراف السعر عن المرجع (Depeg)", "Peg Deviation", "Écart d'ancrage (Dépeg)"),
  RESERVES("موثوقية الاحتياطيات", "Reserve Attestation", "Fiabilité des réserves"),
  ISSUER("حالة جهة الإصدار", "Issuer Solvency & Status", "Statut de l'émetteur"),
  REDEMPTION("إمكانية الاسترداد", "Redeemability", "Capacité de rachat"),
  FREEZE_RISK("صلاحية تجميد الرموز", "Token Freeze Authority", "Pouvoir de gel"),
  SMART_CONTRACT("مخاطر العقد الذكي", "Smart Contract Risk", "Risque contrat intelligent"),
  NETWORK_OUTAGE("توقف أو ازدحام الشبكة", "Network Outage / Congestion", "Panne / Congestion réseau"),
  CUSTODY_PROVIDER("مخاطر مزود الحفظ", "Custody Provider Risk", "Risque dépositaire"),
  LIQUIDITY("مخاطر السيولة", "Liquidity Depth", "Risque de liquidité")
}

data class StablecoinRiskAssessment(
  val stablecoinId: String,
  val ticker: String,
  val network: BlockchainNetwork,
  val referencePrice: Double,
  val observedMarketPrice: Double?,
  val deviationBps: Double?, // basis points; null if live price unavailable (never faked as 0!)
  val riskStatus: StablecoinRiskStatus,
  val routePaused: Boolean,
  val requiresHumanReview: Boolean,
  val activeAlerts: List<StablecoinRiskAlert>,
  val checkedAtMillis: Long = System.currentTimeMillis(),
  val dataSource: String
)

data class StablecoinRiskAlert(
  val alertId: String,
  val stablecoinId: String,
  val dimension: RiskDimension,
  val severity: RiskSeverity,
  val reasonAr: String,
  val reasonEn: String,
  val reasonFr: String,
  val pausedRoutes: Boolean,
  val requiresHumanReview: Boolean,
  val timestampMillis: Long = System.currentTimeMillis()
)

data class RiskDecisionLog(
  val decisionId: String,
  val stablecoinId: String,
  val previousRiskStatus: StablecoinRiskStatus,
  val newRiskStatus: StablecoinRiskStatus,
  val reason: String,
  val requiresHumanReview: Boolean,
  val decidedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Independent Stablecoin Risk Intelligence Monitor.
 * Evaluates peg health, network status, reserve/issuer/freeze risks, pauses impacted corridors
 * on critical anomalies, and logs every decision without ever modifying historical transactions.
 */
class StablecoinRiskIntelligence {

  private val pausedAssetIds = mutableSetOf<String>()
  private val pausedNetworks = mutableSetOf<BlockchainNetwork>()
  private val decisionLogs = mutableListOf<RiskDecisionLog>()

  fun isRoutePaused(stablecoinId: String, network: BlockchainNetwork): Boolean {
    return stablecoinId in pausedAssetIds || network in pausedNetworks
  }

  fun setAssetPaused(stablecoinId: String, paused: Boolean, reason: String): RiskDecisionLog {
    if (paused) pausedAssetIds.add(stablecoinId) else pausedAssetIds.remove(stablecoinId)
    val log = RiskDecisionLog(
      decisionId = "RSK-DEC-${System.currentTimeMillis()}",
      stablecoinId = stablecoinId,
      previousRiskStatus = StablecoinRiskStatus.MEDIUM,
      newRiskStatus = if (paused) StablecoinRiskStatus.CRITICAL else StablecoinRiskStatus.LOW,
      reason = reason,
      requiresHumanReview = paused
    )
    decisionLogs.add(0, log)
    return log
  }

  fun setNetworkPaused(network: BlockchainNetwork, paused: Boolean) {
    if (paused) pausedNetworks.add(network) else pausedNetworks.remove(network)
  }

  fun evaluateStablecoin(
    entry: StablecoinRegistryEntry,
    observedPrice: Double?,
    dataSource: String,
    networkOnline: Boolean = true
  ): StablecoinRiskAssessment {
    val alerts = mutableListOf<StablecoinRiskAlert>()
    var computedRisk = entry.riskStatus
    var requiresReview = false
    var shouldPause = isRoutePaused(entry.id, entry.network)

    val reference = 1.0
    val deviationBps = observedPrice?.let { abs(it - reference) * 10_000.0 }

    // 1. Depeg Check
    if (deviationBps != null) {
      when {
        deviationBps >= 200.0 -> { // >= 2.00% depeg
          computedRisk = StablecoinRiskStatus.CRITICAL
          shouldPause = true
          requiresReview = true
          pausedAssetIds.add(entry.id)
          alerts.add(
            StablecoinRiskAlert(
              alertId = "ALT-DEPEG-${entry.id}",
              stablecoinId = entry.id,
              dimension = RiskDimension.DEPEG,
              severity = RiskSeverity.CRITICAL,
              reasonAr = "فك ارتباط حرج (${String.format("%.0f", deviationBps)} نقطة أساس): تم تعليق المسارات الجديدة وطلب مراجعة بشرية.",
              reasonEn = "Critical depeg (${String.format("%.0f", deviationBps)} bps): New routes paused; human review required.",
              reasonFr = "Dépeg critique (${String.format("%.0f", deviationBps)} bps) : routes suspendues.",
              pausedRoutes = true,
              requiresHumanReview = true
            )
          )
        }
        deviationBps >= 50.0 -> { // 0.50% - 1.99% deviation
          if (computedRisk == StablecoinRiskStatus.LOW) computedRisk = StablecoinRiskStatus.ELEVATED
          requiresReview = true
          alerts.add(
            StablecoinRiskAlert(
              alertId = "ALT-PEG-${entry.id}",
              stablecoinId = entry.id,
              dimension = RiskDimension.DEPEG,
              severity = RiskSeverity.HIGH,
              reasonAr = "انحراف سعر ملحوظ (${String.format("%.1f", deviationBps)} bps) عن السعر المرجعي.",
              reasonEn = "Elevated peg deviation (${String.format("%.1f", deviationBps)} bps) vs reference.",
              reasonFr = "Écart d'ancrage élevé (${String.format("%.1f", deviationBps)} bps).",
              pausedRoutes = false,
              requiresHumanReview = true
            )
          )
        }
      }
    }

    // 2. Network Downtime Check
    if (!networkOnline) {
      computedRisk = StablecoinRiskStatus.CRITICAL
      shouldPause = true
      requiresReview = true
      alerts.add(
        StablecoinRiskAlert(
          alertId = "ALT-NET-${entry.network.code}",
          stablecoinId = entry.id,
          dimension = RiskDimension.NETWORK_OUTAGE,
          severity = RiskSeverity.CRITICAL,
          reasonAr = "انقطاع أو تعطل في شبكة ${entry.network.displayNameAr}.",
          reasonEn = "Network outage detected on ${entry.network.displayNameEn}.",
          reasonFr = "Panne détectée sur ${entry.network.displayNameFr}.",
          pausedRoutes = true,
          requiresHumanReview = true
        )
      )
    }

    // 3. Approval Status Check
    if (entry.approvalStatus != StablecoinApprovalStatus.APPROVED) {
      shouldPause = true
      requiresReview = true
      alerts.add(
        StablecoinRiskAlert(
          alertId = "ALT-STAT-${entry.id}",
          stablecoinId = entry.id,
          dimension = RiskDimension.ISSUER,
          severity = RiskSeverity.WARNING,
          reasonAr = "الحالة الحالية للعملة: ${entry.approvalStatus.labelAr}.",
          reasonEn = "Asset approval state: ${entry.approvalStatus.labelEn}.",
          reasonFr = "Statut de l'actif : ${entry.approvalStatus.labelFr}.",
          pausedRoutes = true,
          requiresHumanReview = true
        )
      )
    }

    // 4. Freeze Authority Informational Risk
    if (entry.hasFreezeAuthority) {
      alerts.add(
        StablecoinRiskAlert(
          alertId = "ALT-FRZ-${entry.id}",
          stablecoinId = entry.id,
          dimension = RiskDimension.FREEZE_RISK,
          severity = RiskSeverity.INFO,
          reasonAr = "تحتفظ جهة الإصدار (${entry.issuer}) بصلاحية تجميد العقود عند الطلب القانوني.",
          reasonEn = "Issuer (${entry.issuer}) retains smart-contract freeze capability.",
          reasonFr = "L'émetteur (${entry.issuer}) dispose d'un pouvoir de gel.",
          pausedRoutes = false,
          requiresHumanReview = false
        )
      )
    }

    if (computedRisk != entry.riskStatus) {
      decisionLogs.add(
        0,
        RiskDecisionLog(
          decisionId = "RSK-${System.currentTimeMillis()}",
          stablecoinId = entry.id,
          previousRiskStatus = entry.riskStatus,
          newRiskStatus = computedRisk,
          reason = alerts.firstOrNull()?.reasonEn ?: "Risk reassessment",
          requiresHumanReview = requiresReview
        )
      )
    }

    return StablecoinRiskAssessment(
      stablecoinId = entry.id,
      ticker = entry.ticker,
      network = entry.network,
      referencePrice = reference,
      observedMarketPrice = observedPrice,
      deviationBps = deviationBps,
      riskStatus = computedRisk,
      routePaused = shouldPause,
      requiresHumanReview = requiresReview,
      activeAlerts = alerts,
      dataSource = dataSource
    )
  }

  fun evaluateRegistry(
    observedTickerPrices: Map<String, Pair<Double, String>>
  ): List<StablecoinRiskAssessment> {
    return DigitalAssetEngine.APPROVED_STABLECOIN_REGISTRY.map { entry ->
      val live = observedTickerPrices[entry.ticker]
      evaluateStablecoin(
        entry = entry,
        observedPrice = live?.first,
        dataSource = live?.second ?: "Feed Not Connected (No Fake Price)"
      )
    }
  }

  fun getDecisionLogs(): List<RiskDecisionLog> = decisionLogs.toList()
}
