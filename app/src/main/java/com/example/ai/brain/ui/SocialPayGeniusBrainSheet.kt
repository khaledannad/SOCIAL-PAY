package com.example.ai.brain.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.brain.action.PendingActionProposal
import com.example.ai.brain.core.BrainResponse
import com.example.ai.brain.core.DetectedLanguage
import com.example.ai.brain.core.SocialPayGeniusBrainEngine
import com.example.ai.brain.memory.BrainChatMessage
import com.example.ai.brain.modules.BrainModuleType
import com.example.ui.components.SocialPayIconSymbol
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialPayGeniusBrainSheet(
  isRtl: Boolean,
  onDismiss: () -> Unit,
  brainEngine: SocialPayGeniusBrainEngine = remember { SocialPayGeniusBrainEngine() }
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current
  val listState = rememberLazyListState()

  val userId = remember { "USER-CURRENT-NODE" }
  var currentSessionId by remember { mutableStateOf(brainEngine.memoryManager.getActiveSessionId(userId)) }

  var inputMessage by remember { mutableStateOf("") }
  var isStreaming by remember { mutableStateOf(false) }
  var streamingJob by remember { mutableStateOf<Job?>(null) }
  var currentStreamingText by remember { mutableStateOf("") }
  var lastPrompt by remember { mutableStateOf("") }

  // Action Proposal awaiting confirmation
  var activeProposal by remember { mutableStateOf<PendingActionProposal?>(null) }

  // Dialogs
  var showMemoryDialog by remember { mutableStateOf(false) }
  var showDocumentUploadDialog by remember { mutableStateOf(false) }
  var showSessionsDrawer by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var isSearchActive by remember { mutableStateOf(false) }

  // Voice recording simulation toggle
  var isRecordingVoice by remember { mutableStateOf(false) }
  // Audio Readout (Text-to-Speech simulation) toggle
  var isAudioReadoutEnabled by remember { mutableStateOf(false) }

  // Messages list state derived from active session
  val activeSession = brainEngine.memoryManager.getSession(currentSessionId)
  val messages = remember(activeSession, currentSessionId, currentStreamingText, isStreaming) {
    activeSession?.messages ?: listOf(
      BrainChatMessage(
        role = "assistant",
        content = if (isRtl)
          "مرحباً بك! أنا SOCIAL PAY GENIUS BRAIN، عقلك المالي والتقني الذكي. أستطيع فهم الأسئلة الطبيعية بكل اللغات واللهجات (فصحى، دارجة جزائرية، إنجليزية، فرنسية)، إدارة الفواتير، التحقق من أمان التثليث، وتطبيق رسوم 0.80$ الثابتة. كيف أساعدك اليوم؟"
        else
          "Welcome! I am SOCIAL PAY GENIUS BRAIN, your financial and technical AI assistant. I understand natural speech in MSA, Algerian Darija, English, and French. How can I empower your transactions today?"
      )
    )
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = BackgroundDark,
    tonalElevation = 8.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      // 1. Top Bar Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          SocialPayIconSymbol(size = 42.dp)
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "SOCIAL PAY GENIUS BRAIN",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color.White
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(color = EmeraldPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                Text("Gemini 3.8", color = EmeraldPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
              }
            }
            Text(
              text = "الوحدة: ${brainEngine.activeModule.titleAr} (${brainEngine.activeModule.iconEmoji})",
              fontSize = 11.sp,
              color = EmeraldPrimary
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { isSearchActive = !isSearchActive }) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isSearchActive) EmeraldPrimary else Color.White)
          }
          IconButton(onClick = { showMemoryDialog = true }) {
            Icon(Icons.Default.Memory, contentDescription = "Memory Settings", tint = GoldAccent)
          }
          IconButton(onClick = { showSessionsDrawer = true }) {
            Icon(Icons.Default.History, contentDescription = "Chat History", tint = Color.White)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }
      }

      // Search bar if toggled
      if (isSearchActive) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("بحث داخل محتوى المحادثة...", fontSize = 11.sp) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 2. Intelligent Service Modules Selector (7 Modules)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(BrainModuleType.values()) { module ->
          val isSelected = brainEngine.activeModule == module
          Surface(
            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.clickable { brainEngine.activeModule = module }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(module.iconEmoji, fontSize = 12.sp)
              Text(
                text = module.titleAr,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) BackgroundDark else Color.White
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 3. Language & Fast Query Chips
      val fastPrompts = if (isRtl) {
        listOf(
          "كم رسوم المعاملة الحالية؟",
          "واش راك خويا، فهمني في التيذر ترون",
          "انشئ فاتورة بمبلغ 250 دولار",
          "كيف يحميني درع منع التثليث؟",
          "ما هي أسعار الصرف الحية اللحظية؟"
        )
      } else {
        listOf(
          "What is the flat fee structure?",
          "Create a payment link for 500 SAR",
          "Explain anti-triangulation guard",
          "Show live real-time market rates"
        )
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(fastPrompts) { prompt ->
          Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
            modifier = Modifier.clickable { inputMessage = prompt }
          ) {
            Text(
              text = prompt,
              fontSize = 11.sp,
              color = Color.LightGray,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 4. Messages LazyColumn
      val filteredMessages = if (searchQuery.isBlank()) messages else messages.filter { it.content.contains(searchQuery, ignoreCase = true) }

      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        items(filteredMessages) { msg ->
          val isUser = msg.role == "user"
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
          ) {
            Surface(
              color = if (isUser) EmeraldDark else SurfaceDark,
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (isUser) EmeraldPrimary.copy(alpha = 0.5f) else SurfaceBorderDark),
              modifier = Modifier.widthIn(max = 320.dp)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Psychology, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("GENIUS BRAIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }

                    // Copy & Readout actions
                    Row {
                      IconButton(
                        onClick = {
                          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                          clipboard.setPrimaryClip(ClipData.newPlainText("Social Pay AI", msg.content))
                        },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                  text = msg.content,
                  fontSize = 13.sp,
                  color = Color.White,
                  lineHeight = 20.sp
                )

                // Source citations display (RAG proof)
                if (msg.sourceAttributions.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Surface(
                    color = BackgroundDark.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                      Text(text = "📚 المصادر المعتمدة الموثقة:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                      msg.sourceAttributions.forEach { citation ->
                        Text(text = "• $citation", fontSize = 9.sp, color = Color.LightGray)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Action Proposal Confirmation Card
        activeProposal?.let { proposal ->
          item {
            Surface(
              color = GoldAccent.copy(alpha = 0.12f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Security, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = "إشعار طلب الموافقة على إجراء رسمي", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldAccent)
                }
                Text(text = proposal.titleAr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = proposal.summaryAr, fontSize = 11.sp, color = Color.LightGray)

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = {
                      val result = brainEngine.actionEngine.confirmProposal(proposal.proposalId, userId)
                      activeProposal = null
                      if (result is com.example.ai.brain.action.ActionResult.Success) {
                        brainEngine.memoryManager.appendMessage(userId, currentSessionId, "assistant", result.messageAr)
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("موافقة وتأكيد ✓", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                  }
                  OutlinedButton(
                    onClick = {
                      brainEngine.actionEngine.rejectProposal(proposal.proposalId)
                      activeProposal = null
                    },
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("إلغاء الإجراء ✗", color = StatusError, fontSize = 11.sp)
                  }
                }
              }
            }
          }
        }

        // Live Streaming Bubble
        if (isStreaming) {
          item {
            Surface(
              color = SurfaceDark,
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
              modifier = Modifier.widthIn(max = 320.dp)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("جاري التفكير والتوليد المتدفق...", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = if (currentStreamingText.isNotBlank()) currentStreamingText else "...",
                  fontSize = 13.sp,
                  color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Stop Generation button
                TextButton(
                  onClick = {
                    streamingJob?.cancel()
                    isStreaming = false
                  },
                  modifier = Modifier.align(Alignment.End)
                ) {
                  Text("إيقاف التوليد ⏹", fontSize = 10.sp, color = StatusError)
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 5. Input Controls Row (Text, Voice, Attachment, Send)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Document / Image Upload button
        IconButton(
          onClick = { showDocumentUploadDialog = true },
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(SurfaceDark)
        ) {
          Icon(Icons.Default.AttachFile, contentDescription = "Upload Document", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
        }

        // Voice Dictation button
        IconButton(
          onClick = {
            isRecordingVoice = !isRecordingVoice
            if (isRecordingVoice) {
              inputMessage = "أريد إنشاء رابط سداد بقيمة 500 ريال عبر الواتساب"
            }
          },
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isRecordingVoice) StatusError else SurfaceDark)
        ) {
          Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = if (isRecordingVoice) Color.White else EmeraldPrimary, modifier = Modifier.size(20.dp))
        }

        // Text input field
        OutlinedTextField(
          value = inputMessage,
          onValueChange = { inputMessage = it },
          placeholder = { Text("اسأل SOCIAL PAY GENIUS BRAIN...", fontSize = 12.sp) },
          singleLine = true,
          shape = RoundedCornerShape(22.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(onSend = {
            if (inputMessage.isNotBlank() && !isStreaming) {
              val prompt = inputMessage.trim()
              lastPrompt = prompt
              inputMessage = ""
              isStreaming = true
              currentStreamingText = ""

              streamingJob = coroutineScope.launch {
                val response = brainEngine.processUserMessage(prompt, userId, currentSessionId)
                isStreaming = false
                if (response is BrainResponse.Completed) {
                  if (response.actionProposal != null) {
                    activeProposal = response.actionProposal
                  }
                }
                listState.animateScrollToItem(messages.size)
              }
            }
          }),
          modifier = Modifier
            .weight(1f)
            .testTag("genius_brain_input")
        )

        // Send Button
        IconButton(
          onClick = {
            if (inputMessage.isNotBlank() && !isStreaming) {
              val prompt = inputMessage.trim()
              lastPrompt = prompt
              inputMessage = ""
              isStreaming = true
              currentStreamingText = ""

              streamingJob = coroutineScope.launch {
                val response = brainEngine.processUserMessage(prompt, userId, currentSessionId)
                isStreaming = false
                if (response is BrainResponse.Completed) {
                  if (response.actionProposal != null) {
                    activeProposal = response.actionProposal
                  }
                }
                listState.animateScrollToItem(messages.size)
              }
            }
          },
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(EmeraldPrimary)
            .testTag("genius_brain_send_btn")
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BackgroundDark)
        }
      }
    }
  }

  // =========================================================================
  // DIALOG 1: User Memory Controls & Long-Term Preferences
  // =========================================================================
  if (showMemoryDialog) {
    AlertDialog(
      onDismissRequest = { showMemoryDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Memory, contentDescription = null, tint = GoldAccent)
          Spacer(modifier = Modifier.width(8.dp))
          Text("إدارة الذاكرة والتفضيلات المحفوظة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "لك كامل الصلاحية في مراجعة، تعديل، أو حذف أي معلومات وافقت على حفظها. لا يتم حفظ كلمات المرور أو أرقام البطاقات أبداً.",
            fontSize = 11.sp,
            color = Color.LightGray
          )

          val memories = brainEngine.memoryManager.getUserLongTermMemories(userId)
          if (memories.isEmpty()) {
            Text("لا توجد ذكريات أو تفضيلات محفوظة حالياً.", fontSize = 11.sp, color = Color.Gray)
          } else {
            memories.forEach { item ->
              Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(item.key, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    Text(item.value, fontSize = 10.sp, color = Color.White)
                  }
                  IconButton(
                    onClick = { brainEngine.memoryManager.deleteMemoryItem(userId, item.id) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Button(
            onClick = {
              brainEngine.memoryManager.wipeAllUserData(userId)
              showMemoryDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("حذف جميع الذكريات وسجل المحادثات (Right to be Forgotten)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      },
      confirmButton = {
        Button(onClick = { showMemoryDialog = false }) {
          Text("إغلاق")
        }
      }
    )
  }

  // =========================================================================
  // DIALOG 2: Custom Document & PDF RAG Indexer
  // =========================================================================
  if (showDocumentUploadDialog) {
    var docTitle by remember { mutableStateOf("فاتورة استيراد أجهزة حاسوب") }
    var docContent by remember { mutableStateOf("تفاصيل استيراد شحنة 50 حاسوب بقيمة 25,000$ مع تسليم الميناء.") }

    AlertDialog(
      onDismissRequest = { showDocumentUploadDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.UploadFile, contentDescription = null, tint = EmeraldPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("رفع مستند / فاتورة لقاعدة المعرفة (RAG)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("أدخل محتوى المستند أو الفاتورة ليقوم المساعد بفهمها والاستشهاد بها دون هلوسة:", fontSize = 11.sp, color = Color.LightGray)
          OutlinedTextField(
            value = docTitle,
            onValueChange = { docTitle = it },
            label = { Text("عنوان المستند", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = docContent,
            onValueChange = { docContent = it },
            label = { Text("نص المستند أو الفاتورة", fontSize = 11.sp) },
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            brainEngine.knowledgeEngine.indexCustomDocument(
              id = "DOC-USR-" + System.currentTimeMillis(),
              title = docTitle,
              content = docContent,
              sourceFileName = "$docTitle.pdf"
            )
            showDocumentUploadDialog = false
            brainEngine.memoryManager.appendMessage(userId, currentSessionId, "assistant", "✅ تم فهرسة المستند \"$docTitle\" بنجاح في قاعدة المعرفة. يمكنك سؤالي عنه الآن وسأستشهد به بدقة.")
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
          Text("فهرسة المستند ✓", color = BackgroundDark, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDocumentUploadDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // =========================================================================
  // DIALOG 3: Conversation History Sessions Drawer
  // =========================================================================
  if (showSessionsDrawer) {
    val sessions = brainEngine.memoryManager.getUserSessions(userId)
    AlertDialog(
      onDismissRequest = { showSessionsDrawer = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Forum, contentDescription = null, tint = EmeraldPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("سجل جلسات المحادثة السابقة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Button(
            onClick = {
              val newSession = brainEngine.memoryManager.createNewSession(userId)
              currentSessionId = newSession.sessionId
              showSessionsDrawer = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("بدء محادثة جديدة +", color = BackgroundDark, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(4.dp))

          LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
            items(sessions) { session ->
              Surface(
                color = if (session.sessionId == currentSessionId) EmeraldPrimary.copy(alpha = 0.2f) else SurfaceDark,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (session.sessionId == currentSessionId) EmeraldPrimary else SurfaceBorderDark),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp)
                  .clickable {
                    currentSessionId = session.sessionId
                    showSessionsDrawer = false
                  }
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(session.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  Text("${session.messages.size} رسائل", fontSize = 9.sp, color = Color.LightGray)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { showSessionsDrawer = false }) {
          Text("إغلاق")
        }
      }
    )
  }
}
