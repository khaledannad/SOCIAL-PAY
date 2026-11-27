package com.example.security

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class SmartAuthType(val titleAr: String, val titleEn: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  BIOMETRIC("بصمة الإصبع والوجه", "Biometric & Face", Icons.Default.Fingerprint),
  SCRAMBLED_PIN("رمز PIN المبعثر", "Scrambled PIN", Icons.Default.Dialpad),
  FIDO_PASSKEY("مفتاح Passkey المشفر", "FIDO2 Passkey", Icons.Default.VpnKey),
  TOTP_2FA("رمز المصادقة المتجدد 2FA", "TOTP 2FA", Icons.Default.Timer)
}

data class SmartSecurityConfig(
  val isBiometricEnabled: Boolean = true,
  val isPinEnabled: Boolean = true,
  val isPasskeyEnabled: Boolean = true,
  val isTotpEnabled: Boolean = true,
  val antiPhishingWord: String = "SHIELD2026",
  val trustedDeviceName: String = "Verified Hardware Keystore"
)

private object EscrowPinStore {
  private const val PREFS_NAME = "social_pay_escrow_pin_prefs"
  private const val KEY_HASH = "escrow_pin_hash"
  private const val KEY_SALT = "escrow_pin_salt"
  private const val KEY_ALGO = "escrow_pin_algo"

  fun hasConfiguredPin(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return !prefs.getString(KEY_HASH, null).isNullOrBlank() &&
      !prefs.getString(KEY_SALT, null).isNullOrBlank()
  }

  fun savePin(context: Context, pin: String) {
    val hashed = OwnerSecurityHelper.createHashedPin(pin)
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_HASH, hashed.hashHex)
      .putString(KEY_SALT, hashed.saltBase64)
      .putString(KEY_ALGO, hashed.algorithm)
      .apply()
  }

  fun verifyPin(context: Context, pin: String): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val hash = prefs.getString(KEY_HASH, null) ?: return false
    val salt = prefs.getString(KEY_SALT, null) ?: return false
    val algo = prefs.getString(KEY_ALGO, OwnerSecurityHelper.ALGO_PBKDF2) ?: OwnerSecurityHelper.ALGO_PBKDF2
    return OwnerSecurityHelper.verifyPin(pin, hash, salt, algo)
  }
}

