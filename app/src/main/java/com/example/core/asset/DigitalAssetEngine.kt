package com.example.core.asset

/**
 * Supported Settlement Networks for Approved Stablecoins and Accredited Settlement Rails.
 * Volatile speculative Layer-1 token transfers are intentionally excluded from payment orchestration.
 */
enum class BlockchainNetwork(
  val code: String,
  val displayNameAr: String,
  val displayNameEn: String,
  val displayNameFr: String,
  val nativeFeeAsset: String,
  val requiredConfirmations: Int,
  val blockExplorerUrl: String,
  val isOperational: Boolean = true
) {
  TRON_TRC20(
    code = "TRC20",
    displayNameAr = "شبكة ترون (TRC-20)",
    displayNameEn = "Tron Network (TRC-20)",
    displayNameFr = "Réseau Tron (TRC-20)",
    nativeFeeAsset = "TRX",
    requiredConfirmations = 19,
    blockExplorerUrl = "https://tronscan.org/#/transaction/"
  ),
  ETHEREUM_ERC20(
    code = "ERC20",
    displayNameAr = "شبكة إيثيريوم (ERC-20)",
    displayNameEn = "Ethereum Mainnet (ERC-20)",
    displayNameFr = "Réseau Ethereum (ERC-20)",
    nativeFeeAsset = "ETH",
    requiredConfirmations = 12,
    blockExplorerUrl = "https://etherscan.io/tx/"
  ),
  SOLANA(
    code = "SOL",
    displayNameAr = "شبكة سولانا (Solana SPL)",
    displayNameEn = "Solana SPL Network",
    displayNameFr = "Réseau Solana (SPL)",
    nativeFeeAsset = "SOL",
    requiredConfirmations = 32,
    blockExplorerUrl = "https://solscan.io/tx/"
  ),
  POLYGON_POS(
    code = "POLYGON",
    displayNameAr = "شبكة بوليجون (Polygon PoS)",
    displayNameEn = "Polygon PoS",
    displayNameFr = "Réseau Polygon PoS",
    nativeFeeAsset = "POL",
    requiredConfirmations = 64,
    blockExplorerUrl = "https://polygonscan.com/tx/"
  ),
  ARBITRUM_ONE(
    code = "ARBITRUM",
    displayNameAr = "شبكة أربيتروم (Arbitrum One)",
    displayNameEn = "Arbitrum One",
    displayNameFr = "Réseau Arbitrum One",
    nativeFeeAsset = "ETH",
    requiredConfirmations = 12,
    blockExplorerUrl = "https://arbiscan.io/tx/"
  ),
  BASE_L2(
    code = "BASE",
    displayNameAr = "شبكة بيس (Base L2)",
    displayNameEn = "Base Network",
    displayNameFr = "Réseau Base",
    nativeFeeAsset = "ETH",
    requiredConfirmations = 12,
    blockExplorerUrl = "https://basescan.org/tx/"
  ),
  BSC_BEP20(
    code = "BEP20",
    displayNameAr = "شبكة بي إن بي (BEP-20)",
    displayNameEn = "BNB Smart Chain (BEP-20)",
    displayNameFr = "BNB Smart Chain (BEP-20)",
    nativeFeeAsset = "BNB",
    requiredConfirmations = 15,
    blockExplorerUrl = "https://bscscan.com/tx/"
  ),
  FIAT_BANKING(
    code = "FIAT",
    displayNameAr = "قنوات التسوية المصرفية المرخصة",
    displayNameEn = "Licensed Banking Settlement Rail",
    displayNameFr = "Rail Bancaire Agréé",
    nativeFeeAsset = "FIAT",
    requiredConfirmations = 1,
    blockExplorerUrl = ""
  )
}

enum class AssetType {
  APPROVED_STABLECOIN,
  FIAT_SETTLEMENT_RAIL
}

enum class StablecoinApprovalStatus(
  val labelAr: String,
  val labelEn: String,
  val labelFr: String
) {
  APPROVED("معتمدة رسمياً", "Officially Approved", "Approuvée"),
  UNDER_REVIEW("قيد المراجعة التنظيمية", "Under Review", "En cours d'examen"),
  PAUSED("موقوفة مؤقتاً بسبب المخاطر", "Paused Due to Risk", "Suspendue"),
  REJECTED("مرفوضة وغير مصرح بها", "Rejected", "Rejetée")
}

