package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.governance.tr
import com.example.data.CloudLedgerTransactionRecord
import com.example.data.local.entity.TransactionEntity
import com.example.model.AppLanguage
import com.example.ui.components.QrCodeCard
import com.example.ui.theme.*
import com.example.util.SocialIntegrationEngine
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialTransactionsHistoryScreen(
  transactions: List<TransactionEntity>,
  cloudLedgerTransactions: List<CloudLedgerTransactionRecord> = emptyList(),
  currentLanguage: AppLanguage,
  onConfirmPayment: (String, String) -> Unit,
  onCancelTransaction: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()
  val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isRtl = currentLanguage.isRtl

  var searchQuery by remember { mutableStateOf("") }
  var selectedStatusFilter by remember { mutableStateOf("ALL") }
  var activeDetailTxn by remember { mutableStateOf<TransactionEntity?>(null) }

  val statusFilters = listOf(
    "ALL" to tr(currentLanguage, "الكل", "All", "Tout"),
    "PENDING" to tr(currentLanguage, "قيد الانتظار", "Pending", "En attente"),
    "AUTHORIZED" to tr(currentLanguage, "مفوّضة", "Authorized", "Autorisées"),
    "PROCESSING" to tr(currentLanguage, "قيد التنفيذ", "Processing", "En cours"),
    "SETTLED" to tr(currentLanguage, "تمت التسوية", "Settled", "Réglées"),
    "UNDER_REVIEW" to tr(currentLanguage, "قيد المراجعة", "Under Review", "En examen"),
    "CANCELLED" to tr(currentLanguage, "ملغاة", "Cancelled", "Annulées")
  )

  val filteredTransactions = transactions.filter { txn ->
    val matchesFilter = when (selectedStatusFilter) {
      "ALL" -> true
      "SETTLED" -> txn.status in setOf("SETTLED", "CONFIRMED", "COMPLETED")
      "PENDING" -> txn.status in setOf("PENDING", "CREATED", "ESCROW_LOCKED")
      else -> txn.status == selectedStatusFilter
    }
    val matchesSearch = searchQuery.isBlank() ||
      txn.id.contains(searchQuery, ignoreCase = true) ||
      txn.title.contains(searchQuery, ignoreCase = true) ||
      txn.recipientName.contains(searchQuery, ignoreCase = true) ||
      txn.currency.contains(searchQuery, ignoreCase = true)
    matchesFilter && matchesSearch
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = tr(currentLanguage, "الدفتر المالي وسجل المعاملات", "Financial Ledger & Transactions", "Registre Financier"),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = tr(
              currentLanguage,
              "تتبع حالات الطلبات، المطابقة المحاسبية، وسجل التدقيق غير القابل للتعديل",
              "Immutable payment intents, settlement proof & audit trail",
              "Suivi des règlements, réconciliation et journal d'audit"
            ),
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
        }
        Surface(color = BrandPrimaryAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
          Text(
            text = "${transactions.size}",
            color = BrandPrimaryAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(
            tr(currentLanguage, "بحث بالمعرف أو المستلم أو العملة...", "Search by ID, Recipient or Stablecoin...", "Rechercher..."),
            fontSize = 12.sp
          )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandPrimaryAccent) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear")
            }
          }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("search_transactions_input")
      )
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(statusFilters) { (filterKey, label) ->
          FilterChip(
            selected = selectedStatusFilter == filterKey,
            onClick = { selectedStatusFilter = filterKey },
            label = { Text(label, fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandPrimaryAccent,
              selectedLabelColor = BrandPrimaryBg
            )
          )
        }
      }
    }

    if (filteredTransactions.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(36.dp))
            Text(
              tr(currentLanguage, "لا توجد معاملات مطابقة", "No matching transactions found", "Aucune transaction trouvée"),
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    } else {
      items(filteredTransactions) { txn ->
        TransactionDetailCard(
          txn = txn,
          currentLanguage = currentLanguage,
          onClick = { activeDetailTxn = txn }
        )
      }
    }

    // Cloud Ledger Reconciliation Section
    if (cloudLedgerTransactions.isNotEmpty()) {
      item {
        Text(
          text = tr(
            currentLanguage,
            "سجل التسوية السحابي (${cloudLedgerTransactions.size}):",
            "Cloud Ledger Reconciliation (${cloudLedgerTransactions.size}):",
            "Registre Cloud (${cloudLedgerTransactions.size}) :"
          ),
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleSmall
        )
      }
      items(cloudLedgerTransactions.take(15), key = { it.id }) { rec ->
        Surface(
          color = BrandSecondaryBg,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, BrandSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${rec.transactionId} • ${rec.stablecoinTicker} (${rec.networkCode})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              Text(rec.state, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Text(
              text = "${String.format(Locale.US, "%,.2f", rec.amount)} ${rec.stablecoinTicker} • ${rec.recipientName}",
              fontSize = 11.sp,
              color = BrandTextPrimary
            )
            Text(
              text = tr(
                currentLanguage,
                "المطابقة: ${rec.reconciliationStatus} • الرسوم: $${String.format(Locale.US, "%.2f", rec.declaredFeeUsd)}",
                "Reconciliation: ${rec.reconciliationStatus} • Fee: $${String.format(Locale.US, "%.2f", rec.declaredFeeUsd)}",
                "Réconciliation : ${rec.reconciliationStatus}"
              ),
              fontSize = 10.sp,
              color = BrandTextSecondary
            )
          }
        }
      }
    }
  }

  activeDetailTxn?.let { txn ->
    val isSettled = txn.status in setOf("SETTLED", "CONFIRMED", "COMPLETED")
    val isCancelled = txn.status in setOf("CANCELLED", "FAILED")
    val statusColor = when {
      isSettled -> StatusSuccess
      isCancelled -> StatusError
      txn.status == "UNDER_REVIEW" -> BrandIntelligenceAccent
      else -> StatusWarning
    }

    ModalBottomSheet(
      onDismissRequest = {
        coroutineScope.launch {
          detailSheetState.hide()
          activeDetailTxn = null
        }
      },
      sheetState = detailSheetState,
      containerColor = BrandSecondaryBg
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = txn.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "#${txn.id}", fontSize = 11.sp, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold)
                IconButton(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(txn.id))
                    Toast.makeText(
                      context,
                      tr(currentLanguage, "تم نسخ المعرف", "ID copied", "ID copié"),
                      Toast.LENGTH_SHORT
                    ).show()
                  },
                  modifier = Modifier.size(20.dp)
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BrandPrimaryAccent, modifier = Modifier.size(14.dp))
                }
              }
            }
            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
              Text(
                text = txn.status,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandPrimaryBg)
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr(currentLanguage, "المبلغ:", "Amount:", "Montant :"), fontSize = 12.sp, color = BrandTextSecondary)
                Text("${String.format(Locale.US, "%,.2f", txn.amount)} ${txn.currency}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = BrandPrimaryAccent)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr(currentLanguage, "الرسوم المعلنة:", "Declared Fee:", "Frais déclarés :"), fontSize = 12.sp, color = BrandTextSecondary)
                Text("$0.80 USD", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandSecondaryAccent)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr(currentLanguage, "المستلم:", "Recipient:", "Bénéficiaire :"), fontSize = 12.sp, color = BrandTextSecondary)
                Text("${txn.recipientName} (${txn.recipientContact})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr(currentLanguage, "مزود/مسار التسوية:", "Rail / Provider:", "Canal / Réseau :"), fontSize = 12.sp, color = BrandTextSecondary)
                Text(txn.paymentProvider, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        item {
          QrCodeCard(
            qrData = txn.qrCodeData,
            title = txn.title,
            amountText = "${String.format(Locale.US, "%,.2f", txn.amount)} ${txn.currency}",
            onShare = { SocialIntegrationEngine.shareViaGenericIntent(context, txn, isRtl) },
            isRtl = isRtl
          )
        }

        item {
          Text(
            text = tr(currentLanguage, "سجل التدقيق:", "Audit Trail:", "Journal d'audit :"),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
          Surface(color = BrandPrimaryBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              txn.auditLog.split("|").map { it.trim() }.filter { it.isNotEmpty() }.forEach { logItem ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(16.dp))
                  Text(text = logItem, fontSize = 11.sp)
                }
              }
            }
          }
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isSettled && !isCancelled) {
              Button(
                onClick = {
                  onConfirmPayment(txn.id, txn.paymentProvider)
                  activeDetailTxn = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("confirm_payment_modal_btn")
              ) {
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = tr(
                    currentLanguage,
                    "اختبار تأكيد التسوية (Sandbox Proof)",
                    "Test Sandbox Settlement Proof",
                    "Tester la preuve Sandbox"
                  ),
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = {
                  clipboardManager.setText(AnnotatedString(txn.shareUrl))
                  Toast.makeText(
                    context,
                    tr(currentLanguage, "تم نسخ الرابط", "Link copied!", "Lien copié !"),
                    Toast.LENGTH_SHORT
                  ).show()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(tr(currentLanguage, "نسخ الرابط", "Copy Link", "Copier"), fontSize = 11.sp)
              }

              if (!isCancelled && !isSettled) {
                OutlinedButton(
                  onClick = {
                    onCancelTransaction(txn.id)
                    activeDetailTxn = null
                  },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text(tr(currentLanguage, "إلغاء الطلب", "Cancel", "Annuler"), fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun TransactionDetailCard(
  txn: TransactionEntity,
  currentLanguage: AppLanguage,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isSettled = txn.status in setOf("SETTLED", "CONFIRMED", "COMPLETED")
  val isCancelled = txn.status in setOf("CANCELLED", "FAILED")
  val badgeColor = when {
    isSettled -> StatusSuccess
    isCancelled -> StatusError
    txn.status == "UNDER_REVIEW" -> BrandIntelligenceAccent
    else -> StatusWarning
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(badgeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (txn.type) {
                "INVOICE" -> Icons.Default.ReceiptLong
                "BUY" -> Icons.Default.ArrowDownward
                "SELL" -> Icons.Default.ArrowUpward
                else -> Icons.Default.Link
              },
              contentDescription = null,
              tint = badgeColor,
              modifier = Modifier.size(18.dp)
            )
          }

          Column {
            Text(text = txn.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Text(text = "#${txn.id} • ${txn.channel}", fontSize = 10.sp, color = BrandTextSecondary)
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${String.format(Locale.US, "%,.2f", txn.amount)} ${txn.currency}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = BrandPrimaryAccent
          )
          Surface(color = badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
            Text(
              text = when (txn.status) {
                "SETTLED", "CONFIRMED", "COMPLETED" -> tr(currentLanguage, "تمت التسوية", "Settled", "Réglée")
                "AUTHORIZED" -> tr(currentLanguage, "مفوّضة", "Authorized", "Autorisée")
                "PROCESSING" -> tr(currentLanguage, "قيد التنفيذ", "Processing", "En cours")
                "UNDER_REVIEW" -> tr(currentLanguage, "قيد المراجعة", "Under Review", "En examen")
                "CANCELLED" -> tr(currentLanguage, "ملغاة", "Cancelled", "Annulée")
                "FAILED" -> tr(currentLanguage, "فشلت", "Failed", "Échouée")
                else -> tr(currentLanguage, "قيد الانتظار", "Pending", "En attente")
              },
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }
      }

      Text(text = txn.description, fontSize = 11.sp, color = BrandTextSecondary, maxLines = 2)
      HorizontalDivider(color = BrandSurfaceBorder.copy(alpha = 0.35f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "${txn.senderName} -> ${txn.recipientName}", fontSize = 10.sp, color = BrandTextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = tr(currentLanguage, "التفاصيل والـ QR", "Details & QR", "Détails & QR"),
            fontSize = 10.sp,
            color = BrandPrimaryAccent,
            fontWeight = FontWeight.Bold
          )
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(14.dp))
        }
      }
    }
  }
}
