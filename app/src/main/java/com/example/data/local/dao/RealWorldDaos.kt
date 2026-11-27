package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RealWorldMarketDao {
  @Query("SELECT * FROM market_pairs ORDER BY volume24h DESC")
  fun getAllMarketPairs(): Flow<List<MarketPairEntity>>

  @Query("SELECT * FROM market_pairs WHERE symbol = :symbol")
  fun getPairsBySymbol(symbol: String): Flow<List<MarketPairEntity>>

  @Query("SELECT * FROM market_pairs WHERE id = :id LIMIT 1")
  suspend fun getPairById(id: String): MarketPairEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllPairs(pairs: List<MarketPairEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPair(pair: MarketPairEntity)

  @Query("SELECT * FROM market_providers ORDER BY name ASC")
  fun getAllProviders(): Flow<List<MarketProviderEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllProviders(providers: List<MarketProviderEntity>)

  @Query("SELECT * FROM market_candles WHERE symbol = :symbol AND exchange = :exchange ORDER BY openTime DESC LIMIT :limit")
  suspend fun getCandles(symbol: String, exchange: String, limit: Int = 50): List<MarketCandleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCandles(candles: List<MarketCandleEntity>)
}

@Dao
interface RealWorldSocialDao {
  @Query("SELECT * FROM social_connections ORDER BY name ASC")
  fun getAllSocialConnections(): Flow<List<SocialConnectionEntity>>

  @Query("SELECT * FROM social_connections WHERE connectionState = 'CONNECTED'")
  fun getActiveConnections(): Flow<List<SocialConnectionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllConnections(connections: List<SocialConnectionEntity>)

  @Query("UPDATE social_connections SET connectionState = :state, latencyMs = :latency, lastSyncAt = :time WHERE networkId = :id")
  suspend fun updateConnectionStatus(id: String, state: String, latency: Long, time: Long = System.currentTimeMillis())

  @Query("SELECT * FROM integration_health_logs ORDER BY timestamp DESC LIMIT 50")
  fun getRecentHealthLogs(): Flow<List<IntegrationHealthLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHealthLog(log: IntegrationHealthLogEntity)

  @Query("SELECT * FROM data_quality_logs ORDER BY timestamp DESC LIMIT 50")
  fun getDataQualityLogs(): Flow<List<DataQualityLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQualityLog(log: DataQualityLogEntity)
}
