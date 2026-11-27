package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_providers")
data class MarketProviderEntity(
  @PrimaryKey
  val providerId: String, // "BINANCE", "COINBASE", "KRAKEN", "OKX", "BYBIT", "FOREX_INTERBANK"
  val name: String,
  val type: String, // "CRYPTO_EXCHANGE", "FOREX_INTERBANK", "COMMODITIES"
  val baseUrl: String,
  val hasWebSocket: Boolean,
  val isOperational: Boolean,
  val latencyMs: Long,
  val lastPingAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "market_pairs")
data class MarketPairEntity(
  @PrimaryKey
  val id: String, // e.g. "BINANCE_BTC_USDT"
  val symbol: String, // "BTC/USDT"
  val baseAsset: String, // "BTC"
  val quoteAsset: String, // "USDT"
  val exchange: String, // "Binance"
  val lastPrice: Double,
  val bid: Double,
  val ask: Double,
  val volume24h: Double,
  val high24h: Double,
  val low24h: Double,
  val priceChangePercent: Double,
  val sourceTimestamp: Long,
  val receivedTimestamp: Long = System.currentTimeMillis(),
  val dataStatus: String = "LIVE"
)

@Entity(tableName = "market_candles")
data class MarketCandleEntity(
  @PrimaryKey
  val id: String, // "BINANCE_BTC_USDT_1h_1731175200000"
  val symbol: String,
  val exchange: String,
  val interval: String,
  val openTime: Long,
  val open: Double,
  val high: Double,
  val low: Double,
  val close: Double,
  val volume: Double
)

@Entity(tableName = "social_connections")
data class SocialConnectionEntity(
  @PrimaryKey
  val networkId: String, // "WHATSAPP_BUSINESS", "TELEGRAM_BOT", "INSTAGRAM_GRAPH", etc.
  val name: String,
  val category: String,
  val connectionState: String, // "CONNECTED", "PENDING_APPROVAL", "DISCONNECTED", "RATE_LIMITED"
  val authMethod: String,
  val latencyMs: Long,
  val lastSyncAt: Long = System.currentTimeMillis(),
  val activeFeatures: String,
  val complianceNotes: String
)

@Entity(tableName = "integration_health_logs")
data class IntegrationHealthLogEntity(
  @PrimaryKey
  val id: String,
  val serviceCategory: String, // "MARKET_EXCHANGE", "SOCIAL_INTEGRATION", "PAYMENT_GATEWAY"
  val serviceName: String,
  val status: String,
  val latencyMs: Long,
  val timestamp: Long = System.currentTimeMillis(),
  val eventSummary: String
)

@Entity(tableName = "data_quality_logs")
data class DataQualityLogEntity(
  @PrimaryKey
  val id: String,
  val assetSymbol: String,
  val exchangeName: String,
  val qualityStatus: String, // "VERIFIED", "ANOMALY", "STALE"
  val detectedDiscrepancy: String,
  val timestamp: Long = System.currentTimeMillis()
)
