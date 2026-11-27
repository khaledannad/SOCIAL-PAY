package com.example.core.audit

import java.security.MessageDigest

/**
 * An individual audit log record cryptographically chained to its predecessor.
 */
data class AuditEntry(
  val index: Long,
  val timestamp: Long,
  val transactionId: String,
  val action: String,
  val actorId: String,
  val details: String,
  val previousHash: String,
  val entryHash: String
)

/**
 * Audit Engine:
 * Implements a tamper-evident, append-only chronological hash chain.
 * Any modification, tampering, or deletion breaks the hash chain immediately.
 */
class AuditEngine {

  private val entries = mutableListOf<AuditEntry>()
  private val genesisHash = "0000000000000000000000000000000000000000000000000000000000000000"

  @Synchronized
  fun record(
    transactionId: String,
    action: String,
    actorId: String,
    details: String
  ): AuditEntry {
    val index = entries.size.toLong()
    val timestamp = System.currentTimeMillis()
    val prevHash = if (entries.isEmpty()) genesisHash else entries.last().entryHash
    val entryHash = computeHash(index, timestamp, transactionId, action, actorId, details, prevHash)

    val entry = AuditEntry(
      index = index,
      timestamp = timestamp,
      transactionId = transactionId,
      action = action,
      actorId = actorId,
      details = details,
      previousHash = prevHash,
      entryHash = entryHash
    )
    entries.add(entry)
    return entry
  }

  /**
   * Verifies the full integrity of the audit chain from genesis to the latest record.
   * Returns true if chain is intact, or false with the corrupted index.
   */
  @Synchronized
  fun verifyChainIntegrity(): Pair<Boolean, String?> {
    var expectedPrevHash = genesisHash

    for (entry in entries) {
      if (entry.previousHash != expectedPrevHash) {
        return Pair(false, "انقطاع في السلسلة المشفرة عند السجل #${entry.index}: الهاش السابق غير متطابق")
      }

      val recomputed = computeHash(
        entry.index,
        entry.timestamp,
        entry.transactionId,
        entry.action,
        entry.actorId,
        entry.details,
        entry.previousHash
      )

      if (entry.entryHash != recomputed) {
        return Pair(false, "تم رصد تلاعب في محتوى السجل #${entry.index}: الهاش المسجل لا يطابق التوقيع المشفر")
      }

      expectedPrevHash = entry.entryHash
    }

    return Pair(true, null)
  }

  fun getEntriesForTransaction(transactionId: String): List<AuditEntry> {
    return entries.filter { it.transactionId == transactionId }
  }

  fun getAllEntries(): List<AuditEntry> = entries.toList()

  companion object {
    fun computeHash(
      index: Long,
      timestamp: Long,
      transactionId: String,
      action: String,
      actorId: String,
      details: String,
      previousHash: String
    ): String {
      val raw = "$index|$timestamp|$transactionId|$action|$actorId|$details|$previousHash"
      val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
      return digest.joinToString("") { "%02x".format(it) }
    }
  }
}
