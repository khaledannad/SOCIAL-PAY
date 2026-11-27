# SOCIAL PAY — CORE SPECIFICATION & API DOCUMENTATION
**Version:** 1.0.0-CORE-PHASE1  
**Architecture:** Independent Social Transaction Engine (Non-Custodial, Multi-Rail, Zero-Riba)

---

## 1. Executive Summary & Core Principles
Social Pay is an independent, non-custodial social transaction and payment orchestration engine. It enables users, merchants, and creators to issue, share, and settle payment requests, smart escrows, and formal invoices across social channels (WhatsApp, Telegram, Instagram, Facebook, and Web Embeds) without forcing recipients to install a proprietary application.

### Key Architectural Tenets:
1. **Strictly Non-Custodial:** Social Pay never holds user balances, private keys, or deposit funds. All fiat settlements terminate into verified user bank accounts/IBANs or non-custodial wallet addresses (TRON, Ethereum, Solana, Bitcoin).
2. **Fixed Protocol Fee:** Flat **$0.80** protocol fee per transaction (designed to adjust to $0.90 after crossing $1,000,000 cumulative volume).
3. **Explicit Multi-Network Registry:** Digital assets with identical tickers (e.g. USDT) are strictly isolated by their underlying network (`USDT_TRC20`, `USDT_ERC20`, `USDT_SOL`) to completely prevent cross-chain address loss or ticker confusion.
4. **Tamper-Evident Chronological Audit Chain:** Every event is recorded in a cryptographic SHA-256 hash chain linked to the previous transaction hash.
5. **Decoupled Payment Adapters:** Pluggable payment adapter architecture allowing seamless addition and replacement of banks, gateways, and smart contracts without touching the core transaction engine.

---

## 2. System Architecture & The 8 Core Engines

| Engine | Namespace | Functionality |
| :--- | :--- | :--- |
| **Identity Engine** | `com.example.core.identity` | Non-custodial user profiles, KYC Tiers (0 to 3), limits, verified payout endpoints, identity seals. |
| **Digital Asset Engine** | `com.example.core.asset` | Unified fiat and crypto asset registry, network validation (TRC-20, ERC-20, Solana SPL, SegWit), fee estimation. |
| **Transaction Engine** | `com.example.core.transaction` | Payment intents, attempts, 9-state machine transitions, itemized invoices, tamper-proof receipts. |
| **Payment Adapter Engine** | `com.example.core.payment` | Abstract adapter interfaces, Sandbox simulation (8 edge scenarios), HMAC-SHA256 webhooks, refunding. |
| **Social Integration Engine** | `com.example.core.social` | Deep links & intents for WhatsApp/Telegram/Instagram, dynamic QR code payloads, HTML web embed button. |
| **Notification Engine** | `com.example.core.notification` | Event bus for payment creation, escrow locking, confirmation, alerts, and receipt issuance. |
| **Security Engine** | `com.example.core.security` | Anti-phishing seal, anti-triangulation guard (KYC vs remitter name match), off-platform link scanner. |
| **Audit Engine** | `com.example.core.audit` | Append-only cryptographic SHA-256 hash chain verifying complete history integrity. |

---

## 3. Unified Currency & Asset Specification

### Supported Networks
- `TRON_TRC20`: Tron Network (TRC-20, addresses start with `T`, 34 chars)
- `ETHEREUM_ERC20`: Ethereum Mainnet (ERC-20, 0x format, 42 chars)
- `SOLANA`: Solana SPL (Base58 format)
- `BITCOIN_NATIVE`: Bitcoin Mainnet (SegWit / Bech32 / Legacy)
- `FIAT_BANKING`: Instant Local Banking Rails (Saudi Sarie, UAE FTS, SEPA Instant)

