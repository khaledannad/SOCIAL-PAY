package com.example.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.community.*
import com.example.core.governance.tr
import com.example.model.AppLanguage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCommentaireScreen(
  currentLanguage: AppLanguage,
  currentUserName: String? = null,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onDismiss)

  val context = LocalContext.current
  val repository = remember { LiveCommentaireRepository.getInstance(context) }
  val commentsList by repository.comments.collectAsStateWithLifecycle()
  val onlineCount by repository.onlineUsersCount.collectAsStateWithLifecycle()

  val coroutineScope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  var selectedLanguageFilter by remember { mutableStateOf("ALL") }
  var searchQuery by remember { mutableStateOf("") }
  var selectedSortOrder by remember { mutableStateOf(CommentSortOrder.NEWEST) }

  var authorInput by remember { mutableStateOf(currentUserName.orEmpty()) }
  var commentInput by remember { mutableStateOf("") }
  var selectedCountry by remember { mutableStateOf(GLOBAL_COUNTRY_OPTIONS.first()) }
  var showCountryPicker by remember { mutableStateOf(false) }

  var replyTargetCommentId by remember { mutableStateOf<String?>(null) }
  var replyInput by remember { mutableStateOf("") }

  val filteredComments = remember(commentsList, selectedLanguageFilter, searchQuery, selectedSortOrder) {
    var result = commentsList
    if (selectedLanguageFilter != "ALL") {
      result = result.filter { it.languageCode.equals(selectedLanguageFilter, ignoreCase = true) }
    }
    if (searchQuery.isNotBlank()) {
      val q = searchQuery.trim().lowercase()
      result = result.filter {
        it.message.lowercase().contains(q) ||
          it.authorName.lowercase().contains(q) ||
          it.cityOrRegion.lowercase().contains(q)
      }
    }
    when (selectedSortOrder) {
      CommentSortOrder.NEWEST -> result.sortedByDescending { it.timestampMillis }
      CommentSortOrder.MOST_ACTIVE -> result.sortedByDescending { it.likesCount + (it.replies.size * 2) }
    }
  }

  val quickEmojis = listOf("❤️", "🌍", "🤝", "🚀", "💬", "✨", "🔥", "👏", "🥐", "🏮")

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BrandPrimaryBg)
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Header
      Surface(
        color = BrandSecondaryBg,
        border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BrandPrimaryBg)
                .testTag("live_commentaire_back_btn")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BrandTextPrimary,
                modifier = Modifier.size(20.dp)
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "LIVE COMMENTAIRE 🌍",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = BrandPrimaryAccent
                )
                // Pulsing Live Indicator
                Surface(
                  color = Color(0xFFEF4444).copy(alpha = 0.2f),
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                    )
                    Text(
                      text = "LIVE",
                      color = Color(0xFFEF4444),
                      fontWeight = FontWeight.Bold,
                      fontSize = 9.sp
                    )
                  }
                }
              }

              Text(
                text = tr(
                  currentLanguage,
                  "المساحة الاجتماعية العالمية • تعارف وحوار حر",
                  "Worldwide Open Social Space • Free Connections",
                  "Espace Social Mondial • Échanges Libres",
                  "Мировое сообщество • Свободное общение",
                  "全球开放社交空间 • 自由连结"
                ),
                fontSize = 10.sp,
                color = BrandTextSecondary
              )
            }

            // Stat Counter Pill
            Surface(
              color = BrandPrimaryAccent.copy(alpha = 0.14f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.4f))
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Groups,
                  contentDescription = null,
                  tint = BrandPrimaryAccent,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = String.format(java.util.Locale.US, "%,d", onlineCount),
                  color = BrandPrimaryAccent,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                )
              }
            }
          }

          // Architectural Isolation Notice Badge
          Surface(
            color = BrandIntelligenceAccent.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.8.dp, BrandIntelligenceAccent.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = BrandIntelligenceAccent,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = tr(
                  currentLanguage,
                  "مجتمع عالمي مستقل ومعزول تماماً عن المحافظ والمعاملات المالية • دخول حر دون شروط",
                  "Independent global community space isolated from financial accounts • Open to all",
                  "Espace communautaire indépendant isolé des comptes financiers • Accès libre",
                  "Свободное мировое сообщество, изолированное от финансовых счетов",
                  "完全独立于金融账户的全球社交社区 • 自由开放"
                ),
                color = BrandTextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          // Search + Language Filters Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            TextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = {
                Text(
                  text = tr(currentLanguage, "بحث في التعليقات...", "Search messages...", "Rechercher...", "Поиск...", "搜索留言..."),
                  fontSize = 12.sp,
                  color = BrandTextMuted
                )
              },
              singleLine = true,
              leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(16.dp))
              },
              trailingIcon = {
                if (searchQuery.isNotBlank()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = BrandTextSecondary, modifier = Modifier.size(14.dp))
                  }
                }
              },
              colors = TextFieldDefaults.colors(
                focusedContainerColor = BrandPrimaryBg,
                unfocusedContainerColor = BrandPrimaryBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = BrandTextPrimary,
                unfocusedTextColor = BrandTextPrimary
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .testTag("live_commentaire_search_input")
            )

            // Sort Toggle Button
            OutlinedButton(
              onClick = {
                selectedSortOrder = if (selectedSortOrder == CommentSortOrder.NEWEST) {
                  CommentSortOrder.MOST_ACTIVE
                } else {
                  CommentSortOrder.NEWEST
                }
              },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.5f)),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.heightIn(min = 40.dp)
            ) {
              Text(
                text = when (currentLanguage) {
                  AppLanguage.ARABIC -> selectedSortOrder.titleAr
                  AppLanguage.FRENCH -> selectedSortOrder.titleFr
                  AppLanguage.RUSSIAN -> selectedSortOrder.titleRu
                  AppLanguage.CHINESE -> selectedSortOrder.titleZh
                  AppLanguage.ENGLISH -> selectedSortOrder.titleEn
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandSecondaryAccent
              )
            }
          }

          // Language Filter Chips
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            val filters = listOf(
              "ALL" to tr(currentLanguage, "🌍 الكل", "🌍 All", "🌍 Tous", "🌍 Все", "🌍 全部"),
              "ar" to "🇸🇦 العربية",
              "en" to "🇬🇧 English",
              "ru" to "🇷🇺 Русский",
              "zh" to "🇨🇳 中文",
              "fr" to "🇫🇷 Français"
            )
            items(filters) { (code, label) ->
              val isSelected = selectedLanguageFilter == code
              FilterChip(
                selected = isSelected,
                onClick = { selectedLanguageFilter = code },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = BrandPrimaryAccent.copy(alpha = 0.22f),
                  selectedLabelColor = BrandPrimaryAccent,
                  containerColor = BrandPrimaryBg,
                  labelColor = BrandTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = if (isSelected) BrandPrimaryAccent else BrandSurfaceBorder
                )
              )
            }
          }
        }
      }

      // Comments List Feed
      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        items(filteredComments, key = { it.id }) { comment ->
          CommunityCommentCard(
            comment = comment,
            currentLanguage = currentLanguage,
            onLike = { repository.likeComment(comment.id) },
            onOpenReply = {
              replyTargetCommentId = if (replyTargetCommentId == comment.id) null else comment.id
            }
          )

          // Inline Threaded Reply Composer
          AnimatedVisibility(visible = replyTargetCommentId == comment.id) {
            Surface(
              color = BrandSecondaryBg.copy(alpha = 0.7f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 4.dp, bottom = 4.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                TextField(
                  value = replyInput,
                  onValueChange = { replyInput = it },
                  placeholder = {
                    Text(
                      text = tr(currentLanguage, "أضف رداً سريعاً...", "Write a reply...", "Répondre...", "Написать ответ...", "写下回复..."),
                      fontSize = 12.sp,
                      color = BrandTextMuted
                    )
                  },
                  colors = TextFieldDefaults.colors(
                    focusedContainerColor = BrandPrimaryBg,
                    unfocusedContainerColor = BrandPrimaryBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = BrandTextPrimary,
                    unfocusedTextColor = BrandTextPrimary
                  ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                )

                IconButton(
                  onClick = {
                    if (replyInput.isNotBlank()) {
                      repository.replyToComment(
                        commentId = comment.id,
                        authorNameInput = authorInput,
                        replyText = replyInput,
                        countryFlag = selectedCountry.flag
                      )
                      replyInput = ""
                      replyTargetCommentId = null
                    }
                  },
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandSecondaryAccent)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Reply",
                    tint = BrandPrimaryBg,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }

      // Bottom Sticky Compose Section
      Surface(
        color = BrandSecondaryBg,
        border = BorderStroke(1.dp, BrandSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Quick Reaction Emojis
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(quickEmojis) { emoji ->
              Surface(
                color = BrandPrimaryBg,
                shape = CircleShape,
                border = BorderStroke(0.8.dp, BrandSurfaceBorder),
                modifier = Modifier
                  .size(32.dp)
                  .clickable { commentInput += emoji }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = emoji, fontSize = 14.sp)
                }
              }
            }
          }

          // Author Name & Country Selector Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Country Flag Button
            OutlinedButton(
              onClick = { showCountryPicker = true },
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, BrandSurfaceBorder),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.heightIn(min = 36.dp)
            ) {
              Text(text = "${selectedCountry.flag} ${selectedCountry.code}", fontSize = 11.sp, color = BrandTextPrimary)
            }

            // Name / Alias Field
            TextField(
              value = authorInput,
              onValueChange = { authorInput = it },
              placeholder = {
                Text(
                  text = tr(currentLanguage, "اسمك / لقب مستعار (اختياري)", "Name / Alias (Optional)", "Nom / Pseudo (Optionnel)", "Имя / Псевдоним", "昵称 (可选)"),
                  fontSize = 11.sp,
                  color = BrandTextMuted
                )
              },
              singleLine = true,
              colors = TextFieldDefaults.colors(
                focusedContainerColor = BrandPrimaryBg,
                unfocusedContainerColor = BrandPrimaryBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = BrandTextPrimary,
                unfocusedTextColor = BrandTextPrimary
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp)
                .testTag("live_commentaire_author_input")
            )
          }

          // Comment Input + Send Button
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            TextField(
              value = commentInput,
              onValueChange = { commentInput = it },
              placeholder = {
                Text(
                  text = tr(
                    currentLanguage,
                    "شارك برأيك أو تحيتك للمجتمع العالمي...",
                    "Share thoughts or greetings with the world...",
                    "Partagez vos pensées avec le monde...",
                    "Поделитесь мыслями с мировым сообществом...",
                    "与全球社区分享您的想法或问候..."
                  ),
                  fontSize = 12.sp,
                  color = BrandTextMuted
                )
              },
              maxLines = 4,
              colors = TextFieldDefaults.colors(
                focusedContainerColor = BrandPrimaryBg,
                unfocusedContainerColor = BrandPrimaryBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = BrandTextPrimary,
                unfocusedTextColor = BrandTextPrimary
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .testTag("live_commentaire_msg_input")
            )

            Button(
              onClick = {
                if (commentInput.isNotBlank()) {
                  val langCode = when (currentLanguage) {
                    AppLanguage.ARABIC -> "ar"
                    AppLanguage.FRENCH -> "fr"
                    AppLanguage.RUSSIAN -> "ru"
                    AppLanguage.CHINESE -> "zh"
                    AppLanguage.ENGLISH -> "en"
                  }
                  repository.postComment(
                    authorNameInput = authorInput,
                    messageInput = commentInput,
                    countryCode = selectedCountry.code,
                    countryFlag = selectedCountry.flag,
                    languageCode = langCode
                  )
                  commentInput = ""
                  coroutineScope.launch {
                    listState.animateScrollToItem(0)
                  }
                }
              },
              enabled = commentInput.trim().length >= 2,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimaryAccent,
                contentColor = BrandPrimaryBg
              ),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              modifier = Modifier
                .heightIn(min = 44.dp)
                .testTag("live_commentaire_send_btn")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }

  // Country Selection Dialog
  if (showCountryPicker) {
    AlertDialog(
      onDismissRequest = { showCountryPicker = false },
      containerColor = BrandSecondaryBg,
      title = {
        Text(
          text = tr(currentLanguage, "اختر دولتك / موقعك", "Select Country / Region", "Choisir le pays", "Выберите страну", "选择国家/地区"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary
        )
      },
      text = {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(GLOBAL_COUNTRY_OPTIONS) { opt ->
            Surface(
              color = if (opt.code == selectedCountry.code) BrandPrimaryAccent.copy(alpha = 0.2f) else BrandPrimaryBg,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, if (opt.code == selectedCountry.code) BrandPrimaryAccent else BrandSurfaceBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedCountry = opt
                  showCountryPicker = false
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text(text = opt.flag, fontSize = 20.sp)
                Text(
                  text = if (currentLanguage.isRtl) opt.nameAr else opt.nameEn,
                  color = BrandTextPrimary,
                  fontWeight = FontWeight.Medium,
                  fontSize = 13.sp
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCountryPicker = false }) {
          Text(text = tr(currentLanguage, "إغلاق", "Close", "Fermer", "Закрыть", "关闭"), color = BrandPrimaryAccent)
        }
      }
    )
  }
}

@Composable
fun CommunityCommentCard(
  comment: CommunityComment,
  currentLanguage: AppLanguage,
  onLike: () -> Unit,
  onOpenReply: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
    border = BorderStroke(1.dp, BrandSurfaceBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Top Author Info Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Flag Avatar Circle
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(BrandPrimaryBg)
              .border(1.dp, BrandPrimaryAccent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(text = comment.countryFlag, fontSize = 18.sp)
          }

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = comment.authorName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = BrandTextPrimary
              )
              if (comment.cityOrRegion.isNotBlank()) {
                Text(
                  text = "• ${comment.cityOrRegion}",
                  fontSize = 11.sp,
                  color = BrandTextSecondary
                )
              }
            }

            val timeAgo = DateUtils.getRelativeTimeSpanString(
              comment.timestampMillis,
              System.currentTimeMillis(),
              DateUtils.MINUTE_IN_MILLIS,
              DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()

            Text(
              text = timeAgo,
              fontSize = 10.sp,
              color = BrandTextMuted
            )
          }
        }

        // Language Tag Badge
        Surface(
          color = BrandPrimaryBg,
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(0.8.dp, BrandSurfaceBorder)
        ) {
          Text(
            text = comment.languageCode.uppercase(),
            fontSize = 9.sp,
            color = BrandPrimaryAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      // Comment Message
      Text(
        text = comment.message,
        fontSize = 13.5.sp,
        color = BrandTextPrimary,
        lineHeight = 20.sp
      )

      // Actions Row: Like + Reply Counter
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
          // Like Button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .clickable { onLike() }
              .padding(4.dp)
          ) {
            Icon(
              imageVector = if (comment.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Like",
              tint = if (comment.isLikedByMe) Color(0xFFEF4444) else BrandTextSecondary,
              modifier = Modifier.size(17.dp)
            )
            Text(
              text = comment.likesCount.toString(),
              fontSize = 12.sp,
              color = if (comment.isLikedByMe) Color(0xFFEF4444) else BrandTextSecondary,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Reply Action Button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .clickable { onOpenReply() }
              .padding(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ChatBubbleOutline,
              contentDescription = "Reply",
              tint = BrandSecondaryAccent,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = if (comment.replies.isNotEmpty()) comment.replies.size.toString() else tr(currentLanguage, "رد", "Reply", "Répondre", "Ответ", "回复"),
              fontSize = 12.sp,
              color = BrandSecondaryAccent,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // Global community share icon
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = "Share",
          tint = BrandTextMuted,
          modifier = Modifier
            .size(16.dp)
            .clickable { /* decorative share */ }
        )
      }

      // Threaded User Replies (if any)
      if (comment.replies.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          comment.replies.forEach { rep ->
            Surface(
              color = BrandPrimaryBg.copy(alpha = 0.6f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(0.8.dp, BrandSurfaceBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(text = rep.countryFlag, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = rep.authorName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = BrandSecondaryAccent
                  )
                  Text(
                    text = rep.message,
                    fontSize = 12.sp,
                    color = BrandTextPrimary
                  )
                }
              }
            }
          }
        }
      }

      // Friendly Welcoming AI Reply
      comment.aiReply?.let { ai ->
        Surface(
          color = BrandIntelligenceAccent.copy(alpha = 0.12f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, BrandIntelligenceAccent.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(listOf(BrandIntelligenceAccent, BrandSecondaryAccent))
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = BrandPrimaryBg,
                modifier = Modifier.size(15.dp)
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = ai.botName,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = BrandIntelligenceAccent
                )
                Surface(
                  color = BrandIntelligenceAccent.copy(alpha = 0.25f),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = "AI FRIENDLY WELCOME",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandIntelligenceAccent,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }

              Text(
                text = ai.message,
                fontSize = 12.sp,
                color = BrandTextPrimary,
                lineHeight = 17.sp
              )
            }
          }
        }
      }
    }
  }
}
