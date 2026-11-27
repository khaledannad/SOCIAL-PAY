package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ParsedPaymentRequest(
  val recipientName: String = "",
  val assetSymbol: String = "USDT",
  val network: String = "TRC20",
  val amount: Double? = null,
  val currency: String = "USDT",
  val walletOrRailAddress: String = "",
  val memo: String = "",
  val referenceId: String = "",
  val rawUri: String = ""
)

object QrCodeGenerator {

  private val blockedSchemes = listOf("javascript:", "data:", "file:", "content:", "intent:")

  fun generateQrBitmap(
    content: String,
    sizePx: Int = 512,
    darkColor: Int = AndroidColor.BLACK,
    lightColor: Int = AndroidColor.WHITE
  ): Bitmap? {
    val sanitized = content.trim()
    if (sanitized.isEmpty() || sizePx <= 0) return null
    return try {
      val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1
      )
      val bitMatrix = QRCodeWriter().encode(
        sanitized,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        hints
      )
      val width = bitMatrix.width
      val height = bitMatrix.height
      val pixels = IntArray(width * height)
      for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
          pixels[offset + x] = if (bitMatrix[x, y]) darkColor else lightColor
        }
      }
      Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
      }
    } catch (_: Exception) {
      null
    }
  }

  fun buildWalletPaymentRequestUri(
    recipientName: String,
    assetSymbol: String,
    network: String,
    walletOrRailAddress: String,
    amount: Double? = null,
    currency: String = assetSymbol,
    memo: String = "",
    referenceId: String = ""
  ): String {
    val params = linkedMapOf<String, String>()
    if (recipientName.isNotBlank()) params["recipient"] = recipientName.trim().take(80)
    if (assetSymbol.isNotBlank()) params["asset"] = assetSymbol.trim().uppercase().take(16)
    if (network.isNotBlank()) params["network"] = network.trim().take(32)
    if (walletOrRailAddress.isNotBlank()) params["address"] = walletOrRailAddress.trim().take(160)
    if (amount != null && amount > 0.0 && amount.isFinite()) {
      params["amount"] = String.format(java.util.Locale.US, "%.4f", amount)
        .trimEnd('0')
        .trimEnd('.')
    }
    if (currency.isNotBlank()) params["currency"] = currency.trim().uppercase().take(12)
    if (memo.isNotBlank()) params["memo"] = memo.trim().take(140)
    if (referenceId.isNotBlank()) params["ref"] = referenceId.trim().take(48)

    val query = params.entries.joinToString("&") { (k, v) ->
      "${encodeUtf8(k)}=${encodeUtf8(v)}"
    }
    return "socialpay://pay?$query"
  }

  fun parsePaymentRequestUri(raw: String): ParsedPaymentRequest? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val lower = trimmed.lowercase()
    if (blockedSchemes.any { lower.startsWith(it) }) {
      return null
    }

    if (lower.startsWith("socialpay://") || lower.startsWith("https://socialpay.app/")) {
      val queryIndex = trimmed.indexOf('?')
      val queryPart = if (queryIndex >= 0 && queryIndex < trimmed.length - 1) {
        trimmed.substring(queryIndex + 1)
      } else {
        ""
      }
      val map = parseQueryParams(queryPart)
      val asset = map["asset"]?.uppercase()?.ifBlank { "USDT" } ?: "USDT"
      return ParsedPaymentRequest(
        recipientName = map["recipient"].orEmpty(),
        assetSymbol = asset,
        network = map["network"].orEmpty().ifBlank { "TRC20" },
        amount = map["amount"]?.toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() },
        currency = map["currency"]?.uppercase().orEmpty().ifBlank { asset },
        walletOrRailAddress = map["address"].orEmpty(),
        memo = map["memo"].orEmpty(),
        referenceId = map["ref"].orEmpty(),
        rawUri = trimmed
      )
    }

    // Standard crypto payment URI schemes: bitcoin:<addr>?amount=..., ethereum:<addr>, tron:<addr>, solana:<addr>
    val schemeMatch = Regex("^(bitcoin|ethereum|tron|solana):([^?]+)(?:\\?(.*))?$", RegexOption.IGNORE_CASE)
      .matchEntire(trimmed)
    if (schemeMatch != null) {
      val scheme = schemeMatch.groupValues[1].uppercase()
      val address = decodeUtf8(schemeMatch.groupValues[2])
      val params = parseQueryParams(schemeMatch.groupValues[3])
      val symbol = when (scheme) {
        "BITCOIN" -> "BTC"
        "ETHEREUM" -> "ETH"
        "TRON" -> "USDT"
        "SOLANA" -> "SOL"
        else -> "USDT"
      }
      return ParsedPaymentRequest(
        recipientName = params["label"].orEmpty(),
        assetSymbol = symbol,
        network = scheme,
        amount = params["amount"]?.toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() },
        currency = symbol,
        walletOrRailAddress = address,
        memo = params["message"].orEmpty(),
        rawUri = trimmed
      )
    }

    return null
  }

  fun decodeQrFromImageUri(context: Context, imageUri: Uri): Result<String> {
    return try {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(imageUri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }
      val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight).coerceAtLeast(1)
      val sampleSize = if (maxDim > 1024) Integer.highestOneBit(maxDim / 512).coerceAtLeast(1) else 1

      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      } ?: return Result.failure(IllegalArgumentException("Unable to load selected image"))

      try {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
          DecodeHintType.TRY_HARDER to true,
          DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        val decoded = MultiFormatReader().decode(binaryBitmap, hints)
        Result.success(decoded.text)
      } finally {
        bitmap.recycle()
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun parseQueryParams(query: String): Map<String, String> {
    if (query.isBlank()) return emptyMap()
    val map = mutableMapOf<String, String>()
    query.split('&').forEach { pair ->
      val eq = pair.indexOf('=')
      if (eq > 0) {
        val key = decodeUtf8(pair.substring(0, eq))
        val value = decodeUtf8(pair.substring(eq + 1))
        map[key] = value
      }
    }
    return map
  }

  private fun encodeUtf8(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())

  private fun decodeUtf8(value: String): String =
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
