package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AirdropReward
import com.example.model.RevenueMilestoneInfo
import com.example.ui.theme.*

@Composable
fun MilestoneAndAirdropScreen(
  milestoneInfo: RevenueMilestoneInfo,
  airdropRewards: List<AirdropReward>,
  userTrustScore: Int,
  userPoints: Int,
  isRtl: Boolean,
  onClaimAirdrop: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    // 1. Live $1M Milestone Progress Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(GoldAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.MonetizationOn,
                  contentDescription = "Milestone",
                  tint = GoldAccent,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = if (isRtl) "مخطط هدف المليون دولار" else "$1,000,000 Milestone Tracker",
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = if (isRtl) "خطة الرسوم الثابتة 0.8$ ➔ 0.9$" else "Flat Fee Model $0.80 ➔ $0.90",
                  style = MaterialTheme.typography.labelSmall,
                  color = GoldAccent
                )
              }
            }

            Surface(
              color = EmeraldPrimary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text(
                text = "${(milestoneInfo.progressFraction * 100).toInt()}%",
                color = EmeraldPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          // Revenue Numbers
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            Column {
              Text(
                text = if (isRtl) "المدخول التراكمي المحقق للمشروع" else "Cumulative Project Revenue",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "$${String.format("%,.0f", milestoneInfo.totalPlatformRevenueUsd)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = if (isRtl) "الهدف النهائي" else "Milestone Goal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "$1,000,000",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
              )
            }
          }

          // Visual Progress Bar
          LinearProgressIndicator(
            progress = { milestoneInfo.progressFraction },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp)),
            color = GoldAccent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )

          // Detailed Explanation of the 0.8$ to 0.9$ Transition
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = if (isRtl)
                  "📌 قانون الرسوم المعتمد: الرسوم حالياً 0.8$ ثابتة لكل معاملة بدون نسب مئوية. بمجرد أن يبلغ دخل المشروع مليون دولار ($1,000,000)، سترتفع الرسوم إلى 0.9$ وتثبت عليها دائماً وأبداً لتمويل احتياطي الأمان وصندوق الأيردروبات المجتمعي."
                else
                  "📌 Rule: Current fee is a fixed $0.80 per deal. When cumulative platform revenue hits $1,000,000, the fee will step up to $0.90 fixed forever to fund escrow insurance & community airdrop pool.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "${if (isRtl) "إجمالي الصفقات المنفذة بنجاح:" else "Completed deals:"} ${String.format("%,d", milestoneInfo.totalCompletedDeals)} ${if (isRtl) "صفقة" else "deals"}",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 2. Trust Score & User Airdrop Points Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(listOf(EmeraldPrimary, GoldAccent))
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Trust",
                tint = BackgroundDark,
                modifier = Modifier.size(28.dp)
              )
            }

            Column {
              Text(
                text = if (isRtl) "مؤشر ثقتك وانضباطك" else "Your Trust & Discipline Score",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "$userTrustScore%",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = EmeraldPrimary.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = if (isRtl) "نخبة التداول النزيه" else "Elite Honest Trader",
                    color = EmeraldPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = if (isRtl) "رصيد نقاط الأيردروب" else "Airdrop Points",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Stars, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "$userPoints pt",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
              )
            }
          }
        }
      }
    }

    // 3. Airdrop Rewards List
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isRtl) "مكافآت وأيردروبات الثقة المتاحة:" else "Available Trust Airdrops:",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        Text(
          text = if (isRtl) "+50 نقطة لكل صفقة ناجحة" else "+50 pts per trade",
          style = MaterialTheme.typography.labelSmall,
          color = EmeraldPrimary,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    items(airdropRewards) { reward ->
      AirdropRewardCard(
        reward = reward,
        userPoints = userPoints,
        isRtl = isRtl,
        onClaim = { onClaimAirdrop(reward.id) }
      )
    }

    // 4. Discipline, Ethics & Zero Riba Rules
    item {
      Text(
        text = if (isRtl) "ميثاق الانضباط والمعاملات بلا ربى:" else "Discipline & Zero-Riba Charter:",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          RuleItem(
            icon = Icons.Default.Handshake,
            title = if (isRtl) "تقابض فوري خالي من الربا 0%" else "0% Riba Spot Handover",
            description = if (isRtl)
              "كل معاملة تتم فوراً بلا أي فوائد أو تأجيل (يداً بيد بمجلس العقد) التزاماً بالضوابط المالية الأخلاقية."
            else
              "Spot hand-off with zero delay, no interest, and strict ethical compliance."
          )

          Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

          RuleItem(
            icon = Icons.Default.AccountBalance,
            title = if (isRtl) "منع حسابات الطرف الثالث (Anti-Fraud)" else "Strict Anti-Third-Party Ban",
            description = if (isRtl)
              "يمنع منعاً باتاً التحويل من حساب بنكي باسم شخص مختلف عن الهوية الموثقة، ويعرض صاحبه لتجميد النقاط فوراً."
            else
              "All transfers must originate from the registered trader's official bank account."
          )

          Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

          RuleItem(
            icon = Icons.Default.Gavel,
            title = if (isRtl) "نظام النزاعات والتحكيم السريع" else "Swift Smart Arbitration",
            description = if (isRtl)
              "في حال النزاع، يتم تجميد الأمانة تلقائياً ويقوم فريق التحكيم المالي بفحص كشف الحساب خلال 10 دقائق."
            else
              "Escrow freezes immediately upon dispute. Neutral arbitration resolves cases within 10 minutes."
          )
        }
      }
    }
  }
}

@Composable
fun AirdropRewardCard(
  reward: AirdropReward,
  userPoints: Int,
  isRtl: Boolean,
  onClaim: () -> Unit,
  modifier: Modifier = Modifier
) {
  val canClaim = userPoints >= reward.pointsRequired && !reward.isClaimed

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (canClaim) GoldAccent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    ),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(GoldAccent.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CardGiftcard,
            contentDescription = "Airdrop",
            tint = GoldAccent,
            modifier = Modifier.size(22.dp)
          )
        }

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = if (isRtl) reward.titleAr else reward.titleEn,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = reward.tag,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            text = "${if (isRtl) "المطلوب:" else "Cost:"} ${reward.pointsRequired} ${if (isRtl) "نقطة ثقة" else "pts"}",
            style = MaterialTheme.typography.labelSmall,
            color = if (userPoints >= reward.pointsRequired) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Button(
        onClick = onClaim,
        enabled = canClaim,
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldAccent,
          disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.testTag("claim_airdrop_${reward.id}")
      ) {
        Text(
          text = when {
            reward.isClaimed -> if (isRtl) "تم الاستلام ✓" else "Claimed ✓"
            canClaim -> if (isRtl) "مطالبة الآن" else "Claim Now"
            else -> if (isRtl) "غير متاح بعد" else "Locked"
          },
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = if (canClaim) BackgroundDark else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun RuleItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
    Column {
      Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
