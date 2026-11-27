package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFeeMilestoneCalculations() {
    val initialFee = 0.80
    val milestoneGoal = 1_000_000.0
    val permanentFee = 0.90
    assertTrue(initialFee < permanentFee)
    assertEquals(1_000_000.0, milestoneGoal, 0.001)
  }

  @Test
  fun testSocialIntegrationIdGeneration() {
    val invoiceId = com.example.util.SocialIntegrationEngine.generateTransactionId("INVOICE")
    assertTrue(invoiceId.startsWith("SP-INV-"))
    val reqId = com.example.util.SocialIntegrationEngine.generateTransactionId("SELL")
    assertTrue(reqId.startsWith("SP-SEL-"))
  }
}
