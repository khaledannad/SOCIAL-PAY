package com.example

import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.brain.BrainAgentId
import com.example.core.brain.SocialFinancialBrain
import com.example.core.connectivity.ConnectorConnectionState
import com.example.core.connectivity.GlobalConnectivityEngine
import com.example.core.ledger.ReconciliationStatus
import com.example.core.privacy.PrivacyAndComplianceEngine
import com.example.core.privacy.PrivacyConsentState
import com.example.core.transaction.TransactionEngine
import com.example.core.transaction.TransactionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StablecoinPolicyAndBrainTest {

  @Test
  fun volatileAndUnapprovedAssetsAreStrictlyBlocked() {
    val btcCheck = DigitalAssetEngine.validateStablecoinEligibility("BTC", BlockchainNetwork.ETHEREUM_ERC20)
    assertTrue("BTC must be rejected by Stablecoins-Only policy", btcCheck.isFailure)

    val dogeCheck = DigitalAssetEngine.validateStablecoinEligibility("DOGE", BlockchainNetwork.SOLANA)
    assertTrue("DOGE must be rejected", dogeCheck.isFailure)

    val spoofedContractCheck = DigitalAssetEngine.validateStablecoinEligibility(
      ticker = "USDC",
      network = BlockchainNetwork.SOLANA,
      contractAddress = "FAKE_SPOOFED_CONTRACT_12345"
    )
    assertTrue("Spoofed stablecoin contract must be rejected", spoofedContractCheck.isFailure)

    val validUsdc = DigitalAssetEngine.validateStablecoinEligibility("USDC", BlockchainNetwork.SOLANA)
    assertTrue("Official USDC on Solana must pass", validUsdc.isSuccess)
  }

  @Test
  fun preExecutionRoutingPipelineAndTamperDetectionWork() {
    val engine = TransactionEngine()
    val validPlan = engine.evaluateRoutingPlan(
      senderUserId = "USR-101",
      senderVerified = true,
      recipientName = "Nadia Benali",
      recipientAddressOrRail = "9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin",
      ticker = "USDC",
      network = BlockchainNetwork.SOLANA,
      amount = 150.0,
      declaredFeeUsd = 0.80
    )
    assertTrue("Valid USDC Solana plan must be eligible", validPlan.isEligible)
    assertNotNull(validPlan.summary)
    assertTrue(validPlan.summary!!.requiresExplicitUserApproval)

    val intent = engine.createPaymentIntent(
      amount = 150.0,
      assetId = "USDC_SOL",
      senderUserId = "USR-101",
      senderName = "Khaled",
      recipientUserId = "USR-202",
      recipientName = "Nadia Benali",
      idempotencyKey = "IDEM-TAMPER-1",
      recipientAddressOrRail = "9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin"
    ).getOrThrow()

    // Settlement without explicit approval must fail
    val unapprovedSettle = engine.transitionState(intent.intentId, TransactionState.SETTLED)
    assertTrue("Cannot settle without explicit user consent", unapprovedSettle.isFailure)

    // Mutated amount at authorization triggers UNDER_REVIEW security flag
    val tamperResult = engine.authorizeByUser(intent.intentId, expectedAmount = 999.0, expectedRecipientName = "Nadia Benali")
    assertTrue("Tampered amount must be blocked", tamperResult.isFailure)
    assertEquals(TransactionState.UNDER_REVIEW, engine.getIntent(intent.intentId)?.state)
  }

  @Test
  fun doubleEntryLedgerBlocksSettlementOnProviderMismatch() {
    val engine = TransactionEngine()
    val intent = engine.createPaymentIntent(
      amount = 200.0,
      assetId = "USDT_TRC20",
      senderUserId = "USR-1",
      senderName = "Ali",
      recipientUserId = "USR-2",
      recipientName = "Sami",
      idempotencyKey = "IDEM-RECON-1",
      recipientAddressOrRail = "TYDzsYUEpvnYmQk4zGP9sWWcTEd2MiAtW6"
    ).getOrThrow()

    engine.authorizeByUser(intent.intentId, 200.0, "Sami").getOrThrow()

    // Mismatched provider reported amount blocks settlement
    val mismatchRes = engine.settleWithProof(
      intentId = intent.intentId,
      providerId = "SANDBOX_MOCK_GATEWAY",
      providerReference = "REF-TX-888",
      providerReportedAmount = 175.0,
      networkConfirmed = true
    )
    assertTrue("Mismatch between ledger and provider must block settlement", mismatchRes.isFailure)
    assertEquals(TransactionState.UNDER_REVIEW, engine.getIntent(intent.intentId)?.state)

    // Balanced matching settlement succeeds cleanly
    val intent2 = engine.createPaymentIntent(
      amount = 200.0,
      assetId = "USDT_TRC20",
      senderUserId = "USR-1",
      senderName = "Ali",
      recipientUserId = "USR-2",
      recipientName = "Sami",
      idempotencyKey = "IDEM-RECON-2",
      recipientAddressOrRail = "TYDzsYUEpvnYmQk4zGP9sWWcTEd2MiAtW6"
    ).getOrThrow()
    engine.authorizeByUser(intent2.intentId, 200.0, "Sami").getOrThrow()
    val matchedRes = engine.settleWithProof(
      intentId = intent2.intentId,
      providerId = "SANDBOX_MOCK_GATEWAY",
      providerReference = "REF-TX-999",
      providerReportedAmount = 200.0,
      networkConfirmed = true
    )
    assertTrue("Matching proof & balanced ledger must settle", matchedRes.isSuccess)
    assertEquals(ReconciliationStatus.MATCHED, matchedRes.getOrThrow().second.status)
  }

  @Test
  fun socialFinancialBrainEnforces18AgentsAndBoundaries() {
    val brain = SocialFinancialBrain()
    assertEquals(18, brain.getAllAgentSnapshots().size)

    // Mandatory guardrails cannot be disabled even by Owner
    assertFalse(brain.setAgentEnabled(BrainAgentId.SECURITY_GUARD, false, "OWNER"))
    assertFalse(brain.setAgentEnabled(BrainAgentId.PRIVACY_PROTECTION, false, "OWNER"))
    assertFalse(brain.setAgentEnabled(BrainAgentId.FINANCIAL_AUDIT, false, "OWNER"))

    // Non-negotiable privilege escalation blocked
    val privilegeAttempt = brain.authorizeAgentPrivilegeRequest(BrainAgentId.CUSTOMER_SUPPORT, "MOVE_FUNDS_DIRECTLY")
    assertTrue("Agents must never unilaterally move funds", privilegeAttempt.isFailure)
  }

  @Test
  fun privacyExportAndHonestConnectivityStates() = runTest {
    val exportJson = PrivacyAndComplianceEngine.buildUserDataExportJson(
      userId = "UID-123",
      fullName = "Karim",
      email = "karim@example.com",
      languageCode = "en",
      countryCode = "DZ",
      timezone = "Africa/Algiers",
      kycLevel = "BASIC_LEVEL_1",
      consents = PrivacyConsentState(aiAssistantContextMemory = false, externalPartnerSharing = false),
      linkedAccountLabels = listOf("BaridiMob (DZD)"),
      transactionIds = listOf("SP-REQ-100001")
    )
    assertTrue(exportJson.contains("SOCIAL_PAY_PRIVACY_EXPORT_V1"))
    assertTrue(exportJson.contains("\"dataSaleProhibited\": true"))

    val connectivityEngine = GlobalConnectivityEngine()
    val bankConnector = connectivityEngine.defaultConnectors.first { it.id == "BANK_OPEN_BANKING_GW" }
    val testResult = connectivityEngine.testConnectorConnection(bankConnector)
    assertEquals(
      "Unprovisioned banking gateway must honestly remain INTEGRATION_PENDING",
      ConnectorConnectionState.INTEGRATION_PENDING,
      testResult.resultingState
    )
  }
}
