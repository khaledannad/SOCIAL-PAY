package com.example.core.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class SecurityScanResult(
  val isSafe: Boolean,
  val threatType: String? = null,
  val warningAr: String? = null,
  val warningEn: String? = null
)

data class TriangulationCheckResult(
  val isMatch: Boolean,
  val riskLevel: String, // "NONE", "LOW", "CRITICAL"
  val descriptionAr: String,
  val confidenceScore: Int
)

object SecurityEngine {

  const val DEFAULT_WATERMARK_PREFIX = "SOCIAL_PAY_VERIFIED_2026"

  /**
   * Scans text for off-platform diversion, suspicious links, phishing domains, or .apk files
   */
  fun scanMessageContent(text: String): SecurityScanResult {
    val lower = text.lowercase()
    val suspiciousPatterns = listOf(
      "برة المنصة" to "محاولة تحويل خارج المنصة",
      "خارج التطبيق" to "طلب التعامل بدون عقد الأمانة",
      "تليجرام خاص" to "طلب تواصل جانبي غير موثق",
      "رابط التفعيل" to "احتيال تصيد روابط",
      "bit.ly" to "رابط مختصر مجهول المصدر",
      ".apk" to "محاولة تثبيت تطبيق ضار",
      "http://" to "اتصال غير مشفر وغير آمن"
    )

    for ((pattern, reason) in suspiciousPatterns) {
      if (lower.contains(pattern)) {
        return SecurityScanResult(
          isSafe = false,
          threatType = reason,
          warningAr = "⚠️ تحذير أمني صارم: تم اكتشاف ($reason). ابقَ دائماً داخل بيئة Social Pay المحمية.",
          warningEn = "⚠️ Strict Security Alert: Detected ($reason). Never transact outside Social Pay."
        )
      }
    }

    return SecurityScanResult(isSafe = true)
  }

  /**
   * Anti-Triangulation Guard:
   * Compares the name on the bank transfer / payment attempt with the verified KYC identity.
   * If names differ, triggers a CRITICAL triangulation alert to protect merchants and buyers.
   */
  fun verifyBeneficiaryTriangulation(remitterName: String, kycVerifiedName: String): TriangulationCheckResult {
    val cleanRemitter = remitterName.trim().lowercase()
    val cleanKyc = kycVerifiedName.trim().lowercase()

    if (cleanRemitter == cleanKyc || cleanKyc.contains(cleanRemitter) || cleanRemitter.contains(cleanKyc)) {
      return TriangulationCheckResult(
        isMatch = true,
        riskLevel = "NONE",
        descriptionAr = "الاسم في الحوالة البنكية متطابق 100% مع الهوية المعتمدة (خطر التثليث 0%)",
        confidenceScore = 99
      )
    }

    // Name mismatch indicates potential 3rd party triangulation scam
    return TriangulationCheckResult(
      isMatch = false,
      riskLevel = "CRITICAL",
      descriptionAr = "🚨 تحذير احتيال التثليث: اسم صاحب الحساب المحول ($remitterName) لا يطابق الهوية المعتمدة ($kycVerifiedName)!",
      confidenceScore = 15
    )
  }

  /**
   * Computes an HMAC-SHA256 signature for secure webhooks or links
   */
  fun computeHmacSha256(data: String, key: String): String {
    val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secretKeySpec)
    val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
    return hash.joinToString("") { "%02x".format(it) }
  }

  /**
   * Verifies an HMAC-SHA256 signature using constant-time comparison to prevent timing attacks
   */
  fun verifySignature(data: String, signature: String, key: String): Boolean {
    val expected = computeHmacSha256(data, key)
    if (expected.length != signature.length) return false
    var result = 0
    for (i in expected.indices) {
      result = result or (expected[i].code xor signature[i].code)
    }
    return result == 0
  }

  /**
   * Generates a unique Anti-Phishing dynamic watermark for the user session
   */
  fun generateAntiPhishingWatermark(userId: String, secretWord: String): String {
    val input = "$DEFAULT_WATERMARK_PREFIX:$userId:$secretWord"
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.take(6).joinToString("") { "%02x".format(it) }.uppercase()
  }
}
