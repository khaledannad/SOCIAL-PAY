package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class SocialPayLogoStyle {
  FULL_HORIZONTAL,
  FULL_VERTICAL,
  ICON_ONLY,
  MONOCHROME,
  LIGHT_BG,
  DARK_BG
}

@Composable
fun SocialPayLogo(
  modifier: Modifier = Modifier,
  style: SocialPayLogoStyle = SocialPayLogoStyle.FULL_HORIZONTAL,
  iconSize: Dp = 38.dp,
  showTagline: Boolean = true
) {
  when (style) {
    SocialPayLogoStyle.ICON_ONLY -> {
      SocialPayIconSymbol(size = iconSize, modifier = modifier)
    }
    SocialPayLogoStyle.MONOCHROME -> {
      SocialPayIconSymbol(size = iconSize, isMonochrome = true, modifier = modifier)
    }
    SocialPayLogoStyle.FULL_VERTICAL -> {
      Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SocialPayIconSymbol(size = iconSize)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "SOCIAL PAY",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            letterSpacing = 2.sp,
            color = BrandTextPrimary
          )
          if (showTagline) {
            Text(
              text = "GLOBAL CONNECTED EXCHANGE",
              fontSize = 9.sp,
              letterSpacing = 1.sp,
              fontWeight = FontWeight.SemiBold,
              color = BrandPrimaryAccent
            )
          }
        }
      }
    }
    SocialPayLogoStyle.LIGHT_BG -> {
      Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        SocialPayIconSymbol(size = iconSize)
        Column {
          Text(
            text = "SOCIAL PAY",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            letterSpacing = 1.5.sp,
            color = Color(0xFF071923)
          )
          if (showTagline) {
            Text(
              text = "Universal Connected Exchange",
              fontSize = 9.sp,
              color = Color(0xFF0D9488)
            )
          }
        }
      }
    }
    SocialPayLogoStyle.FULL_HORIZONTAL, SocialPayLogoStyle.DARK_BG -> {
      Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        SocialPayIconSymbol(size = iconSize)
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "SOCIAL",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 17.sp,
              letterSpacing = 1.sp,
              color = BrandTextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "PAY",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 17.sp,
              letterSpacing = 1.sp,
              color = BrandPrimaryAccent
            )
          }
          if (showTagline) {
            Text(
              text = "Global Connected Exchange",
              fontSize = 9.sp,
              letterSpacing = 0.5.sp,
              fontWeight = FontWeight.Medium,
              color = BrandTextSecondary
            )
          }
        }
      }
    }
  }
}

/**
 * Geometric Vector 'S' Symbol combining network nodes and exchange arrows
 */
@Composable
fun SocialPayIconSymbol(
  size: Dp = 38.dp,
  isMonochrome: Boolean = false,
  modifier: Modifier = Modifier
) {
  val turquoise = if (isMonochrome) Color.White else BrandPrimaryAccent
  val cobalt = if (isMonochrome) Color.White else BrandSecondaryAccent
  val violet = if (isMonochrome) Color.White else BrandIntelligenceAccent
  val borderTrack = if (isMonochrome) Color.White.copy(alpha = 0.3f) else BrandSurfaceBorder

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(BrandPrimaryBg),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.12f)) {
      val w = this.size.width
      val h = this.size.height
      val strokeW = (w * 0.10f).coerceAtLeast(2.5f)

      // 1. Outer Network Ring
      drawCircle(
        color = borderTrack,
        radius = (w / 2f) * 0.95f,
        style = Stroke(width = strokeW * 0.5f)
      )

      // 2. Continuous S-Curve with 3-stop Linear Gradient
      val sGradient = Brush.linearGradient(
        colors = listOf(turquoise, cobalt, violet),
        start = Offset(w * 0.2f, h * 0.15f),
        end = Offset(w * 0.8f, h * 0.85f)
      )

      val sPath = Path().apply {
        // Top loop of 'S'
        moveTo(w * 0.65f, h * 0.30f)
        cubicTo(w * 0.65f, h * 0.16f, w * 0.32f, h * 0.16f, w * 0.32f, h * 0.34f)
        // Mid traverse
        cubicTo(w * 0.32f, h * 0.52f, w * 0.68f, h * 0.48f, w * 0.68f, h * 0.66f)
        // Bottom loop of 'S'
        cubicTo(w * 0.68f, h * 0.84f, w * 0.35f, h * 0.84f, w * 0.35f, h * 0.70f)
      }

      drawPath(
        path = sPath,
        brush = sGradient,
        style = Stroke(
          width = strokeW,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )

      // 3. Directional exchange arrow heads at terminals
      // Top arrow
      val arrowSize = w * 0.16f
      val topArrowPath = Path().apply {
        moveTo(w * 0.55f, h * 0.22f)
        lineTo(w * 0.68f, h * 0.30f)
        lineTo(w * 0.55f, h * 0.38f)
      }
      drawPath(
        path = topArrowPath,
        color = turquoise,
        style = Stroke(width = strokeW * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
      )

      // Bottom arrow
      val bottomArrowPath = Path().apply {
        moveTo(w * 0.45f, h * 0.78f)
        lineTo(w * 0.32f, h * 0.70f)
        lineTo(w * 0.45f, h * 0.62f)
      }
      drawPath(
        path = bottomArrowPath,
        color = violet,
        style = Stroke(width = strokeW * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
      )

      // 4. Central Network Nexus Node
      drawCircle(
        color = cobalt,
        radius = strokeW * 0.75f,
        center = Offset(w * 0.50f, h * 0.50f)
      )

      // 5. Network Satellites (Social Network Nodes)
      drawCircle(
        color = turquoise,
        radius = strokeW * 0.45f,
        center = Offset(w * 0.24f, h * 0.32f)
      )
      drawCircle(
        color = violet,
        radius = strokeW * 0.45f,
        center = Offset(w * 0.76f, h * 0.68f)
      )
    }
  }
}
