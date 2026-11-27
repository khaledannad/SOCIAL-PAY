package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ContactEntity
import com.example.ui.theme.*

@Composable
fun SocialContactsScreen(
  contacts: List<ContactEntity>,
  isRtl: Boolean,
  onAddContact: (name: String, handleOrPhone: String, channel: String) -> Unit,
  onInitiatePaymentToContact: (ContactEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddDialog by remember { mutableStateOf(false) }

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
            text = if (isRtl) "جهات الاتصال والمتعاملون الموثقون" else "Verified Social Contacts",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = if (isRtl) "قائمة العملاء والمتاجر المعتمدة لسرعة إرسال وطلب الأموال" else "Verified contacts for instant social requests & billing",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Button(
          onClick = { showAddDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("add_contact_btn")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isRtl) "إضافة جهة" else "Add Contact", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
      }
    }

    // 2. Contacts List
    items(contacts) { contact ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Text(text = contact.avatarInitials, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary, fontSize = 14.sp)
            }

            Column {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (contact.isVerified) {
                  Icon(Icons.Default.Verified, contentDescription = "Verified", tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                }
              }
              Text(text = contact.phoneOrHandle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = GoldAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                  Text(
                    text = "ثقة ${contact.trustScore}%",
                    color = GoldAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
                Text(text = "${contact.totalTransactionsCount} معاملة ناجحة", fontSize = 9.sp, color = EmeraldLight)
              }
            }
          }

          Button(
            onClick = { onInitiatePaymentToContact(contact) },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.Send, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isRtl) "طلب دفع" else "Request", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  // Add Contact Dialog
  if (showAddDialog) {
    var nameInput by remember { mutableStateOf("") }
    var handleInput by remember { mutableStateOf("") }
    var channelInput by remember { mutableStateOf("WHATSAPP") }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text(if (isRtl) "إضافة جهة اتصال موثقة" else "Add Verified Contact") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text(if (isRtl) "الاسم الكامل" else "Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = handleInput,
            onValueChange = { handleInput = it },
            label = { Text(if (isRtl) "رقم الهاتف أو المعرف الاجتماعي" else "Phone / Social Handle") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Text(text = if (isRtl) "القناة المفضلة:" else "Preferred Channel:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = channelInput == "WHATSAPP",
              onClick = { channelInput = "WHATSAPP" },
              label = { Text("WhatsApp", fontSize = 11.sp) }
            )
            FilterChip(
              selected = channelInput == "TELEGRAM",
              onClick = { channelInput = "TELEGRAM" },
              label = { Text("Telegram", fontSize = 11.sp) }
            )
            FilterChip(
              selected = channelInput == "INSTAGRAM",
              onClick = { channelInput = "INSTAGRAM" },
              label = { Text("Instagram", fontSize = 11.sp) }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (nameInput.isNotBlank()) {
              onAddContact(nameInput, handleInput, channelInput)
              showAddDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
          Text(if (isRtl) "حفظ في Room" else "Save Contact", color = BackgroundDark, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text(if (isRtl) "إلغاء" else "Cancel")
        }
      }
    )
  }
}
