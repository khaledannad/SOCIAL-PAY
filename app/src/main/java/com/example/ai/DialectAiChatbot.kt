package com.example.ai

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch

data class BotMessage(
  val id: String,
  val text: String,
  val isUser: Boolean,
  val dialectTag: String? = null,
  val timestamp: String = "الآن"
)

class DialectAiAssistantEngine {

  private val generativeModel by lazy {
    try {
      Firebase.ai.generativeModel(
        modelName = "gemini-3.8-flash",
        systemInstruction = content {
          text(
            """
            أنت "المساعد الذكي P2P وخبير الأمان والتداول" (P2P Omni-Dialect AI Assistant).
            أنت تفهم وتتحدث بطلاقة وبراعة استثنائية بجميع لغات العالم وجميع اللهجات العربية:
            - اللهجة الخليجية (السعودية، الكويتية، الإماراتية، القطرية، العمانية، البحرينية)
            - اللهجة المصرية
            - اللهجات المغاربية (الجزائرية، المغربية الدارجة، التونسية، الليبية)
            - اللهجات الشامية (السورية، اللبنانية، الأردنية، الفلسطينية)
            - اللهجة العراقية، اليمنية، السودانية، والعربية الفصحى
            - الإنجليزية والفرنسية
            قواعدك الأساسية:
            1. الرد دائماً بنفس لهجة أو لغة المستخدم، مع بث الطمأنينة والأمان المالي.
            2. شرح ميزات الأمان: حجز الأمانة المشفرة التلقائي (Smart Escrow)، كشف احتيال التثليث (Triangulation)، ورمز مكافحة التصيد السري.
            3. توضيح نظام الرسوم: رسوم ثابتة 0.80$ لكل معاملة حالياً، وتصبح 0.90$ للأبد عند بلوغ المشروع مليون دولار ($1,000,000) لتمويل صندوق الأمان والأيردروبات.
            4. تأكيد المعاملات الخالية من الربا 0% بالتقابض الفوري الحقيقي.
            """.trimIndent()
          )
        }
      )
    } catch (e: Exception) {
      null
    }
  }

  suspend fun askAssistant(userPrompt: String, selectedDialect: String): String {
    // 1. Try Firebase AI Logic (Gemini API)
    try {
      val model = generativeModel
      if (model != null) {
        val promptWithContext = if (selectedDialect != "تلقائي الذكاء") {
          "أجبني باللهجة $selectedDialect حصراً: $userPrompt"
        } else {
          userPrompt
        }
        val response = model.generateContent(promptWithContext)
        val text = response.text
        if (!text.isNullOrBlank()) {
          return text.trim()
        }
      }
    } catch (e: Exception) {
      // Graceful fallback to smart dialect engine
    }

    // 2. High-Fidelity Dialect-Aware Fallback Engine
    return generateDialectFallback(userPrompt, selectedDialect)
  }