### Asset Registry Matrix
| Asset ID | Ticker | Network | Decimals | Min Amount | Max Amount | Protocol Fee |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `SAR_FIAT` | SAR | FIAT_BANKING | 2 | 10.00 | 50,000.00 | $0.80 |
| `AED_FIAT` | AED | FIAT_BANKING | 2 | 10.00 | 50,000.00 | $0.80 |
| `USD_FIAT` | USD | FIAT_BANKING | 2 | 5.00 | 25,000.00 | $0.80 |
| `EUR_FIAT` | EUR | FIAT_BANKING | 2 | 5.00 | 25,000.00 | $0.80 |
| `EGP_FIAT` | EGP | FIAT_BANKING | 2 | 50.00 | 200,000.00 | $0.80 |
| `USDT_TRC20` | USDT | TRON_TRC20 | 6 | 5.00 | 100,000.00 | $0.80 |
| `USDT_ERC20` | USDT | ETHEREUM_ERC20| 6 | 50.00 | 100,000.00 | $0.80 |
| `USDT_SOL` | USDT | SOLANA | 6 | 5.00 | 50,000.00 | $0.80 |
| `BTC_NATIVE` | BTC | BITCOIN_NATIVE| 8 | 0.0005 | 5.00 | $0.80 |
| `ETH_MAINNET`| ETH | ETHEREUM_ERC20| 18 | 0.01 | 50.00 | $0.80 |
| `SOL_NATIVE` | SOL | SOLANA | 9 | 0.10 | 500.00 | $0.80 |

---

## 4. Transaction Lifecycle & State Machine

```
              ┌─────────┐
              │ CREATED │
              └────┬────┘
                   │
         ┌─────────┴─────────┐
         ▼                   ▼
┌─────────────────┐ ┌─────────────────┐
│ PENDING_PAYMENT │ │  ESCROW_LOCKED  │
└────────┬────────┘ └────────┬────────┘
         │                   │
         ├───────────────────┤
         ▼                   ▼
┌──────────────────┐ ┌───────────────┐
│   UNDER_REVIEW   │ │   CONFIRMED   │
└────────┬─────────┘ └───────┬───────┘
         │                   │
         └─────────┬─────────┘
                   ▼
             ┌───────────┐
             │ COMPLETED │  (Terminal)
             └───────────┘

Terminal Alternate States:
- CANCELLED: Safely terminated by user or merchant before irreversible settlement.
- EXPIRED: Intent exceeded TTL (default: 24 to 48 hours).
- REFUNDED: Reversed to remitter via verified audit authorization.
```

---

## 5. REST & Webhook API Endpoints

### 5.1 Create Payment Intent
- **Endpoint:** `POST /api/v1/intents`
- **Headers:**
  - `Content-Type: application/json`
  - `Idempotency-Key: <UUID>`
  - `Authorization: Bearer <API_TOKEN>`
- **Request Body:**
```json
{
  "typePrefix": "SP-REQ",
  "amount": 1875.00,
  "assetId": "SAR_FIAT",
  "senderUserId": "USR-8821",
  "senderName": "خالد العناد",
  "recipientUserId": "USR-9932",
  "recipientName": "عبدالرحمن الشمري",
  "channel": "WHATSAPP",
  "metadata": {
    "dealNote": "P2P Spot Exchange"
  }
}
```
- **Response (201 Created):**
```json
{
  "intentId": "SP-PI-849201",
  "transactionId": "SP-REQ-849201",
  "amount": 1875.00,
  "currencyTicker": "SAR",
  "assetId": "SAR_FIAT",
  "network": "FIAT_BANKING",
  "state": "CREATED",
  "feeUsd": 0.80,
  "paymentLink": "https://pay.socialpay.io/req/SP-REQ-849201",
  "qrCodePayload": "socialpay://req/SP-REQ-849201?amount=1875.0&cur=SAR&asset=SAR_FIAT",
  "expiresAt": 1731175200000
}
```

### 5.2 Provider Webhook Ingestion
- **Endpoint:** `POST /api/v1/webhooks/payment`
- **Headers:**
  - `X-SocialPay-Signature: <HMAC-SHA256(payload, secret)>`
- **Payload:**
```
txId=SP-REQ-849201&amount=1875.00&status=CONFIRMED&ref=REF-SBX-99881122
```
- **Signature Algorithm:**
`HMAC-SHA256(raw_body, webhook_signing_secret)` in constant-time comparison to prevent timing side-channel attacks.

---

## 6. Security & Anti-Fraud Algorithms

### 6.1 Anti-Triangulation Verification
```
Score = (RemitterName == KycName) ? 99% (Risk: NONE) : 15% (Risk: CRITICAL)
```
Prevents fraudulent third-party wire transfers from victim accounts.

### 6.2 Off-Platform Phishing Filter
Analyzes deal chat strings for redirection keywords (`"برة المنصة"`, `"خارج التطبيق"`, `"bit.ly"`, `".apk"`). Flags and alerts users instantly.

