package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.market.core.MarketCandle
import com.example.market.core.OrderBook
import com.example.market.core.OrderBookLevel
import com.example.ui.theme.*

@Composable
fun MarketDepthAndCandleView(
  symbol: String,
  lastPrice: Double,
  candles: List<MarketCandle>,
  orderBook: OrderBook?,
  isRtl: Boolean,
  onIntervalChanged: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedInterval by remember { mutableStateOf("1h") }
  val intervals = listOf("15m", "1h", "4h", "1D")
  var activeViewTab by remember { mutableStateOf(0) } // 0 = Candlesticks, 1 = Order Book Depth

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // 1. Header with symbol, price and view mode switcher
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(symbol, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
            Surface(
              color = EmeraldPrimary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "Live Orderbook",
                color = EmeraldPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            text = "${String.format("%,.2f", lastPrice)} USD",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = EmeraldPrimary
          )
        }

        // View Mode Switcher
        Row(
          modifier = Modifier
            .background(BackgroundDark, RoundedCornerShape(8.dp))
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(8.dp))
            .padding(2.dp)
        ) {
          IconButton(
            onClick = { activeViewTab = 0 },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CandlestickChart,
              contentDescription = "Candles",
              tint = if (activeViewTab == 0) EmeraldPrimary else Color.Gray,
              modifier = Modifier.size(18.dp)
            )
          }
          IconButton(
            onClick = { activeViewTab = 1 },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ShowChart,
              contentDescription = "Depth",
              tint = if (activeViewTab == 1) EmeraldPrimary else Color.Gray,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // 2. Interval selector for Candles
      if (activeViewTab == 0) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          intervals.forEach { interval ->
            FilterChip(
              selected = selectedInterval == interval,
              onClick = {
                selectedInterval = interval
                onIntervalChanged(interval)
              },
              label = { Text(interval, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EmeraldPrimary,
                selectedLabelColor = BackgroundDark,
                containerColor = BackgroundDark,
                labelColor = Color.LightGray
              ),
              modifier = Modifier.height(28.dp)
            )
          }
        }

        // Candlestick Chart Canvas
        CandlestickCanvas(
          candles = candles,
          currentPrice = lastPrice,
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        )
      } else {
        // Order Book Depth Visualizer
        OrderBookDepthVisualizer(
          orderBook = orderBook,
          currentPrice = lastPrice,
          isRtl = isRtl,
          modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
        )
      }
    }
  }
}

@Composable
fun CandlestickCanvas(
  candles: List<MarketCandle>,
  currentPrice: Double,
  modifier: Modifier = Modifier
) {
  if (candles.isEmpty()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      Text("جاري تحميل بيانات الشموع الحية...", color = Color.Gray, fontSize = 11.sp)
    }
    return
  }

  val bullishColor = Color(0xFF00C853)
  val bearishColor = Color(0xFFFF3D00)
  val maLineColor = Color(0xFFFFD700)
  val gridColor = Color(0xFF2C3240)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val maxPrice = (candles.maxOfOrNull { it.high } ?: (currentPrice * 1.05)).coerceAtLeast(currentPrice * 1.01)
    val minPrice = (candles.minOfOrNull { it.low } ?: (currentPrice * 0.95)).coerceAtMost(currentPrice * 0.99)
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)

    val candleCount = candles.size
    val slotWidth = w / candleCount.coerceAtLeast(1)
    val candleBodyWidth = (slotWidth * 0.65f).coerceAtLeast(2f)

    // Draw horizontal grid lines
    val gridSteps = 4
    for (i in 0..gridSteps) {
      val y = h * (i.toFloat() / gridSteps)
      drawLine(
        color = gridColor,
        start = Offset(0f, y),
        end = Offset(w, y),
        strokeWidth = 1f
      )
    }

    // Helper to calculate Y from price
    fun priceToY(p: Double): Float {
      val normalized = (p - minPrice) / priceRange
      return (h - (normalized * h)).toFloat().coerceIn(0f, h)
    }

    // Draw candles
    val maPoints = mutableListOf<Offset>()

    candles.forEachIndexed { idx, c ->
      val centerX = (idx * slotWidth) + (slotWidth / 2f)
      val openY = priceToY(c.open)
      val closeY = priceToY(c.close)
      val highY = priceToY(c.high)
      val lowY = priceToY(c.low)

      val isBullish = c.close >= c.open
      val candleColor = if (isBullish) bullishColor else bearishColor

      // Wick (high to low)
      drawLine(
        color = candleColor,
        start = Offset(centerX, highY),
        end = Offset(centerX, lowY),
        strokeWidth = 1.5f
      )

      // Body (open to close)
      val bodyTop = minOf(openY, closeY)
      val bodyHeight = maxOf(Math.abs(openY - closeY), 2f)
      drawRect(
        color = candleColor,
        topLeft = Offset(centerX - (candleBodyWidth / 2f), bodyTop),
        size = Size(candleBodyWidth, bodyHeight)
      )

      // Collect points for 5-period Simple Moving Average
      if (idx >= 4) {
        val avg5 = candles.subList(idx - 4, idx + 1).map { it.close }.average()
        maPoints.add(Offset(centerX, priceToY(avg5)))
      }
    }

    // Draw Moving Average curve
    if (maPoints.size > 1) {
      val path = Path().apply {
        moveTo(maPoints.first().x, maPoints.first().y)
        for (i in 1 until maPoints.size) {
          lineTo(maPoints[i].x, maPoints[i].y)
        }
      }
      drawPath(
        path = path,
        color = maLineColor,
        style = Stroke(width = 1.5f)
      )
    }

    // Current Price Dashed Line
    val currentY = priceToY(currentPrice)
    drawLine(
      color = Color(0xFF00FF9D),
      start = Offset(0f, currentY),
      end = Offset(w, currentY),
      strokeWidth = 1f,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )
  }
}

