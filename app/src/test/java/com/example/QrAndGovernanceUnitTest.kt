package com.example

import com.example.data.FeedbackCategory
import com.example.data.PartnerPlatformType
import com.example.data.PartnerSector
import com.example.security.OwnerPinAttemptGuard
import com.example.security.OwnerSecurityHelper
import com.example.util.QrCodeGenerator
import org.junit.Assert.*
import org.junit.Test

class QrAndGovernanceUnitTest {

  @Test
  fun paymentRequestUri_buildsAndParsesCleanly() {
    val uri = QrCodeGenerator.buildWalletPaymentRequestUri(
      recipientName = "Khaled Merchant",
      assetSymbol = "usdt",
      network = "TRC20",
      walletOrRailAddress = "TXyzSocialPayWallet123",
      amount = 150.5,
      currency = "usdt",
      memo = "Invoice #901",
      referenceId = "REQ-901"
    )
    assertTrue(uri.startsWith("socialpay://pay?"))
    val parsed = QrCodeGenerator.parsePaymentRequestUri(uri)
    assertNotNull(parsed)
    assertEquals("Khaled Merchant", parsed!!.recipientName)
    assertEquals("USDT", parsed.assetSymbol)
    assertEquals("TRC20", parsed.network)
    assertEquals(150.5, parsed.amount!!, 0.001)
    assertEquals("USDT", parsed.currency)
    assertEquals("TXyzSocialPayWallet123", parsed.walletOrRailAddress)
    assertEquals("Invoice #901", parsed.memo)
    assertEquals("REQ-901", parsed.referenceId)
  }

  @Test
  fun paymentRequestUri_rejectsUnsafeSchemesAndParsesCryptoSchemes() {
    assertNull(QrCodeGenerator.parsePaymentRequestUri("javascript:alert(1)"))
    assertNull(QrCodeGenerator.parsePaymentRequestUri("data:text/html,<script>"))
    assertNull(QrCodeGenerator.parsePaymentRequestUri("file:///etc/passwd"))
    assertNull(QrCodeGenerator.parsePaymentRequestUri("intent://scan/#Intent"))
    assertNull(QrCodeGenerator.parsePaymentRequestUri(""))

    val btcParsed = QrCodeGenerator.parsePaymentRequestUri("bitcoin:bc1qsocialpay001?amount=0.05&label=Escrow")
    assertNotNull(btcParsed)
    assertEquals("BTC", btcParsed!!.assetSymbol)
    assertEquals("bc1qsocialpay001", btcParsed.walletOrRailAddress)
    assertEquals(0.05, btcParsed.amount!!, 0.0001)
    assertEquals("Escrow", btcParsed.recipientName)
  }

  @Test
  fun ownerSecurityHelper_hashesAndVerifiesSaltedPinsAndRejectsWeakPins() {
    assertTrue(OwnerSecurityHelper.isWeakPin("123456"))
    assertTrue(OwnerSecurityHelper.isWeakPin("000000"))
    assertTrue(OwnerSecurityHelper.isWeakPin("12345"))
    assertFalse(OwnerSecurityHelper.isWeakPin("482917"))

    assertTrue(OwnerSecurityHelper.isPlatformOwnerEmail("khaledannad29@gmail.com"))
    assertTrue(OwnerSecurityHelper.isPlatformOwnerEmail("KhaledAnnad29@gmail.com"))
    assertFalse(OwnerSecurityHelper.isPlatformOwnerEmail("attacker@example.com"))

    val hashed1 = OwnerSecurityHelper.createHashedPin("482917")
    val hashed2 = OwnerSecurityHelper.createHashedPin("482917")
    assertNotEquals(hashed1.saltBase64, hashed2.saltBase64)
    assertNotEquals(hashed1.hashHex, hashed2.hashHex)

    assertTrue(
      OwnerSecurityHelper.verifyPin("482917", hashed1.hashHex, hashed1.saltBase64, hashed1.algorithm)
    )
    assertFalse(
      OwnerSecurityHelper.verifyPin("999999", hashed1.hashHex, hashed1.saltBase64, hashed1.algorithm)
    )
  }

  @Test
  fun ownerPinAttemptGuard_enforcesCooldownAfterMaxFailures() {
    var fakeTime = 10_000L
    val guard = OwnerPinAttemptGuard(maxAttempts = 3, lockoutDurationMs = 30_000L) { fakeTime }
    assertFalse(guard.isLockedOut())
    assertEquals(3, guard.remainingAttempts())

    guard.recordFailure()
    guard.recordFailure()
    assertFalse(guard.isLockedOut())
    assertEquals(1, guard.remainingAttempts())

    guard.recordFailure()
    assertTrue(guard.isLockedOut())
    assertEquals(30, guard.remainingLockoutSeconds())

    fakeTime += 31_000L
    assertFalse(guard.isLockedOut())
    assertEquals(3, guard.remainingAttempts())
  }

  @Test
  fun allPartnerSectorsAndFeedbackCategoriesAreConfigured() {
    assertEquals(12, PartnerSector.entries.size)
    assertEquals(7, PartnerPlatformType.entries.size)
    assertEquals(7, FeedbackCategory.entries.size)
  }
}