  private fun generateDialectFallback(prompt: String, dialect: String): String {
    val lower = prompt.lowercase()

    // Fraud & Scam check
    if (lower.contains("نصب") || lower.contains("احتيال") || lower.contains("تثليث") || lower.contains("أمان") || lower.contains("scam") || lower.contains("fraud")) {
      return when {
        dialect.contains("خليجي") || lower.contains("شلون") || lower.contains("وش") ->
          "يا هلا وغلا! لا تشيل هم النصب أبد يا خوي 🛡️. بنظامنا، الطرف الثاني ما يقدر يسحب ولا هللة لأن العملات تكون محجوزة في العقد الذكي تلقائياً. وعندنا ميزة كشف التثليث وفحص اسم الحساب البنكي، مستحيل أحد يحول لك من حساب مو باسمه. وإذا صار أي تلاعب، التحكيم الذكي يرجع حقك خلال 10 دقايق!"

        dialect.contains("جزائري") || lower.contains("خويا") || lower.contains("بريديموب") ->
          "مرحبا بيك خويا لعزيز! 🇩🇿 مكان حتى ذرة نكوس ولا احتيال عندنا. كي تفتح الصفقة، العملة الرقمية تتبلوكا أوتوماتيك في الأمانة (Escrow). وسيستيم الذكاء يفحص وصل بريديموب والاسم باش مكانش التثليث (Triangulation). متحررش الدراهم حتى تشوف حسابك، وراك محمي 100%!"

        dialect.contains("مصري") || lower.contains("ازاي") || lower.contains("إزاي") || lower.contains("يا باشا") ->
          "يا مرحب بيك يا باشا! منورنا يا غالي. متقلقش من أي نصب خالص، لإن العملات بتتحجز في خزنة العقد الذكي أول ما الصفقة تفتح. وفيه نظام فحص الإيصالات وكشف الفوتوشوب، ومستحيل يقدر يلغي ويهرب بالفلوس. كل معاملة بأمان 100% ورسوم ثابتة 0.8$ بس!"

        dialect.contains("شامي") || lower.contains("شو") || lower.contains("حبيبنا") ->
          "أهلاً وسهلاً بحبيب قلبي! نظام الأمانة المشفرة هون محكم ومستحيل حدا يقدر ينصب عليك. العملات بتنحجز بالخزنة فوراً، وما بتتحرر إلا لتتأكد من حسابك بالبنك، وفيه درع كشف التثليث والتحكيم الفوري!"

        else ->
          "نظام P2P Social Pay مزود بدرع ذكاء المعاملات (AI Risk Shield)؛ حيث تُحجز العملات في عقد الأمانة المشفر فور فتح الصفقة، مع فحص تطابق الهوية البنكية وكشف احتيال التثليث. لا يمكن لأي طرف سحب الأمانة حتى إتمام الشروط وتأكيد الإيداع الفعلي."
      }
    }

    // Fees & Milestone check
    if (lower.contains("رسوم") || lower.contains("0.8") || lower.contains("0.9") || lower.contains("مليون") || lower.contains("fee")) {
      return when {
        dialect.contains("خليجي") ->
          "الرسوم ثابتة 0.80$ فقط للصفقة مهما كان المبلغ! ومشروعنا عنده خطة واضحة: أول ما نوصل دخل مليون دولار ($1,000,000)، بتزيد الرسوم إلى 0.90$ وتثبت للأبد، والفرق هذا يمول صندوق الأيردروبات والتأمين لكل المتداولين النشطين!"
        dialect.contains("جزائري") ->
          "الرسوم هنا فيكسي 0.80$ برك للصفقة قاع واش كاين! وكي نحققو مدخول مليون دولار في المشروع، نزيدوها لـ 0.90$ وتثبت للأبد باش نمولو بيها الأيردروبات وصندوق الضمان للمستخدمين النشيطين خويا!"
        dialect.contains("مصري") ->
          "الرسوم ثابتة يا سيدي بـ 0.80$ بس على المعاملة مهما كان حجمها! وأول ما المشروع يوصل لمدخول مليون دولار، الرسوم هتزيد لـ 0.90$ ثابتة على طول، وده عشان يغطي صندوق الأيردروب والتأمين ضد أي مخاطر!"
        else ->
          "الرسوم ثابتة ومحددة بـ 0.80$ لكل معاملة دون أي نسب مئوية خفية. وعند وصول دخل المشروع التراكمي إلى 1,000,000$، ستنتقل الرسوم إلى 0.90$ ثابتة دائماً لتمويل احتياطي الأمان والأيردروبات المجتمعية."
      }
    }

    // Zero Riba & Islamic compliance
    if (lower.contains("ربا") || lower.contains("حلال") || lower.contains("شرع") || lower.contains("islamic")) {
      return "نعم يا غالي، جميع صفقات المنصة قائمة على مبدأ التقابض الفوري الحقيقي في مجلس العقد (Spot Handover) يداً بيد مع الأمانة المشفرة، بدون أي تأخير، وبدون أي فوائد أو هوامش ربوية (0% ربا)، وهي متوافقة تماماً مع الضوابط المالية الأخلاقية."
    }

    // Default friendly response
    return when {
      dialect.contains("خليجي") -> "أمر وتدلل يا خوي! أنا معك خبير التداول الذكي، تسألني عن أي شي يخص صفقات P2P، الرسوم، حماية الحسابات، أو الأسعار الحية وأجاوبك فوراً!"
      dialect.contains("جزائري") -> "واش راك خويا العزيز! راني هنا في خدمتك في أي وقت، سقسيني على التحويلات، بريديموب، الأمان، ولا شروط البيع والشراء وراني معاك دقيقة بدقيقة!"
      dialect.contains("مصري") -> "منور يا سيدي! أنا معاك في أي وقت عشان أساعدك في أي معاملة أو سؤال عن الكريبتو والتحويلات البنكية بأمان تام ورسوم 0.8$ ثابتة!"
      dialect.contains("شامي") -> "تكرم عينك حبيبنا! أنا جاهز ساعدك بكل تفاصيل التداول والأسعار وحماية أموالك من أي تلاعب وبأحسن سعر!"
      else -> "Hello! I am your P2P Smart Assistant. I speak and understand all global languages and Arabic dialects fluently. How can I assist your trades today with zero risk?"
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialectAiChatbotSheet(
  isRtl: Boolean,
  onDismiss: () -> Unit
) {
  com.example.ai.brain.ui.SocialPayGeniusBrainSheet(
    isRtl = isRtl,
    onDismiss = onDismiss
  )
}