enum class StablecoinRiskStatus(
  val labelAr: String,
  val labelEn: String,
  val labelFr: String
) {
  LOW("مخاطر منخفضة (ليست معدومة)", "Low Risk (Not Risk-Free)", "Risque faible (Non garanti)"),
  MEDIUM("مخاطر متوسطة — مراقبة نشطة", "Medium Risk — Monitored", "Risque modéré"),
  ELEVATED("مخاطر مرتفعة — انحراف أو سيولة", "Elevated Risk", "Risque élevé"),
  CRITICAL("مخاطر حرجة — تم تعليق المسار", "Critical Risk — Halted", "Risque critique")
}

/**
 * Approved Stablecoin Registry Entry containing strict accreditation metadata.
 */
data class StablecoinRegistryEntry(
  val id: String,
  val assetName: String,
  val ticker: String,
  val referencePeg: String, // e.g. "USD", "EUR"
  val issuer: String,
  val contractAddress: String,
  val network: BlockchainNetwork,
  val decimals: Int,
  val approvalStatus: StablecoinApprovalStatus,
  val riskStatus: StablecoinRiskStatus,
  val lastReviewDate: String,
  val verificationSource: String,
  val minAmount: Double,
  val maxAmount: Double,
  val flatServiceFeeUsd: Double = 0.80,
  val estimatedNetworkFeeUsd: Double,
  val hasFreezeAuthority: Boolean,
  val riskDisclosureAr: String,
  val riskDisclosureEn: String,
  val riskDisclosureFr: String
)

/**
 * Unified representation of a Currency / Asset on Social Pay.
 * Restricted exclusively to Approved Stablecoins and Fiat Settlement reference rails.
 */
data class UnifiedAsset(
  val id: String,
  val ticker: String,
  val nameAr: String,
  val nameEn: String,
  val type: AssetType,
  val network: BlockchainNetwork,
  val decimals: Int,
  val minAmount: Double,
  val maxAmount: Double,
  val flatServiceFeeUsd: Double = 0.80,
  val estimatedNetworkFeeUsd: Double,
  val isEnabled: Boolean = true,
  val contractAddress: String = "",
  val issuer: String = "",
  val approvalStatus: StablecoinApprovalStatus = StablecoinApprovalStatus.APPROVED,
  val riskStatus: StablecoinRiskStatus = StablecoinRiskStatus.LOW
)

object DigitalAssetEngine {

  /**
   * Explicitly blocked volatile, meme, or unaccredited crypto symbols.
   * SOCIAL PAY strictly permits approved stablecoins only.
   */
  val BLOCKED_VOLATILE_AND_MEME_TICKERS = setOf(
    "BTC", "BITCOIN", "ETH", "ETHEREUM", "SOL", "BNB", "XRP", "ADA", "DOGE",
    "SHIB", "PEPE", "WIF", "BONK", "FLOKI", "MEME", "AVAX", "TRX", "TON", "LTC"
  )

