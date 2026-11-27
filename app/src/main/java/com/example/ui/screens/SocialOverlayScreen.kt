package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ButtonColorTheme
import com.example.model.ButtonSizePreset
import com.example.model.FloatingButtonConfig
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SocialOverlayScreen(
  uiState: com.example.viewmodel.P2PUiState,
  onUpdateConfig: ((FloatingButtonConfig) -> FloatingButtonConfig) -> Unit,
  onSimulatedAppSelected: (String) -> Unit,
  onOpenQuickSwap: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isRtl = uiState.currentLanguage.isRtl
  val config = uiState.floatingButtonConfig
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header & Explanation
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
      shape = RoundedCornerShape(14.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(EmeraldPrimary.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.SmartButton,
            contentDescription = "Social P2P Widget",
            tint = EmeraldPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = if (isRtl) "محاكي زر P2P على وسائل التواصل" else "Social Media P2P Widget Simulator",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
          )
          Text(
            text = if (isRtl)
              "يمكن للمستخدم تحريك الزر أو تغيير ألوانه أو إخفائه، وبضغطة واحدة يفتح التحويل المالي."
            else
              "Users can drag, customize colors, resize, dock or hide. 1-tap opens P2P transfer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 2. Platform Selector Tabs
    Text(
      text = if (isRtl) "اختر منصة التواصل لتجربة الظهور:" else "Choose Social Platform:",
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold
    )

    val platforms = listOf("Telegram", "WhatsApp", "Instagram", "X", "TikTok")
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(platforms) { platform ->
        val isSelected = uiState.simulatedSocialApp == platform
        Surface(
          color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) EmeraldLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
          ),
          modifier = Modifier
            .clickable { onSimulatedAppSelected(platform) }
            .testTag("platform_tab_$platform")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            val emoji = when (platform) {
              "Telegram" -> "✈️"
              "WhatsApp" -> "💬"
              "Instagram" -> "📸"
              "X" -> "✖️"
              else -> "🎵"
            }
            Text(text = emoji)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = platform,
              fontWeight = FontWeight.Bold,
              color = if (isSelected) BackgroundDark else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    // 3. Interactive Social App Viewport with Floating Draggable Button
    SocialAppViewport(
      appName = uiState.simulatedSocialApp,
      config = config,
      currentFeeUsd = uiState.revenueMilestone.currentFeeUsd,
      isRtl = isRtl,
      onButtonClicked = onOpenQuickSwap
    )

    // 4. Settings Panel for the Floating Button
    Text(
      text = if (isRtl) "إعدادات وتخصيص الزر العائم:" else "Floating Button Customization:",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Toggle Visibility
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isRtl) "إظهار الزر على الشاشة" else "Show Floating Button",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyMedium
            )
            Text(
              text = if (isRtl) "إخفاء الزر مؤقتاً عند عدم الحاجة" else "Hide widget when not needed",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = config.isVisible,
            onCheckedChange = { isChecked ->
              onUpdateConfig { it.copy(isVisible = isChecked) }
            },
            modifier = Modifier.testTag("switch_button_visible")
          )
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Color Theme
        Text(
          text = if (isRtl) "نسق ألوان الزر:" else "Button Theme Color:",
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.bodyMedium
        )

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(ButtonColorTheme.values()) { themeItem ->
            val isSelected = config.theme == themeItem
            Surface(
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (isSelected) GoldAccent else Color.Transparent
              ),
              modifier = Modifier
                .clickable { onUpdateConfig { it.copy(theme = themeItem) } }
                .testTag("theme_${themeItem.name}")
            ) {
              Box(
                modifier = Modifier
                  .size(width = 90.dp, height = 48.dp)
                  .background(
                    Brush.horizontalGradient(
                      listOf(Color(themeItem.hexCode), Color(themeItem.gradientSecond))
                    )
                  )
                  .padding(4.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = themeItem.titleAr.split(" ").first(),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Size Presets
        Text(
          text = if (isRtl) "حجم الزر العائم:" else "Button Size Preset:",
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.bodyMedium
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          ButtonSizePreset.values().forEach { preset ->
            val isSelected = config.sizePreset == preset
            OutlinedButton(
              onClick = { onUpdateConfig { it.copy(sizePreset = preset) } },
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
              ),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("size_preset_${preset.name}")
            ) {
              Text(
                text = preset.titleAr.split(" ").first(),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Pulsing Glow & Docking Toggles
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isRtl) "تأثير النبض الجذاب (Pulse Glow)" else "Pulse Glow Animation",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyMedium
            )
            Text(
              text = if (isRtl) "يجعل الزر ينبض برفق لجذب الانتباه" else "Gentle animated pulsing glow",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = config.pulseAnimation,
            onCheckedChange = { isChecked ->
              onUpdateConfig { it.copy(pulseAnimation = isChecked) }
            },
            modifier = Modifier.testTag("switch_pulse_glow")
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isRtl) "الالتصاق بحافة الشاشة (Dock)" else "Dock to Screen Edge",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyMedium
            )
            Text(
              text = if (isRtl) "يثبت الزر على الجانب عند تركه" else "Snaps smoothly to side edge",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = config.dockToEdge,
            onCheckedChange = { isChecked ->
              onUpdateConfig { it.copy(dockToEdge = isChecked) }
            },
            modifier = Modifier.testTag("switch_dock_edge")
          )
        }

        // Opacity Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = if (isRtl) "درجة الشفافية" else "Opacity",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "${(config.opacity * 100).toInt()}%",
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldPrimary,
              fontWeight = FontWeight.Bold
            )
          }
          Slider(
            value = config.opacity,
            onValueChange = { newOpacity ->
              onUpdateConfig { it.copy(opacity = newOpacity) }
            },
            valueRange = 0.5f..1.0f,
            modifier = Modifier.testTag("slider_opacity")
          )
        }

        // Test Trigger Button
        Button(
          onClick = onOpenQuickSwap,
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("test_trigger_btn")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = "Test", tint = BackgroundDark)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isRtl) "تجربة ضغط الزر لفتح التحويلات المباشرة" else "Test Click: Open P2P Transfer",
            color = BackgroundDark,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