@Composable
fun OrderBookDepthVisualizer(
  orderBook: OrderBook?,
  currentPrice: Double,
  isRtl: Boolean,
  modifier: Modifier = Modifier
) {
  val defaultBids = listOf(
    OrderBookLevel(currentPrice - 2.5, 1.25),
    OrderBookLevel(currentPrice - 5.0, 2.80),
    OrderBookLevel(currentPrice - 8.0, 4.15),
    OrderBookLevel(currentPrice - 12.0, 6.50),
    OrderBookLevel(currentPrice - 16.0, 9.20)
  )

  val defaultAsks = listOf(
    OrderBookLevel(currentPrice + 2.5, 0.95),
    OrderBookLevel(currentPrice + 5.0, 2.10),
    OrderBookLevel(currentPrice + 8.5, 3.80),
    OrderBookLevel(currentPrice + 11.5, 5.40),
    OrderBookLevel(currentPrice + 15.0, 8.10)
  )

  val bids = orderBook?.bids?.take(5) ?: defaultBids
  val asks = orderBook?.asks?.take(5) ?: defaultAsks
  val maxBidQty = bids.map { it.quantity }.maxOrNull() ?: 1.0
  val maxAskQty = asks.map { it.quantity }.maxOrNull() ?: 1.0
  val maxVolume = maxOf(maxBidQty, maxAskQty, 1.0)

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    // Header labels
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(if (isRtl) "طلبات الشراء (Bids)" else "Buy Orders (Bids)", color = Color(0xFF00C853), fontSize = 11.sp, fontWeight = FontWeight.Bold)
      Text(if (isRtl) "الفارق (Spread): 5.00$" else "Spread: 5.00$", color = Color.Gray, fontSize = 10.sp)
      Text(if (isRtl) "طلبات البيع (Asks)" else "Sell Orders (Asks)", color = Color(0xFFFF3D00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Bids column (Green)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        bids.forEach { bid ->
          val fillPct = (bid.quantity / maxVolume).toFloat().coerceIn(0.1f, 1f)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(24.dp)
              .background(BackgroundDark, RoundedCornerShape(4.dp))
          ) {
            // Fill background bar
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillPct)
                .background(Color(0xFF00C853).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .align(Alignment.CenterStart)
            )
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(String.format("%,.1f", bid.price), fontSize = 10.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
              Text(String.format("%.2f", bid.quantity), fontSize = 9.sp, color = Color.LightGray)
            }
          }
        }
      }

      // Asks column (Red)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        asks.forEach { ask ->
          val fillPct = (ask.quantity / maxVolume).toFloat().coerceIn(0.1f, 1f)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(24.dp)
              .background(BackgroundDark, RoundedCornerShape(4.dp))
          ) {
            // Fill background bar
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillPct)
                .background(Color(0xFFFF3D00).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .align(Alignment.CenterEnd)
            )
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(String.format("%.2f", ask.quantity), fontSize = 9.sp, color = Color.LightGray)
              Text(String.format("%,.1f", ask.price), fontSize = 10.sp, color = Color(0xFFFF3D00), fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
