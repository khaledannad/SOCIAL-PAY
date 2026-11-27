package com.example.market.core

import com.example.market.adapters.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stablecoin Peg Deviation Comparison Record (Zero Speculative Crypto Arbitrage)
 */
data class MarketSpreadComparison(
  val symbol: String,
  val exchangeA: String,
  val priceA: Double,
  val exchangeB: String,
  val priceB: Double,
  val priceDifference: Double,
  val spreadPercent: Double,
  val timestamp: Long = System.currentTimeMillis()
)

/**
 * Settlement Currency Reference Conversion Result
 */
data class CurrencyConversionResult(
  val fromAmount: Double,
  val fromAsset: String,
  val toAsset: String,
  val convertedAmount: Double,
  val exchangeRate: Double,
  val exchangeSource: String,
  val timestamp: Long = System.currentTimeMillis()
)

/**
 * Central Market Data & Stablecoin Reference Engine.
 * Monitors Approved Stablecoin peg health (USDC, USDT, PYUSD, EURC) and Forex settlement
 * reference rates using read-only public exchange endpoints. Zero speculative trading or orders.
 */
class MarketDataEngine(
  val binanceAdapter: ExchangeAdapter = BinanceAdapter(),
  val coinbaseAdapter: ExchangeAdapter = CoinbaseAdapter(),
  val krakenAdapter: ExchangeAdapter = KrakenAdapter(),
  val okxAdapter: ExchangeAdapter = OkxAdapter(),
  val bybitAdapter: ExchangeAdapter = BybitAdapter(),
  val globalAssetAdapter: ExchangeAdapter = GlobalAssetAdapter()
) {

  private val allAdapters = listOf(
    binanceAdapter,
    coinbaseAdapter,
    krakenAdapter,
    okxAdapter,
    bybitAdapter,
    globalAssetAdapter
  )

  private val _liveTicks = MutableStateFlow<Map<String, NormalizedMarketTick>>(emptyMap())
  val liveTicks: StateFlow<Map<String, NormalizedMarketTick>> = _liveTicks.asStateFlow()

  private val _exchangeHealth = MutableStateFlow<List<ExchangeHealthStatus>>(emptyList())
  val exchangeHealth: StateFlow<List<ExchangeHealthStatus>> = _exchangeHealth.asStateFlow()

  /**
   * Refreshes real live Stablecoin Peg & Settlement FX reference feeds.
   */
  suspend fun refreshAllMarketData(): Map<String, NormalizedMarketTick> {
    val results = mutableMapOf<String, NormalizedMarketTick>()

    // 1. Approved Stablecoin Peg Monitoring (USDC/USDT, USDC/USD, USDT/USD)
    binanceAdapter.fetchTicker("USDC/USDT").getOrNull()?.let { results["BINANCE_USDC_USDT"] = it }
    binanceAdapter.fetchTicker("FDUSD/USDT").getOrNull()?.let { results["BINANCE_FDUSD_USDT"] = it }
    coinbaseAdapter.fetchTicker("USDC/USD").getOrNull()?.let { results["COINBASE_USDC_USD"] = it }
    coinbaseAdapter.fetchTicker("USDT/USD").getOrNull()?.let { results["COINBASE_USDT_USD"] = it }
    krakenAdapter.fetchTicker("USDC/USD").getOrNull()?.let { results["KRAKEN_USDC_USD"] = it }
    krakenAdapter.fetchTicker("USDT/USD").getOrNull()?.let { results["KRAKEN_USDT_USD"] = it }
    okxAdapter.fetchTicker("USDC/USDT").getOrNull()?.let { results["OKX_USDC_USDT"] = it }
    bybitAdapter.fetchTicker("USDC/USDT").getOrNull()?.let { results["BYBIT_USDC_USDT"] = it }

    // 2. Settlement Reference Forex Rates (USD -> SAR, AED, EUR, EGP)
    globalAssetAdapter.fetchTicker("USD/SAR").getOrNull()?.let { results["FOREX_USD_SAR"] = it }
    globalAssetAdapter.fetchTicker("USD/AED").getOrNull()?.let { results["FOREX_USD_AED"] = it }
    globalAssetAdapter.fetchTicker("USD/EGP").getOrNull()?.let { results["FOREX_USD_EGP"] = it }

    _liveTicks.value = results
    _exchangeHealth.value = allAdapters.map { it.checkHealth() }
    return results
  }

  /**
   * Compares stablecoin peg pricing across two independent feeds without mixing pairs.
   */
  fun calculateCrossExchangeSpread(
    symbol: String = "USDC/USD",
    adapterA: ExchangeAdapter = coinbaseAdapter,
    adapterB: ExchangeAdapter = krakenAdapter,
    tickA: NormalizedMarketTick,
    tickB: NormalizedMarketTick
  ): MarketSpreadComparison {
    val diff = kotlin.math.abs(tickA.lastPrice - tickB.lastPrice)
    val spreadPct = if (tickA.lastPrice > 0) (diff / tickA.lastPrice) * 100.0 else 0.0

    return MarketSpreadComparison(
      symbol = symbol,
      exchangeA = adapterA.exchangeName,
      priceA = tickA.lastPrice,
      exchangeB = adapterB.exchangeName,
      priceB = tickB.lastPrice,
      priceDifference = diff,
      spreadPercent = spreadPct
    )
  }

  /**
   * Settlement reference converter strictly between Approved Stablecoins and Fiat Settlement currencies.
   */
  fun convertCurrency(
    amount: Double,
    fromCurrency: String,
    toCurrency: String
  ): CurrencyConversionResult {
    val ticks = _liveTicks.value
    val cleanFrom = fromCurrency.uppercase().trim()
    val cleanTo = toCurrency.uppercase().trim()

    if (cleanFrom == cleanTo) {
      return CurrencyConversionResult(amount, fromCurrency, toCurrency, amount, 1.0, "Direct Peg 1:1")
    }

    val usdRates = mapOf(
      "USD" to 1.0,
      "USDC" to (ticks["COINBASE_USDC_USD"]?.lastPrice ?: 1.0),
      "USDT" to (ticks["COINBASE_USDT_USD"]?.lastPrice ?: 1.0),
      "EURC" to 1.08,
      "PYUSD" to 1.0,
      "SAR" to (ticks["FOREX_USD_SAR"]?.lastPrice ?: 3.7505),
      "AED" to (ticks["FOREX_USD_AED"]?.lastPrice ?: 3.6725),
      "EGP" to (ticks["FOREX_USD_EGP"]?.lastPrice ?: 49.20),
      "EUR" to 0.92,
      "DZD" to 134.50
    )

    val fromUsd = usdRates[cleanFrom]?.let { 1.0 / it } ?: 1.0
    val toUsd = usdRates[cleanTo] ?: 1.0
    val rate = fromUsd * toUsd
    val source = if (ticks.isEmpty()) "Indicative Reference Peg" else "Live Public Feed"

    return CurrencyConversionResult(
      fromAmount = amount,
      fromAsset = cleanFrom,
      toAsset = cleanTo,
      convertedAmount = amount * rate,
      exchangeRate = rate,
      exchangeSource = source
    )
  }
}
