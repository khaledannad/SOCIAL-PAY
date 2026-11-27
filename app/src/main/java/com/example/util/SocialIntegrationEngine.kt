package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.entity.TransactionEntity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SocialIntegrationEngine {

  const val BASE_SHARE_URL = "https://pay.socialpay.io/req/"

  /**
   * Generates a unique Transaction ID following the standard format SP-REQ-XXXXXX
   */
  fun generateTransactionId(type: String): String {
    val prefix = when (type.uppercase()) {
      "INVOICE" -> "SP-INV"
      "BUY" -> "SP-BUY"
      "SELL" -> "SP-SEL"
      else -> "SP-REQ"
    }
    val randomNum = (100000..999999).random()
    return "$prefix-$randomNum"
  }

  /**
   * Generates formatted message copy for social sharing
   */
  fun formatSocialShareMessage(txn: TransactionEntity, isRtl: Boolean): String {
    val currencySymbol = when (txn.currency) {
      "SAR" -> "ر.س"
      "AED" -> "د.إ"
      "EGP" -> "ج.م"
      "USD" -> "$"
      "USDT" -> "USDT"
      "EUR" -> "€"
      else -> txn.currency
    }

    return if (isRtl) {
      """
      🟢 *طلب معاملة معتمد عبر Social Pay* 🟢
      
      📌 *العنوان:* ${txn.title}
      💰 *المبلغ:* ${String.format("%,.2f", txn.amount)} $currencySymbol
      🔒 *نوع المعاملة:* ${if (txn.type == "INVOICE") "فاتورة سداد رسمية" else "أمانة ذكية (0% ربا)"}
      👤 *صاحب الطلب:* ${txn.senderName}
      🆔 *رقم المعاملة:* #${txn.id}
      
      📲 *رابط السداد الفوري الآمن:*
      ${txn.shareUrl}
      
      ✨ لا تحتاج لتثبيت أي تطبيق — افتح الرابط وأكمل السداد مباشرة عبر البنك أو البطاقة!
      🛡️ محمي ضد التثليث والتصيد بكود الأمان المعتمد.
      """.trimIndent()
    } else {
      """
      🟢 *Verified Social Pay Request* 🟢
      
      📌 *Title:* ${txn.title}
      💰 *Amount:* ${String.format("%,.2f", txn.amount)} $currencySymbol
      🔒 *Security:* Smart Escrow Protected (0% Riba)
      👤 *Requester:* ${txn.senderName}
      🆔 *Transaction ID:* #${txn.id}
      
      📲 *Instant Secure Payment Link:*
      ${txn.shareUrl}
      
      ✨ No app installation needed — open link to pay directly via bank transfer or card!
      """.trimIndent()
    }
  }

  /**
   * Launches WhatsApp with pre-filled message using official API
   */
  fun shareViaWhatsApp(context: Context, txn: TransactionEntity, phone: String? = null, isRtl: Boolean = true) {
    val text = formatSocialShareMessage(txn, isRtl)
    val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
    val cleanPhone = phone?.replace("+", "")?.replace(" ", "") ?: ""

    val url = if (cleanPhone.isNotEmpty()) {
      "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText"
    } else {
      "https://api.whatsapp.com/send?text=$encodedText"
    }

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
      context.startActivity(intent)
    } catch (_: Exception) {
      shareViaGenericIntent(context, txn, isRtl)
    }
  }

  /**
   * Launches Telegram official share link
   */
  fun shareViaTelegram(context: Context, txn: TransactionEntity, isRtl: Boolean = true) {
    val text = formatSocialShareMessage(txn, isRtl)
    val encodedUrl = URLEncoder.encode(txn.shareUrl, StandardCharsets.UTF_8.toString())
    val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
    val url = "https://t.me/share/url?url=$encodedUrl&text=$encodedText"

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
      context.startActivity(intent)
    } catch (_: Exception) {
      shareViaGenericIntent(context, txn, isRtl)
    }
  }

  /**
   * Universal share via Android Intent
   */
  fun shareViaGenericIntent(context: Context, txn: TransactionEntity, isRtl: Boolean = true) {
    val text = formatSocialShareMessage(txn, isRtl)
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, text)
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, if (isRtl) "مشاركة الطلب عبر التطبيقات الاجتماعية" else "Share via Social Apps").apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(shareIntent)
  }

  /**
   * Generates a 1-click HTML/JS Embed Code for e-commerce sites and online stores
   */
  fun generateWebEmbedCode(txn: TransactionEntity): String {
    return """
    <!-- Social Pay Smart Button -->
    <script src="https://cdn.socialpay.io/sdk/v1/button.js" async></script>
    <div class="social-pay-button" 
         data-request-id="${txn.id}" 
         data-amount="${txn.amount}" 
         data-currency="${txn.currency}" 
         data-theme="dark" 
         data-lang="ar" 
         data-fee="0.80">
      <a href="${txn.shareUrl}" target="_blank" style="display:inline-flex;align-items:center;background:#00E676;color:#121212;padding:12px 24px;border-radius:12px;font-weight:bold;text-decoration:none;font-family:sans-serif;">
        <span style="margin-left:8px;">الدفع فوراً عبر Social Pay (${txn.amount} ${txn.currency})</span>
      </a>
    </div>
    """.trimIndent()
  }
}