  /**
   * Official Approved Stablecoin Registry (USDC, USDT, PYUSD, FDUSD, EURC).
   * Every contract is mapped explicitly to a single chain to eliminate symbol spoofing.
   */
  val APPROVED_STABLECOIN_REGISTRY: List<StablecoinRegistryEntry> = listOf(
    StablecoinRegistryEntry(
      id = "USDC_SOL",
      assetName = "USD Coin (Solana SPL)",
      ticker = "USDC",
      referencePeg = "USD",
      issuer = "Circle Internet Financial, LLC",
      contractAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
      network = BlockchainNetwork.SOLANA,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.APPROVED,
      riskStatus = StablecoinRiskStatus.LOW,
      lastReviewDate = "2026-11-20",
      verificationSource = "Circle Official Contract Registry & Attestation Reports",
      minAmount = 1.0,
      maxAmount = 100000.0,
      estimatedNetworkFeeUsd = 0.01,
      hasFreezeAuthority = true,
      riskDisclosureAr = "تخضع USDC لمخاطر جهة الإصدار (Circle) والاحتياطيات المصرفية والاسترداد واحتمال فك الارتباط في الأزمات ولا تُعد نقداً مضموناً.",
      riskDisclosureEn = "USDC carries issuer, reserve custody, freeze-authority, redemption, and depeg risks; it is not government-insured cash.",
      riskDisclosureFr = "USDC comporte des risques d'émetteur, de réserves, de gel et de dépeg ; il ne s'agit pas d'espèces garanties."
    ),
    StablecoinRegistryEntry(
      id = "USDC_ERC20",
      assetName = "USD Coin (Ethereum Mainnet)",
      ticker = "USDC",
      referencePeg = "USD",
      issuer = "Circle Internet Financial, LLC",
      contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
      network = BlockchainNetwork.ETHEREUM_ERC20,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.APPROVED,
      riskStatus = StablecoinRiskStatus.LOW,
      lastReviewDate = "2026-11-20",
      verificationSource = "Etherscan Verified Contract & Circle Reserve Disclosure",
      minAmount = 20.0,
      maxAmount = 250000.0,
      estimatedNetworkFeeUsd = 3.20,
      hasFreezeAuthority = true,
      riskDisclosureAr = "تخضع USDC لمخاطر جهة الإصدار والعقد الذكي والاحتياطيات ورسوم شبكة إيثيريوم، ولا تُعد خالية من المخاطر.",
      riskDisclosureEn = "USDC carries issuer, smart-contract, reserve, and depeg risk and is not guaranteed cash.",
      riskDisclosureFr = "USDC présente des risques d'émetteur, de contrat intelligent et de liquidité."
    ),
    StablecoinRegistryEntry(
      id = "USDC_POLYGON",
      assetName = "USD Coin (Polygon Native)",
      ticker = "USDC",
      referencePeg = "USD",
      issuer = "Circle Internet Financial, LLC",
      contractAddress = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359",
      network = BlockchainNetwork.POLYGON_POS,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.APPROVED,
      riskStatus = StablecoinRiskStatus.LOW,
      lastReviewDate = "2026-11-20",
      verificationSource = "PolygonScan Verified & Circle CCTP Registry",
      minAmount = 1.0,
      maxAmount = 100000.0,
      estimatedNetworkFeeUsd = 0.02,
      hasFreezeAuthority = true,
      riskDisclosureAr = "عقد USDC الأصلي على Polygon يخضع لمخاطر جهة الإصدار وبنية الشبكة والاحتياطيات.",
      riskDisclosureEn = "Native Polygon USDC carries issuer, network, and redemption risks.",
      riskDisclosureFr = "USDC natif sur Polygon comporte des risques d'émetteur et de réseau."
    ),
    StablecoinRegistryEntry(
      id = "USDC_BASE",
      assetName = "USD Coin (Base L2)",
      ticker = "USDC",
      referencePeg = "USD",
      issuer = "Circle Internet Financial, LLC",
      contractAddress = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913",
      network = BlockchainNetwork.BASE_L2,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.APPROVED,
      riskStatus = StablecoinRiskStatus.LOW,
      lastReviewDate = "2026-11-20",
      verificationSource = "BaseScan Verified & Circle Official Addresses",
      minAmount = 1.0,
      maxAmount = 100000.0,
      estimatedNetworkFeeUsd = 0.01,
      hasFreezeAuthority = true,
      riskDisclosureAr = "تخضع USDC على Base لمخاطر التسوية على الطبقة الثانية والاحتياطيات ولا تُعد ودائع بنكية مضمونة.",
      riskDisclosureEn = "USDC on Base carries L2 sequencer, issuer, and depeg risk.",
      riskDisclosureFr = "USDC sur Base comporte des risques d'émetteur et de séquenceur L2."
    ),
    StablecoinRegistryEntry(
      id = "USDT_TRC20",
      assetName = "Tether USD (TRC-20)",
      ticker = "USDT",
      referencePeg = "USD",
      issuer = "Tether Operations Limited",
      contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
      network = BlockchainNetwork.TRON_TRC20,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.APPROVED,
      riskStatus = StablecoinRiskStatus.MEDIUM,
      lastReviewDate = "2026-11-20",
      verificationSource = "TronScan Official Contract & Tether Transparency Page",
      minAmount = 5.0,
      maxAmount = 100000.0,
      estimatedNetworkFeeUsd = 1.20,
      hasFreezeAuthority = true,
      riskDisclosureAr = "تخضع USDT لمخاطر جهة الإصدار (Tether) وتجميد العناوين وتكوين الاحتياطيات وفك الارتباط ولا تُعد نقداً مضموناً.",
      riskDisclosureEn = "USDT carries issuer reserve composition, blacklist/freeze, redemption, and depeg risks.",
      riskDisclosureFr = "USDT présente des risques liés aux réserves de l'émetteur, au gel d'adresses et au dépeg."
    ),
    StablecoinRegistryEntry(
      id = "USDT_ERC20",
      assetName = "Tether USD (ERC-20)",
      ticker = "USDT",
      referencePeg = "USD",
      issuer = "Tether Operations Limited",
      contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
      network = BlockchainNetwork.ETHEREUM_ERC20,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.APPROVED,
      riskStatus = StablecoinRiskStatus.MEDIUM,
      lastReviewDate = "2026-11-20",
      verificationSource = "Etherscan Verified Contract & Tether Transparency",
      minAmount = 25.0,
      maxAmount = 250000.0,
      estimatedNetworkFeeUsd = 4.20,
      hasFreezeAuthority = true,
      riskDisclosureAr = "تخضع USDT على إيثيريوم لمخاطر الاحتياطيات ورسوم الشبكة والتجميد من جهة الإصدار.",
      riskDisclosureEn = "USDT ERC-20 carries issuer reserve, freeze authority, and gas volatility risks.",
      riskDisclosureFr = "USDT ERC-20 comporte des risques d'émetteur et de frais de réseau."
    ),
    StablecoinRegistryEntry(
      id = "USDT_SOL",
      assetName = "Tether USD (Solana SPL)",
      ticker = "USDT",
      referencePeg = "USD",
      issuer = "Tether Operations Limited",
      contractAddress = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",
      network = BlockchainNetwork.SOLANA,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.APPROVED,
      riskStatus = StablecoinRiskStatus.MEDIUM,
      lastReviewDate = "2026-11-20",
      verificationSource = "Solscan Verified SPL Mint & Tether Registry",
      minAmount = 2.0,
      maxAmount = 50000.0,
      estimatedNetworkFeeUsd = 0.01,
      hasFreezeAuthority = true,
      riskDisclosureAr = "USDT-SPL تخضع لمخاطر جهة الإصدار وتوفر السيولة على شبكة سولانا ولا تُعد خالية من المخاطر.",
      riskDisclosureEn = "USDT SPL carries issuer, bridge/liquidity, and network availability risks.",
      riskDisclosureFr = "USDT SPL présente des risques d'émetteur et de liquidité réseau."
    ),
    StablecoinRegistryEntry(
      id = "PYUSD_ERC20",
      assetName = "PayPal USD (ERC-20)",
      ticker = "PYUSD",
      referencePeg = "USD",
      issuer = "Paxos Trust Company, LLC",
      contractAddress = "0x6c3ea9036406852006290770BEdFcAbA0e23A0e8",
      network = BlockchainNetwork.ETHEREUM_ERC20,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.UNDER_REVIEW,
      riskStatus = StablecoinRiskStatus.LOW,
      lastReviewDate = "2026-11-18",
      verificationSource = "Paxos Monthly Attestation & NYDFS Registry",
      minAmount = 20.0,
      maxAmount = 50000.0,
      estimatedNetworkFeeUsd = 3.50,
      hasFreezeAuthority = true,
      riskDisclosureAr = "تخضع PYUSD لإشراف جهة الإصدار (Paxos) ومخاطر التنظيم والسيولة ولا تُعد مضمونة المخاطر.",
      riskDisclosureEn = "PYUSD is under review; carries issuer, regulatory, and smart contract risks.",
      riskDisclosureFr = "PYUSD est en cours d'examen et comporte des risques d'émetteur et de liquidité."
    ),
    StablecoinRegistryEntry(
      id = "EURC_BASE",
      assetName = "Euro Coin (Base L2)",
      ticker = "EURC",
      referencePeg = "EUR",
      issuer = "Circle Internet Financial Europe SAS",
      contractAddress = "0x60a3E35Cc302bFA44Cb288Bc5a4F316Fdb1adb42",
      network = BlockchainNetwork.BASE_L2,
      decimals = 6,
      approvalStatus = StablecoinApprovalStatus.APPROVED,
      riskStatus = StablecoinRiskStatus.LOW,
      lastReviewDate = "2026-11-20",
      verificationSource = "Circle MiCA Compliance Disclosure & BaseScan",
      minAmount = 5.0,
      maxAmount = 100000.0,
      estimatedNetworkFeeUsd = 0.01,
      hasFreezeAuthority = true,
      riskDisclosureAr = "تخضع EURC لمخاطر تقلب اليورو وجهة الإصدار واحتياطيات اليورو ولا تُعد خالية من المخاطر.",
      riskDisclosureEn = "EURC carries Euro FX peg, issuer reserve, and redemption risks.",
      riskDisclosureFr = "EURC comporte des risques liés à l'émetteur, aux réserves en euros et au dépeg."
    )
  )

