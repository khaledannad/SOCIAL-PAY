package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CryptoAsset
import com.example.model.FiatCurrency
import com.example.model.TradeType
import com.example.ui.theme.*

@Composable
fun CreateOfferDialog(
  supportedCrypto: List<CryptoAsset>,
  supportedFiat: List<FiatCurrency>,
  isRtl: Boolean,
  onDismiss: () -> Unit,
  onPublish: (TradeType, String, String, Double, Double, Double, Double, String) -> Unit
) {
  var tradeType by remember { mutableStateOf(TradeType.BUY) }
  var selectedCryptoSymbol by remember { mutableStateOf(supportedCrypto.first().symbol) }
  var selectedFiatCode by remember { mutableStateOf(supportedFiat.first().code) }
  var priceInput by remember { mutableStateOf("3.76") }
  var amountInput by remember { mutableStateOf("1000") }
  var minLimitInput by remember { mutableStateOf("100") }
  var maxLimitInput by remember { mutableStateOf("5000") }
  var termsInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isRtl) "نشر إعلان تداول P2P جديد" else "Publish New P2P Offer",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Buy or Sell Switcher
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = tradeType == TradeType.BUY,
            onClick = { tradeType = TradeType.BUY },
            label = { Text(if (isRtl) "شراء" else "Buy") }
          )
          FilterChip(
            selected = tradeType == TradeType.SELL,
            onClick = { tradeType = TradeType.SELL },
            label = { Text(if (isRtl) "بيع" else "Sell") }
          )
        }

        // Crypto asset
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(supportedCrypto) { crypto ->
            FilterChip(
              selected = selectedCryptoSymbol == crypto.symbol,
              onClick = { selectedCryptoSymbol = crypto.symbol },
              label = { Text("${crypto.iconEmoji} ${crypto.symbol}") }
            )
          }
        }

        // Price per unit
        OutlinedTextField(
          value = priceInput,
          onValueChange = { priceInput = it },
          label = { Text(if (isRtl) "سعر الوحدة بالعملة المحلية" else "Unit Price") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Available Amount
        OutlinedTextField(
          value = amountInput,
          onValueChange = { amountInput = it },
          label = { Text(if (isRtl) "الكمية المتاحة للتداول" else "Available Amount") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Min & Max Limits
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = minLimitInput,
            onValueChange = { minLimitInput = it },
            label = { Text(if (isRtl) "الحد الأدنى" else "Min Limit") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = maxLimitInput,
            onValueChange = { maxLimitInput = it },
            label = { Text(if (isRtl) "الحد الأقصى" else "Max Limit") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        // Terms
        OutlinedTextField(
          value = termsInput,
          onValueChange = { termsInput = it },
          label = { Text(if (isRtl) "شروط التداول الخاصة بك" else "Your Terms & Notes") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val price = priceInput.toDoubleOrNull() ?: 1.0
          val amount = amountInput.toDoubleOrNull() ?: 100.0
          val minL = minLimitInput.toDoubleOrNull() ?: 10.0
          val maxL = maxLimitInput.toDoubleOrNull() ?: 1000.0
          onPublish(tradeType, selectedCryptoSymbol, selectedFiatCode, price, amount, minL, maxL, termsInput)
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
        modifier = Modifier.testTag("publish_offer_confirm_btn")
      ) {
        Text(if (isRtl) "نشر الإعلان فوراً" else "Publish Offer", color = BackgroundDark, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isRtl) "إلغاء" else "Cancel")
      }
    }
  )
}

@Composable
fun DisputeDialog(
  dealId: String,
  isRtl: Boolean,
  onDismiss: () -> Unit,
  onSubmitDispute: (String) -> Unit
) {
  val disputeReasons = if (isRtl) {
    listOf(
      "لم أستلم المبلغ في حسابي البنكي رغم تأكيد الطرف الآخر",
      "المبلغ المحول ناقص عن القيمة المتفق عليها في العقد",
      "التحويل وصل من حساب شخص ثالث غير مطابق للهوية",
      "الطرف الآخر غير متجاوب وتجاوز الوقت المحدد"
    )
  } else {
    listOf(
      "Payment not received in bank account",
      "Transferred amount is less than agreed",
      "Payment from third-party non-matching name",
      "Counterparty unresponsive after timer expired"
    )
  }

  var selectedReason by remember { mutableStateOf(disputeReasons.first()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ReportProblem, contentDescription = null, tint = StatusError)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isRtl) "رفع نزاع ذكي رسمي" else "Raise Smart Dispute", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = if (isRtl)
            "سيتم تجميد الأمانة في العقد الذكي وإحالة القضية للتحكيم المالي لفحص كشف الحساب البنكي رسمياً."
          else
            "Escrow will be frozen immediately and reviewed by financial arbitration against verified bank statements.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        disputeReasons.forEach { reason ->
          val isSelected = selectedReason == reason
          Surface(
            color = if (isSelected) StatusError.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSelected) StatusError else androidx.compose.ui.graphics.Color.Transparent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedReason = reason }
          ) {
            Text(
              text = reason,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmitDispute(selectedReason) },
        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
        modifier = Modifier.testTag("submit_dispute_confirm_btn")
      ) {
        Text(if (isRtl) "تأكيد رفع النزاع" else "Submit Dispute")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isRtl) "تراجع" else "Dismiss")
      }
    }
  )
}
