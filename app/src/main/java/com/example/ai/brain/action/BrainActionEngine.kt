package com.example.ai.brain.action

import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.transaction.TransactionEngine
import java.util.UUID

data class BrainActionTool(
  val name: String,
  val descriptionAr: String,
  val descriptionEn: String,
  val requiresUserConfirmation: Boolean,
  val parameters: Map<String, String>
)

data class PendingActionProposal(
  val proposalId: String = UUID.randomUUID().toString(),
  val actionName: String,
  val titleAr: String,
  val summaryAr: String,
  val payload: Map<String, String>,
  val proposedAt: Long = System.currentTimeMillis()
)

sealed class ActionResult {
  data class Success(val messageAr: String, val data: Map<String, Any> = emptyMap()) : ActionResult()
  data class NeedsConfirmation(val proposal: PendingActionProposal) : ActionResult()
  data class Denied(val reasonAr: String) : ActionResult()
  data class Error(val errorMessageAr: String) : ActionResult()
}

/**
 * Brain Action Engine:
 * Coordinates scoped assistant tools (create invoice, create payment link, inspect status,
 * open support ticket) bound to the deterministic TransactionEngine.
 * Strictly enforces Stablecoins-only policy and NEVER returns fabricated transaction stats
 * or possesses unilateral transfer privileges.
 */
