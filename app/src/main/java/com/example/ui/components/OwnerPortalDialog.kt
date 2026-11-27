package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.security.OwnerPinAttemptGuard
import com.example.security.OwnerSecurityHelper
import com.example.ui.theme.*

@Composable
fun OwnerPortalDialog(
  isRtl: Boolean,
  isUserSignedIn: Boolean,
  signedInEmail: String,
  isEmailVerified: Boolean,
  isPinConfigured: Boolean?,
  isOwnerUnlocked: Boolean,
  isBusy: Boolean,
  ownerError: String?,
  platformSettings: OwnerPlatformSettings,
  allPartners: List<PartnerApplicationItem>,
  allFeedback: List<UserFeedbackItem>,
  onSignInAsOwner: () -> Unit,
  onSetupInitialPin: (String) -> Unit,
  onVerifyOwnerPin: (String) -> Unit,
  onChangeOwnerPin: (String, String) -> Unit,
  onUpdatePlatformSettings: (OwnerPlatformSettings) -> Unit,
  onUpdatePartnerStatus: (String, PartnerStatus, Double, String) -> Unit,
  onUpdateFeedbackStatus: (String, FeedbackStatus, String) -> Unit,
  onLockAndClose: () -> Unit
) {
  val isVerifiedOwnerAccount = isUserSignedIn &&
    OwnerSecurityHelper.isPlatformOwnerEmail(signedInEmail) &&
    isEmailVerified

  var enteredPin by remember { mutableStateOf("") }
  var setupConfirmPin by remember { mutableStateOf("") }
  var isConfirmingNewPin by remember { mutableStateOf(false) }
  var localError by remember { mutableStateOf<String?>(null) }
  val attemptGuard = remember { OwnerPinAttemptGuard() }
  val scrambledDigits = remember { (0..9).shuffled() }

  LaunchedEffect(ownerError) {
    if (!ownerError.isNullOrBlank()) {
      attemptGuard.recordFailure()
      enteredPin = ""
    }
  }

  // Control Panel Editable State
  var selectedAdminTab by remember { mutableStateOf(0) } // 0: Settings, 1: PIN Security, 2: Partners, 3: Feedback
  var feeInput by remember(platformSettings.fixedFeeUsd) { mutableStateOf(platformSettings.fixedFeeUsd.toString()) }
  var shareInput by remember(platformSettings.partnerCommissionPercent) { mutableStateOf(platformSettings.partnerCommissionPercent.toString()) }
  var antiPhishInput by remember(platformSettings.antiPhishingWord) { mutableStateOf(platformSettings.antiPhishingWord) }
  var allowPartnersToggle by remember(platformSettings.allowPartnerRegistrations) { mutableStateOf(platformSettings.allowPartnerRegistrations) }
  var maintenanceToggle by remember(platformSettings.maintenanceMode) { mutableStateOf(platformSettings.maintenanceMode) }

  var currentPinChangeInput by remember { mutableStateOf("") }
  var newPinChangeInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onLockAndClose,
    containerColor = BrandSecondaryBg,
    titleContentColor = BrandTextPrimary,
    textContentColor = BrandTextPrimary,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(GoldAccent.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldAccent)
        }
        Column {
          Text(
            text = if (isRtl) "بوابة المالك السرية والتحكم" else "Owner Portal & Control",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = if (isOwnerUnlocked) {
              if (isRtl) "الجلسة مفعلة • صلاحية تعديل كاملة" else "Unlocked • Full Governance Control"
            } else {
              if (isRtl) "وصول محصور برقم سري مشفر للمالك فقط" else "Restricted to Verified Owner + Secret PIN"
            },
            fontSize = 11.sp,
            color = if (isOwnerUnlocked) BrandPrimaryAccent else GoldAccent
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 500.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (!isVerifiedOwnerAccount) {
          Surface(
            color = BrandPrimaryBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = if (isRtl)
                  "لأمان التطبيق، يتطلب دخول المالك تسجيل الدخول أولاً بحساب Google الموثق الخاص بالمالك (${OwnerSecurityHelper.PLATFORM_OWNER_EMAIL}) ثم إدخال الرقم السري المشفر."
                else
                  "Owner access requires signing in with the verified platform owner Google account (${OwnerSecurityHelper.PLATFORM_OWNER_EMAIL}) before PIN verification.",
                fontSize = 12.sp,
                color = BrandTextPrimary
              )
              if (isUserSignedIn) {
                Text(
                  text = (if (isRtl) "الحساب الحالي: " else "Current session: ") + signedInEmail.ifBlank { "-" },
                  fontSize = 11.sp,
                  color = StatusError
                )
              }
              Button(
                onClick = onSignInAsOwner,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BrandPrimaryBg),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("owner_google_signin_btn")
              ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  if (isRtl) "تسجيل الدخول بحساب المالك" else "Sign In with Owner Account",
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        } else if (!isOwnerUnlocked) {
          Text(
            text = if (isRtl) "أدخل الرقم السري للمالك (لوحة مبعثرة للحماية)" else "Enter 6-Digit Owner Secret PIN",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = GoldAccent
          )

          if (isPinConfigured != null) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
              repeat(6) { idx ->
                val filled = idx < enteredPin.length
                Box(
                  modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(if (filled) BrandPrimaryAccent else BrandPrimaryBg)
                    .border(1.5.dp, if (filled) BrandPrimaryAccent else BrandSurfaceBorder, CircleShape)
                )
              }
            }

            (localError ?: ownerError)?.let { err ->
              Text(text = err, color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            LazyVerticalGrid(
              columns = GridCells.Fixed(3),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .width(220.dp)
                .heightIn(max = 236.dp)
                .align(Alignment.CenterHorizontally)
            ) {
              items(scrambledDigits) { digit ->
                Surface(
                  color = BrandPrimaryBg,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, BrandSurfaceBorder),
                  modifier = Modifier
                    .size(50.dp)
                    .clickable(enabled = !isBusy) {
                      localError = null
                      if (attemptGuard.isLockedOut()) {
                        localError = if (isRtl) {
                          "تم القفل مؤقتاً (${attemptGuard.remainingLockoutSeconds()} ث)"
                        } else {
                          "Locked out for ${attemptGuard.remainingLockoutSeconds()}s"
                        }
                        return@clickable
                      }
                      if (enteredPin.length < 6) {
                        enteredPin += digit.toString()
                        if (enteredPin.length == 6) {
                          val pinToCheck = enteredPin
                          enteredPin = ""
                          onVerifyOwnerPin(pinToCheck)
                        }
                      }
                    }
                    .testTag("owner_pin_key_$digit")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(digit.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandTextPrimary)
                  }
                }
              }

              item {
                Surface(
                  color = BrandPrimaryBg,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .size(50.dp)
                    .clickable {
                      enteredPin = ""
                      localError = null
                    }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = BrandTextSecondary, modifier = Modifier.size(18.dp))
                  }
                }
              }

              item {
                Surface(
                  color = BrandPrimaryBg,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .size(50.dp)
                    .clickable {
                      if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                    }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = BrandTextSecondary, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        } else {
          // Unlocked Owner Governance Panel
          ScrollableTabRow(
            selectedTabIndex = selectedAdminTab,
            edgePadding = 4.dp,
            containerColor = BrandPrimaryBg,
            contentColor = GoldAccent
          ) {
            val tabs = if (isRtl) {
              listOf("الإعدادات", "الرقم السري", "الشركاء (${allPartners.size})", "التعليقات (${allFeedback.size})")
            } else {
              listOf("Settings", "Secret PIN", "Partners (${allPartners.size})", "Feedback (${allFeedback.size})")
            }
            tabs.forEachIndexed { idx, label ->
              Tab(
                selected = selectedAdminTab == idx,
                onClick = { selectedAdminTab = idx },
                text = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
              )
            }
          }

          when (selectedAdminTab) {
            0 -> {
              // Platform Governance Settings
              OutlinedTextField(
                value = feeInput,
                onValueChange = { feeInput = it },
                label = { Text(if (isRtl) "الرسوم الثابتة للصفقة (USD)" else "Flat Deal Fee (USD)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("owner_fee_input")
              )
              OutlinedTextField(
                value = shareInput,
                onValueChange = { shareInput = it },
                label = { Text(if (isRtl) "نسبة عمولة الشركاء (%)" else "Partner Commission Share (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("owner_share_input")
              )
              OutlinedTextField(
                value = antiPhishInput,
                onValueChange = { antiPhishInput = it },
                label = { Text(if (isRtl) "ختم مكافحة التصيد المعتمد" else "Platform Anti-Phishing Seal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("owner_antiphish_input")
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(if (isRtl) "السماح بتسجيل شركاء جدد" else "Allow Partner Registrations", fontSize = 12.sp)
                Switch(checked = allowPartnersToggle, onCheckedChange = { allowPartnersToggle = it })
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(if (isRtl) "وضع الصيانة الاحترازي" else "Maintenance Mode", fontSize = 12.sp)
                Switch(checked = maintenanceToggle, onCheckedChange = { maintenanceToggle = it })
              }

              Button(
                onClick = {
                  val fee = feeInput.toDoubleOrNull() ?: platformSettings.fixedFeeUsd
                  val share = shareInput.toDoubleOrNull() ?: platformSettings.partnerCommissionPercent
                  onUpdatePlatformSettings(
                    OwnerPlatformSettings(
                      fixedFeeUsd = fee,
                      partnerCommissionPercent = share,
                      antiPhishingWord = antiPhishInput,
                      allowPartnerRegistrations = allowPartnersToggle,
                      maintenanceMode = maintenanceToggle
                    )
                  )
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("owner_save_settings_btn")
              ) {
                Text(if (isRtl) "حفظ تعديلات المنصة فوراً" else "Save Platform Changes", fontWeight = FontWeight.Bold)
              }
            }

            1 -> {
              // Change Secret Owner PIN
              OutlinedTextField(
                value = currentPinChangeInput,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) currentPinChangeInput = it },
                label = { Text(if (isRtl) "الرقم السري الحالي (6 أرقام)" else "Current 6-Digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = newPinChangeInput,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPinChangeInput = it },
                label = { Text(if (isRtl) "الرقم السري الجديد القوي (6 أرقام)" else "New Strong 6-Digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              Button(
                onClick = {
                  onChangeOwnerPin(currentPinChangeInput, newPinChangeInput)
                  currentPinChangeInput = ""
                  newPinChangeInput = ""
                },
                enabled = !isBusy && currentPinChangeInput.length == 6 && newPinChangeInput.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BrandPrimaryBg),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
              ) {
                Text(if (isRtl) "تحديث الرقم السري للمالك" else "Update Owner Secret PIN", fontWeight = FontWeight.Bold)
              }
            }

            2 -> {
              // Partner Applications Moderation
              if (allPartners.isEmpty()) {
                Text(if (isRtl) "لا توجد طلبات شراكة مسجلة بعد." else "No partner applications yet.", fontSize = 12.sp, color = BrandTextSecondary)
              } else {
                allPartners.forEach { partner ->
                  Surface(
                    color = BrandPrimaryBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(partner.companyName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(if (isRtl) partner.status.titleAr else partner.status.titleEn, color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                      Text(
                        "${if (isRtl) partner.sector.titleAr else partner.sector.titleEn} • ${partner.countryCode} • ${partner.commissionPercent}%",
                        fontSize = 11.sp,
                        color = BrandTextSecondary
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                          onClick = { onUpdatePartnerStatus(partner.id, PartnerStatus.APPROVED, 50.0, "Approved by Owner") },
                          modifier = Modifier.weight(1f)
                        ) {
                          Text(if (isRtl) "اعتماد" else "Approve", fontSize = 11.sp, color = BrandPrimaryAccent)
                        }
                        OutlinedButton(
                          onClick = { onUpdatePartnerStatus(partner.id, PartnerStatus.ACTIVE, 55.0, "Activated VIP Partner") },
                          modifier = Modifier.weight(1f)
                        ) {
                          Text(if (isRtl) "تفعيل 55%" else "Activate", fontSize = 11.sp, color = GoldAccent)
                        }
                        OutlinedButton(
                          onClick = { onUpdatePartnerStatus(partner.id, PartnerStatus.SUSPENDED, partner.commissionPercent, "Paused by Owner") },
                          modifier = Modifier.weight(1f)
                        ) {
                          Text(if (isRtl) "إيقاف" else "Pause", fontSize = 11.sp, color = StatusError)
                        }
                      }
                    }
                  }
                }
              }
            }

            3 -> {
              // User Feedback Inbox Moderation
              if (allFeedback.isEmpty()) {
                Text(if (isRtl) "لا توجد تعليقات مستخدمين بعد." else "No user feedback yet.", fontSize = 12.sp, color = BrandTextSecondary)
              } else {
                allFeedback.forEach { fb ->
                  Surface(
                    color = BrandPrimaryBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${fb.authorName} (${fb.rating}★)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(if (isRtl) fb.status.titleAr else fb.status.titleEn, color = BrandPrimaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      }
                      Text(fb.message, fontSize = 11.sp)
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                          onClick = { onUpdateFeedbackStatus(fb.id, FeedbackStatus.REVIEWED, "Under review") },
                          modifier = Modifier.weight(1f)
                        ) {
                          Text(if (isRtl) "مراجعة" else "Reviewed", fontSize = 11.sp)
                        }
                        OutlinedButton(
                          onClick = { onUpdateFeedbackStatus(fb.id, FeedbackStatus.RESOLVED, "Implemented in Social Pay") },
                          modifier = Modifier.weight(1f)
                        ) {
                          Text(if (isRtl) "تم التنفيذ" else "Resolve", fontSize = 11.sp, color = BrandPrimaryAccent)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onLockAndClose) {
        Text(
          text = if (isOwnerUnlocked) {
            if (isRtl) "قفل وإغلاق" else "Lock & Close"
          } else {
            if (isRtl) "إغلاق" else "Close"
          },
          color = BrandTextSecondary
        )
      }
    }
  )
}
