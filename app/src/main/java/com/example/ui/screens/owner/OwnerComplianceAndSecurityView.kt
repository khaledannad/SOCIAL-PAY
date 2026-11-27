package com.example.ui.screens.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.brain.BrainAgentId
import com.example.core.connectivity.RuntimeEnvironmentMode
import com.example.core.governance.tr
import com.example.core.privacy.EmergencyStepPhase
import com.example.core.privacy.PrivacyAndComplianceEngine
import com.example.core.privacy.VerificationWorkflowStatus
import com.example.data.FeedbackStatus
import com.example.data.OwnerPlatformSettings
import com.example.security.AntiFraudEngine
import com.example.ui.theme.*
import com.example.viewmodel.P2PUiState

@Composable
fun OwnerComplianceAndSecurityView(
  module: OwnerModule,
  uiState: P2PUiState,
  onReviewKycKyb: (submissionId: String, status: VerificationWorkflowStatus, note: String) -> Unit,
  onChangeOwnerPin: (currentPin: String, newPin: String) -> Unit,
  onToggleBrainAgent: (BrainAgentId, Boolean) -> Unit,
  onSetRuntimeEnvironment: (RuntimeEnvironmentMode) -> Unit,
  onUpdatePlatformSettings: (OwnerPlatformSettings) -> Unit,
  onUpdateFeedbackStatus: (String, FeedbackStatus, String) -> Unit,
  onTriggerKillSwitch: (title: String, service: String, severity: String, haltAll: Boolean) -> Unit,
  onAdvanceEmergencyPhase: (incidentId: String, nextPhase: EmergencyStepPhase, isolated: Boolean) -> Unit
) {
  val lang = uiState.currentLanguage
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    when (module) {
      OwnerModule.KYC_KYB_CENTER -> {
        Text(
          text = tr(lang, "مركز مراجعة التوثيق (KYC / KYB) (${uiState.ownerAllKycKyb.size})", "KYC / KYB Compliance Review Queue (${uiState.ownerAllKycKyb.size})", "File de Vérification KYC / KYB"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        if (uiState.ownerAllKycKyb.isEmpty()) {
          EmptyOwnerNotice(tr(lang, "لا توجد ملفات تحقق مرسلة للمراجعة حالياً.", "No KYC/KYB submissions awaiting review.", "Aucun dossier soumis."))
        } else {
          uiState.ownerAllKycKyb.forEach { sub ->
            var reviewerNote by remember(sub.id) { mutableStateOf(sub.reviewerNote) }
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("${sub.entityOrFullName} (${sub.applicantType})", fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 13.sp)
                  Text(sub.status, fontSize = 10.sp, color = BrandPrimaryAccent, fontWeight = FontWeight.Bold)
                }
                Text("${sub.countryCode} • Reg/ID: ${sub.registryOrIdNumber}", fontSize = 11.sp, color = BrandTextSecondary)
                Text("Doc SHA-256: ${sub.documentReferenceHash.take(24)}...", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = BrandTextMuted)
                OutlinedTextField(value = reviewerNote, onValueChange = { reviewerNote = it }, label = { Text(tr(lang, "ملاحظة المراجع", "Reviewer Note", "Note")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Button(
                    onClick = { onReviewKycKyb(sub.id, VerificationWorkflowStatus.VERIFIED, reviewerNote.ifBlank { "Approved by Owner" }) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
                    modifier = Modifier.weight(1f)
                  ) { Text(tr(lang, "اعتماد", "Verify", "Valider"), color = BrandPrimaryBg, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                  OutlinedButton(
                    onClick = { onReviewKycKyb(sub.id, VerificationWorkflowStatus.ACTION_REQUIRED, reviewerNote.ifBlank { "Additional document required" }) },
                    modifier = Modifier.weight(1f)
                  ) { Text(tr(lang, "استكمال", "Action Req", "Compléter"), fontSize = 10.sp) }
                  OutlinedButton(
                    onClick = { onReviewKycKyb(sub.id, VerificationWorkflowStatus.REJECTED, reviewerNote.ifBlank { "Rejected due to mismatch" }) },
                    modifier = Modifier.weight(1f)
                  ) { Text(tr(lang, "رفض", "Reject", "Rejeter"), fontSize = 10.sp, color = StatusError) }
                }
              }
            }
          }
        }
      }

      OwnerModule.SECURITY_CENTER -> {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr(lang, "مركز الأمان وتغيير الرمز السري للمالك", "Owner Security Posture & Master PIN Rotation", "Sécurité & Rotation Code PIN"), fontWeight = FontWeight.Bold, color = BrandTextPrimary)
            Text("Anti-Phishing Seal: ${AntiFraudEngine.ANTI_PHISHING_CODE} • PBKDF2-HMAC-SHA256 Verified", fontSize = 11.sp, color = BrandPrimaryAccent)
            OutlinedTextField(value = currentPin, onValueChange = { if (it.all(Char::isDigit) && it.length <= 8) currentPin = it }, label = { Text(tr(lang, "الرمز السري الحالي", "Current PIN", "PIN actuel")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newPin, onValueChange = { if (it.all(Char::isDigit) && it.length <= 8) newPin = it }, label = { Text(tr(lang, "الرمز السري الجديد (6-8 أرقام)", "New PIN (6-8 digits)", "Nouveau PIN")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
              onClick = {
                if (currentPin.length >= 6 && newPin.length >= 6) {
                  onChangeOwnerPin(currentPin, newPin)
                  currentPin = ""
                  newPin = ""
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(tr(lang, "تحديث الرمز السري للمالك", "Rotate Master Owner PIN", "Mettre à jour le PIN"), color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      OwnerModule.PRIVACY_CENTER -> {
        Text(
          text = tr(lang, "سجل تقليل البيانات وعزل النطاقات (Privacy by Design)", "Data Minimization & Domain Isolation Registry", "Registre de Minimisation des Données"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        Text(PrivacyAndComplianceEngine.privacyPolicySummary(lang), fontSize = 11.sp, color = BrandTextSecondary)
        PrivacyAndComplianceEngine.DATA_MINIMIZATION_REGISTRY.forEach { field ->
          Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(field.fieldName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandPrimaryAccent)
                Text(field.domainIsolationGroup, fontSize = 10.sp, color = BrandSecondaryAccent, fontWeight = FontWeight.Bold)
              }
              Text("Legal Basis: ${field.legalBasis}", fontSize = 10.sp, color = BrandTextPrimary)
              Text("Purpose: ${field.technicalPurpose}", fontSize = 10.sp, color = BrandTextSecondary)
              Text("Retention: ${field.retentionPolicy}", fontSize = 10.sp, color = BrandTextMuted)
            }
          }
        }
      }

      OwnerModule.AI_AGENTS_CONTROL -> {
        Text(
          text = tr(lang, "إدارة الوكلاء الـ 18 لـ SOCIAL FINANCIAL BRAIN", "18 Specialist AI Agents & Scope Enforcement", "18 Agents Spécialisés & Limites"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        uiState.brainAgents.forEach { snap ->
          Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandIntelligenceAccent.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("${snap.agent.code} — ${tr(lang, snap.agent.titleAr, snap.agent.titleEn, snap.agent.titleFr)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
                  Text(snap.agent.missionEn, fontSize = 10.sp, color = BrandTextSecondary)
                }
                Switch(
                  checked = snap.isEnabled,
                  onCheckedChange = { onToggleBrainAgent(snap.agent, it) },
                  modifier = Modifier.testTag("toggle_agent_${snap.agent.code}")
                )
              }
              Text("Scope: ${snap.agent.allowedPermissions.joinToString()} • Limit: ${snap.agent.operationalLimitEn}", fontSize = 9.sp, color = BrandIntelligenceAccent)
              Text("Tasks: ${snap.tasksHandled} • Blocked: ${snap.violationsBlocked} • Last: ${snap.lastActionSummary}", fontSize = 10.sp, color = BrandTextMuted)
            }
          }
        }
      }

      OwnerModule.API_DEVELOPERS -> {
        Text(
          text = tr(lang, "بوابة المطورين وواجهات برمجة التجار", "Developer API Keys & Merchant Webhooks", "API Développeurs & Webhooks"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        if (uiState.ownerAllMerchants.isEmpty()) {
          EmptyOwnerNotice(tr(lang, "لا توجد مفاتيح API تجارية مسجلة بعد.", "No merchant API configurations found.", "Aucune configuration API."))
        } else {
          uiState.ownerAllMerchants.forEach { m ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(m.businessName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
                Text("Masked API Ref: ${m.apiKeyReference}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = BrandPrimaryAccent)
                Text("Webhook: ${m.webhookUrl.ifBlank { "Not configured" }}", fontSize = 10.sp, color = BrandTextSecondary)
              }
            }
          }
        }
      }

      OwnerModule.ANALYTICS_REPORTS -> {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr(lang, "التقارير التحليلية الحقيقية", "Honest Operational Analytics", "Analytiques Opérationnelles"), fontWeight = FontWeight.Bold, color = BrandTextPrimary)
            Text("• Verified Users in Firestore: ${uiState.ownerAllUsers.size}", fontSize = 12.sp, color = BrandTextSecondary)
            Text("• Registered Merchants: ${uiState.ownerAllMerchants.size}", fontSize = 12.sp, color = BrandTextSecondary)
            Text("• Partner Applications: ${uiState.ownerAllPartners.size}", fontSize = 12.sp, color = BrandTextSecondary)
            Text("• Cloud Ledger Records: ${uiState.ownerAllLedgerTransactions.size}", fontSize = 12.sp, color = BrandTextSecondary)
            Text("• Immutable Audit Entries: ${uiState.ownerAuditLogs.size}", fontSize = 12.sp, color = BrandTextSecondary)
            Text("• Live External Exchange Ticks Loaded: ${if (uiState.liveStablecoinTicks.isEmpty()) "N/A (Refresh in Live Dashboard)" else "${uiState.liveStablecoinTicks.size}"}", fontSize = 12.sp, color = BrandPrimaryAccent)
          }
        }
      }

      OwnerModule.SYSTEM_SETTINGS -> {
        var allowPartners by remember(uiState.platformSettings) { mutableStateOf(uiState.platformSettings.allowPartnerRegistrations) }
        var maintenanceMode by remember(uiState.platformSettings) { mutableStateOf(uiState.platformSettings.maintenanceMode) }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr(lang, "بيئة التشغيل وإعدادات المنصة", "Runtime Environment & Platform Governance", "Environnement & Gouvernance"), fontWeight = FontWeight.Bold, color = BrandTextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              RuntimeEnvironmentMode.entries.forEach { mode ->
                FilterChip(
                  selected = uiState.runtimeEnvironment == mode,
                  onClick = { onSetRuntimeEnvironment(mode) },
                  label = { Text(mode.name.take(4), fontSize = 10.sp) }
                )
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(tr(lang, "السماح بتسجيل الشركاء", "Allow Partner Registrations", "Inscriptions Partenaires"), fontSize = 12.sp, color = BrandTextPrimary)
              Switch(checked = allowPartners, onCheckedChange = { allowPartners = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(tr(lang, "وضع الصيانة العامة", "Platform Maintenance Mode", "Mode Maintenance"), fontSize = 12.sp, color = StatusWarning)
              Switch(checked = maintenanceMode, onCheckedChange = { maintenanceMode = it })
            }
            Button(
              onClick = { onUpdatePlatformSettings(uiState.platformSettings.copy(allowPartnerRegistrations = allowPartners, maintenanceMode = maintenanceMode)) },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(tr(lang, "حفظ إعدادات النظام", "Save System Settings", "Enregistrer"), color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
            }
          }
        }

        // User Feedback Moderation
        Text(
          text = tr(lang, "ملاحظات المستخدمين لتحسين التطبيق (${uiState.ownerAllFeedback.size})", "Direct User Feedback Moderation (${uiState.ownerAllFeedback.size})", "Retours Utilisateurs"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        uiState.ownerAllFeedback.forEach { fb ->
          Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${fb.authorName} (${fb.rating}/5)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
                Text(fb.status.name, fontSize = 10.sp, color = BrandPrimaryAccent)
              }
              Text(fb.message, fontSize = 11.sp, color = BrandTextSecondary)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onUpdateFeedbackStatus(fb.id, FeedbackStatus.REVIEWED, "Reviewed by Owner") }, modifier = Modifier.weight(1f)) { Text("Reviewed", fontSize = 10.sp) }
                OutlinedButton(onClick = { onUpdateFeedbackStatus(fb.id, FeedbackStatus.RESOLVED, "Resolved") }, modifier = Modifier.weight(1f)) { Text("Resolved", fontSize = 10.sp, color = BrandPrimaryAccent) }
              }
            }
          }
        }
      }

      OwnerModule.AUDIT_LOGS -> {
        Text(
          text = tr(lang, "سجل التدقيق المشفر غير القابل للحذف (${uiState.ownerAuditLogs.size})", "Immutable Append-Only Audit Log (${uiState.ownerAuditLogs.size})", "Journal d'Audit Immuable"),
          fontWeight = FontWeight.Bold,
          color = BrandTextPrimary,
          style = MaterialTheme.typography.titleSmall
        )
        if (uiState.ownerAuditLogs.isEmpty()) {
          EmptyOwnerNotice(tr(lang, "سجل التدقيق السحابي فارغ حالياً.", "No immutable audit entries recorded yet.", "Aucune entrée d'audit."))
        } else {
          uiState.ownerAuditLogs.forEach { log ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${log.action} • Target: ${log.targetId}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandPrimaryAccent)
                Text(log.details, fontSize = 11.sp, color = BrandTextSecondary)
                Text("SHA-256: ${log.entryHash.take(28)}...", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = BrandTextMuted)
              }
            }
          }
        }
      }

      OwnerModule.EMERGENCY_CONTROL -> {
        var incidentTitle by remember { mutableStateOf("") }
        var affectedService by remember { mutableStateOf("PAYMENT_ROUTING") }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.1f)), border = BorderStroke(1.5.dp, StatusError.copy(alpha = 0.7f)), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Emergency, contentDescription = null, tint = StatusError)
              Text(tr(lang, "مفتاح الإيقاف الطارئ وإدارة الحوادث (10 خطوات)", "Emergency Kill Switch & 10-Step Isolation", "Arrêt d'Urgence & Isolation"), fontWeight = FontWeight.ExtraBold, color = StatusError)
            }
            OutlinedTextField(value = incidentTitle, onValueChange = { incidentTitle = it }, label = { Text(tr(lang, "وصف الحادث", "Incident Title", "Titre de l'incident")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = affectedService, onValueChange = { affectedService = it }, label = { Text(tr(lang, "الخدمة المتضررة", "Affected Service", "Service affecté")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = {
                  onTriggerKillSwitch(incidentTitle, affectedService, "HIGH", false)
                  incidentTitle = ""
                },
                border = BorderStroke(1.dp, StatusWarning),
                modifier = Modifier.weight(1f)
              ) { Text(tr(lang, "عزل الخدمة", "Isolate Service", "Isoler"), color = StatusWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

              Button(
                onClick = {
                  onTriggerKillSwitch(incidentTitle.ifBlank { "Full Platform Halt" }, affectedService, "CRITICAL", true)
                  incidentTitle = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                modifier = Modifier.weight(1f).testTag("owner_emergency_halt_btn")
              ) { Text(tr(lang, "إيقاف شامل", "Kill Switch", "Arrêt Global"), color = BrandTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
          }
        }

        uiState.ownerEmergencyIncidents.forEach { inc ->
          val curPhase = enumValues<EmergencyStepPhase>().firstOrNull { it.name == inc.phase } ?: EmergencyStepPhase.IDENTIFY_SERVICE
          val nextPhase = EmergencyStepPhase.entries.getOrNull((curPhase.ordinal + 1).coerceAtMost(EmergencyStepPhase.entries.lastIndex)) ?: EmergencyStepPhase.AUTHORIZED_RECOVERY
          Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg), border = BorderStroke(1.dp, StatusError.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${inc.title} (${inc.severity})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTextPrimary)
                Text("Step ${curPhase.stepNumber}/10", fontSize = 10.sp, color = StatusWarning, fontWeight = FontWeight.Bold)
              }
              Text("${inc.affectedService} • ${tr(lang, curPhase.labelAr, curPhase.labelEn, curPhase.labelFr)}", fontSize = 11.sp, color = BrandTextSecondary)
              if (curPhase != EmergencyStepPhase.AUTHORIZED_RECOVERY) {
                OutlinedButton(
                  onClick = {
                    val stillIsolated = nextPhase != EmergencyStepPhase.AUTHORIZED_RECOVERY
                    onAdvanceEmergencyPhase(inc.id, nextPhase, stillIsolated)
                  },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(tr(lang, "الانتقال للخطوة ${nextPhase.stepNumber}: ${nextPhase.labelAr}", "Advance to Step ${nextPhase.stepNumber}: ${nextPhase.labelEn}", "Étape ${nextPhase.stepNumber}: ${nextPhase.labelFr}"), fontSize = 11.sp)
                }
              }
            }
          }
        }
      }

      else -> Unit
    }
  }
}
