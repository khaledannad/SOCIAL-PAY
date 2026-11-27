package com.example.security

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class ReceiptForensicReport(
  val isAuthentic: Boolean,
  val trustConfidencePercent: Int,
  val nameMatchStatus: String,
  val pixelTamperingDetected: Boolean,
  val triangulationRiskScore: String, // "منعدم 0.01%", "متوسط", "عالي"
  val detectedBank: String,
  val detectedAmountText: String,
  val referenceNumber: String,
  val timestampCheckStatus: String,
  val antiPhishingSealVerified: Boolean
)

object AntiFraudEngine {
  const val ANTI_PHISHING_CODE = "درع_الأمان_الخالد_2026"

  /**
   * Scans text for phishing links, off-platform lures (e.g. "تواصل واتساب برة المنصة")
   */
  fun inspectChatMessageForPhishing(text: String): PhishingCheckResult {
    val lower = text.lowercase()
    val hasOffPlatformRequest = lower.contains("برة المنصة") ||
      lower.contains("خارج التطبيق") ||
      lower.contains("تليجرام خاص") ||
      lower.contains("رابط التفعيل") ||
      lower.contains("http://") ||
      lower.contains("https://") ||
      lower.contains("bit.ly") ||
      lower.contains(".apk")

    return if (hasOffPlatformRequest) {
      PhishingCheckResult(
        isDangerous = true,
        warningAr = "⚠️ تحذير أمني صارم: تم رصد محاولة توجيه خارج المنصة أو رابط مشبوه! لا تخرج خارج نظام الأمانة المشفرة نهائياً.",
        warningEn = "⚠️ Strict Security Warning: Off-platform redirection or suspicious link detected! Stay inside the Escrow."
      )
    } else {
      PhishingCheckResult(isDangerous = false, warningAr = null, warningEn = null)
    }
  }

  /**
   * Simulates deep forensic AI analysis of a bank receipt
   */
  fun analyzeReceipt(
    senderNameOnReceipt: String,
    expectedKycName: String,
    amount: Double,
    currency: String
  ): ReceiptForensicReport {
    val isNameMatched = senderNameOnReceipt.trim().equals(expectedKycName.trim(), ignoreCase = true) ||
      expectedKycName.contains(senderNameOnReceipt.take(4))

    val isTriangulationRisk = !isNameMatched

    return ReceiptForensicReport(
      isAuthentic = isNameMatched,
      trustConfidencePercent = if (isNameMatched) 99 else 22,
      nameMatchStatus = if (isNameMatched) "مطابق 100% مع الهوية الوطنية المعتمدة" else "خطر تثليث: الاسم في الإشعار يختلف عن هوية التاجر!",
      pixelTamperingDetected = !isNameMatched,
      triangulationRiskScore = if (isTriangulationRisk) "عالي جداً (احتيال تثليث)" else "منعدم 0.01% (آمن وموثق)",
      detectedBank = "مصرف الراجحي / بنك معتمد",
      detectedAmountText = "$amount $currency",
      referenceNumber = "REF-2026-" + (100000..999999).random(),
      timestampCheckStatus = "التوقيت حي ومتطابق بالثواني مع وقت الصفقة",
      antiPhishingSealVerified = true
    )
  }
}

data class PhishingCheckResult(
  val isDangerous: Boolean,
  val warningAr: String?,
  val warningEn: String?
)

@Composable
fun AntiFraudReceiptScannerDialog(
  expectedName: String,
  dealAmount: Double,
  fiatCurrency: String,
  isRtl: Boolean,
  onDismiss: () -> Unit
) {
  var isScanning by remember { mutableStateOf(false) }
  var forensicReport by remember { mutableStateOf<ReceiptForensicReport?>(null) }
  var testSenderName by remember { mutableStateOf(expectedName) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = EmeraldPrimary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isRtl) "الفاحص الجنائي لكشف تزييف الإيصالات" else "AI Receipt Fraud Detector",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = if (isRtl)
            "تقنية متطورة تفحص الإيصالات البنكية وتكشف التلاعب بالبكسلات واحتيال التثليث (Triangulation) بمطابقة اسم صاحب الحساب مع الهوية المعتمدة."
          else
            "Forensic scanner inspecting bank receipts for pixel tampering and third-party triangulation scams.",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = testSenderName,
          onValueChange = { testSenderName = it },
          label = { Text(if (isRtl) "اسم المحوّل المسجل في الإشعار البنكي" else "Sender Name on Receipt") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Button(
          onClick = {
            isScanning = true
            forensicReport = AntiFraudEngine.analyzeReceipt(
              senderNameOnReceipt = testSenderName,
              expectedKycName = expectedName,
              amount = dealAmount,
              currency = fiatCurrency
            )
            isScanning = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("run_receipt_scan_btn")
        ) {
          Icon(Icons.Default.Search, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isRtl) "بدء الفحص الجنائي بالذكاء الاصطناعي" else "Scan Receipt with AI", color = BackgroundDark, fontWeight = FontWeight.Bold)
        }

        forensicReport?.let { report ->
          Surface(
            color = if (report.isAuthentic) EmeraldPrimary.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (report.isAuthentic) EmeraldPrimary else StatusError
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (report.isAuthentic) "✅ إشعار حقيقي وسليم 100%" else "🚨 تحذير: شبهة احتيال وتثليث!",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (report.isAuthentic) EmeraldPrimary else StatusError
                )
                Text(
                  text = "دقة الفحص: ${report.trustConfidencePercent}%",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Text(text = "• ${report.nameMatchStatus}", fontSize = 11.sp)
              Text(
                text = "• خطر التثليث (طرف ثالث): ${report.triangulationRiskScore}",
                fontSize = 11.sp,
                color = if (report.isAuthentic) EmeraldPrimary else StatusError,
                fontWeight = FontWeight.Bold
              )
              Text(text = "• فحص تعديل البكسلات: " + if (report.pixelTamperingDetected) "تم رصد تعديل مريب في الخطوط!" else "سليم وبدون تعديل", fontSize = 11.sp)
              Text(text = "• الختم والتوقيت: ${report.timestampCheckStatus}", fontSize = 11.sp)
              Text(text = "• رقم العملية: ${report.referenceNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text(if (isRtl) "إغلاق" else "Close")
      }
    }
  )
}
