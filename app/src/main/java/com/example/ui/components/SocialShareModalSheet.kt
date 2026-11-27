package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.transaction.PaymentIntent
import com.example.data.local.entity.TransactionEntity
import com.example.social.adapters.SocialAdapterRegistry
import com.example.social.adapters.SocialNetwork
import com.example.ui.theme.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SocialChannelDisplayItem(
  val network: SocialNetwork,
  val nameAr: String,
  val nameEn: String,
  val icon: ImageVector,
  val brandColor: Color,
  val isLiveVerified: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialShareModalSheet(
  transaction: TransactionEntity,
  isRtl: Boolean,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val dismissAction: () -> Unit = {
    coroutineScope.launch {
      sheetState.hide()
      onDismiss()
    }
  }

  val channels = listOf(
    SocialChannelDisplayItem(SocialNetwork.WHATSAPP_BUSINESS, "واتساب للأعمال", "WhatsApp Business", Icons.Default.Chat, Color(0xFF25D366), true),
    SocialChannelDisplayItem(SocialNetwork.TELEGRAM_BOT, "تيليجرام بوت", "Telegram Bot", Icons.Default.Send, Color(0xFF229ED9), true),
    SocialChannelDisplayItem(SocialNetwork.TELEGRAM_MINI_APP, "تطبيق تيليجرام مصغر (TMA)", "Telegram Mini App", Icons.Default.SmartToy, Color(0xFF0088CC), true),
    SocialChannelDisplayItem(SocialNetwork.INSTAGRAM_GRAPH, "إنستغرام دايركت", "Instagram Direct", Icons.Default.CameraAlt, Color(0xFFE1306C), true),
    SocialChannelDisplayItem(SocialNetwork.FACEBOOK_MESSENGER, "فيسبوك ماسنجر", "Messenger", Icons.Default.Message, Color(0xFF0084FF), true),
    SocialChannelDisplayItem(SocialNetwork.X_PLATFORM, "منصة إكس (تويتر)", "X / Twitter", Icons.Default.Tag, Color(0xFF1DA1F2), true),
    SocialChannelDisplayItem(SocialNetwork.DISCORD_BOT, "ديسكورد ويب هوك", "Discord Embed", Icons.Default.Forum, Color(0xFF5865F2), true),
    SocialChannelDisplayItem(SocialNetwork.TIKTOK_SHARE, "تيك توك شير كيت", "TikTok Share", Icons.Default.MusicNote, Color(0xFFEE1D52), false),
    SocialChannelDisplayItem(SocialNetwork.SNAPCHAT_KIT, "سناب شات كريتيف", "Snapchat Kit", Icons.Default.PhotoCamera, Color(0xFFFFFC00), false),
    SocialChannelDisplayItem(SocialNetwork.LINKEDIN_SHARE, "لينكد إن للأعمال", "LinkedIn InMail", Icons.Default.Work, Color(0xFF0077B5), true),
    SocialChannelDisplayItem(SocialNetwork.EMAIL_GATEWAY, "بريد إلكتروني رسمي", "Official Email", Icons.Default.Email, Color(0xFFEA4335), true),
    SocialChannelDisplayItem(SocialNetwork.SMS_GATEWAY, "رسالة نصية SMS", "Verified SMS", Icons.Default.Sms, Color(0xFF34B7F1), true)
  )

  ModalBottomSheet(
    onDismissRequest = dismissAction,
    sheetState = sheetState,
    containerColor = SurfaceDark,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = modifier.testTag("social_share_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Header with Close Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(EmeraldPrimary),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(20.dp))
          }
          Column {
            Text(
              text = if (isRtl) "مشاركة الطلب عبر 12 شبكة اجتماعية" else "Multi-Channel Social Dispatch",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White
            )
            Text(
              text = if (isRtl) "تكامل رسمي ومباشر دون حاجة لتثبيت التطبيق لدى المستلم" else "Official API deep links & universal web embeds",
              fontSize = 11.sp,
              color = Color.Gray
            )
          }
        }

        IconButton(onClick = dismissAction) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
        }
      }

      // 2. Transaction summary card
      Surface(
        color = BackgroundDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = transaction.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            Text(text = "#${transaction.id} • ${transaction.recipientName}", fontSize = 10.sp, color = Color.Gray)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "${String.format("%,.2f", transaction.amount)} ${transaction.currency}",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = EmeraldPrimary
            )
            Text(text = "رسوم 0.80$ (0% ربا)", fontSize = 9.sp, color = GoldAccent)
          }
        }
      }

      // 3. Social Networks Grid (3 columns)
      Text(
        text = if (isRtl) "اختر القناة الاجتماعية للإرسال المباشر:" else "Select Channel for Direct Dispatch:",
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = Color.LightGray
      )

      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 280.dp)
      ) {
        items(channels) { item ->
          Surface(
            color = BackgroundDark,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, item.brandColor.copy(alpha = 0.35f)),
            modifier = Modifier
              .clickable {
                dispatchToChannel(context, item.network, transaction, isRtl)
              }
              .height(72.dp)
          ) {
            Column(
              modifier = Modifier.padding(8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(item.brandColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = null,
                  tint = if (item.brandColor == Color(0xFFFFFC00)) Color.Yellow else item.brandColor,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (isRtl) item.nameAr else item.nameEn,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
              )
            }
          }
        }
      }

      // 4. Quick Actions (Copy Link, Copy Web Embed Snippet)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            clipboardManager.setText(AnnotatedString(transaction.shareUrl))
            Toast.makeText(context, if (isRtl) "تم نسخ رابط السداد المباشر" else "Payment link copied", Toast.LENGTH_SHORT).show()
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isRtl) "نسخ الرابط" else "Copy Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = {
            val embedCode = """<iframe src="${transaction.shareUrl}" width="100%" height="480" frameborder="0"></iframe>"""
            clipboardManager.setText(AnnotatedString(embedCode))
            Toast.makeText(context, if (isRtl) "تم نسخ كود التضمين للمواقع" else "Web embed iframe snippet copied", Toast.LENGTH_SHORT).show()
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isRtl) "كود التضمين" else "Embed Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

/**
 * Dispatches intent to specific social network adapter
 */
private fun dispatchToChannel(
  context: Context,
  network: SocialNetwork,
  transaction: TransactionEntity,
  isRtl: Boolean
) {
  val paymentIntent = PaymentIntent(
    intentId = "PI-${transaction.id}",
    transactionId = transaction.id,
    amount = transaction.amount,
    currencyTicker = transaction.currency,
    assetId = "${transaction.currency}_FIAT",
    network = com.example.core.asset.BlockchainNetwork.FIAT_BANKING,
    senderUserId = "USR-CURRENT",
    senderName = transaction.senderName,
    recipientUserId = "USR-RECIPIENT",
    recipientName = transaction.recipientName,
    paymentLink = transaction.shareUrl,
    qrCodePayload = transaction.qrCodeData,
    idempotencyKey = transaction.idempotencyKey
  )

  val adapter = SocialAdapterRegistry.getAdapter(network)
  val intentUrl = adapter.formatShareIntentUrl(paymentIntent, transaction.recipientContact)

  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl)).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
  } catch (_: Exception) {
    // If native package not installed, fall back to universal Chooser share
    adapter.triggerNativeShare(context, paymentIntent)
  }
}