  private val FIAT_SETTLEMENT_RAILS: List<UnifiedAsset> = listOf(
    UnifiedAsset(
      id = "USD_FIAT",
      ticker = "USD",
      nameAr = "تسوية دولارية مرجعية (USD)",
      nameEn = "USD Settlement Reference",
      type = AssetType.FIAT_SETTLEMENT_RAIL,
      network = BlockchainNetwork.FIAT_BANKING,
      decimals = 2,
      minAmount = 5.0,
      maxAmount = 50000.0,
      flatServiceFeeUsd = 0.80,
      estimatedNetworkFeeUsd = 0.0
    ),
    UnifiedAsset(
      id = "EUR_FIAT",
      ticker = "EUR",
      nameAr = "تسوية باليورو المرجعي (EUR)",
      nameEn = "EUR Settlement Reference",
      type = AssetType.FIAT_SETTLEMENT_RAIL,
      network = BlockchainNetwork.FIAT_BANKING,
      decimals = 2,
      minAmount = 5.0,
      maxAmount = 50000.0,
      flatServiceFeeUsd = 0.80,
      estimatedNetworkFeeUsd = 0.0
    ),
    UnifiedAsset(
      id = "SAR_FIAT",
      ticker = "SAR",
      nameAr = "تسوية بالريال السعودي (SAR)",
      nameEn = "SAR Settlement Reference",
      type = AssetType.FIAT_SETTLEMENT_RAIL,
      network = BlockchainNetwork.FIAT_BANKING,
      decimals = 2,
      minAmount = 10.0,
      maxAmount = 100000.0,
      flatServiceFeeUsd = 0.80,
      estimatedNetworkFeeUsd = 0.0
    ),
    UnifiedAsset(
      id = "AED_FIAT",
      ticker = "AED",
      nameAr = "تسوية بالدرهم الإماراتي (AED)",
      nameEn = "AED Settlement Reference",
      type = AssetType.FIAT_SETTLEMENT_RAIL,
      network = BlockchainNetwork.FIAT_BANKING,
      decimals = 2,
      minAmount = 10.0,
      maxAmount = 100000.0,
      flatServiceFeeUsd = 0.80,
      estimatedNetworkFeeUsd = 0.0
    ),
    UnifiedAsset(
      id = "DZD_FIAT",
      ticker = "DZD",
      nameAr = "تسوية بالدينار الجزائري (DZD)",
      nameEn = "DZD Settlement Reference",
      type = AssetType.FIAT_SETTLEMENT_RAIL,
      network = BlockchainNetwork.FIAT_BANKING,
      decimals = 2,
      minAmount = 500.0,
      maxAmount = 2000000.0,
      flatServiceFeeUsd = 0.80,
      estimatedNetworkFeeUsd = 0.0
    )
  )

