package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EscrowStatus
import com.example.model.SmartDeal
import com.example.model.TradeType
import com.example.security.AntiFraudEngine
import com.example.security.AntiFraudReceiptScannerDialog
import com.example.security.SmartAuthDialog
import com.example.ui.theme.*

@Composable
fun SmartDealDetailScreen(
  deal: SmartDeal,
  isRtl: Boolean,
  onBack: () -> Unit,
  onMarkPaid: (String) -> Unit,
  onReleaseEscrow: (String) -> Unit,
  onCancelDeal: (String) -> Unit,
  onOpenDispute: (String) -> Unit,
  onSendMessage: (String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var chatInput by remember { mutableStateOf("") }
  var showReleaseConfirmDialog by remember { mutableStateOf(false) }
  var showCancelConfirmDialog by remember { mutableStateOf(false) }
  var showReceiptScannerDialog by remember { mutableStateOf(false) }
  var showSmartAuthForRelease by remember { mutableStateOf(false) }
  var chatPhishingWarning by remember { mutableStateOf<String?>(null) }

  val listState = rememberLazyListState()

  Scaffold(
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("deal_detail_back_btn")
              ) {
                Icon(
                  imageVector = if (isRtl) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                  contentDescription = "Back"
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "صفقة ذكية #${deal.dealId.takeLast(6)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    color = EmeraldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "0% ربا",
                      color = EmeraldPrimary,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  text = "${if (isRtl) "الطرف الآخر:" else "Counterparty:"} ${deal.counterpartyName}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Expiry Timer Pill
            Surface(
              color = StatusWarning.copy(alpha = 0.15f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.4f))
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Timer,
                  contentDescription = "Timer",
                  tint = StatusWarning,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val minutes = deal.expirySecondsRemaining / 60
                val seconds = deal.expirySecondsRemaining % 60
                Text(
                  text = String.format("%02d:%02d", minutes, seconds),
                  fontWeight = FontWeight.Bold,
                  color = StatusWarning,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }
    },
    bottomBar = {
      // Bottom Action Bar based on Deal Status
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .navigationBarsPadding()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          when (deal.status) {
            EscrowStatus.LOCKED_IN_ESCROW -> {
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                OutlinedButton(
                  onClick = { showCancelConfirmDialog = true },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("deal_cancel_btn")
                ) {
                  Text(if (isRtl) "إلغاء الصفقة" else "Cancel")
                }

                Button(
                  onClick = { onMarkPaid(deal.dealId) },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(2f)
                    .testTag("deal_mark_paid_btn")
                ) {
                  Icon(Icons.Default.Check, contentDescription = "Paid", tint = BackgroundDark)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isRtl) "أنا قمت بالدفع (إشعار البائع)" else "I Have Paid",
                    color = BackgroundDark,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            EscrowStatus.PAID_PENDING_CONFIRMATION -> {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                  onClick = { showReleaseConfirmDialog = true },
                  colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deal_release_escrow_btn")
                ) {
                  Icon(Icons.Default.LockOpen, contentDescription = "Release", tint = BackgroundDark)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (isRtl) "تأكيد الاستلام في البنك وتحرير الأمانة" else "Confirm Bank Receipt & Release",
                    color = BackgroundDark,
                    fontWeight = FontWeight.Bold
                  )
                }

                OutlinedButton(
                  onClick = { onOpenDispute(deal.dealId) },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusWarning),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deal_open_dispute_btn")
                ) {
                  Icon(Icons.Default.ReportProblem, contentDescription = "Dispute", tint = StatusWarning)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(if (isRtl) "لم أستلم المبلغ؟ فتح نزاع ذكي" else "Payment not received? Open Dispute")
                }
              }
            }

            EscrowStatus.RELEASED_COMPLETED -> {
              Surface(
                color = StatusSuccess.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(Icons.Default.Verified, contentDescription = "Completed", tint = StatusSuccess)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (isRtl) "تم تحرير الأمانة واكتمال الصفقة بنجاح 100%" else "Completed & Released 100%",
                    fontWeight = FontWeight.Bold,
                    color = StatusSuccess
                  )
                }
              }
            }

            EscrowStatus.DISPUTED -> {
              Surface(
                color = StatusError.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, contentDescription = "Dispute", tint = StatusError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = if (isRtl) "الصفقة قيد التحكيم والنزاع الذكي" else "Deal Under Smart Arbitration",
                      fontWeight = FontWeight.Bold,
                      color = StatusError
                    )
                  }
                  Text(
                    text = if (isRtl)
                      "الأمانة مجمدة بأمان وسيتم مراجعة كشف الحساب البنكي رسمياً."
                    else
                      "Escrow safely frozen. Bank statement review in progress.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }

            EscrowStatus.CANCELLED -> {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(Icons.Default.Cancel, contentDescription = "Cancelled", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (isRtl) "تم إلغاء هذه الصفقة بأمان" else "Deal Cancelled",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
    ) {
      // 0. Anti-Phishing Hologram & Secret Watermark
      item {
        Surface(
          color = GoldAccent.copy(alpha = 0.12f),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = if (isRtl) "العلامة المائية المانعة للتصيد (Anti-Phishing Seal):" else "Anti-Phishing Secret Watermark:",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (isRtl) "إذا غاب هذا الرمز عن أي رسالة رسمية، فهو محتال قطعاً!" else "If this secret word is missing, it is a scam!",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Surface(
              color = BackgroundDark,
              shape = RoundedCornerShape(6.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
            ) {
              Text(
                text = "درع_الأمان_الخالد_2026",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = GoldAccent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      // 1. Escrow Financial Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Status Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (deal.tradeType == TradeType.BUY) {
                  if (isRtl) "أنت تشتري" else "You are Buying"
                } else {
                  if (isRtl) "أنت تبيع" else "You are Selling"
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Surface(
                color = when (deal.status) {
                  EscrowStatus.LOCKED_IN_ESCROW -> GoldAccent.copy(alpha = 0.15f)
                  EscrowStatus.PAID_PENDING_CONFIRMATION -> CyberBlue.copy(alpha = 0.15f)
                  EscrowStatus.RELEASED_COMPLETED -> StatusSuccess.copy(alpha = 0.15f)
                  EscrowStatus.DISPUTED -> StatusError.copy(alpha = 0.15f)
                  EscrowStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = if (isRtl) deal.status.labelAr else deal.status.labelEn,
                  color = when (deal.status) {
                    EscrowStatus.LOCKED_IN_ESCROW -> GoldAccent
                    EscrowStatus.PAID_PENDING_CONFIRMATION -> CyberBlue
                    EscrowStatus.RELEASED_COMPLETED -> StatusSuccess
                    EscrowStatus.DISPUTED -> StatusError
                    EscrowStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Amount
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom
            ) {
              Column {
                Text(
                  text = if (isRtl) "إجمالي المبلغ المطلوب تحويله" else "Total Fiat Amount",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "${String.format("%,.2f", deal.fiatAmount)} ${deal.fiatCode}",
                  fontSize = 24.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = if (isRtl) "الكمية المحجوزة بالأمانة" else "Crypto in Escrow",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "${String.format("%.2f", deal.cryptoAmount)} ${deal.assetSymbol}",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldPrimary
                )
              }
            }

            Divider(
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
              modifier = Modifier.padding(vertical = 12.dp)
            )

            // Flat Fee Breakdown
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = if (isRtl) "رسوم المنصة الثابتة للمشروع:" else "Platform Fixed Flat Fee:",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "$${String.format("%.2f", deal.fixedFeeUsd)} ثابتة",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = GoldAccent
                )
              }

              Text(
                text = "${deal.unitPrice} ${deal.fiatCode}/${deal.assetSymbol}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // 2. Transaction Intelligence & AI Risk Engine Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Psychology, contentDescription = "AI Risk", tint = EmeraldPrimary)
              Text(
                text = if (isRtl) "ذكاء المعاملات ومؤشر الأمان (AI Risk Shield)" else "AI Transaction Intelligence",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = "✓ " + if (isRtl) "مطابقة هوية الحساب البنكي 100%" else "Bank Name Match 100%",
                  fontSize = 11.sp,
                  color = EmeraldPrimary,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "✓ " + if (isRtl) "حماية من الاحتيال والتثليث (Triangulation)" else "Anti-Triangulation Shield",
                  fontSize = 11.sp,
                  color = EmeraldPrimary,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "✓ " + if (isRtl) "معاملة شرعية 0% ربا وتقابض فوري" else "0% Riba Spot Handover",
                  fontSize = 11.sp,
                  color = GoldAccent,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "✓ " + if (isRtl) "حجز تأميني في خزينة العقد الذكي" else "Collateral Locked in Escrow",
                  fontSize = 11.sp,
                  color = GoldAccent,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = { showReceiptScannerDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary.copy(alpha = 0.2f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("open_receipt_scanner_btn")
            ) {
              Icon(
                Icons.Default.DocumentScanner,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isRtl) "🔍 فحص إشعار التحويل البنكي بالذكاء الاصطناعي (AI Forensic)" else "Scan Bank Receipt with AI",
                color = EmeraldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // 3. Smart Checklist (الشروط الذكية الخمسة)
      item {
        Text(
          text = if (isRtl) "الشروط الذكية لإتمام الصفقة:" else "Smart Deal Conditions:",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ConditionItem(
            title = if (isRtl) "حجز الأمانة المشفرة في العقد الذكي" else "Crypto Locked in Smart Escrow",
            isDone = deal.sellerDepositLocked,
            detail = "${deal.cryptoAmount} ${deal.assetSymbol} " + if (isRtl) "محجوزة بأمان ولا يمكن سحبها" else "locked"
          )
          ConditionItem(
            title = if (isRtl) "تحويل المشتري للمبلغ البنكي وإرسال الإشعار" else "Buyer Bank Transfer & Proof",
            isDone = deal.buyerPaidProofAttached || deal.status != EscrowStatus.LOCKED_IN_ESCROW,
            detail = if (deal.buyerPaidProofAttached) (if (isRtl) "تم تأكيد التحويل بنجاح" else "Proof attached") else (if (isRtl) "بانتظار تحويل المشتري" else "Waiting for buyer")
          )
          ConditionItem(
            title = if (isRtl) "تأكيد البائع وصول الأموال لحسابه الرسمي" else "Seller Bank Confirmation",
            isDone = deal.sellerConfirmedReceived || deal.status == EscrowStatus.RELEASED_COMPLETED,
            detail = if (deal.sellerConfirmedReceived) (if (isRtl) "تم التحقق الفعلي من الإيداع" else "Deposit confirmed") else (if (isRtl) "بانتظار مراجعة البائع" else "Awaiting seller check")
          )
          ConditionItem(
            title = if (isRtl) "التحرير الفوري للأمانة بدون ربا وبلا تأخير" else "Instant Spot Asset Release",
            isDone = deal.status == EscrowStatus.RELEASED_COMPLETED,
            detail = if (deal.status == EscrowStatus.RELEASED_COMPLETED) (if (isRtl) "تم التحرير فوراً للمحفظة" else "Released to wallet") else (if (isRtl) "يتم فور تأكيد البائع" else "Executes instantly upon confirmation")
          )
        }
      }

      // 4. Real-time Deal Chat
      item {
        Text(
          text = if (isRtl) "المحادثة الفورية المشفرة للصفقة:" else "Encrypted Deal Chat:",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
      }

      // Quick Response Chips
      item {
        val quickChips = if (isRtl) {
          listOf(
            "تم تحويل المبلغ عبر الحساب البنكي 💸",
            "أرجو تزويدي بالآيبان أو رقم الحساب البنكي",
            "تم إرفاق إشعار الدفع، تفضل بالمراجعة",
            "أنا أراجع الحساب البنكي حالياً وسأحرر فوراً"
          )
        } else {
          listOf(
            "Payment sent via bank transfer 💸",
            "Please send your IBAN / Account info",
            "Proof attached, please review",
            "Checking my bank now, releasing shortly"
          )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(quickChips) { chipText ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.clickable {
                onSendMessage(deal.dealId, chipText)
              }
            ) {
              Text(
                text = chipText,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }
      }

      // Chat Messages List
      items(deal.chatHistory) { msg ->
        if (msg.isSystem) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "System",
                tint = EmeraldPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = msg.text,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
          ) {
            Surface(
              color = if (msg.isMe) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.widthIn(max = 280.dp)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = msg.senderName,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (msg.isMe) EmeraldLight else GoldAccent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = msg.text,
                  fontSize = 13.sp,
                  color = Color.White
                )
                Text(
                  text = msg.timestampText,
                  fontSize = 9.sp,
                  color = Color.White.copy(alpha = 0.6f),
                  modifier = Modifier.align(Alignment.End)
                )
              }
            }
          }
        }
      }

      // Chat Input Field
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          chatPhishingWarning?.let { warningText ->
            Surface(
              color = StatusError.copy(alpha = 0.15f),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = warningText,
                  color = StatusError,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = { chatPhishingWarning = null },
                  modifier = Modifier.size(20.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Close", tint = StatusError, modifier = Modifier.size(14.dp))
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = chatInput,
              onValueChange = {
                chatInput = it
                if (chatPhishingWarning != null) chatPhishingWarning = null
              },
              placeholder = { Text(if (isRtl) "اكتب رسالة للطرف الآخر..." else "Type message...", fontSize = 12.sp) },
              singleLine = true,
              shape = RoundedCornerShape(20.dp),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions = KeyboardActions(onSend = {
                if (chatInput.isNotBlank()) {
                  val check = AntiFraudEngine.inspectChatMessageForPhishing(chatInput)
                  if (check.isDangerous) {
                    chatPhishingWarning = if (isRtl) check.warningAr else check.warningEn
                  } else {
                    chatPhishingWarning = null
                    onSendMessage(deal.dealId, chatInput)
                    chatInput = ""
                  }
                }
              }),
              modifier = Modifier
                .weight(1f)
                .testTag("deal_chat_input")
            )

            IconButton(
              onClick = {
                if (chatInput.isNotBlank()) {
                  val check = AntiFraudEngine.inspectChatMessageForPhishing(chatInput)
                  if (check.isDangerous) {
                    chatPhishingWarning = if (isRtl) check.warningAr else check.warningEn
                  } else {
                    chatPhishingWarning = null
                    onSendMessage(deal.dealId, chatInput)
                    chatInput = ""
                  }
                }
              },
              modifier = Modifier
                .clip(CircleShape)
                .background(EmeraldPrimary)
                .testTag("deal_chat_send_btn")
            ) {
              Icon(Icons.Default.Send, contentDescription = "Send", tint = BackgroundDark)
            }
          }
        }
      }
    }
  }

  // Confirmation Dialogs
  if (showReleaseConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showReleaseConfirmDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = "Warning", tint = GoldAccent)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isRtl) "تأكيد تحرير الأمانة" else "Confirm Escrow Release")
        }
      },
      text = {
        Text(
          text = if (isRtl)
            "هل تأكدت بالفعل من دخول مبلغ ${deal.fiatAmount} ${deal.fiatCode} في حسابك البنكي شخصياً؟ لا تعتمد على لقطات الشاشة بل تحقق من تطبيق البنك الخاص بك. ستتطلب هذه العملية مصادقة ذكية (بصمة / PIN مشفر / Passkey) لإتمام التحرير."
          else
            "Have you personally verified the receipt of ${deal.fiatAmount} ${deal.fiatCode} in your official bank account? Never rely on screenshots alone. This operation will require Smart Multi-Modal Authentication."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showReleaseConfirmDialog = false
            showSmartAuthForRelease = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
          Text(if (isRtl) "المتابعة للمصادقة الذكية 🔐" else "Proceed to Smart Auth 🔐", color = BackgroundDark, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showReleaseConfirmDialog = false }) {
          Text(if (isRtl) "إلغاء" else "Cancel")
        }
      }
    )
  }

  if (showCancelConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showCancelConfirmDialog = false },
      title = { Text(if (isRtl) "إلغاء الصفقة" else "Cancel Deal") },
      text = {
        Text(
          text = if (isRtl)
            "إذا لم تكن قد حولت أي أموال للطرف الآخر، يمكنك إلغاء الصفقة وسيتم تحرير الأمانة للبائع بأمان."
          else
            "If you have not sent any money yet, you may safely cancel this deal and the escrow will be returned to the seller."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showCancelConfirmDialog = false
            onCancelDeal(deal.dealId)
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusError)
        ) {
          Text(if (isRtl) "تأكيد الإلغاء" else "Confirm Cancel")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelConfirmDialog = false }) {
          Text(if (isRtl) "رجوع" else "Back")
        }
      }
    )
  }

  // 5. Anti-Fraud AI Receipt Scanner Dialog
  if (showReceiptScannerDialog) {
    AntiFraudReceiptScannerDialog(
      expectedName = deal.counterpartyName,
      dealAmount = deal.fiatAmount,
      fiatCurrency = deal.fiatCode,
      isRtl = isRtl,
      onDismiss = { showReceiptScannerDialog = false }
    )
  }

  // 6. Smart Multi-Modal Auth Dialog before release
  if (showSmartAuthForRelease) {
    SmartAuthDialog(
      title = if (isRtl) "مصادقة ذكية لتحرير الأمانة" else "Smart Auth: Escrow Release",
      subtitle = if (isRtl)
        "تحرير ${deal.cryptoAmount} ${deal.assetSymbol} بقيمة ${deal.fiatAmount} ${deal.fiatCode}. اختر وسيلة المصادقة لإثبات ملكيتك."
      else
        "Release ${deal.cryptoAmount} ${deal.assetSymbol} (${deal.fiatAmount} ${deal.fiatCode}). Authenticate to confirm ownership.",
      isRtl = isRtl,
      onAuthSuccess = {
        showSmartAuthForRelease = false
        onReleaseEscrow(deal.dealId)
      },
      onDismiss = { showSmartAuthForRelease = false }
    )
  }
}

@Composable
fun ConditionItem(
  title: String,
  isDone: Boolean,
  detail: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isDone) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    ),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isDone) Icons.Default.Check else Icons.Default.HourglassEmpty,
          contentDescription = null,
          tint = if (isDone) BackgroundDark else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = detail,
          style = MaterialTheme.typography.labelSmall,
          color = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
