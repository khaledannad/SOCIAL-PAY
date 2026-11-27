package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PinHashResult(
  val hashHex: String,
  val saltBase64: String,
  val algorithm: String
)

object OwnerSecurityHelper {

  const val PLATFORM_OWNER_EMAIL = "khaledannad29@gmail.com"
  private const val PBKDF2_ITERATIONS = 10_000
  private const val KEY_LENGTH_BITS = 256
  const val ALGO_PBKDF2 = "PBKDF2_SHA256"
  const val ALGO_SHA256_SALTED = "SHA256_SALTED"

  private val weakPins = setOf(
    "000000", "111111", "222222", "333333", "444444",
    "555555", "666666", "777777", "888888", "999999",
    "123456", "654321", "012345", "543210"
  )

  fun isPlatformOwnerEmail(email: String?): Boolean {
    return email?.trim()?.equals(PLATFORM_OWNER_EMAIL, ignoreCase = true) == true
  }

  fun isValidPinFormat(pin: String): Boolean {
    return pin.length == 6 && pin.all { it.isDigit() }
  }

  fun isWeakPin(pin: String): Boolean {
    return !isValidPinFormat(pin) || pin in weakPins
  }

  fun generateSalt(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return Base64.getEncoder().encodeToString(bytes)
  }

  fun createHashedPin(pin: String): PinHashResult {
    require(isValidPinFormat(pin)) { "PIN must be strictly 6 numeric digits" }
    val salt = generateSalt()
    val (hashHex, algo) = hashPin(pin, salt)
    return PinHashResult(hashHex = hashHex, saltBase64 = salt, algorithm = algo)
  }

  fun hashPin(pin: String, saltBase64: String): Pair<String, String> {
    val saltBytes = Base64.getDecoder().decode(saltBase64)
    return try {
      val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
      val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      val derived = factory.generateSecret(spec).encoded
      spec.clearPassword()
      toHex(derived) to ALGO_PBKDF2
    } catch (_: Exception) {
      val digest = MessageDigest.getInstance("SHA-256")
      digest.update(saltBytes)
      val derived = digest.digest(pin.toByteArray(Charsets.UTF_8))
      toHex(derived) to ALGO_SHA256_SALTED
    }
  }

  fun verifyPin(
    enteredPin: String,
    expectedHashHex: String,
    saltBase64: String,
    algorithm: String = ALGO_PBKDF2
  ): Boolean {
    if (!isValidPinFormat(enteredPin) || expectedHashHex.isBlank() || saltBase64.isBlank()) {
      return false
    }
    return try {
      val saltBytes = Base64.getDecoder().decode(saltBase64)
      val computedBytes = if (algorithm == ALGO_PBKDF2) {
        val spec = PBEKeySpec(enteredPin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        spec.clearPassword()
        derived
      } else {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltBytes)
        digest.digest(enteredPin.toByteArray(Charsets.UTF_8))
      }
      val expectedBytes = fromHex(expectedHashHex)
      MessageDigest.isEqual(computedBytes, expectedBytes)
    } catch (_: Exception) {
      false
    }
  }

  private fun toHex(bytes: ByteArray): String {
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
      sb.append(String.format("%02x", b))
    }
    return sb.toString()
  }

  private fun fromHex(hex: String): ByteArray {
    if (hex.length % 2 != 0) return ByteArray(0)
    return ByteArray(hex.length / 2) { i ->
      hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
  }
}

class OwnerPinAttemptGuard(
  private val maxAttempts: Int = 5,
  private val lockoutDurationMs: Long = 30_000L,
  private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
  private var failedAttempts = 0
  private var lockedUntilMs = 0L

  fun isLockedOut(): Boolean {
    val now = clockMs()
    if (now < lockedUntilMs) return true
    if (lockedUntilMs != 0L && now >= lockedUntilMs) {
      lockedUntilMs = 0L
      failedAttempts = 0
    }
    return false
  }

  fun remainingLockoutSeconds(): Int {
    val remaining = lockedUntilMs - clockMs()
    return if (remaining > 0L) ((remaining + 999L) / 1000L).toInt() else 0
  }

  fun remainingAttempts(): Int = (maxAttempts - failedAttempts).coerceAtLeast(0)

  fun recordSuccess() {
    failedAttempts = 0
    lockedUntilMs = 0L
  }

  fun recordFailure() {
    if (isLockedOut()) return
    failedAttempts += 1
    if (failedAttempts >= maxAttempts) {
      lockedUntilMs = clockMs() + lockoutDurationMs
    }
  }
}