  val ALL_ASSETS: List<UnifiedAsset> = APPROVED_STABLECOIN_REGISTRY.map { entry ->
    UnifiedAsset(
      id = entry.id,
      ticker = entry.ticker,
      nameAr = entry.assetName,
      nameEn = entry.assetName,
      type = AssetType.APPROVED_STABLECOIN,
      network = entry.network,
      decimals = entry.decimals,
      minAmount = entry.minAmount,
      maxAmount = entry.maxAmount,
      flatServiceFeeUsd = entry.flatServiceFeeUsd,
      estimatedNetworkFeeUsd = entry.estimatedNetworkFeeUsd,
      isEnabled = entry.approvalStatus == StablecoinApprovalStatus.APPROVED &&
        entry.riskStatus != StablecoinRiskStatus.CRITICAL,
      contractAddress = entry.contractAddress,
      issuer = entry.issuer,
      approvalStatus = entry.approvalStatus,
      riskStatus = entry.riskStatus
    )
  } + FIAT_SETTLEMENT_RAILS

  fun findRegistryEntryById(id: String): StablecoinRegistryEntry? {
    return APPROVED_STABLECOIN_REGISTRY.find { it.id.equals(id, ignoreCase = true) }
  }

  fun findRegistryByContract(contractAddress: String, network: BlockchainNetwork): StablecoinRegistryEntry? {
    val clean = contractAddress.trim()
    return APPROVED_STABLECOIN_REGISTRY.find {
      it.network == network && it.contractAddress.equals(clean, ignoreCase = true)
    }
  }

  fun findAssetById(id: String): UnifiedAsset? {
    return ALL_ASSETS.find { it.id.equals(id, ignoreCase = true) }
  }

