package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.governance.tr
import com.example.model.AppLanguage
import com.example.ui.theme.*

@Composable
fun TopNavBar(
  currentLanguage: AppLanguage,
  currentFeeUsd: Double,
  unreadNotificationsCount: Int = 0,
  showOwnerWorkspaceSwitch: Boolean = false,
  onOpenOwnerWorkspace: () -> Unit = {},
  onLanguageSelected: (AppLanguage) -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenContacts: () -> Unit,
  onOpenOffice: () -> Unit,
  onOpenAiChatbot: () -> Unit,
  onOpenWalletQr: () -> Unit = {},
  onBrandClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showLanguageMenu by remember { mutableStateOf(false) }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .statusBarsPadding()
        .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // App Identity Brand with Unified Logo
        Box(modifier = Modifier.clickable { onBrandClick() }) {
          SocialPayLogo(
            style = SocialPayLogoStyle.FULL_HORIZONTAL,
            iconSize = 32.dp,
            showTagline = false
          )
        }

        // Action Indicators
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          // Flat Protocol Fee Pill
          Surface(
            color = GoldAccent.copy(alpha = 0.15f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
          ) {
            Text(
              text = "$${String.format(java.util.Locale.US, "%.2f", currentFeeUsd)}",
              color = GoldAccent,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.sp,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }

          // Owner Command Center Switch (Rendered ONLY when verified Owner has unlocked session)
          if (showOwnerWorkspaceSwitch) {
            Surface(
              color = BrandIntelligenceAccent.copy(alpha = 0.22f),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, BrandIntelligenceAccent.copy(alpha = 0.65f)),
              modifier = Modifier
                .clickable { onOpenOwnerWorkspace() }
                .testTag("top_owner_command_switch")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = tr(currentLanguage, "مركز قيادة المالك", "Owner Command", "Centre Propriétaire"),
                  tint = BrandIntelligenceAccent,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = tr(currentLanguage, "القيادة", "Command", "Admin"),
                  color = BrandTextPrimary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          // Wallet QR Quick Request Button
          IconButton(
            onClick = onOpenWalletQr,
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(BrandPrimaryAccent.copy(alpha = 0.18f))
              .testTag("top_wallet_qr_btn")
          ) {
            Icon(
              imageVector = Icons.Default.QrCode2,
              contentDescription = tr(currentLanguage, "رمز QR للمحفظة", "Wallet QR", "QR Portefeuille"),
              tint = BrandPrimaryAccent,
              modifier = Modifier.size(18.dp)
            )
          }

          // Notifications Button with Unread Badge
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag("top_notifications_btn")
          ) {
            BadgedBox(
              badge = {
                if (unreadNotificationsCount > 0) {
                  Badge(containerColor = EmeraldPrimary) {
                    Text(unreadNotificationsCount.toString(), fontSize = 8.sp)
                  }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = tr(currentLanguage, "الإشعارات", "Notifications", "Notifications"),
                tint = if (unreadNotificationsCount > 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
              )
            }
          }

          // Contacts Button
          IconButton(
            onClick = onOpenContacts,
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag("top_contacts_btn")
          ) {
            Icon(
              imageVector = Icons.Default.People,
              contentDescription = tr(currentLanguage, "جهات الاتصال", "Contacts", "Contacts"),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(17.dp)
            )
          }

          // AI Genius Brain Assistant Button
          IconButton(
            onClick = onOpenAiChatbot,
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(listOf(EmeraldPrimary.copy(alpha = 0.25f), BrandIntelligenceAccent.copy(alpha = 0.28f)))
              )
              .testTag("top_ai_chatbot_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = tr(currentLanguage, "المساعد الذكي", "AI Assistant", "Assistant IA"),
              tint = BrandPrimaryAccent,
              modifier = Modifier.size(18.dp)
            )
          }

          // Profile / Private Office Button
          IconButton(
            onClick = onOpenOffice,
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(EmeraldPrimary.copy(alpha = 0.2f))
              .testTag("top_office_btn")
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = tr(currentLanguage, "المكتب الخاص والملف", "Private Office", "Bureau Privé"),
              tint = EmeraldPrimary,
              modifier = Modifier.size(18.dp)
            )
          }

          // Language Selector
          Box {
            IconButton(
              onClick = { showLanguageMenu = true },
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("language_selector_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = tr(currentLanguage, "تغيير اللغة", "Change Language", "Changer de langue"),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
              )
            }

            if (showLanguageMenu) {
              AlertDialog(
                onDismissRequest = { showLanguageMenu = false },
                containerColor = BrandSecondaryBg,
                title = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = EmeraldPrimary)
                    Text(
                      text = tr(currentLanguage, "اختر لغة التطبيق", "Select Language", "Choisir la langue", "Выберите язык", "选择语言"),
                      fontWeight = FontWeight.Bold,
                      style = MaterialTheme.typography.titleMedium
                    )
                  }
                },
                text = {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                      val isSelected = lang == currentLanguage
                      Surface(
                        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else BrandPrimaryBg,
                        shape = RoundedCornerShape(10.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, EmeraldPrimary) else BorderStroke(1.dp, BrandSurfaceBorder),
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable {
                            showLanguageMenu = false
                            onLanguageSelected(lang)
                          }
                      ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                          ) {
                            Text(text = lang.flag, fontSize = 18.sp)
                            Text(
                              text = lang.displayName,
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                              color = if (isSelected) EmeraldPrimary else BrandTextPrimary
                            )
                          }
                          if (isSelected) {
                            Icon(
                              imageVector = Icons.Default.Check,
                              contentDescription = tr(currentLanguage, "محدد", "Selected", "Sélectionné", "Выбрано", "已选择"),
                              tint = EmeraldPrimary,
                              modifier = Modifier.size(18.dp)
                            )
                          }
                        }
                      }
                    }
                  }
                },
                confirmButton = {},
                dismissButton = {
                  TextButton(onClick = { showLanguageMenu = false }) {
                    Text(tr(currentLanguage, "إغلاق", "Close", "Fermer", "Закрыть", "关闭"), color = EmeraldPrimary)
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}