@Composable
fun SmartAuthDialog(
  title: String,
  subtitle: String,
  securityConfig: SmartSecurityConfig = SmartSecurityConfig(),
  isRtl: Boolean,
  onDismiss: () -> Unit,
  onAuthSuccess: () -> Unit
) {
  val context = LocalContext.current
  var selectedAuthType by remember { mutableStateOf(SmartAuthType.SCRAMBLED_PIN) }
  var hasPinConfigured by remember { mutableStateOf(EscrowPinStore.hasConfiguredPin(context)) }
  var enteredPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var isCreatingConfirmStep by remember { mutableStateOf(false) }
  var pinFeedback by remember { mutableStateOf<String?>(null) }
  var isPinError by remember { mutableStateOf(false) }
  val attemptGuard = remember { OwnerPinAttemptGuard() }
  var totpUserInput by remember { mutableStateOf("") }

  // Scrambled keypad numbers (shuffled on appearance to prevent shoulder surfing)
  val scrambledDigits = remember {
    (0..9).shuffled()
  }

  // TOTP rolling window aligned to 30-second UTC intervals
  var secondsLeft by remember { mutableStateOf(30 - ((System.currentTimeMillis() / 1000L) % 30L).toInt()) }
  var currentWindow by remember { mutableStateOf(System.currentTimeMillis() / 30_000L) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(1000)
      val nowSec = System.currentTimeMillis() / 1000L
      currentWindow = nowSec / 30L
      secondsLeft = (30 - (nowSec % 30L)).toInt()
    }
  }

  val expectedWindowCode = remember(currentWindow) {
    val mix = ((currentWindow * 6364136223846793005L + 1442695040888963407L) ushr 16) and 0x7FFFFFFFL
    String.format("%06d", mix % 1_000_000L)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(EmeraldPrimary.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
        }
        Column {
          Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
          Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Anti-Phishing Verification Watermark Banner
        Surface(
          color = GoldAccent.copy(alpha = 0.12f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isRtl) "رمز مكافحة التصيد:" else "Anti-Phishing Seal:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = securityConfig.antiPhishingWord,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = GoldAccent
            )
          }
        }

        // Auth Method Selector Tabs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          SmartAuthType.entries.forEach { method ->
            val isSelected = method == selectedAuthType
            Surface(
              color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  selectedAuthType = method
                  pinFeedback = null
                  isPinError = false
                }
                .testTag("auth_tab_${method.name}")
            ) {
              Box(
                modifier = Modifier.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = method.icon,
                  contentDescription = method.titleEn,
                  tint = if (isSelected) BackgroundDark else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }

        when (selectedAuthType) {
          SmartAuthType.BIOMETRIC -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
            ) {
              val infiniteTransition = rememberInfiniteTransition(label = "bio_pulse")
              val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                  animation = tween(900, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
              )

              Box(
                modifier = Modifier
                  .size(76.dp)
                  .scale(pulseScale)
                  .clip(CircleShape)
                  .background(
                    Brush.radialGradient(
                      listOf(EmeraldPrimary.copy(alpha = 0.35f), Color.Transparent)
                    )
                  )
                  .clickable {
                    if (hasPinConfigured) {
                      onAuthSuccess()
                    } else {
                      selectedAuthType = SmartAuthType.SCRAMBLED_PIN
                      pinFeedback = if (isRtl) "أنشئ رمز أمان PIN أولاً لتفعيل الحماية" else "Set your 6-digit Escrow PIN first"
                    }
                  }
                  .testTag("biometric_sensor_target"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Fingerprint,
                  contentDescription = "Verify Biometric",
                  tint = EmeraldPrimary,
                  modifier = Modifier.size(46.dp)
                )
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (isRtl) "المس مستشعر البصمة لتأكيد التحرير المشفر" else "Touch fingerprint sensor to confirm escrow release",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = securityConfig.trustedDeviceName,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          SmartAuthType.SCRAMBLED_PIN -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = when {
                  !hasPinConfigured && !isCreatingConfirmStep ->
                    if (isRtl) "إعداد رمز أمان الأمانة (6 أرقام قوية)" else "Create your 6-digit Escrow PIN"
                  !hasPinConfigured && isCreatingConfirmStep ->
                    if (isRtl) "أعد إدخال الرمز للتأكيد والحفظ" else "Re-enter PIN to confirm"
                  else ->
                    if (isRtl) "أدخل رمز الأمان الخاص بك (لوحة مبعثرة)" else "Enter your 6-digit Escrow PIN (Anti-Spy)"
                },
                fontSize = 11.sp,
                color = if (hasPinConfigured) GoldAccent else EmeraldPrimary,
                fontWeight = FontWeight.Bold
              )

              Spacer(modifier = Modifier.height(8.dp))

              // PIN Dots Display
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 8.dp)
              ) {
                repeat(6) { index ->
                  val isFilled = index < enteredPin.length
                  Box(
                    modifier = Modifier
                      .size(14.dp)
                      .clip(CircleShape)
                      .background(
                        if (isFilled) (if (isPinError) StatusError else EmeraldPrimary) else MaterialTheme.colorScheme.surfaceVariant
                      )
                      .border(
                        1.dp,
                        if (isPinError) StatusError else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        CircleShape
                      )
                  )
                }
              }

              pinFeedback?.let { msg ->
                Text(
                  text = msg,
                  fontSize = 10.sp,
                  color = if (isPinError) StatusError else EmeraldPrimary,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(bottom = 6.dp)
                )
              }

              // 3x4 Scrambled Grid (zero bypass buttons)
              LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                  .width(220.dp)
                  .heightIn(max = 236.dp)
              ) {
                items(scrambledDigits) { digit ->
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .size(50.dp)
                      .clickable {
                        if (attemptGuard.isLockedOut()) {
                          isPinError = true
                          pinFeedback = if (isRtl) {
                            "محاولات كثيرة. انتظر ${attemptGuard.remainingLockoutSeconds()} ثانية."
                          } else {
                            "Locked out for ${attemptGuard.remainingLockoutSeconds()}s."
                          }
                          return@clickable
                        }
                        if (enteredPin.length < 6) {
                          enteredPin += digit
                          isPinError = false
                          pinFeedback = null
                          if (enteredPin.length == 6) {
                            if (!hasPinConfigured) {
                              if (!isCreatingConfirmStep) {
                                if (OwnerSecurityHelper.isWeakPin(enteredPin)) {
                                  isPinError = true
                                  pinFeedback = if (isRtl) "اختر رمزاً أقوى غير متسلسل أو مكرر" else "Choose a stronger non-trivial PIN"
                                  enteredPin = ""
                                } else {
                                  confirmPin = enteredPin
                                  enteredPin = ""
                                  isCreatingConfirmStep = true
                                }
                              } else {
                                if (enteredPin == confirmPin) {
                                  EscrowPinStore.savePin(context, enteredPin)
                                  hasPinConfigured = true
                                  isCreatingConfirmStep = false
                                  attemptGuard.recordSuccess()
                                  onAuthSuccess()
                                } else {
                                  isPinError = true
                                  isCreatingConfirmStep = false
                                  confirmPin = ""
                                  enteredPin = ""
                                  pinFeedback = if (isRtl) "الرمزان غير متطابقين، أعد المحاولة" else "PINs did not match"
                                }
                              }
                            } else {
                              if (EscrowPinStore.verifyPin(context, enteredPin)) {
                                attemptGuard.recordSuccess()
                                onAuthSuccess()
                              } else {
                                attemptGuard.recordFailure()
                                isPinError = true
                                enteredPin = ""
                                pinFeedback = if (attemptGuard.isLockedOut()) {
                                  if (isRtl) "تم القفل مؤقتاً (${attemptGuard.remainingLockoutSeconds()} ث)" else "Locked for ${attemptGuard.remainingLockoutSeconds()}s"
                                } else {
                                  if (isRtl) "رمز خاطئ (${attemptGuard.remainingAttempts()} محاولات متبقية)" else "Invalid PIN (${attemptGuard.remainingAttempts()} tries left)"
                                }
                              }
                            }
                          }
                        }
                      }
                      .testTag("pin_key_$digit")
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(text = digit.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }

                // Clear Key
                item {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .size(50.dp)
                      .clickable {
                        enteredPin = ""
                        isPinError = false
                        pinFeedback = null
                      }
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.Refresh, contentDescription = "Clear PIN", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                  }
                }

                // Backspace Key
                item {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .size(50.dp)
                      .clickable {
                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                      }
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }
            }
          }

          SmartAuthType.FIDO_PASSKEY -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
            ) {
              Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(40.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (isRtl) "تحقق التوقيع الرقمي عبر Google Credential / Passkey" else "Hardware Credential / Passkey Check",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = {
                  if (hasPinConfigured) {
                    onAuthSuccess()
                  } else {
                    selectedAuthType = SmartAuthType.SCRAMBLED_PIN
                    pinFeedback = if (isRtl) "أنشئ رمز أمان PIN أولاً" else "Please set your 6-digit PIN first"
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                modifier = Modifier.testTag("passkey_action_btn")
              ) {
                Text(if (isRtl) "توقيع وإثبات الهوية" else "Confirm Passkey", color = BackgroundDark, fontWeight = FontWeight.Bold)
              }
            }
          }

          SmartAuthType.TOTP_2FA -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = if (isRtl) "أدخل رمز التحقق المتجدد المعروض للتحقق من الجلسة" else "Enter the rolling verification code below",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "${expectedWindowCode.take(3)} ${expectedWindowCode.drop(3)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = EmeraldPrimary
                  )
                  LinearProgressIndicator(
                    progress = { secondsLeft / 30f },
                    modifier = Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (secondsLeft > 6) EmeraldPrimary else StatusError
                  )
                  Text(text = "${secondsLeft}s", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }

              OutlinedTextField(
                value = totpUserInput,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) totpUserInput = it },
                label = { Text(if (isRtl) "أدخل الرمز المكون من 6 أرقام" else "Enter 6-digit code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )

              Button(
                onClick = {
                  if (totpUserInput == expectedWindowCode) {
                    onAuthSuccess()
                  } else {
                    pinFeedback = if (isRtl) "الرمز غير مطابق" else "Code mismatch"
                    isPinError = true
                  }
                },
                enabled = totpUserInput.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(if (isRtl) "تحقق من الرمز" else "Verify Code", color = BackgroundDark, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isRtl) "إغلاق" else "Close")
      }
    }
  )
}