fun SocialAppViewport(
  appName: String,
  config: FloatingButtonConfig,
  currentFeeUsd: Double,
  isRtl: Boolean,
  onButtonClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Draggable offsets inside the simulator frame
  var offsetX by remember { mutableStateOf(220f) }
  var offsetY by remember { mutableStateOf(160f) }

  // Pulsing animation
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (config.pulseAnimation) 1.14f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = when (appName) {
        "Telegram" -> Color(0xFF17212B)
        "WhatsApp" -> Color(0xFF0B141A)
        "Instagram" -> Color(0xFF000000)
        "X" -> Color(0xFF000000)
        else -> Color(0xFF121212)
      }
    ),
    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF334155)),
    modifier = modifier
      .fillMaxWidth()
      .height(380.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // 1. Simulated App Interface Mockup
      Column(modifier = Modifier.fillMaxSize()) {
        // App Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              when (appName) {
                "Telegram" -> Color(0xFF242F3D)
                "WhatsApp" -> Color(0xFF202C33)
                else -> Color(0xFF1E293B)
              }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(EmeraldPrimary),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "P2P", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BackgroundDark)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = when (appName) {
                "Telegram" -> "قناة التداول السريع والوساطة P2P"
                "WhatsApp" -> "مجموعة تبادل العملات والتجار 🤝"
                "Instagram" -> "Crypto_P2P_Merchant"
                else -> "Crypto Trading Lounge"
              },
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
            Text(
              text = "24,850 متصل الآن • تحويلات آمنة",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp
            )
          }
        }

        // Mock chat messages
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Received Message
          Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 240.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "خالد: السلام عليكم، أحتاج 500 USDT بالراجحي أو STC Pay بسعر معتمد، هل يوجد وسيط موثوق؟",
                color = Color.White,
                fontSize = 11.sp
              )
              Text(
                text = "10:42 AM",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End)
              )
            }
          }

          // Reply Message
          Surface(
            color = when (appName) {
              "Telegram" -> Color(0xFF2B5278)
              "WhatsApp" -> Color(0xFF005C4B)
              else -> Color(0xFF334155)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .widthIn(max = 240.dp)
              .align(Alignment.End)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "الراشد للتداول: وعليكم السلام، اضغط على زر P2P المعلق على الشاشة وافتح الصفقة الذكية فوراً، الأمانة محجوزة ورسومها 0.80$ فقط!",
                color = Color.White,
                fontSize = 11.sp
              )
              Text(
                text = "10:43 AM",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End)
              )
            }
          }

          // Sticky hint banner at bottom of chat
          Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.8f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Text(
                text = "💡 اسحب الزر العائم في أي مكان واضغط عليه للتجربة",
                color = GoldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // 2. The Actual Draggable Floating P2P Button!
      if (config.isVisible) {
        val buttonSize = config.sizePreset.sizeDp.dp
        val gradientBrush = Brush.linearGradient(
          listOf(Color(config.theme.hexCode), Color(config.theme.gradientSecond))
        )

        Box(
          modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                offsetX = (offsetX + dragAmount.x).coerceIn(10f, 650f)
                offsetY = (offsetY + dragAmount.y).coerceIn(10f, 650f)
              }
            }
            .alpha(config.opacity)
            .testTag("floating_p2p_widget")
        ) {
          // Pulsing Halo Glow
          if (config.pulseAnimation) {
            Box(
              modifier = Modifier
                .size(buttonSize * 1.35f)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Color(config.theme.hexCode).copy(alpha = 0.25f))
                .align(Alignment.Center)
            )
          }

          // Main Floating Button Body
          Surface(
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 8.dp,
            modifier = Modifier
              .size(buttonSize)
              .align(Alignment.Center)
              .clip(CircleShape)
              .background(gradientBrush)
              .clickable(onClick = onButtonClicked)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CurrencyExchange,
                  contentDescription = "P2P Quick Swap",
                  tint = BackgroundDark,
                  modifier = Modifier.size(if (config.sizePreset == ButtonSizePreset.COMPACT) 22.dp else 26.dp)
                )
                if (config.sizePreset != ButtonSizePreset.COMPACT) {
                  Text(
                    text = "P2P",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BackgroundDark
                  )
                }
              }
            }
          }

          // Mini Rate & Fee Badge
          Surface(
            color = BackgroundDark,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .offset(y = 12.dp)
          ) {
            Text(
              text = "$${String.format("%.2f", currentFeeUsd)}",
              color = GoldAccent,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }
      }
    }
  }
}
