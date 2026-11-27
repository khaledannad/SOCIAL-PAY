package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSwapBottomSheet(
  supportedCrypto: List<CryptoAsset>,
  supportedFiat: List<FiatCurrency>,
  paymentMethods: List<PaymentMethod>,
  currentFeeUsd: Double,
  isRtl: Boolean,
  onDismiss: () -> Unit,
  onInitiateQuickDeal: (CryptoAsset, FiatCurrency, TradeType, Double, Double) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedTradeType by remember { mutableStateOf(TradeType.BUY) }
  var selectedCrypto by remember { mutableStateOf(supportedCrypto.first()) }
  var selectedFiat by remember { mutableStateOf(supportedFiat.first()) }
  var selectedPayment by remember { mutableStateOf(paymentMethods.first()) }
  var amountInput by remember { mutableStateOf("1000") }

  val parsedFiat = amountInput.toDoubleOrNull() ?: 0.0
  val rate = (selectedCrypto.basePriceUsd * selectedFiat.rateToUsd)
  val calculatedCrypto = if (rate > 0) parsedFiat / rate else 0.0

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldAccent)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isRtl) "تحويل فوري عبر زر P2P السريع" else "Fast P2P Social Swap",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
        }

        Surface(
          color = GoldAccent.copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "$${String.format("%.2f", currentFeeUsd)} ثابتة",
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Buy / Sell Selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { selectedTradeType = TradeType.BUY },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedTradeType == TradeType.BUY) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = if (isRtl) "شراء" else "Buy",
            fontWeight = FontWeight.Bold,
            color = if (selectedTradeType == TradeType.BUY) BackgroundDark else MaterialTheme.colorScheme.onSurface
          )
        }

        Button(
          onClick = { selectedTradeType = TradeType.SELL },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedTradeType == TradeType.SELL) StatusError else MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = if (isRtl) "بيع" else "Sell",
            fontWeight = FontWeight.Bold,
            color = if (selectedTradeType == TradeType.SELL) Color.White else MaterialTheme.colorScheme.onSurface
          )
        }
      }

      // Crypto selection
      Text(
        text = if (isRtl) "الأصل المطلوب:" else "Select Asset:",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
      )

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(supportedCrypto) { crypto ->
          val isSelected = crypto.symbol == selectedCrypto.symbol
          Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSelected) EmeraldPrimary else Color.Transparent
            ),
            modifier = Modifier.clickable { selectedCrypto = crypto }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(crypto.iconEmoji)
              Spacer(modifier = Modifier.width(6.dp))
              Text(crypto.symbol, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Amount Input
      OutlinedTextField(
        value = amountInput,
        onValueChange = { amountInput = it },
        label = { Text(if (isRtl) "المبلغ المطلوب بالـ ${selectedFiat.symbol}" else "Amount in ${selectedFiat.symbol}") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        trailingIcon = {
          Text(
            text = selectedFiat.code,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 12.dp)
          )
        },
        modifier = Modifier.fillMaxWidth()
      )

      // Conversion Summary
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(if (isRtl) "ستحصل على تقريباً:" else "Estimated Output:", fontSize = 12.sp)
            Text(
              text = "${String.format("%.4f", calculatedCrypto)} ${selectedCrypto.symbol}",
              fontWeight = FontWeight.Bold,
              color = EmeraldPrimary,
              fontSize = 14.sp
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(if (isRtl) "رسوم المعاملة الثابتة:" else "Fixed Flat Fee:", fontSize = 12.sp)
            Text("$${String.format("%.2f", currentFeeUsd)}", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 12.sp)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(if (isRtl) "الضمان الشرعي:" else "Ethics:", fontSize = 12.sp)
            Text("0% ربا • تقابض فوري بالأمانة", fontWeight = FontWeight.Bold, color = EmeraldLight, fontSize = 11.sp)
          }
        }
      }

      // CTA Button
      Button(
        onClick = {
          if (parsedFiat > 0) {
            onInitiateQuickDeal(
              selectedCrypto,
              selectedFiat,
              selectedTradeType,
              calculatedCrypto,
              parsedFiat
            )
            onDismiss()
          }
        },
        enabled = parsedFiat > 0,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("execute_quick_swap_btn")
      ) {
        Icon(Icons.Default.FlashOn, contentDescription = null, tint = BackgroundDark)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isRtl) "تأكيد وبدء العقد الذكي الفوري" else "Start Instant Smart Contract",
          fontWeight = FontWeight.Bold,
          color = BackgroundDark,
          fontSize = 15.sp
        )
      }
    }
  }
}
