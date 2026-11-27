package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SocialNotificationEntity
import com.example.ui.theme.*

@Composable
fun SocialNotificationsScreen(
  notifications: List<SocialNotificationEntity>,
  isRtl: Boolean,
  onMarkAsRead: (String) -> Unit,
  onMarkAllAsRead: () -> Unit,
  onClearAll: () -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    // 1. Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (isRtl) "مركز الإشعارات وتنبيهات المعاملات" else "Notifications & Webhook Alerts",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = if (isRtl) "تنبيهات فورية عند وصول الحوالات البنكية، تحديثات الأمانة، وتقارير الأمان" else "Real-time updates on payments, escrow status & security alerts",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          TextButton(onClick = onMarkAllAsRead) {
            Text(if (isRtl) "قراءة الكل" else "Read All", fontSize = 11.sp, color = EmeraldPrimary)
          }
          IconButton(onClick = onClearAll) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    if (notifications.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            Text(if (isRtl) "لا توجد إشعارات جديدة حالياً" else "No new notifications", fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      items(notifications) { notif ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (!notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!notif.isRead) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                  when (notif.type) {
                    "PAYMENT_CONFIRMED" -> StatusSuccess.copy(alpha = 0.15f)
                    "SECURITY_ALERT" -> StatusError.copy(alpha = 0.15f)
                    "INVOICE_SENT" -> EmeraldPrimary.copy(alpha = 0.15f)
                    else -> GoldAccent.copy(alpha = 0.15f)
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (notif.type) {
                  "PAYMENT_CONFIRMED" -> Icons.Default.Verified
                  "SECURITY_ALERT" -> Icons.Default.Shield
                  "INVOICE_SENT" -> Icons.Default.Receipt
                  else -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = when (notif.type) {
                  "PAYMENT_CONFIRMED" -> StatusSuccess
                  "SECURITY_ALERT" -> StatusError
                  "INVOICE_SENT" -> EmeraldPrimary
                  else -> GoldAccent
                },
                modifier = Modifier.size(20.dp)
              )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (!notif.isRead) {
                  Surface(color = EmeraldPrimary, shape = CircleShape) {
                    Box(modifier = Modifier.size(6.dp))
                  }
                }
              }
              Text(text = notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              notif.transactionId?.let { txnId ->
                Text(text = "رقم المعاملة: #$txnId", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