class BrainActionEngine(
  private val transactionEngine: TransactionEngine = TransactionEngine()
) {

  private val supportTickets = mutableListOf<Map<String, String>>()
  private val pendingProposals = mutableMapOf<String, PendingActionProposal>()

  companion object {
    val AVAILABLE_TOOLS = listOf(
      BrainActionTool(
        name = "create_invoice",
        descriptionAr = "إصدار فاتورة عملات مستقرة مع رابط سداد ورمز QR",
        descriptionEn = "Generate itemized stablecoin invoice with payment link and QR",
        requiresUserConfirmation = true,
        parameters = mapOf("clientName" to "Client Name", "amount" to "Amount", "currency" to "Stablecoin (USDC/USDT)", "description" to "Description")
      ),
      BrainActionTool(
        name = "create_payment_link",
        descriptionAr = "توليد رابط طلب دفع مستقر آمن (USDC/USDT)",
        descriptionEn = "Generate secure stablecoin social payment link",
        requiresUserConfirmation = true,
        parameters = mapOf("amount" to "Amount", "currency" to "Stablecoin (USDC/USDT)", "title" to "Title", "channel" to "Channel")
      ),
      BrainActionTool(
        name = "get_transaction_status",
        descriptionAr = "الاستعلام عن حالة معاملة محددة عبر معرفها",
        descriptionEn = "Check live status of a transaction by ID",
        requiresUserConfirmation = false,
        parameters = mapOf("transactionId" to "Transaction ID")
      ),
      BrainActionTool(
        name = "search_transaction",
        descriptionAr = "البحث في سجل المعاملات المصرح به للمستخدم",
        descriptionEn = "Search authorized transaction history",
        requiresUserConfirmation = false,
        parameters = mapOf("query" to "Query or recipient name")
      ),
      BrainActionTool(
        name = "create_support_ticket",
        descriptionAr = "فتح تذكرة دعم فني جديدة للشكاوى أو النزاعات",
        descriptionEn = "Open a formal customer support or dispute ticket",
        requiresUserConfirmation = false,
        parameters = mapOf("subject" to "Subject", "details" to "Details", "transactionId" to "Transaction ID")
      ),
      BrainActionTool(
        name = "get_user_history",
        descriptionAr = "عرض ملخص المعاملات المسجلة للمستخدم المصرح به",
        descriptionEn = "Retrieve authorized summary of recorded user transactions",
        requiresUserConfirmation = false,
        parameters = emptyMap()
      )
    )

    private val PROHIBITED_OPERATIONS = setOf(
      "transfer_funds", "send_money", "confirm_payment_direct", "alter_balance", "withdraw_funds",
      "modify_security_rules", "read_private_messages", "delete_audit_log"
    )
  }

  fun executeTool(
    toolName: String,
    params: Map<String, String>,
    userId: String,
    confirmedProposalId: String? = null
  ): ActionResult {
    if (PROHIBITED_OPERATIONS.contains(toolName.lowercase())) {
      return ActionResult.Denied("⛔ إجراء محظور أمنياً: لا يملك الذكاء الاصطناعي صلاحية نقل الأموال أو تعديل قواعد الأمان أو قراءة المحادثات.")
    }

    val tool = AVAILABLE_TOOLS.find { it.name.equals(toolName, ignoreCase = true) }
      ?: return ActionResult.Error("الأداة المطلوبة غير معروفة: $toolName")

    // Validate stablecoin policy early if currency param provided
    val requestedCurrency = params["currency"]?.uppercase()
    if (!requestedCurrency.isNullOrBlank() &&
      requestedCurrency in DigitalAssetEngine.BLOCKED_VOLATILE_AND_MEME_TICKERS
    ) {
      return ActionResult.Denied("⛔ السياسة المالية: المنصة مخصصة حصرياً للعملات المستقرة المعتمدة (USDC / USDT / EURC) ويُمنع استخدام $requestedCurrency.")
    }

    if (tool.requiresUserConfirmation && confirmedProposalId == null) {
      val proposal = PendingActionProposal(
        actionName = tool.name,
        titleAr = when (tool.name) {
          "create_invoice" -> "تأكيد إصدار فاتورة بقيمة ${params["amount"]} ${params["currency"] ?: "USDC"}"
          "create_payment_link" -> "تأكيد إنشاء رابط طلب دفع بقيمة ${params["amount"]} ${params["currency"] ?: "USDC"}"
          else -> "تأكيد تنفيذ: ${tool.descriptionAr}"
        },
        summaryAr = "المستلم: ${params["clientName"] ?: "غير محدد"} | البيان: ${params["description"] ?: params["title"] ?: "طلب دفع"}",
        payload = params
      )
      pendingProposals[proposal.proposalId] = proposal
      return ActionResult.NeedsConfirmation(proposal)
    }

    return when (tool.name) {
      "create_payment_link" -> {
        val amount = params["amount"]?.toDoubleOrNull() ?: return ActionResult.Error("يرجى تحديد مبلغ صالح")
        val currency = (params["currency"] ?: "USDC").uppercase()
        val assetId = when (currency) {
          "USDT" -> "USDT_TRC20"
          "EURC" -> "EURC_BASE"
          else -> "USDC_SOL"
        }
        val intentRes = transactionEngine.createPaymentIntent(
          amount = amount,
          assetId = assetId,
          senderUserId = userId,
          senderName = "مستخدم Social Pay",
          recipientUserId = "USR-TARGET",
          recipientName = params["clientName"] ?: "المستلم",
          idempotencyKey = "IDEM-AI-" + UUID.randomUUID()
        )
        if (intentRes.isSuccess) {
          val intent = intentRes.getOrThrow()
          ActionResult.Success(
            messageAr = "✅ تم إنشاء طلب الدفع #${intent.transactionId}\n🔗 الرابط: ${intent.paymentLink}\n💰 المبلغ: $amount ${intent.currencyTicker}\nالرسوم المعلنة: $${String.format("%.2f", intent.feeUsd)}",
            data = mapOf("transactionId" to intent.transactionId, "paymentLink" to intent.paymentLink)
          )
        } else {
          ActionResult.Error("تعذر إنشاء الطلب: ${intentRes.exceptionOrNull()?.message}")
        }
      }

      "create_invoice" -> {
        val amount = params["amount"]?.toDoubleOrNull() ?: return ActionResult.Error("يرجى تحديد مبلغ صالح")
        val currency = (params["currency"] ?: "USDC").uppercase()
        val client = params["clientName"] ?: "عميل تجاري"
        val assetId = when (currency) {
          "USDT" -> "USDT_TRC20"
          "EURC" -> "EURC_BASE"
          else -> "USDC_SOL"
        }
        val intentRes = transactionEngine.createPaymentIntent(
          typePrefix = "SP-INV",
          amount = amount,
          assetId = assetId,
          senderUserId = userId,
          senderName = "مستخدم Social Pay",
          recipientUserId = "CLIENT-REF",
          recipientName = client,
          idempotencyKey = "IDEM-INV-" + UUID.randomUUID()
        )
        if (intentRes.isSuccess) {
          val intent = intentRes.getOrThrow()
          ActionResult.Success(
            messageAr = "✅ تم إصدار الفاتورة الرسمية بنجاح #${intent.transactionId} للعميل $client بمبلغ $amount ${intent.currencyTicker}.\n📲 رابط الدفع: ${intent.paymentLink}",
            data = mapOf("invoiceNumber" to intent.transactionId, "paymentLink" to intent.paymentLink, "amount" to amount)
          )
        } else {
          ActionResult.Error(intentRes.exceptionOrNull()?.message ?: "تعذر إصدار الفاتورة")
        }
      }

      "get_transaction_status" -> {
        val txId = params["transactionId"] ?: ""
        if (txId.isBlank()) return ActionResult.Error("يرجى تزويد رقم المعاملة")
        val intent = transactionEngine.getAllIntents().find { it.transactionId.equals(txId, ignoreCase = true) }
        if (intent != null) {
          ActionResult.Success(
            messageAr = "📊 حالة المعاملة #${intent.transactionId}: ${intent.state.labelAr} (${intent.amount} ${intent.currencyTicker} على ${intent.network.code})",
            data = mapOf("status" to intent.state.name, "amount" to intent.amount)
          )
        } else {
          ActionResult.Error("لم يتم العثور على المعاملة #$txId في سجل الجلسة المصرح به.")
        }
      }

      "create_support_ticket" -> {
        val ticketId = "TCK-" + (10000..99999).random()
        val subject = params["subject"] ?: "استفسار عام"
        val details = params["details"] ?: ""
        supportTickets.add(mapOf("ticketId" to ticketId, "userId" to userId, "subject" to subject, "details" to details))
        ActionResult.Success(
          messageAr = "🎫 تم فتح تذكرة الدعم #$ticketId للموضوع: $subject.",
          data = mapOf("ticketId" to ticketId)
        )
      }

      "search_transaction" -> {
        val query = params["query"].orEmpty().trim()
        val matches = transactionEngine.getAllIntents().filter {
          it.senderUserId == userId &&
            (query.isBlank() || it.transactionId.contains(query, true) || it.recipientName.contains(query, true))
        }
        ActionResult.Success(
          messageAr = if (matches.isEmpty()) {
            "🔍 لا توجد معاملات مطابقة لـ ($query) في حسابك المصرح به."
          } else {
            "🔍 تم العثور على ${matches.size} معاملة مطابقة: " + matches.joinToString(" | ") { "#${it.transactionId} (${it.state.labelAr})" }
          },
          data = mapOf("matchCount" to matches.size)
        )
      }

      "get_user_history" -> {
        val userIntents = transactionEngine.getAllIntents().filter { it.senderUserId == userId }
        val settled = userIntents.count { it.state.name == "SETTLED" }
        ActionResult.Success(
          messageAr = "📈 ملخص حسابك المصرح به: إجمالي الطلبات المسجلة ${userIntents.size}، المكتملة بالتسوية $settled.",
          data = mapOf("totalCount" to userIntents.size, "settledCount" to settled)
        )
      }

      else -> ActionResult.Error("لم يتم العثور على تنفيذ للأداة: ${tool.name}")
    }
  }

  fun confirmProposal(proposalId: String, userId: String): ActionResult {
    val proposal = pendingProposals.remove(proposalId)
      ?: return ActionResult.Error("المقترح غير موجود أو انتهت صلاحيته")
    return executeTool(proposal.actionName, proposal.payload, userId, confirmedProposalId = proposalId)
  }

  fun rejectProposal(proposalId: String): ActionResult {
    pendingProposals.remove(proposalId)
    return ActionResult.Denied("تم إلغاء الإجراء بناءً على رغبتك.")
  }
}