### 6.3 Verifiable Digital Receipt
Receipt hash is computed as:
```
SHA-256(receiptId + ":" + amount + ":" + currency + ":" + providerReference + ":" + recipientUserId)
```

---

## 7. Web Embed Snippet Specification
Merchants and online stores can accept Social Pay with 1 script tag:
```html
<script src="https://cdn.socialpay.io/v1/button.js" async></script>
<div class="social-pay-smart-button"
     data-tx-id="SP-REQ-849201"
     data-amount="1875.00"
     data-currency="SAR"
     data-network="FIAT"
     data-fee="0.80"
     data-lang="ar">
  <a href="https://pay.socialpay.io/req/SP-REQ-849201" target="_blank"
     style="display:inline-flex;align-items:center;background:#00E676;color:#121212;padding:12px 24px;border-radius:12px;font-weight:bold;text-decoration:none;">
    <span>ادفع بأمان عبر Social Pay (1875.00 SAR)</span>
  </a>
</div>
```

---

## 8. SOCIAL PAY GENIUS BRAIN — Master AI Engine Specification

### 8.1 AI Core & Model Provider Adapter
The AI subsystem is decoupled from any single model via `ModelProviderAdapter`:
- **Primary Provider:** `FirebaseAiModelAdapter` routing through `Firebase.ai` (`gemini-3.8-flash` by default with fallback to `gemini-3.5-flash-lite`).
- **Local Fallback:** `LocalFallbackModelAdapter` ensuring offline and network-degraded operations never break UI responsiveness.

### 8.2 Multilingual Intelligence
Automatic language and dialect detection via `LanguageDetector`:
- **العربية الفصحى (Modern Standard Arabic):** Formal, grammatically sound financial terminology.
- **الدارجة الجزائرية (Algerian Darija):** Native expressions (واش راك، شحال، دراهم، ليسكرو، برك) with exact preservation of financial integrity.
- **English & French:** Native fluency with institutional fintech terminology.

### 8.3 Multi-Level Memory Architecture
- **Short-Term Context:** Session-isolated sliding window preserving the last 8-10 turns.
- **Long-Term Consented Memory:** Explicit user-authorized preferences (`saveLongTermPreference`), isolated per `userId`.
- **User Memory Controls:** Full user autonomy to inspect, edit, delete specific memories, or execute `wipeAllUserData` (Right to be Forgotten).
- **Sensitive Data Sanitizer:** Invariant filter redacting card numbers, passwords, CVVs, seed phrases, and private keys (`[REDACTED_CARD_NUMBER]`, `[REDACTED_CREDENTIAL]`).

### 8.4 RAG Knowledge Engine & Source Attribution
Ground-truth retrieval over authoritative Social Pay documents:
- Protocol fee schedule ($0.80 fixed fee; $0.90 after $1,000,000 threshold).
- Non-custodial smart escrow and spot delivery (0% riba).
- Anti-triangulation and anti-phishing defense.
- Dynamic indexing of custom user PDFs and invoices (`indexCustomDocument`).
- Zero hallucination guarantee: Strict citation generation with author, ref, and last-updated timestamps.

### 8.5 7 Specialized Service Modules
1. **General Assistant:** Reasoning, analysis, and general education.
2. **Financial Assistant:** Invoices, fees, settlements, and escrow rules.
3. **Commerce Assistant:** Buyers, sellers, and e-commerce embed setups.
4. **Translation Assistant:** Cross-lingual & dialectal translation.
5. **Technical Assistant:** APIs, Webhook HMAC-SHA256, and Web Embed scripts.
6. **Customer Support:** Dispute escalation and support tickets.
7. **Social Pay Core Assistant:** Official platform guides and features.

### 8.6 Action Engine (Function Calling & Confirmation Gates)
Authorized tool calling:
- `create_invoice`: Issues official invoice.
- `create_payment_link`: Issues social payment link.
- `get_transaction_status`: Checks live transaction status.
- `search_transaction`: Searches user history.
- `create_support_ticket`: Opens formal support ticket.
- `get_user_history`: Queries account summary.
**Confirmation Gate:** High-impact tools generate a `PendingActionProposal` requiring explicit user approval before execution. Direct fund transfers or balance changes by the AI model are strictly forbidden.

### 8.7 Real-Time Telemetry & Market Knowledge
Dynamic market snapshot (`RealTimeKnowledgeEngine`):
- Live fiat rates (USD/SAR 3.75, USD/AED 3.67, USD/EGP 49.30).
- Live crypto gas estimates (USDT TRC20 $1.20, Solana $0.02, Ethereum $4.50).
- Explicit timestamps and regulatory source authority attribution.

