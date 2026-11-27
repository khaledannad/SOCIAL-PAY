package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class FloatingButtonPosition(val labelAr: String, val labelEn: String) {
  BOTTOM_END("أسفل اليمين/اليسار", "Bottom Corner"),
  BOTTOM_START("أسفل البداية", "Bottom Opposite"),
  CENTER_END("منتصف الحافة", "Center Edge"),
  BOTTOM_CENTER("أسفل المنتصف", "Bottom Center")
}

enum class FloatingButtonSize(val dpSize: Dp, val labelAr: String, val labelEn: String) {
  SMALL(48.dp, "صغير", "Small"),
  MEDIUM(58.dp, "متوسط", "Medium"),
  LARGE(68.dp, "كبير", "Large")
}

enum class FloatingButtonShape(val labelAr: String, val labelEn: String) {
  CIRCLE("دائري", "Circle"),
  SQUIRCLE("مربع ناعم", "Squircle"),
  CAPSULE("كبسولة", "Capsule")
}

enum class FloatingGlowStyle(val labelAr: String, val labelEn: String) {
  TURQUOISE_PULSE("توهج تركوازي ذكي", "Turquoise Pulse"),
  VIOLET_INTELLIGENCE("توهج بنفسجي عبقري", "Violet AI Glow"),
  SUBTLE_BORDER("إطار هادئ", "Subtle Border"),
  NONE("بدون توهج", "None")
}

