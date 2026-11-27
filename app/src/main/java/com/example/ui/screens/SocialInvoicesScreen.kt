package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.governance.tr
import com.example.data.local.entity.TransactionEntity
import com.example.model.AppLanguage
import com.example.ui.components.SocialPayIconSymbol
import com.example.ui.components.SocialShareModalSheet
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun SocialInvoicesScreen(
  transactions: List<TransactionEntity>,
  currentLanguage: AppLanguage,
  flatFeeUsd: Double = 0.80,
  onNavigateToCreateInvoice: () -> Unit,
  onSelectInvoice: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var invoiceToShare by remember { mutableStateOf<TransactionEntity?>(null) }
  val isRtl = currentLanguage.isRtl

  val invoices = transactions.filter { it.type == "INVOICE" }
  val settledStates = setOf("SETTLED", "CONFIRMED", "COMPLETED")
  val pendingStates = setOf("CREATED", "PENDING", "AUTHORIZED", "PROCESSING", "UNDER_REVIEW", "ESCROW_LOCKED")

  val totalIssued = invoices.sumOf { it.amount }
  val totalCollected = invoices.filter { it.status in settledStates }.sumOf { it.amount }
  val pendingCount = invoices.count { it.status in pendingStates }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
          SocialPayIconSymbol(size = 32.dp)
          Column {
            Text(
              text = tr(currentLanguage, "الفواتير الرسمية وسندات السداد", "Official Invoices & Billing", "Factures Officielles"),
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium
            )
            Text(
              text = tr(currentLanguage, "إصدار وتحصيل الفواتير بالعملات المستقرة", "Issue and track stablecoin invoices", "Émettre et suivre les factures"),
              fontSize = 11.sp,
              color = BrandTextSecondary
            )
          }
        }
        Button(
          onClick = onNavigateToCreateInvoice,
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("create_invoice_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(tr(currentLanguage, "فاتورة جديدة", "New Invoice", "Nouvelle"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InvoiceMetricCard(
          title = tr(currentLanguage, "المحصل بنجاح", "Settled", "Réglées"),
          amountText = "${String.format(Locale.US, "%,.2f", totalCollected)}",
          icon = Icons.Default.CheckCircle,
          color = StatusSuccess,
          modifier = Modifier.weight(1f)
        )
        InvoiceMetricCard(
          title = tr(currentLanguage, "إجمالي المصدر", "Total Issued", "Total émis"),
          amountText = "${String.format(Locale.US, "%,.2f", totalIssued)}",
          icon = Icons.Default.ReceiptLong,
          color = BrandPrimaryAccent,
          modifier = Modifier.weight(1f)
        )
        InvoiceMetricCard(
          title = tr(currentLanguage, "قيد الانتظار", "Pending", "En attente"),
          amountText = "$pendingCount",
          icon = Icons.Default.HourglassEmpty,
          color = StatusWarning,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Text(
        text = tr(currentLanguage, "قائمة الفواتير:", "Invoices Ledger:", "Registre des factures :"),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall
      )
    }

    if (invoices.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(36.dp))
            Text(tr(currentLanguage, "لا توجد فواتير مصدرة بعد", "No invoices issued yet", "Aucune facture émise"), fontWeight = FontWeight.Bold)
            Button(
              onClick = onNavigateToCreateInvoice,
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg)
            ) {
              Text(tr(currentLanguage, "إصدار أول فاتورة الآن", "Issue First Invoice", "Créer une facture"), fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else {
      items(invoices) { inv ->
        val isPaid = inv.status in settledStates
        InvoiceListItemCard(
          invoice = inv,
          isPaid = isPaid,
          flatFeeUsd = flatFeeUsd,
          currentLanguage = currentLanguage,
          onClick = { onSelectInvoice(inv) },
          onShare = { invoiceToShare = inv },
          onCopyLink = {
            clipboardManager.setText(AnnotatedString(inv.shareUrl))
            Toast.makeText(
              context,
              tr(currentLanguage, "تم نسخ رابط الفاتورة", "Invoice link copied!", "Lien copié !"),
              Toast.LENGTH_SHORT
            ).show()
          }
        )
      }
    }
  }

  invoiceToShare?.let { inv ->
    SocialShareModalSheet(
      transaction = inv,
      isRtl = isRtl,
      onDismiss = { invoiceToShare = null }
    )
  }
}

@Composable
fun InvoiceMetricCard(
  title: String,
  amountText: String,
  icon: ImageVector,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
      Text(text = amountText, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
      Text(text = title, fontSize = 10.sp, color = BrandTextSecondary)
    }
  }
}

@Composable
fun InvoiceListItemCard(
  invoice: TransactionEntity,
  isPaid: Boolean,
  flatFeeUsd: Double,
  currentLanguage: AppLanguage,
  onClick: () -> Unit,
  onShare: () -> Unit,
  onCopyLink: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier
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
              .background(BrandPrimaryAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(18.dp))
          }
          Column {
            Text(text = invoice.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Text(
              text = "#${invoice.id} • ${invoice.recipientName}",
              fontSize = 10.sp,
              color = BrandTextSecondary
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${String.format(Locale.US, "%,.2f", invoice.amount)} ${invoice.currency}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = BrandPrimaryAccent
          )
          Surface(
            color = if (isPaid) StatusSuccess.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = if (isPaid) tr(currentLanguage, "مدفوعة بالكامل", "Paid", "Payée") else tr(currentLanguage, "بانتظار السداد", "Pending", "En attente"),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (isPaid) StatusSuccess else StatusWarning,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }
      }

      Text(text = invoice.description, fontSize = 11.sp, color = BrandTextSecondary, maxLines = 1)
      HorizontalDivider(color = BrandSurfaceBorder.copy(alpha = 0.3f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = tr(
            currentLanguage,
            "الرسوم المعلنة: $${String.format(Locale.US, "%.2f", flatFeeUsd)}",
            "Declared Fee: $${String.format(Locale.US, "%.2f", flatFeeUsd)}",
            "Frais : $${String.format(Locale.US, "%.2f", flatFeeUsd)}"
          ),
          fontSize = 10.sp,
          color = BrandSecondaryAccent
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(onClick = onCopyLink, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandPrimaryAccent)
            Spacer(modifier = Modifier.width(3.dp))
            Text(tr(currentLanguage, "نسخ", "Copy", "Copier"), fontSize = 10.sp, color = BrandPrimaryAccent)
          }
          Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(tr(currentLanguage, "مشاركة", "Share", "Partager"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