### 8.8 Trust & Safety Guardrails
- **Prompt Injection Defense:** Blocks jailbreak tokens, system override attempts, and prompt extraction.
- **Output Inspection:** Prohibits unauthorized claims of automated fund transfers.

---

## 9. REAL-WORLD PRODUCTION CONNECTIVITY UPGRADE

### 9.1 Real Market Data Engine & Exchange Adapters
Public, read-only feeds providing live tickers, L2 order books, and OHLCV candles without requiring trading private keys:
- **Binance:** `https://api.binance.com/api/v3/ticker/24hr` (WebSocket: `wss://stream.binance.com:9443/ws`)
- **Coinbase Exchange:** `https://api.exchange.coinbase.com/products/{pair}/ticker` (WebSocket: `wss://ws-feed.exchange.coinbase.com`)
- **Kraken:** `https://api.kraken.com/0/public/Ticker` (WebSocket: `wss://ws.kraken.com`)
- **OKX:** `https://www.okx.com/api/v5/market/ticker` (WebSocket: `wss://ws.okx.com:8443/ws/v5/public`)
- **Bybit:** `https://api.bybit.com/v5/market/tickers?category=spot` (WebSocket: `wss://stream.bybit.com/v5/public/spot`)
- **Global Forex & Commodities:** Interbank rates (USD/SAR 3.7505, USD/AED 3.6725, USD/EGP 49.32, Gold XAU/USD 2,685.40$).

### 9.2 Universal Normalization & Zero-Conflation Model
Every tick is represented using `NormalizedMarketTick`:
```
NormalizedMarketTick(
  symbol, baseAsset, quoteAsset, exchangeName,
  lastPrice, bid, ask, spread, high24h, low24h, volume24h,
  sourceTimestamp, receivedTimestamp, latencyMs, dataStatus, dataQuality
)
```
- Guarantees zero conflation between different quote currencies (e.g. BTC/USDT != BTC/USD).
- Preserves genuine cross-exchange spreads for arbitrage monitoring.

### 9.3 12 Social Network Integration Adapters
| Network | Channel Type | Integration Status | Auth & Protocol |
| :--- | :--- | :--- | :--- |
| **WhatsApp Business** | Messaging | **CONNECTED ✓** | Cloud API Bearer Token & Webhooks |
| **Telegram Bot API** | Developer Bot | **CONNECTED ✓** | Bot Token (BotFather) & Webhooks |
| **Telegram Mini Apps** | WebApp | **CONNECTED ✓** | TMA InitData HMAC-SHA256 Attestation |
| **Facebook Messenger** | Messaging | **CONNECTED ✓** | Page Access Token & Send API |
| **X Platform (Twitter)**| Social Media | **CONNECTED ✓** | OAuth 2.0 PKCE / Share Intent |
| **Discord Bot API** | Developer Bot | **CONNECTED ✓** | Bot Application Token & Interaction Webhooks |
| **LinkedIn Share** | Social Media | **CONNECTED ✓** | OAuth 2.0 `w_member_social` |
| **Email Gateway** | Direct Telecom | **CONNECTED ✓** | SMTP TLS / SendGrid REST API |
| **SMS Gateway** | Direct Telecom | **CONNECTED ✓** | SMPP / Twilio REST API |
| **Instagram Graph** | Messaging | **PENDING_APPROVAL** | Requires Meta Business App Review |
| **TikTok Share** | Social Media | **PENDING_APPROVAL** | Requires TikTok Developer App Approval |
| **Snapchat Kit** | Social Media | **PENDING_APPROVAL** | Requires Snap Kit Production Verification |

*Fallback Sharing Layer:* Universal deep links (`https://pay.socialpay.io/req/{id}`), QR codes, and native Android share sheets guarantee 100% interoperability with all apps.

### 9.4 Real-World Database Entities
- `market_providers`: Live status, ping latency, REST/WebSocket support.
- `market_pairs`: Normalized pair prices, 24h volume, bid/ask spreads.
- `market_candles`: Historical OHLCV candlestick records.
- `social_connections`: Connection state, sync times, active capabilities.
- `integration_health_logs`: Real-time response times and error telemetry.
- `data_quality_logs`: Discrepancy, stale feed, and anomaly logging.