  fun findAsset(ticker: String, network: BlockchainNetwork): UnifiedAsset? {
    return ALL_ASSETS.find {
      it.ticker.equals(ticker, ignoreCase = true) && it.network == network
    }
  }

  /**
   * Deterministic Financial Policy Guard:
   * Strictly enforces Stablecoins-Only accreditation and blocks BTC / ETH / SOL / BNB / Meme coins
   * or unverified contracts masquerading under a trusted ticker name.
   */
  fun validateStablecoinEligibility(
    ticker: String,
    network: BlockchainNetwork,
    contractAddress: String? = null
  ): Result<StablecoinRegistryEntry> {
    val cleanTicker = ticker.trim().uppercase()
    if (cleanTicker in BLOCKED_VOLATILE_AND_MEME_TICKERS) {
      return Result.failure(
        IllegalArgumentException(
          "Policy Violation: $cleanTicker is prohibited. SOCIAL PAY strictly supports approved stablecoins only."
        )
      )
    }

    val entry = if (!contractAddress.isNullOrBlank()) {
      findRegistryByContract(contractAddress, network)
        ?: return Result.failure(
          SecurityException(
            "Contract spoofing alert: Address '$contractAddress' on ${network.code} is not an accredited stablecoin contract."
          )
        )
    } else {
      APPROVED_STABLECOIN_REGISTRY.find {
        it.ticker.equals(cleanTicker, ignoreCase = true) && it.network == network
      } ?: return Result.failure(
        IllegalArgumentException(
          "Asset '$cleanTicker' on ${network.code} is not listed in the Approved Stablecoin Registry."
        )
      )
    }

    if (!entry.ticker.equals(cleanTicker, ignoreCase = true)) {
      return Result.failure(
        SecurityException("Ticker/contract mismatch: Contract belongs to ${entry.ticker}, not $cleanTicker.")
      )
    }

    if (entry.approvalStatus != StablecoinApprovalStatus.APPROVED) {
      return Result.failure(
        IllegalStateException(
          "Stablecoin ${entry.assetName} is currently ${entry.approvalStatus.labelEn} and cannot process transfers."
        )
      )
    }

    if (entry.riskStatus == StablecoinRiskStatus.CRITICAL) {
      return Result.failure(
        IllegalStateException(
          "Stablecoin ${entry.assetName} is halted due to CRITICAL risk intelligence alert."
        )
      )
    }

    return Result.success(entry)
  }

  /**
   * Validates non-custodial destination address according to chain specifications
   */
  fun validateAddressForNetwork(address: String, network: BlockchainNetwork): Pair<Boolean, String?> {
    val clean = address.trim()
    if (clean.isBlank()) return Pair(false, "Destination address cannot be empty")

    return when (network) {
      BlockchainNetwork.TRON_TRC20 -> {
        if (clean.startsWith("T") && clean.length == 34) {
          Pair(true, null)
        } else {
          Pair(false, "TRC-20 address must start with 'T' and be 34 characters")
        }
      }
      BlockchainNetwork.ETHEREUM_ERC20,
      BlockchainNetwork.POLYGON_POS,
      BlockchainNetwork.ARBITRUM_ONE,
      BlockchainNetwork.BASE_L2,
      BlockchainNetwork.BSC_BEP20 -> {
        val hexBody = clean.removePrefix("0x")
        val isHex = hexBody.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        if (clean.startsWith("0x") && clean.length == 42 && isHex) {
          Pair(true, null)
        } else {
          Pair(false, "EVM address on ${network.code} must start with 0x and contain 40 hex characters")
        }
      }
      BlockchainNetwork.SOLANA -> {
        if (clean.length in 32..44 && !clean.contains("0") && !clean.contains("O") && !clean.contains("I") && !clean.contains("l")) {
          Pair(true, null)
        } else {
          Pair(false, "Invalid Solana Base58 address")
        }
      }
      BlockchainNetwork.FIAT_BANKING -> {
        if (clean.length >= 12) Pair(true, null) else Pair(false, "Invalid IBAN or settlement bank reference")
      }
    }
  }

  /**
   * Calculates transparent total fees using declared owner fee settings (zero hidden fees).
   */
  fun calculateTotalFees(asset: UnifiedAsset, declaredPlatformFeeUsd: Double = 0.80): Double {
    return declaredPlatformFeeUsd.coerceAtLeast(0.0) + asset.estimatedNetworkFeeUsd
  }
}
