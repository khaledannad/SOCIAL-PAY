package com.example.market.core

/**
 * Data status indicating live responsiveness vs latency
 */
enum class DataStatus(val labelAr: String, val labelEn: String) {
  LIVE("مباشر لحظي", "Live Realtime"),
  DELAYED("متأخر", "Delayed"),
  STALE("بيانات غير محدثة", "Stale Data"),
  DISCONNECTED("غير متصل", "Disconnected")
}

/**
 * Data quality metric verifying authenticity and anomaly checks
 */
enum class DataQuality(val labelAr: String, val labelEn: String) {
  VERIFIED("موثوق ومطابق", "Verified"),
  UNCONFIRMED("غير مؤكد", "Unconfirmed"),
  ANOMALY("شذوذ في السعر", "Price Anomaly")
}

/**
 * Universal Normalized Market Tick as mandated by Module 3:
 * Guarantees zero pair conflation, preserves exchange spreads, and tracks precise latency.
 */
data class NormalizedMarketTick(
  val symbol: String, // e.g. "BTC/USDT", "ETH/USDT", "SAR/USD", "XAU/USD"
  val baseAsset: String, // "BTC"
  val quoteAsset: String, // "USDT"
  val exchangeName: String, // "Binance", "Coinbase", "Kraken", "OKX", "Bybit", "Forex_Interbank"
  val lastPrice: Double,
  val bid: Double,
  val ask: Double,
  val spread: Double = if (ask >= bid) ask - bid else 0.0,
  val high24h: Double = lastPrice,
  val low24h: Double = lastPrice,
  val volume24h: Double = 0.0,
  val priceChangePercent24h: Double = 0.0,
  val sourceTimestamp: Long,
  val receivedTimestamp: Long = System.currentTimeMillis(),
  val dataStatus: DataStatus = DataStatus.LIVE,
  val dataQuality: DataQuality = DataQuality.VERIFIED,
  val latencyMs: Long = (System.currentTimeMillis() - sourceTimestamp).coerceAtLeast(0)
)

/**
 * Level 2 Order Book Price Level
 */
data class OrderBookLevel(
  val price: Double,
  val quantity: Double
)

data class OrderBook(
  val symbol: String,
  val exchangeName: String,
  val bids: List<OrderBookLevel>,
  val asks: List<OrderBookLevel>,
  val timestamp: Long = System.currentTimeMillis()
)

/**
 * OHLCV Candlestick bar for charts
 */
data class MarketCandle(
  val symbol: String,
  val exchangeName: String,
  val interval: String, // "1m", "5m", "1h", "1d"
  val openTime: Long,
  val open: Double,
  val high: Double,
  val low: Double,
  val close: Double,
  val volume: Double
)

/**
 * Operational Health of an Exchange Feed
 */
data class ExchangeHealthStatus(
  val exchangeName: String,
  val isOnline: Boolean,
  val responseTimeMs: Long,
  val lastSuccessfulCheck: Long = System.currentTimeMillis(),
  val activePairsCount: Int,
  val errorMessage: String? = null
)

/**
 * Standard Exchange Adapter Interface
 * Implemented independently for each exchange (Binance, Coinbase, Kraken, OKX, Bybit).
 * Strictly read-only public endpoints with zero trading or custody capabilities.
 */
interface ExchangeAdapter {
  val exchangeName: String
  val supportedPairs: List<String>
  val hasWebSocketSupport: Boolean

  suspend fun fetchTicker(symbol: String): Result<NormalizedMarketTick>
  suspend fun fetchOrderBook(symbol: String, depth: Int = 10): Result<OrderBook>
  suspend fun fetchCandles(symbol: String, interval: String = "1h", limit: Int = 24): Result<List<MarketCandle>>
  suspend fun checkHealth(): ExchangeHealthStatus
}
