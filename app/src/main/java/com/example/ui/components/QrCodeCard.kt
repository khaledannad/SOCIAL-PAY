package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.util.QrCodeGenerator

@Composable
fun QrCodeCard(
  qrData: String,
  title: String,
  amountText: String,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
  isRtl: Boolean = true
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  val qrBitmap = remember(qrData) {
    QrCodeGenerator.generateQrBitmap(
      content = qrData,
      sizePx = 512,
      darkColor = android.graphics.Color.BLACK,
      lightColor = android.graphics.Color.WHITE
    )
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.QrCode2, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
          Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Surface(color = GoldAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
          Text(
            text = amountText,
            color = GoldAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      // Real Scannable ZXing QR Code
      Box(
        modifier = Modifier
          .size(210.dp)
          .background(Color.White, RoundedCornerShape(14.dp))
          .border(2.dp, EmeraldPrimary, RoundedCornerShape(14.dp))
          .padding(12.dp)
          .testTag("zxing_qr_container"),
        contentAlignment = Alignment.Center
      ) {
        if (qrBitmap != null) {
          Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "Scannable ZXing QR Code for $title",
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = "QR unavailable",
            tint = Color.DarkGray,
            modifier = Modifier.size(64.dp)
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = qrData,
          fontSize = 10.sp,
          color = EmeraldPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
      }

      Text(
        text = if (isRtl) "رمز QR حقيقي بمعيار ISO — جاهز للمسح الفوري عبر الكاميرا أو محفظة P2P" else "Scannable ISO QR Code — scan directly with any camera or P2P wallet",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            clipboardManager.setText(AnnotatedString(qrData))
            Toast.makeText(
              context,
              if (isRtl) "تم نسخ رابط الدفع QR" else "QR payment URI copied",
              Toast.LENGTH_SHORT
            ).show()
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("copy_qr_uri_btn")
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isRtl) "نسخ الرابط" else "Copy URI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = onShare,
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("share_qr_btn")
        ) {
          Icon(Icons.Default.Share, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isRtl) "مشاركة QR" else "Share QR",
            color = BackgroundDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
