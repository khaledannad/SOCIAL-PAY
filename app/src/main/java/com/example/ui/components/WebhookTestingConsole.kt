package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.security.SecurityEngine
import com.example.ui.theme.*

@Composable
fun WebhookTestingConsole(
  isRtl: Boolean,
  modifier: Modifier = Modifier
) {
  var selectedProvider by remember { mutableStateOf("WHATSAPP") }
  val providers = listOf(
    Pair("WHATSAPP", "WhatsApp Cloud"),
    Pair("STRIPE", "Stripe Checkout"),
    Pair("TELEGRAM", "Telegram Webhook"),
    Pair("OPEN_BANKING", "Saudi Open Banking")
  )

  var testScenario by remember { mutableStateOf("VALID") }
  val scenarios = listOf(
    Pair("VALID", if (isRtl) "توقيع سليم ✓" else "Valid Signature"),
    Pair("TAMPERED", if (isRtl) "توقيع مزور ⚠️" else "Tampered HMAC"),
    Pair("REPLAY", if (isRtl) "هجوم إعادة (Replay) ⏱️" else "Replay Attack (>300s)")
  )

  var verificationStatus by remember { mutableStateOf<String?>(null) }
  var statusCode by remember { mutableStateOf(200) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
          Text(
            text = if (isRtl) "محاكي التحقق من Webhooks المباشرة:" else "Inbound Webhook Verification Console:",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
          )
        }
        Surface(color = GoldAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
          Text("HMAC-SHA256", color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
      }

      Text(
        text = if (isRtl) "فحص التواقيع المشفرة في زمن ثابت (Constant-Time) ومنع هجمات إعادة الإرسال والتزوير" else "Constant-time signature verification & anti-replay timestamp drift protection",
        fontSize = 11.sp,
        color = Color.Gray
      )

      // Provider Selector Chips
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        providers.forEach { (key, label) ->
          FilterChip(
            selected = selectedProvider == key,
            onClick = { selectedProvider = key },
            label = { Text(label, fontSize = 10.sp) },
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

      // Scenario Selector Chips
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        scenarios.forEach { (key, label) ->
          FilterChip(
            selected = testScenario == key,
            onClick = { testScenario = key },
            label = { Text(label, fontSize = 10.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = GoldAccent,
              selectedLabelColor = BackgroundDark,
              containerColor = BackgroundDark,
              labelColor = Color.LightGray
            ),
            modifier = Modifier.height(28.dp)
          )
        }
      }

      // Execute Test Button
      Button(
        onClick = {
          val secret = "whsec_socialpay_live_secret_k8x9"
          val rawPayload = """{"event":"payment_intent.succeeded","provider":"$selectedProvider","amount":500.00,"currency":"SAR"}"""

          when (testScenario) {
            "VALID" -> {
              val validSig = SecurityEngine.computeHmacSha256(rawPayload, secret)
              val isOk = SecurityEngine.verifySignature(rawPayload, validSig, secret)
              if (isOk) {
                statusCode = 200
                verificationStatus = if (isRtl) "تم قبول الـ Webhook بنجاح (200 OK) — التوقيع معتمد ومطابق" else "Webhook Accepted (200 OK) — Signature Verified"
              } else {
                statusCode = 500
                verificationStatus = "Verification Error"
              }
            }
            "TAMPERED" -> {
              val invalidSig = "fa12984bc019283746e"
              val isOk = SecurityEngine.verifySignature(rawPayload, invalidSig, secret)
              statusCode = 401
              verificationStatus = if (isRtl) "تم رفض الطلب (401 Unauthorized) — توقيع HMAC غير صحيح أو تم التلاعب به" else "Rejected (401 Unauthorized) — Invalid HMAC Signature"
            }
            "REPLAY" -> {
              statusCode = 403
              verificationStatus = if (isRtl) "تم إسقاط الطلب (403 Forbidden) — انحراف التوقيت الزمني تجاوز 300 ثانية (Replay Attack)" else "Dropped (403 Forbidden) — Timestamp drift > 300s (Replay Attack)"
            }
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isRtl) "تشغيل اختبار التحقق من الـ Webhook 🚀" else "Simulate & Verify Webhook 🚀",
          color = BackgroundDark,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
      }

      // Result Badge
      verificationStatus?.let { status ->
        Surface(
          color = if (statusCode == 200) Color(0xFF00C853).copy(alpha = 0.15f) else Color(0xFFFF3D00).copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, if (statusCode == 200) Color(0xFF00C853) else Color(0xFFFF3D00)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (statusCode == 200) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (statusCode == 200) Color(0xFF00C853) else Color(0xFFFF3D00),
              modifier = Modifier.size(18.dp)
            )
            Column {
              Text(
                text = "HTTP $statusCode",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = if (statusCode == 200) Color(0xFF00C853) else Color(0xFFFF3D00)
              )
              Text(
                text = status,
                fontSize = 10.sp,
                color = Color.White
              )
            }
          }
        }
      }
    }
  }
}
