package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FeedbackCategory
import com.example.data.UserFeedbackItem
import com.example.ui.theme.*

@Composable
fun DirectFeedbackDialog(
  isRtl: Boolean,
  isUserSignedIn: Boolean,
  defaultName: String = "",
  defaultEmail: String = "",
  myFeedbackList: List<UserFeedbackItem> = emptyList(),
  isSubmitting: Boolean = false,
  onDismiss: () -> Unit,
  onSubmitFeedback: (
    authorName: String,
    authorEmail: String,
    category: FeedbackCategory,
    rating: Int,
    message: String
  ) -> Unit
) {
  var nameInput by remember(defaultName) { mutableStateOf(defaultName) }
  var emailInput by remember(defaultEmail) { mutableStateOf(defaultEmail) }
  var selectedCategory by remember { mutableStateOf(FeedbackCategory.FEATURE_IDEA) }
  var rating by remember { mutableStateOf(5) }
  var messageInput by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = BrandSecondaryBg,
    titleContentColor = BrandTextPrimary,
    textContentColor = BrandTextPrimary,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.RateReview, contentDescription = null, tint = BrandPrimaryAccent)
        Column {
          Text(
            text = if (isRtl) "تعليقات المستخدمين المباشرة للتحسين" else "Direct User Feedback",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = if (isRtl) "رأيك يساعدنا على تطوير Social Pay باستمرار" else "Share ideas or issues to improve Social Pay",
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 460.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (!isUserSignedIn) {
          Surface(
            color = BrandSecondaryAccent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = if (isRtl)
                "سيتم حفظ تعليقك محلياً بأمان ومزامنته مع سحابة Social Pay فور إتمام تسجيل الدخول بحساب Google."
              else
                "Your comment is safely queued locally and synced to Cloud Firestore automatically once you sign in with Google.",
              fontSize = 11.sp,
              color = BrandTextPrimary,
              modifier = Modifier.padding(10.dp)
            )
          }

          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text(if (isRtl) "الاسم (اختياري)" else "Your Name (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text(if (isRtl) "البريد للتواصل (اختياري)" else "Email for follow-up (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Text(
          text = if (isRtl) "التقييم العام:" else "Overall Rating:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          (1..5).forEach { star ->
            IconButton(
              onClick = { rating = star },
              modifier = Modifier
                .size(40.dp)
                .testTag("feedback_star_$star")
            ) {
              Icon(
                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$star stars",
                tint = if (star <= rating) GoldAccent else BrandTextMuted,
                modifier = Modifier.size(28.dp)
              )
            }
          }
        }

        Text(
          text = if (isRtl) "تصنيف التعليق:" else "Feedback Category:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(FeedbackCategory.entries) { category ->
            FilterChip(
              selected = selectedCategory == category,
              onClick = { selectedCategory = category },
              label = { Text(if (isRtl) category.titleAr else category.titleEn, fontSize = 11.sp) }
            )
          }
        }

        OutlinedTextField(
          value = messageInput,
          onValueChange = {
            messageInput = it
            validationError = null
          },
          label = { Text(if (isRtl) "اكتب ملاحظتك أو اقتراحك للتحسين..." else "Write your feedback or suggestion...") },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("feedback_message_input")
        )

        validationError?.let { err ->
          Text(text = err, color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        if (myFeedbackList.isNotEmpty()) {
          HorizontalDivider(color = BrandSurfaceBorder)
          Text(
            text = if (isRtl) "ملاحظاتك المرسلة (${myFeedbackList.size}):" else "Your Submitted Feedback (${myFeedbackList.size}):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPrimaryAccent
          )
          myFeedbackList.take(3).forEach { item ->
            Surface(
              color = BrandPrimaryBg,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, BrandSurfaceBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(if (isRtl) item.category.titleAr else item.category.titleEn, fontSize = 10.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                  Text(if (isRtl) item.status.titleAr else item.status.titleEn, fontSize = 10.sp, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold)
                }
                Text(item.message, fontSize = 11.sp, color = BrandTextPrimary)
                if (item.ownerNote.isNotBlank()) {
                  Text(
                    text = (if (isRtl) "رد المالك: " else "Owner Reply: ") + item.ownerNote,
                    fontSize = 10.sp,
                    color = BrandSecondaryAccent
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (messageInput.trim().length < 3) {
            validationError = if (isRtl) "الرجاء كتابة 3 أحرف على الأقل" else "Please enter at least 3 characters"
          } else {
            onSubmitFeedback(nameInput, emailInput, selectedCategory, rating, messageInput)
            messageInput = ""
          }
        },
        enabled = !isSubmitting,
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("submit_feedback_btn")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(color = BrandPrimaryBg, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isRtl) "إرسال التعليق" else "Send Feedback", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isRtl) "إغلاق" else "Close", color = BrandTextSecondary)
      }
    }
  )
}
