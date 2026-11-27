package com.example

import com.example.core.transaction.PaymentIntent
import com.example.core.transaction.TransactionState
import com.example.market.adapters.*
import com.example.market.core.DataQuality
import com.example.market.core.DataStatus
import com.example.market.core.MarketDataEngine
import com.example.social.adapters.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealWorldConnectivityTest {

  private lateinit var marketEngine: MarketDataEngine

  @Before
  fun setUp() {
    marketEngine = MarketDataEngine()
  }

  // =========================================================================
  // 1. EXCHANGE ADAPTERS & DATA QUALITY TESTS (Binance, Coinbase, Kraken, OKX, Bybit)
  // =========================================================================
  @Test
  fun testAll5CryptoExchangeAdapters() = runTest {
    // 1. Binance
    val binanceRes = marketEngine.binanceAdapter.fetchTicker("BTC/USDT")
    assertTrue("Binance must return tick", binanceRes.isSuccess)
    val binanceTick = binanceRes.getOrThrow()
    assertEquals("BTC/USDT", binanceTick.symbol)
    assertEquals("Binance", binanceTick.exchangeName)
    assertTrue("Price must be positive", binanceTick.lastPrice > 0.0)
    assertTrue("Bid must be valid", binanceTick.bid > 0.0)
    assertTrue("Ask must be valid", binanceTick.ask > 0.0)
    assertTrue("Timestamp must be valid", binanceTick.sourceTimestamp > 0)
    assertEquals(DataQuality.VERIFIED, binanceTick.dataQuality)

    // 2. Coinbase
    val coinbaseRes = marketEngine.coinbaseAdapter.fetchTicker("BTC/USD")
    assertTrue(coinbaseRes.isSuccess)
    val coinbaseTick = coinbaseRes.getOrThrow()
    assertEquals("Coinbase", coinbaseTick.exchangeName)
    assertTrue(coinbaseTick.lastPrice > 0.0)

    // 3. Kraken
    val krakenRes = marketEngine.krakenAdapter.fetchTicker("BTC/USDT")
    assertTrue(krakenRes.isSuccess)
    val krakenTick = krakenRes.getOrThrow()
    assertEquals("Kraken", krakenTick.exchangeName)
    assertTrue(krakenTick.lastPrice > 0.0)

    // 4. OKX
    val okxRes = marketEngine.okxAdapter.fetchTicker("BTC/USDT")
    assertTrue(okxRes.isSuccess)
    val okxTick = okxRes.getOrThrow()
    assertEquals("OKX", okxTick.exchangeName)
    assertTrue(okxTick.lastPrice > 0.0)

    // 5. Bybit
    val bybitRes = marketEngine.bybitAdapter.fetchTicker("BTC/USDT")
    assertTrue(bybitRes.isSuccess)
    val bybitTick = bybitRes.getOrThrow()
    assertEquals("Bybit", bybitTick.exchangeName)
    assertTrue(bybitTick.lastPrice > 0.0)
  }

  // =========================================================================
  // 2. UNIVERSAL NORMALIZATION & ZERO-CONFLATION TEST
  // =========================================================================
  @Test
  fun testUniversalNormalizationAndSpreadPreservation() = runTest {
    val binanceTick = marketEngine.binanceAdapter.fetchTicker("BTC/USDT").getOrThrow()
    val coinbaseTick = marketEngine.coinbaseAdapter.fetchTicker("BTC/USD").getOrThrow()

    // Pairs are distinct: BTC/USDT != BTC/USD (Zero pair conflation guarantee)
    assertFalse("Must not conflate USDT pair with USD pair", binanceTick.symbol == coinbaseTick.symbol)
    assertEquals("USDT", binanceTick.quoteAsset)
    assertEquals("USD", coinbaseTick.quoteAsset)

    // Spread computation
    val spreadComp = marketEngine.calculateCrossExchangeSpread(
      symbol = "BTC",
      tickA = binanceTick,
      tickB = coinbaseTick
    )
    assertNotNull(spreadComp)
    assertTrue("Spread difference must be calculated", spreadComp.priceDifference >= 0.0)
    assertTrue("Spread percent must be recorded", spreadComp.spreadPercent >= 0.0)
  }

  // =========================================================================
  // 3. GLOBAL ASSET, FOREX & COMMODITIES TEST (SAR, AED, Gold XAU)
  // =========================================================================
  @Test
  fun testGlobalForexAndCommodities() = runTest {
    val sarRes = marketEngine.globalAssetAdapter.fetchTicker("USD/SAR")
    assertTrue(sarRes.isSuccess)
    val sarTick = sarRes.getOrThrow()
    assertEquals(3.7505, sarTick.lastPrice, 0.01)

    val aedRes = marketEngine.globalAssetAdapter.fetchTicker("USD/AED")
    assertTrue(aedRes.isSuccess)
    val aedTick = aedRes.getOrThrow()
    assertEquals(3.6725, aedTick.lastPrice, 0.01)

    val goldRes = marketEngine.globalAssetAdapter.fetchTicker("XAU/USD")
    assertTrue(goldRes.isSuccess)
    val goldTick = goldRes.getOrThrow()
    assertTrue("Gold price per ounce must be realistic", goldTick.lastPrice > 2000.0)
  }

  // =========================================================================
  // 4. CURRENCY CONVERTER ACCURACY TEST
  // =========================================================================
  @Test
  fun testCurrencyConverterAccuracy() = runTest {
    marketEngine.refreshAllMarketData()

    // 1000 SAR to USD
    val conversion = marketEngine.convertCurrency(1000.0, "SAR", "USD")
    assertEquals(1000.0, conversion.fromAmount, 0.001)
    assertEquals("SAR", conversion.fromAsset)
    assertEquals("USD", conversion.toAsset)
    // 1000 / 3.7505 ~= 266.63
    assertTrue("Conversion should be approximately 266 USD", conversion.convertedAmount in 260.0..275.0)

    // USD to SAR
    val reverse = marketEngine.convertCurrency(100.0, "USD", "SAR")
    assertTrue("100 USD ~= 375 SAR", reverse.convertedAmount in 370.0..380.0)
  }

  // =========================================================================
  // 5. SOCIAL ADAPTER ARCHITECTURE TESTS (All 12 Networks)
  // =========================================================================
  @Test
  fun testAll12SocialAdaptersHealthAndStatus() = runTest {
    val logs = SocialAdapterRegistry.getAllHealthLogs()
    assertEquals("Must support all 12 mandated networks", 12, logs.size)

    // Verify WhatsApp Business
    val waLog = logs.find { it.network == SocialNetwork.WHATSAPP_BUSINESS }
    assertNotNull(waLog)
    assertEquals(SocialConnectionState.CONNECTED, waLog?.state)

    // Verify Telegram Bot
    val tgLog = logs.find { it.network == SocialNetwork.TELEGRAM_BOT }
    assertNotNull(tgLog)
    assertEquals(SocialConnectionState.CONNECTED, tgLog?.state)

    // Verify Telegram Mini Apps
    val tmaLog = logs.find { it.network == SocialNetwork.TELEGRAM_MINI_APP }
    assertNotNull(tmaLog)
    assertEquals(SocialConnectionState.CONNECTED, tmaLog?.state)

    // Verify Instagram Graph is in PENDING_APPROVAL
    val igLog = logs.find { it.network == SocialNetwork.INSTAGRAM_GRAPH }
    assertNotNull(igLog)
    assertEquals(SocialConnectionState.PENDING_APPROVAL, igLog?.state)

    // Verify TikTok is in PENDING_APPROVAL
    val ttLog = logs.find { it.network == SocialNetwork.TIKTOK_SHARE }
    assertNotNull(ttLog)
    assertEquals(SocialConnectionState.PENDING_APPROVAL, ttLog?.state)

    // Verify LinkedIn Share
    val liLog = logs.find { it.network == SocialNetwork.LINKEDIN_SHARE }
    assertNotNull(liLog)
    assertEquals(SocialConnectionState.CONNECTED, liLog?.state)
  }

  // =========================================================================
  // 6. SOCIAL SHARING LAYER & FALLBACK URL GENERATION
  // =========================================================================
  @Test
  fun testSocialShareUrlFormatting() {
    val intent = PaymentIntent(
      intentId = "SP-PI-123456",
      transactionId = "SP-REQ-123456",
      amount = 500.0,
      currencyTicker = "SAR",
      assetId = "SAR_FIAT",
      network = com.example.core.asset.BlockchainNetwork.FIAT_BANKING,
      senderUserId = "USR-A",
      senderName = "سالم",
      recipientUserId = "USR-B",
      recipientName = "أحمد",
      paymentLink = "https://pay.socialpay.io/req/SP-REQ-123456",
      qrCodePayload = "socialpay://req/SP-REQ-123456?amount=500&cur=SAR",
      idempotencyKey = "IDEM-TEST-123",
      state = TransactionState.CREATED
    )

    // WhatsApp Share URL
    val waAdapter = SocialAdapterRegistry.getAdapter(SocialNetwork.WHATSAPP_BUSINESS)
    val waUrl = waAdapter.formatShareIntentUrl(intent, "+966501234567")
    assertTrue("Must use official WhatsApp API", waUrl.startsWith("https://api.whatsapp.com/send"))
    assertTrue("Must contain clean phone", waUrl.contains("phone=966501234567"))

    // Telegram Share URL
    val tgAdapter = SocialAdapterRegistry.getAdapter(SocialNetwork.TELEGRAM_BOT)
    val tgUrl = tgAdapter.formatShareIntentUrl(intent)
    assertTrue("Must use official Telegram share link", tgUrl.startsWith("https://t.me/share/url"))

    // Telegram Mini App Startapp link
    val tmaAdapter = SocialAdapterRegistry.getAdapter(SocialNetwork.TELEGRAM_MINI_APP)
    val tmaUrl = tmaAdapter.formatShareIntentUrl(intent)
    assertTrue("Must point to Mini App startapp payload", tmaUrl.contains("startapp=SP-REQ-123456"))
  }

  // =========================================================================
  // 7. ORDER BOOK & CANDLE DATA INTEGRITY
  // =========================================================================
  @Test
  fun testOrderBookAndCandleFeeds() = runTest {
    val orderBookRes = marketEngine.binanceAdapter.fetchOrderBook("BTC/USDT", depth = 5)
    assertTrue(orderBookRes.isSuccess)
    val orderBook = orderBookRes.getOrThrow()
    assertEquals(5, orderBook.bids.size)
    assertEquals(5, orderBook.asks.size)
    assertTrue("Best ask must be greater than best bid", orderBook.asks.first().price > orderBook.bids.first().price)

    val candlesRes = marketEngine.binanceAdapter.fetchCandles("BTC/USDT", interval = "1h", limit = 10)
    assertTrue(candlesRes.isSuccess)
    val candles = candlesRes.getOrThrow()
    assertEquals(10, candles.size)
    assertTrue("High must be >= Low", candles.first().high >= candles.first().low)
  }

  // =========================================================================
  // 8. WEBSOCKET STREAMER STATES & INBOUND WEBHOOK VERIFICATION
  // =========================================================================
  @Test
  fun testBinanceWebSocketStreamerLifecycle() {
    val streamer = com.example.market.core.BinanceWebSocketStreamer()
    assertEquals(com.example.market.core.WsConnectionState.DISCONNECTED, streamer.connectionState.value)
    streamer.stopStream()
    assertEquals(com.example.market.core.WsConnectionState.DISCONNECTED, streamer.connectionState.value)
  }

  @Test
  fun testInboundWebhookSignatureVerification() {
    val secret = "whsec_socialpay_live_secret_k8x9"
    val payload = """{"event":"payment_intent.succeeded","amount":1000.0,"currency":"SAR"}"""

    // Valid HMAC-SHA256
    val signature = com.example.core.security.SecurityEngine.computeHmacSha256(payload, secret)
    assertTrue("Signature must verify successfully", com.example.core.security.SecurityEngine.verifySignature(payload, signature, secret))

    // Tampered payload
    val tamperedPayload = """{"event":"payment_intent.succeeded","amount":999999.0,"currency":"SAR"}"""
    assertFalse("Tampered payload must be rejected", com.example.core.security.SecurityEngine.verifySignature(tamperedPayload, signature, secret))

    // Tampered signature
    val tamperedSignature = signature.reversed()
    assertFalse("Tampered signature must be rejected", com.example.core.security.SecurityEngine.verifySignature(payload, tamperedSignature, secret))
  }

  @Test
  fun testAll12SocialAdaptersRegisteredAndFormatted() {
    val allAdapters = SocialAdapterRegistry.getAllAdapters()
    assertEquals("Must have 12 official social adapters", 12, allAdapters.size)

    val intent = PaymentIntent(
      intentId = "PI-TEST-999",
      transactionId = "SP-REQ-999",
      amount = 750.0,
      currencyTicker = "USDT",
      assetId = "USDT_TRC20",
      network = com.example.core.asset.BlockchainNetwork.TRON_TRC20,
      senderUserId = "USR-1",
      senderName = "Seller",
      recipientUserId = "USR-2",
      recipientName = "Buyer",
      paymentLink = "https://pay.socialpay.io/req/SP-REQ-999",
      qrCodePayload = "socialpay://req/SP-REQ-999",
      idempotencyKey = "IDEM-999"
    )

    allAdapters.forEach { adapter ->
      val url = adapter.formatShareIntentUrl(intent, "+966500000000")
      assertNotNull("Adapter ${adapter.network.name} must return share URL", url)
      assertTrue("URL must not be empty", url.isNotBlank())
    }
  }
}
