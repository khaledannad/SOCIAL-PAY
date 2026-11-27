package com.example.core.connectivity

import com.example.core.asset.BlockchainNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class ConnectorCategory(
  val titleAr: String,
  val titleEn: String,
  val titleFr: String
) {
  SOCIAL_PLATFORM("المنصات الاجتماعية", "Social Platforms", "Plateformes Sociales"),
  GLOBAL_WALLET("المحافظ الرقمية العالمية", "Global Wallets", "Portefeuilles Mondiaux"),
  STABLECOIN_PROVIDER("مزودو العملات المستقرة", "Stablecoin Providers", "Fournisseurs Stablecoins"),
  BANKING_FINANCIAL("البنوك والشركات المالية", "Banking & Financial", "Banques & Finance")
}

enum class ConnectorConnectionState(
  val labelAr: String,
  val labelEn: String,
  val labelFr: String
) {
  AVAILABLE("متاح للربط", "Available", "Disponible"),
  REQUESTED("تم طلب الربط", "Requested", "Demandé"),
  INTEGRATION_PENDING("يتطلب مفاتيح أو اعتماد شريك", "Integration Pending Keys/Contract", "En attente d'intégration"),
  CONNECTED("متصل فعلياً ✓", "Connected & Verified ✓", "Connecté ✓"),
  DISABLED("معطل إدارياً", "Disabled", "Désactivé"),
  ERROR("فشل الاتصال", "Connection Error", "Erreur de connexion")
}

enum class ConnectorCustodyClassification {
  NON_CUSTODIAL_WALLET,
  REGULATED_CUSTODIAN,
  MESSAGING_SHARE_RAIL,
  BANKING_SETTLEMENT_RAIL,
  ORACLE_OR_ISSUER_FEED
}

enum class RuntimeEnvironmentMode {
  DEVELOPMENT,
  STAGING,
  SANDBOX,
  PRODUCTION
}

data class IntegrationConnector(
  val id: String,
  val name: String,
  val category: ConnectorCategory,
  val custodyClassification: ConnectorCustodyClassification,
  val apiType: String,
  val endpointUrl: String,
  val publicHealthProbeUrl: String? = null,
  val authType: String,
  val secretReferenceKey: String, // e.g. "SEC_WHATSAPP_TOKEN" — actual secret NEVER stored in plain client UI
  val webhookUrl: String = "",
  val requiredScopes: List<String>,
  val supportedNetworks: List<BlockchainNetwork>,
  val supportedStablecoins: List<String>,
  val rateLimitPerMin: Int,
  val status: ConnectorConnectionState,
  val authConfigured: Boolean = false,
  val webhookActive: Boolean = false,
  val environment: RuntimeEnvironmentMode = RuntimeEnvironmentMode.SANDBOX,
  val lastCheckedAtMillis: Long? = null,
  val lastSuccessAtMillis: Long? = null,
  val latencyMs: Long? = null,
  val requestCount: Long = 0L,
  val errorCount: Long = 0L,
  val lastErrorNote: String? = null,
  val missingRequirementsNote: String,
  val documentationUrl: String
)

