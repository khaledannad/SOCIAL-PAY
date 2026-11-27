package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserBankAccount
import com.example.model.UserOfficeProfile
import com.example.ui.theme.*
import com.example.util.ParsedPaymentRequest
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletQrRequestSheet(
  profile: UserOfficeProfile,
  isRtl: Boolean,
  onDismiss: () -> Unit,
  onSavePaymentRequest: (
    assetSymbol: String,
    network: String,
    amount: Double,
    currency: String,
    walletOrRailAddress: String,
    memo: String,
    paymentUri: String
  ) -> Unit,
  onPayFromDecodedQr: (ParsedPaymentRequest) -> Unit
) {
  val context = LocalContext.current
  var activeTab by remember { mutableStateOf(0) } // 0 = Receive Wallet QR, 1 = Scan / Pay P2P QR

  val walletPresets = remember {
    listOf(
      Triple("USDT", "TRC20", "USDT"),
      Triple("USDT", "Polygon", "USDT"),
      Triple("USDC", "Solana", "USDC"),
      Triple("BTC", "Bitcoin", "BTC"),
      Triple("ETH", "ERC20", "ETH"),
      Triple("SAR", "IBAN / Instant", "SAR")
    )
  }

  var selectedPresetIndex by remember { mutableStateOf(0) }
  var selectedLinkedAccount by remember {
    mutableStateOf<UserBankAccount?>(profile.linkedBankAccounts.firstOrNull { it.isDefault } ?: profile.linkedBankAccounts.firstOrNull())
  }
  val defaultWalletAddress = remember(profile.userId, selectedPresetIndex, selectedLinkedAccount) {
    val preset = walletPresets[selectedPresetIndex]
    if (preset.first == "SAR" && selectedLinkedAccount != null) {
      selectedLinkedAccount!!.accountNumberOrIban
    } else {
      val uidTag = profile.userId.takeLast(8).uppercase().ifBlank { "TRADER01" }
      when (preset.second) {
        "TRC20" -> "T${uidTag}SPAY92TRC20NONCUSTODIAL"
        "Polygon", "ERC20" -> "0x${uidTag}9F42E1SOCIALPAY0000"
        "Solana" -> "So1${uidTag}SPAY99NONCUSTODIAL"
        "Bitcoin" -> "bc1q${uidTag.lowercase()}socialpay001"
        else -> selectedLinkedAccount?.accountNumberOrIban ?: "SPAY-$uidTag"
      }
    }
  }

  var walletAddressInput by remember(defaultWalletAddress) { mutableStateOf(defaultWalletAddress) }
  var amountInput by remember { mutableStateOf("250") }
  var memoInput by remember { mutableStateOf("") }
  val requestRef = remember { "REQ-" + (System.currentTimeMillis() % 1000000L) }

  val currentPreset = walletPresets[selectedPresetIndex]
  val livePaymentUri = remember(
    profile.fullName,
    currentPreset,
    walletAddressInput,
    amountInput,
    memoInput,
    requestRef
  ) {
    QrCodeGenerator.buildWalletPaymentRequestUri(
      recipientName = profile.fullName.ifBlank { "Social Pay Trader" },
      assetSymbol = currentPreset.first,
      network = currentPreset.second,
      walletOrRailAddress = walletAddressInput,
      amount = amountInput.toDoubleOrNull(),
      currency = currentPreset.third,
      memo = memoInput,
      referenceId = requestRef
    )
  }

  // Decode QR tab state
  var scannedUriInput by remember { mutableStateOf("") }
  var decodedRequest by remember { mutableStateOf<ParsedPaymentRequest?>(null) }
  var decodeError by remember { mutableStateOf<String?>(null) }

  val qrPhotoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val decoded = QrCodeGenerator.decodeQrFromImageUri(context, uri)
      decoded.onSuccess { text ->
        scannedUriInput = text
        val parsed = QrCodeGenerator.parsePaymentRequestUri(text)
        if (parsed != null) {
          decodedRequest = parsed
          decodeError = null
        } else {
          decodedRequest = null
          decodeError = if (isRtl) "تمت قراءة النص ($text) لكنه لا يطابق صيغة طلب دفع Social Pay." else "Decoded QR text ($text) is not a valid payment request URI."
        }
      }.onFailure {
        decodeError = if (isRtl) "لم يتم العثور على رمز QR واضح داخل الصورة المختارة." else "No readable QR code found in the selected image."
      }
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = BrandSecondaryBg,
    contentColor = BrandTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.QrCode2, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(24.dp))
          Column {
            Text(
              text = if (isRtl) "رموز الدفع وطلبات P2P عبر ZXing" else "ZXing Wallet QR & P2P Requests",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 16.sp
            )
            Text(
              text = if (isRtl) "إنشاء رمز قابل للمسح أو فحص طلب دفع من نظير" else "Generate scannable payment requests or scan peer QR",
              fontSize = 11.sp,
              color = BrandTextSecondary
            )
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      TabRow(
        selectedTabIndex = activeTab,
        containerColor = BrandPrimaryBg,
        contentColor = BrandPrimaryAccent
      ) {
        Tab(
          selected = activeTab == 0,
          onClick = { activeTab = 0 },
          modifier = Modifier.testTag("wallet_qr_receive_tab"),
          text = {
            Text(
              if (isRtl) "استلام (إنشاء QR)" else "Receive (My QR)",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        )
        Tab(
          selected = activeTab == 1,
          onClick = { activeTab = 1 },
          modifier = Modifier.testTag("wallet_qr_scan_tab"),
          text = {
            Text(
              if (isRtl) "دفع (فحص QR)" else "Pay (Import QR)",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        )
      }

      if (activeTab == 0) {
        Text(
          text = if (isRtl) "اختر الأصل أو القناة:" else "Select Asset or Payout Rail:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = BrandTextSecondary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(walletPresets.indices.toList()) { idx ->
            val item = walletPresets[idx]
            FilterChip(
              selected = selectedPresetIndex == idx,
              onClick = { selectedPresetIndex = idx },
              label = { Text("${item.first} • ${item.second}", fontSize = 11.sp) }
            )
          }
        }

        if (profile.linkedBankAccounts.isNotEmpty() && currentPreset.first == "SAR") {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(profile.linkedBankAccounts) { acc ->
              FilterChip(
                selected = selectedLinkedAccount?.id == acc.id,
                onClick = {
                  selectedLinkedAccount = acc
                  walletAddressInput = acc.accountNumberOrIban
                },
                label = { Text("${acc.bankName} (${acc.currency})", fontSize = 11.sp) }
              )
            }
          }
        }

        OutlinedTextField(
          value = walletAddressInput,
          onValueChange = { walletAddressInput = it },
          label = { Text(if (isRtl) "عنوان المحفظة أو رقم الحساب المستلم (غير حافظ)" else "Receiving Wallet Address / IBAN (Non-Custodial)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("wallet_qr_address_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text(if (isRtl) "المبلغ (${currentPreset.third})" else "Amount (${currentPreset.third})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("wallet_qr_amount_input")
          )
          OutlinedTextField(
            value = memoInput,
            onValueChange = { memoInput = it },
            label = { Text(if (isRtl) "ملاحظة (اختياري)" else "Memo (Optional)") },
            singleLine = true,
            modifier = Modifier
              .weight(1.2f)
              .testTag("wallet_qr_memo_input")
          )
        }

        QrCodeCard(
          qrData = livePaymentUri,
          title = "${currentPreset.first} (${currentPreset.second})",
          amountText = "${amountInput.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "Open"} ${currentPreset.third}",
          isRtl = isRtl,
          onShare = {
            val intent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(
                Intent.EXTRA_TEXT,
                "Social Pay P2P Payment Request\nRecipient: ${profile.fullName}\nAsset: ${currentPreset.first} (${currentPreset.second})\nAmount: ${amountInput.ifBlank { "Open" }} ${currentPreset.third}\nLink: $livePaymentUri"
              )
            }
            context.startActivity(Intent.createChooser(intent, if (isRtl) "مشاركة طلب الدفع" else "Share Payment Request"))
          }
        )

        Button(
          onClick = {
            val amt = amountInput.toDoubleOrNull() ?: 0.0
            if (amt <= 0.0) {
              Toast.makeText(context, if (isRtl) "أدخل مبلغاً أكبر من صفر لحفظ الطلب" else "Enter an amount > 0", Toast.LENGTH_SHORT).show()
            } else {
              onSavePaymentRequest(
                currentPreset.first,
                currentPreset.second,
                amt,
                currentPreset.third,
                walletAddressInput,
                memoInput,
                livePaymentUri
              )
              onDismiss()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag("save_wallet_qr_request_btn")
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (isRtl) "تسجيل طلب الدفع في السجل والمشاركة" else "Register P2P Request in Ledger",
            fontWeight = FontWeight.Bold
          )
        }
      } else {
        // Tab 1: Pick QR Image or Paste URI to decode via ZXing MultiFormatReader
        Button(
          onClick = {
            qrPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandSecondaryAccent, contentColor = BrandPrimaryBg),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag("pick_qr_image_btn")
        ) {
          Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (isRtl) "فحص صورة QR من المعرض (ZXing Decoder)" else "Decode QR Image from Photos (Zero Permission)",
            fontWeight = FontWeight.Bold
          )
        }

        OutlinedTextField(
          value = scannedUriInput,
          onValueChange = { raw ->
            scannedUriInput = raw
            val parsed = QrCodeGenerator.parsePaymentRequestUri(raw)
            decodedRequest = parsed
            decodeError = if (raw.isNotBlank() && parsed == null) {
              if (isRtl) "صيغة الرابط غير صالحة أو غير مدعومة" else "Invalid or unsupported payment URI"
            } else null
          },
          label = { Text(if (isRtl) "أو الصق رابط الدفع socialpay://pay?..." else "Or paste scanned payment URI (socialpay://pay?...)") },
          minLines = 2,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("paste_qr_uri_input")
        )

        decodeError?.let { err ->
          Surface(
            color = StatusError.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(err, color = StatusError, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
          }
        }

        decodedRequest?.let { req ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandPrimaryBg),
            border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = if (isRtl) "تفاصيل طلب الدفع المستخرج:" else "Decoded Payment Request:",
                fontWeight = FontWeight.Bold,
                color = BrandPrimaryAccent,
                fontSize = 13.sp
              )
              Text("${if (isRtl) "المستلم:" else "Recipient:"} ${req.recipientName.ifBlank { "Peer Trader" }}", fontSize = 12.sp)
              Text("${if (isRtl) "الأصل والشبكة:" else "Asset & Network:"} ${req.assetSymbol} (${req.network})", fontSize = 12.sp)
              Text(
                "${if (isRtl) "المبلغ:" else "Amount:"} ${req.amount?.let { "$it ${req.currency}" } ?: (if (isRtl) "مبلغ مفتوح" else "Open amount")}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
              )
              if (req.walletOrRailAddress.isNotBlank()) {
                Text("${if (isRtl) "العنوان:" else "Address:"} ${req.walletOrRailAddress}", fontSize = 11.sp, color = BrandTextSecondary)
              }
              if (req.memo.isNotBlank()) {
                Text("${if (isRtl) "ملاحظة:" else "Memo:"} ${req.memo}", fontSize = 11.sp, color = BrandTextSecondary)
              }
              Spacer(modifier = Modifier.height(6.dp))
              Button(
                onClick = {
                  onPayFromDecodedQr(req)
                  onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("confirm_decoded_qr_pay_btn")
              ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isRtl) "فتح معاملة دفع P2P لهذا الرمز" else "Proceed to P2P Payment", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