data class FloatingButtonPreferences(
  val isVisible: Boolean = true,
  val position: FloatingButtonPosition = FloatingButtonPosition.BOTTOM_END,
  val size: FloatingButtonSize = FloatingButtonSize.MEDIUM,
  val shape: FloatingButtonShape = FloatingButtonShape.CIRCLE,
  val glowStyle: FloatingGlowStyle = FloatingGlowStyle.TURQUOISE_PULSE,
  val isAnimationEnabled: Boolean = true,
  val opacity: Float = 0.95f
) {
  fun save(context: Context) {
    val prefs = context.getSharedPreferences("smart_fab_prefs", Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean("isVisible", isVisible)
      .putString("position", position.name)
      .putString("size", size.name)
      .putString("shape", shape.name)
      .putString("glowStyle", glowStyle.name)
      .putBoolean("isAnimationEnabled", isAnimationEnabled)
      .putFloat("opacity", opacity)
      .apply()
  }

  companion object {
    fun load(context: Context): FloatingButtonPreferences {
      val prefs = context.getSharedPreferences("smart_fab_prefs", Context.MODE_PRIVATE)
      return FloatingButtonPreferences(
        isVisible = prefs.getBoolean("isVisible", true),
        position = FloatingButtonPosition.valueOf(prefs.getString("position", FloatingButtonPosition.BOTTOM_END.name) ?: FloatingButtonPosition.BOTTOM_END.name),
        size = FloatingButtonSize.valueOf(prefs.getString("size", FloatingButtonSize.MEDIUM.name) ?: FloatingButtonSize.MEDIUM.name),
        shape = FloatingButtonShape.valueOf(prefs.getString("shape", FloatingButtonShape.CIRCLE.name) ?: FloatingButtonShape.CIRCLE.name),
        glowStyle = FloatingGlowStyle.valueOf(prefs.getString("glowStyle", FloatingGlowStyle.TURQUOISE_PULSE.name) ?: FloatingGlowStyle.TURQUOISE_PULSE.name),
        isAnimationEnabled = prefs.getBoolean("isAnimationEnabled", true),
        opacity = prefs.getFloat("opacity", 0.95f)
      )
    }
  }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SmartFloatingButton(
  preferences: FloatingButtonPreferences,
  onClick: () -> Unit,
  onCustomizeClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!preferences.isVisible) return

  // Pulse animation for glow
  val infiniteTransition = rememberInfiniteTransition(label = "FabGlowPulse")
  val pulseScale by if (preferences.isAnimationEnabled) {
    infiniteTransition.animateFloat(
      initialValue = 1f,
      targetValue = 1.08f,
      animationSpec = infiniteRepeatable(
        animation = tween(1400, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "Scale"
    )
  } else {
    remember { mutableStateOf(1f) }
  }

  val glowBrush = when (preferences.glowStyle) {
    FloatingGlowStyle.TURQUOISE_PULSE -> Brush.radialGradient(
      colors = listOf(BrandPrimaryAccent.copy(alpha = 0.45f), Color.Transparent)
    )
    FloatingGlowStyle.VIOLET_INTELLIGENCE -> Brush.radialGradient(
      colors = listOf(BrandIntelligenceAccent.copy(alpha = 0.50f), Color.Transparent)
    )
    FloatingGlowStyle.SUBTLE_BORDER -> Brush.radialGradient(
      colors = listOf(BrandSecondaryAccent.copy(alpha = 0.25f), Color.Transparent)
    )
    FloatingGlowStyle.NONE -> null
  }

  val shape = when (preferences.shape) {
    FloatingButtonShape.CIRCLE -> CircleShape
    FloatingButtonShape.SQUIRCLE -> RoundedCornerShape(18.dp)
    FloatingButtonShape.CAPSULE -> RoundedCornerShape(24.dp)
  }

  val buttonSize = preferences.size.dpSize

  Box(
    modifier = modifier
      .alpha(preferences.opacity)
      .testTag("smart_floating_button"),
    contentAlignment = Alignment.Center
  ) {
    // 1. Ambient Glow Aura
    if (glowBrush != null) {
      Box(
        modifier = Modifier
          .size(buttonSize + 22.dp)
          .scale(pulseScale)
          .clip(CircleShape)
          .background(glowBrush)
      )
    }

    // 2. Core Interactive Button with Logo Icon
    Surface(
      shape = shape,
      color = BrandSecondaryBg,
      border = androidx.compose.foundation.BorderStroke(
        width = 1.5.dp,
        brush = Brush.linearGradient(listOf(BrandPrimaryAccent, BrandSecondaryAccent, BrandIntelligenceAccent))
      ),
      shadowElevation = 10.dp,
      modifier = Modifier
        .size(buttonSize)
        .combinedClickable(
          onClick = onClick,
          onLongClick = onCustomizeClick
        )
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        SocialPayIconSymbol(size = buttonSize * 0.72f)
      }
    }
  }
}

/**
 * Customization Sheet for Smart Floating Button
 */
@Composable
fun SmartFloatingButtonSettingsDialog(
  preferences: FloatingButtonPreferences,
  isRtl: Boolean,
  onSave: (FloatingButtonPreferences) -> Unit,
  onDismiss: () -> Unit
) {
  var currentPrefs by remember { mutableStateOf(preferences) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Tune, contentDescription = null, tint = BrandPrimaryAccent)
        Text(
          text = if (isRtl) "تخصيص زر SOCIAL PAY العائم" else "Customize Floating Button",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium,
          color = Color.White
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // 1. Visibility toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(if (isRtl) "إظهار الزر على الشاشة" else "Show Button", fontSize = 12.sp, color = Color.White)
          Switch(
            checked = currentPrefs.isVisible,
            onCheckedChange = { currentPrefs = currentPrefs.copy(isVisible = it) },
            colors = SwitchDefaults.colors(checkedThumbColor = BrandPrimaryAccent)
          )
        }

        // 2. Size selector
        Text(if (isRtl) "حجم الزر:" else "Button Size:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandTextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FloatingButtonSize.values().forEach { s ->
            FilterChip(
              selected = currentPrefs.size == s,
              onClick = { currentPrefs = currentPrefs.copy(size = s) },
              label = { Text(if (isRtl) s.labelAr else s.labelEn, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandPrimaryAccent,
                selectedLabelColor = BrandPrimaryBg
              )
            )
          }
        }

        // 3. Shape selector
        Text(if (isRtl) "شكل الزر:" else "Button Shape:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandTextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FloatingButtonShape.values().forEach { sh ->
            FilterChip(
              selected = currentPrefs.shape == sh,
              onClick = { currentPrefs = currentPrefs.copy(shape = sh) },
              label = { Text(if (isRtl) sh.labelAr else sh.labelEn, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandSecondaryAccent,
                selectedLabelColor = BrandPrimaryBg
              )
            )
          }
        }

        // 4. Glow Style selector
        Text(if (isRtl) "نمط التوهج الذكي:" else "Glow Style:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandTextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FloatingGlowStyle.values().take(2).forEach { g ->
            FilterChip(
              selected = currentPrefs.glowStyle == g,
              onClick = { currentPrefs = currentPrefs.copy(glowStyle = g) },
              label = { Text(if (isRtl) g.labelAr else g.labelEn, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandIntelligenceAccent,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        // 5. Opacity Slider
        Text(
          text = if (isRtl) "الشفافية: ${(currentPrefs.opacity * 100).toInt()}%" else "Opacity: ${(currentPrefs.opacity * 100).toInt()}%",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = BrandTextSecondary
        )
        Slider(
          value = currentPrefs.opacity,
          onValueChange = { currentPrefs = currentPrefs.copy(opacity = it) },
          valueRange = 0.4f..1.0f,
          colors = SliderDefaults.colors(thumbColor = BrandPrimaryAccent, activeTrackColor = BrandPrimaryAccent)
        )

        // 6. Animation switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(if (isRtl) "تأثير النبض الحركي" else "Pulse Animation", fontSize = 12.sp, color = Color.White)
          Switch(
            checked = currentPrefs.isAnimationEnabled,
            onCheckedChange = { currentPrefs = currentPrefs.copy(isAnimationEnabled = it) },
            colors = SwitchDefaults.colors(checkedThumbColor = BrandPrimaryAccent)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(currentPrefs) },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent)
      ) {
        Text(if (isRtl) "حفظ التفضيلات ✓" else "Save Settings", color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isRtl) "إلغاء" else "Cancel", color = Color.Gray)
      }
    },
    containerColor = BrandSecondaryBg,
    shape = RoundedCornerShape(20.dp)
  )
}