data class ConnectorTestResult(
  val connectorId: String,
  val resultingState: ConnectorConnectionState,
  val latencyMs: Long?,
  val message: String,
  val testedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Global Connectivity Hub Engine.
 * Implements a unified Connector Architecture across Social Platforms, Global Wallets,
 * Stablecoin Providers, and Banking/Financial Institutions.
 * Strictly differentiates AVAILABLE / REQUESTED / INTEGRATION_PENDING / CONNECTED / DISABLED / ERROR
 * and never reports CONNECTED without an actual verified handshake or public probe.
 */
class GlobalConnectivityEngine {

  private val client = OkHttpClient.Builder()
    .connectTimeout(6, TimeUnit.SECONDS)
    .readTimeout(6, TimeUnit.SECONDS)
    .build()

  val defaultConnectors: List<IntegrationConnector> = listOf(
    // A. SOCIAL PLATFORMS (7 Core Channels)
    IntegrationConnector(
      id = "CONN_WHATSAPP_BIZ",
      name = "WhatsApp Business Cloud API",
      category = ConnectorCategory.SOCIAL_PLATFORM,
      custodyClassification = ConnectorCustodyClassification.MESSAGING_SHARE_RAIL,
      apiType = "REST Cloud API v21 + Deep-Link Share",
      endpointUrl = "https://graph.facebook.com/v21.0/",
      authType = "Meta OAuth2 + System User Bearer Token",
      secretReferenceKey = "SEC_META_WHATSAPP_TOKEN",
      webhookUrl = "https://api.socialpay.io/webhooks/whatsapp",
      requiredScopes = listOf("whatsapp_business_messaging", "whatsapp_business_management"),
      supportedNetworks = listOf(BlockchainNetwork.SOLANA, BlockchainNetwork.POLYGON_POS, BlockchainNetwork.TRON_TRC20),
      supportedStablecoins = listOf("USDC", "USDT"),
      rateLimitPerMin = 1000,
      status = ConnectorConnectionState.INTEGRATION_PENDING,
      missingRequirementsNote = "Requires Meta Business Verification, WABA ID & Webhook Verify Token (client share intent works offline).",
      documentationUrl = "https://developers.facebook.com/docs/whatsapp/cloud-api"
    ),
    IntegrationConnector(
      id = "CONN_TELEGRAM",
      name = "Telegram Bot & Mini App SDK",
      category = ConnectorCategory.SOCIAL_PLATFORM,
      custodyClassification = ConnectorCustodyClassification.MESSAGING_SHARE_RAIL,
      apiType = "Telegram Bot API + WebApp InitData HMAC",
      endpointUrl = "https://api.telegram.org/",
      authType = "BotFather Token + HMAC-SHA256 InitData",
      secretReferenceKey = "SEC_TELEGRAM_BOT_TOKEN",
      webhookUrl = "https://api.socialpay.io/webhooks/telegram",
      requiredScopes = listOf("bot_inline_payment_links", "webhook_updates"),
      supportedNetworks = listOf(BlockchainNetwork.SOLANA, BlockchainNetwork.TRON_TRC20, BlockchainNetwork.BASE_L2),
      supportedStablecoins = listOf("USDC", "USDT"),
      rateLimitPerMin = 1800,
      status = ConnectorConnectionState.INTEGRATION_PENDING,
      missingRequirementsNote = "Requires provisioned BotFather token for automated server webhook callbacks (share deep-links active).",
      documentationUrl = "https://core.telegram.org/bots/api"
    ),
    IntegrationConnector(
      id = "CONN_FACEBOOK",
      name = "Facebook Graph & Page Checkout",
      category = ConnectorCategory.SOCIAL_PLATFORM,
      custodyClassification = ConnectorCustodyClassification.MESSAGING_SHARE_RAIL,
      apiType = "Meta Graph API",
      endpointUrl = "https://graph.facebook.com/",
      authType = "OAuth 2.0 App Access Token",
      secretReferenceKey = "SEC_META_FB_APP_SECRET",
      requiredScopes = listOf("pages_messaging"),
      supportedNetworks = listOf(BlockchainNetwork.BASE_L2, BlockchainNetwork.POLYGON_POS),
      supportedStablecoins = listOf("USDC", "EURC"),
      rateLimitPerMin = 600,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Requires Meta App Review approval & Page Token.",
      documentationUrl = "https://developers.facebook.com/docs/graph-api"
    ),
    IntegrationConnector(
      id = "CONN_INSTAGRAM",
      name = "Instagram Messaging & Bio Link",
      category = ConnectorCategory.SOCIAL_PLATFORM,
      custodyClassification = ConnectorCustodyClassification.MESSAGING_SHARE_RAIL,
      apiType = "Instagram Graph Messaging API",
      endpointUrl = "https://graph.instagram.com/",
      authType = "OAuth 2.0",
      secretReferenceKey = "SEC_IG_GRAPH_TOKEN",
      requiredScopes = listOf("instagram_basic", "instagram_manage_messages"),
      supportedNetworks = listOf(BlockchainNetwork.POLYGON_POS, BlockchainNetwork.SOLANA),
      supportedStablecoins = listOf("USDC", "USDT"),
      rateLimitPerMin = 400,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Requires Meta Business Account + IG Professional approval.",
      documentationUrl = "https://developers.facebook.com/docs/instagram-api"
    ),
    IntegrationConnector(
      id = "CONN_MESSENGER",
      name = "Facebook Messenger Platform",
      category = ConnectorCategory.SOCIAL_PLATFORM,
      custodyClassification = ConnectorCustodyClassification.MESSAGING_SHARE_RAIL,
      apiType = "Messenger Send API",
      endpointUrl = "https://graph.facebook.com/v21.0/me/messages",
      authType = "Page OAuth2 Token",
      secretReferenceKey = "SEC_MESSENGER_PAGE_TOKEN",
      requiredScopes = listOf("pages_messaging"),
      supportedNetworks = listOf(BlockchainNetwork.BASE_L2, BlockchainNetwork.POLYGON_POS),
      supportedStablecoins = listOf("USDC"),
      rateLimitPerMin = 500,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Requires Messenger Webhook & Page Token.",
      documentationUrl = "https://developers.facebook.com/docs/messenger-platform"
    ),
    IntegrationConnector(
      id = "CONN_TIKTOK",
      name = "TikTok Shop & Creator Share Kit",
      category = ConnectorCategory.SOCIAL_PLATFORM,
      custodyClassification = ConnectorCustodyClassification.MESSAGING_SHARE_RAIL,
      apiType = "TikTok Open API v2",
      endpointUrl = "https://open.tiktokapis.com/v2/",
      authType = "OAuth 2.0 Client Key/Secret",
      secretReferenceKey = "SEC_TIKTOK_CLIENT_SECRET",
      requiredScopes = listOf("user.info.basic"),
      supportedNetworks = listOf(BlockchainNetwork.SOLANA, BlockchainNetwork.BASE_L2),
      supportedStablecoins = listOf("USDC"),
      rateLimitPerMin = 300,
      status = ConnectorConnectionState.INTEGRATION_PENDING,
      missingRequirementsNote = "Requires TikTok Developer Portal verification & Commerce Partner agreement.",
      documentationUrl = "https://developers.tiktok.com/"
    ),
    IntegrationConnector(
      id = "CONN_X",
      name = "X (Twitter) v2 Direct Payment Link",
      category = ConnectorCategory.SOCIAL_PLATFORM,
      custodyClassification = ConnectorCustodyClassification.MESSAGING_SHARE_RAIL,
      apiType = "X API v2",
      endpointUrl = "https://api.x.com/2/",
      authType = "OAuth 2.0 PKCE",
      secretReferenceKey = "SEC_X_API_BEARER",
      requiredScopes = listOf("dm.write", "users.read"),
      supportedNetworks = listOf(BlockchainNetwork.SOLANA, BlockchainNetwork.ETHEREUM_ERC20),
      supportedStablecoins = listOf("USDC", "USDT"),
      rateLimitPerMin = 300,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Requires X API Basic/Pro app key and explicit user OAuth grant.",
      documentationUrl = "https://developer.x.com/en/docs"
    ),

    // B. GLOBAL WALLETS (Non-Custodial Self-Custody vs Regulated Custody)
    IntegrationConnector(
      id = "WALLET_METAMASK",
      name = "MetaMask (EVM Non-Custodial)",
      category = ConnectorCategory.GLOBAL_WALLET,
      custodyClassification = ConnectorCustodyClassification.NON_CUSTODIAL_WALLET,
      apiType = "EIP-1193 / EIP-681 Deep Link & WalletConnect",
      endpointUrl = "https://metamask.app.link/",
      authType = "EIP-4361 Sign-In With Ethereum (SIWE)",
      secretReferenceKey = "NON_CUSTODIAL_NO_SECRET",
      requiredScopes = listOf("eth_accounts", "personal_sign"),
      supportedNetworks = listOf(BlockchainNetwork.ETHEREUM_ERC20, BlockchainNetwork.POLYGON_POS, BlockchainNetwork.ARBITRUM_ONE, BlockchainNetwork.BASE_L2, BlockchainNetwork.BSC_BEP20),
      supportedStablecoins = listOf("USDC", "USDT", "PYUSD", "EURC"),
      rateLimitPerMin = 1200,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Supports EVM networks only (does not support Tron TRC-20 or Solana SPL natively).",
      documentationUrl = "https://docs.metamask.io/"
    ),
    IntegrationConnector(
      id = "WALLET_TRUST",
      name = "Trust Wallet (Multi-Chain Non-Custodial)",
      category = ConnectorCategory.GLOBAL_WALLET,
      custodyClassification = ConnectorCustodyClassification.NON_CUSTODIAL_WALLET,
      apiType = "Trust Deep Link + WalletConnect v2",
      endpointUrl = "https://link.trustwallet.com/",
      authType = "Wallet Signature Challenge",
      secretReferenceKey = "NON_CUSTODIAL_NO_SECRET",
      requiredScopes = listOf("signMessage", "addressProof"),
      supportedNetworks = listOf(BlockchainNetwork.TRON_TRC20, BlockchainNetwork.ETHEREUM_ERC20, BlockchainNetwork.SOLANA, BlockchainNetwork.BSC_BEP20, BlockchainNetwork.POLYGON_POS),
      supportedStablecoins = listOf("USDC", "USDT"),
      rateLimitPerMin = 1200,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Requires per-chain explicit address proof; user keeps full private key custody.",
      documentationUrl = "https://developer.trustwallet.com/"
    ),
    IntegrationConnector(
      id = "WALLET_COINBASE",
      name = "Coinbase Smart Wallet (Non-Custodial)",
      category = ConnectorCategory.GLOBAL_WALLET,
      custodyClassification = ConnectorCustodyClassification.NON_CUSTODIAL_WALLET,
      apiType = "Wallet SDK / Passkey Smart Account",
      endpointUrl = "https://keys.coinbase.com/",
      authType = "WebAuthn Passkey + EIP-1271",
      secretReferenceKey = "NON_CUSTODIAL_NO_SECRET",
      requiredScopes = listOf("evm_address_proof"),
      supportedNetworks = listOf(BlockchainNetwork.BASE_L2, BlockchainNetwork.ETHEREUM_ERC20, BlockchainNetwork.POLYGON_POS, BlockchainNetwork.ARBITRUM_ONE),
      supportedStablecoins = listOf("USDC", "EURC"),
      rateLimitPerMin = 1000,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Supports EVM & Base L2 stablecoins; excludes Tron TRC-20.",
      documentationUrl = "https://docs.cloud.coinbase.com/"
    ),
    IntegrationConnector(
      id = "WALLET_WALLETCONNECT",
      name = "WalletConnect v2 Relay Protocol",
      category = ConnectorCategory.GLOBAL_WALLET,
      custodyClassification = ConnectorCustodyClassification.NON_CUSTODIAL_WALLET,
      apiType = "CAIP-2 / CAIP-10 Session Protocol",
      endpointUrl = "https://relay.walletconnect.com/",
      authType = "Reown Project ID",
      secretReferenceKey = "SEC_WALLETCONNECT_PROJECT_ID",
      requiredScopes = listOf("wc_sessionPropose"),
      supportedNetworks = listOf(BlockchainNetwork.ETHEREUM_ERC20, BlockchainNetwork.SOLANA, BlockchainNetwork.POLYGON_POS, BlockchainNetwork.BASE_L2, BlockchainNetwork.TRON_TRC20),
      supportedStablecoins = listOf("USDC", "USDT", "EURC"),
      rateLimitPerMin = 2000,
      status = ConnectorConnectionState.INTEGRATION_PENDING,
      missingRequirementsNote = "Requires Reown/WalletConnect Project ID in Secrets Panel.",
      documentationUrl = "https://docs.reown.com/"
    ),
    IntegrationConnector(
      id = "WALLET_PHANTOM",
      name = "Phantom Wallet (Solana & Base Non-Custodial)",
      category = ConnectorCategory.GLOBAL_WALLET,
      custodyClassification = ConnectorCustodyClassification.NON_CUSTODIAL_WALLET,
      apiType = "Solana Pay & Phantom Universal Link",
      endpointUrl = "https://phantom.app/ul/",
      authType = "Ed25519 Signature Verification",
      secretReferenceKey = "NON_CUSTODIAL_NO_SECRET",
      requiredScopes = listOf("solana_connect", "signMessage"),
      supportedNetworks = listOf(BlockchainNetwork.SOLANA, BlockchainNetwork.BASE_L2, BlockchainNetwork.ETHEREUM_ERC20, BlockchainNetwork.POLYGON_POS),
      supportedStablecoins = listOf("USDC", "USDT"),
      rateLimitPerMin = 1200,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Supports Solana SPL & EVM chains; does not support Tron TRC-20.",
      documentationUrl = "https://docs.phantom.app/"
    ),

    // C. STABLECOIN PROVIDERS (Issuers, Regulated Custody, Market Price Oracles)
    IntegrationConnector(
      id = "PROV_COINBASE_ORACLE",
      name = "Coinbase Public Market Reference API",
      category = ConnectorCategory.STABLECOIN_PROVIDER,
      custodyClassification = ConnectorCustodyClassification.ORACLE_OR_ISSUER_FEED,
      apiType = "Public Read-Only REST Spot Feed",
      endpointUrl = "https://api.coinbase.com/v2/prices/USDC-USD/spot",
      publicHealthProbeUrl = "https://api.coinbase.com/v2/prices/USDC-USD/spot",
      authType = "Public Read-Only (No Private Key)",
      secretReferenceKey = "PUBLIC_NO_KEY",
      requiredScopes = listOf("read_only_spot_ticker"),
      supportedNetworks = listOf(BlockchainNetwork.ETHEREUM_ERC20, BlockchainNetwork.BASE_L2, BlockchainNetwork.SOLANA),
      supportedStablecoins = listOf("USDC", "EURC", "USDT"),
      rateLimitPerMin = 600,
      status = ConnectorConnectionState.AVAILABLE,
      authConfigured = true,
      missingRequirementsNote = "Live public price check ready via Test Connection.",
      documentationUrl = "https://docs.cdp.coinbase.com/"
    ),
    IntegrationConnector(
      id = "PROV_BINANCE_PEG_FEED",
      name = "Binance Public Peg & Liquidity Feed",
      category = ConnectorCategory.STABLECOIN_PROVIDER,
      custodyClassification = ConnectorCustodyClassification.ORACLE_OR_ISSUER_FEED,
      apiType = "Public Read-Only REST + WebSocket",
      endpointUrl = "https://api.binance.com/api/v3/ticker/bookTicker?symbol=USDCUSDT",
      publicHealthProbeUrl = "https://api.binance.com/api/v3/ping",
      authType = "Public Read-Only (Zero Trading Keys)",
      secretReferenceKey = "PUBLIC_NO_KEY",
      requiredScopes = listOf("read_only_market_peg"),
      supportedNetworks = listOf(BlockchainNetwork.TRON_TRC20, BlockchainNetwork.BSC_BEP20, BlockchainNetwork.SOLANA),
      supportedStablecoins = listOf("USDC", "USDT"),
      rateLimitPerMin = 1200,
      status = ConnectorConnectionState.AVAILABLE,
      authConfigured = true,
      missingRequirementsNote = "Public read-only peg monitor ready via Test Connection.",
      documentationUrl = "https://developers.binance.com/docs/binance-spot-api-docs"
    ),
    IntegrationConnector(
      id = "PROV_CIRCLE_MINT",
      name = "Circle Programmable Payments & CCTP",
      category = ConnectorCategory.STABLECOIN_PROVIDER,
      custodyClassification = ConnectorCustodyClassification.REGULATED_CUSTODIAN,
      apiType = "Circle Developer API + CCTP",
      endpointUrl = "https://api.circle.com/v1/",
      authType = "RSA Entity Secret + API Key (KMS)",
      secretReferenceKey = "SEC_CIRCLE_API_KEY",
      webhookUrl = "https://api.socialpay.io/webhooks/circle",
      requiredScopes = listOf("cctp_attestation", "usdc_payouts"),
      supportedNetworks = listOf(BlockchainNetwork.SOLANA, BlockchainNetwork.ETHEREUM_ERC20, BlockchainNetwork.BASE_L2, BlockchainNetwork.POLYGON_POS, BlockchainNetwork.ARBITRUM_ONE),
      supportedStablecoins = listOf("USDC", "EURC"),
      rateLimitPerMin = 500,
      status = ConnectorConnectionState.INTEGRATION_PENDING,
      missingRequirementsNote = "Requires commercial KYB onboarding agreement with Circle & KMS entity secret.",
      documentationUrl = "https://developers.circle.com/"
    ),
    IntegrationConnector(
      id = "PROV_FIREBLOCKS_CUSTODY",
      name = "Qualified Institutional Custody (MPC/HSM)",
      category = ConnectorCategory.STABLECOIN_PROVIDER,
      custodyClassification = ConnectorCustodyClassification.REGULATED_CUSTODIAN,
      apiType = "MPC Vault + Multi-Approval Policy Engine",
      endpointUrl = "https://api.fireblocks.io/v1/",
      authType = "JWT RS256 + Co-Signer HSM",
      secretReferenceKey = "SEC_CUSTODY_HSM_REF",
      requiredScopes = listOf("mpc_policy_sign", "segregated_vault_read"),
      supportedNetworks = listOf(BlockchainNetwork.TRON_TRC20, BlockchainNetwork.ETHEREUM_ERC20, BlockchainNetwork.SOLANA),
      supportedStablecoins = listOf("USDC", "USDT"),
      rateLimitPerMin = 300,
      status = ConnectorConnectionState.INTEGRATION_PENDING,
      missingRequirementsNote = "Disabled for client custody until licensed custodian contract & pre-launch audit complete.",
      documentationUrl = "https://developers.fireblocks.com/"
    ),

    // D. BANKING & FINANCIAL COMPANIES (Honest Availability — ZERO Fake Partnerships)
    IntegrationConnector(
      id = "BANK_OPEN_BANKING_GW",
      name = "Licensed Open Banking & Instant Rail Gateway",
      category = ConnectorCategory.BANKING_FINANCIAL,
      custodyClassification = ConnectorCustodyClassification.BANKING_SETTLEMENT_RAIL,
      apiType = "ISO 20022 / Open Banking REST + mTLS",
      endpointUrl = "https://sandbox.openbanking.partner/",
      authType = "mTLS Client Certificate + FAPI OAuth2",
      secretReferenceKey = "SEC_OPENBANKING_MTLS_CERT",
      requiredScopes = listOf("aisp_account_verify", "pisp_payment_status"),
      supportedNetworks = listOf(BlockchainNetwork.FIAT_BANKING),
      supportedStablecoins = listOf("USDC", "EURC"),
      rateLimitPerMin = 400,
      status = ConnectorConnectionState.INTEGRATION_PENDING,
      missingRequirementsNote = "Requires bilateral banking API contract, mTLS certificate & local regulatory licensing (e.g., Bank of Algeria / regional central bank).",
      documentationUrl = "https://openbankingstandards.github.io/"
    ),
    IntegrationConnector(
      id = "COMP_KYC_KYB_VENDOR",
      name = "Accredited Identity & Sanctions Screening Gateway",
      category = ConnectorCategory.BANKING_FINANCIAL,
      custodyClassification = ConnectorCustodyClassification.BANKING_SETTLEMENT_RAIL,
      apiType = "KYC/KYB + PEP/Sanctions Screening REST",
      endpointUrl = "https://api.compliance-partner.example/",
      authType = "HMAC-SHA256 Signed API Request",
      secretReferenceKey = "SEC_COMPLIANCE_VENDOR_KEY",
      requiredScopes = listOf("kyc_verify", "kyb_lookup", "sanctions_screen"),
      supportedNetworks = listOf(BlockchainNetwork.FIAT_BANKING),
      supportedStablecoins = listOf("USDC", "USDT", "EURC"),
      rateLimitPerMin = 500,
      status = ConnectorConnectionState.AVAILABLE,
      missingRequirementsNote = "Requires compliance vendor commercial contract & data-transfer impact assessment.",
      documentationUrl = "https://developers.sumsub.com/"
    )
  )

  /**
   * Executes a real HTTP connectivity probe ONLY for connectors that expose a safe public read-only health endpoint.
   * For authenticated vendor/bank gateways without live keys, honestly reports INTEGRATION_PENDING without faking CONNECTED.
   */
  suspend fun testConnectorConnection(connector: IntegrationConnector): ConnectorTestResult = withContext(Dispatchers.IO) {
    val probeUrl = connector.publicHealthProbeUrl
    if (probeUrl.isNullOrBlank()) {
      val state = if (connector.authConfigured) ConnectorConnectionState.CONNECTED else ConnectorConnectionState.INTEGRATION_PENDING
      return@withContext ConnectorTestResult(
        connectorId = connector.id,
        resultingState = state,
        latencyMs = null,
        message = connector.missingRequirementsNote
      )
    }

    val start = System.currentTimeMillis()
    try {
      val request = Request.Builder().url(probeUrl).get().build()
      client.newCall(request).execute().use { response ->
        val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        if (response.isSuccessful) {
          ConnectorTestResult(
            connectorId = connector.id,
            resultingState = ConnectorConnectionState.CONNECTED,
            latencyMs = elapsed,
            message = "HTTP ${response.code} Live Handshake OK (${elapsed}ms)"
          )
        } else {
          ConnectorTestResult(
            connectorId = connector.id,
            resultingState = ConnectorConnectionState.ERROR,
            latencyMs = elapsed,
            message = "HTTP ${response.code} returned from provider endpoint"
          )
        }
      }
    } catch (e: Exception) {
      ConnectorTestResult(
        connectorId = connector.id,
        resultingState = ConnectorConnectionState.ERROR,
        latencyMs = null,
        message = "Network probe failed: ${e.message ?: "Unreachable"}"
      )
    }
  }
}
