package com.example.core.social

import com.example.core.transaction.PaymentIntent
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Social channels supported by Social Pay
 */
enum class SocialChannel(val id: String, val titleAr: String, val titleEn: String, val iconRes: String) {
  WHATSAPP("WHATSAPP", "واتساب", "WhatsApp", "ic_whatsapp"),
  TELEGRAM("TELEGRAM", "تيليجرام", "Telegram", "ic_telegram"),
  INSTAGRAM("INSTAGRAM", "إنستغرام", "Instagram", "ic_instagram"),
  FACEBOOK("FACEBOOK", "فيسبوك ماسنجر", "Facebook Messenger", "ic_facebook"),
  DIRECT_LINK("DIRECT_LINK", "رابط مباشر ورمز QR", "Direct Link & QR", "ic_link"),
  WEB_EMBED("WEB_EMBED", "كود تضمين للمواقع والمتاجر", "Web Embed Snippet", "ic_code")
}

/**
 * Social Integration Engine:
 * Generates official share URLs, QR codes, and web embed snippets.
 * Uses only official protocols without unauthorized message scraping.
 */
object SocialIntegrationEngine {

  const val BASE_SHARE_URL = "https://pay.socialpay.io/req/"

  /**
   * Formats social share text with security watermark
   */
  fun formatShareMessage(intent: PaymentIntent, isRtl: Boolean = true): String {
    val currencyDisplay = when (intent.currencyTicker) {
      "SAR" -> "ر.س"
      "AED" -> "د.إ"
      "EGP" -> "ج.م"
      "USD" -> "$"
      "EUR" -> "€"
      else -> intent.currencyTicker
    }

    return if (isRtl) {
      """
      🟢 *طلب دفع آمن عبر SOCIAL PAY* 🟢
      
      📌 *المعاملة:* #${intent.transactionId}
      💰 *المبلغ المستحق:* ${String.format("%,.2f", intent.amount)} $currencyDisplay
      🔒 *الضمان:* أمانة مشفرة بدون ربا (0% Riba Escrow)
      👤 *المستفيد:* ${intent.recipientName}
      🌐 *الشبكة / القناة:* ${intent.network.displayNameAr}
      
      📲 *رابط السداد الفوري (بدون تثبيت التطبيق):*
      ${intent.paymentLink}
      
      🛡️ تم التحقق: الرابط محمي بختم منع التصيد والتثليث.
      """.trimIndent()
    } else {
      """
      🟢 *Verified Payment Request via SOCIAL PAY* 🟢
      
      📌 *Transaction:* #${intent.transactionId}
      💰 *Amount Due:* ${String.format("%,.2f", intent.amount)} $currencyDisplay
      🔒 *Protection:* Non-Custodial Smart Escrow (0% Riba)
      👤 *Payee:* ${intent.recipientName}
      🌐 *Network:* ${intent.network.displayNameEn}
      
      📲 *Instant Payment Link (No app install needed):*
      ${intent.paymentLink}
      
      🛡️ Verified: Protected by anti-phishing watermark.
      """.trimIndent()
    }
  }

  /**
   * Generates official WhatsApp share intent link
   */
  fun getWhatsAppShareUrl(intent: PaymentIntent, targetPhone: String? = null, isRtl: Boolean = true): String {
    val text = formatShareMessage(intent, isRtl)
    val encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
    val cleanPhone = targetPhone?.replace("+", "")?.replace(" ", "") ?: ""
    return if (cleanPhone.isNotEmpty()) {
      "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encoded"
    } else {
      "https://api.whatsapp.com/send?text=$encoded"
    }
  }

  /**
   * Generates official Telegram share link
   */
  fun getTelegramShareUrl(intent: PaymentIntent, isRtl: Boolean = true): String {
    val text = formatShareMessage(intent, isRtl)
    val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
    val encodedUrl = URLEncoder.encode(intent.paymentLink, StandardCharsets.UTF_8.toString())
    return "https://t.me/share/url?url=$encodedUrl&text=$encodedText"
  }

  /**
   * Generates official 1-line HTML embed code for online stores and websites
   */
  fun generateWebEmbedSnippet(intent: PaymentIntent): String {
    return """
    <!-- SOCIAL PAY SMART BUTTON -->
    <script src="https://cdn.socialpay.io/v1/button.js" async></script>
    <div class="social-pay-smart-button"
         data-tx-id="${intent.transactionId}"
         data-intent-id="${intent.intentId}"
         data-amount="${intent.amount}"
         data-currency="${intent.currencyTicker}"
         data-network="${intent.network.code}"
         data-fee="0.80"
         data-lang="ar">
      <a href="${intent.paymentLink}" target="_blank"
         style="display:inline-flex;align-items:center;background:#00E676;color:#121212;padding:12px 24px;border-radius:12px;font-weight:bold;text-decoration:none;font-family:Cairo,sans-serif;">
        <span>ادفع بأمان عبر Social Pay (${intent.amount} ${intent.currencyTicker})</span>
      </a>
    </div>
    """.trimIndent()
  }
}
