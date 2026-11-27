package com.example.core.custody

import com.example.core.governance.tr
import com.example.model.AppLanguage

enum class CustodyModelType {
  NON_CUSTODIAL,
  REGULATED_CUSTODY
}

data class CustodyComparisonProfile(
  val model: CustodyModelType,
  val titleAr: String,
  val titleEn: String,
  val titleFr: String,
  val keyControlAr: String,
  val keyControlEn: String,
  val keyControlFr: String,
  val regulatoryProfileAr: String,
  val regulatoryProfileEn: String,
  val regulatoryProfileFr: String,
  val technicalControls: List<String>,
  val isCurrentlyActive: Boolean
)

/**
 * Technical & Legal Architecture for Custody & Asset Protection.
 * Enforces Non-Custodial Orchestration by default: Social Pay does not hold client funds,
 * does not store private keys, and prohibits synthetic internal cash balances.
 */
object CustodyArchitectureModel {

  val ACTIVE_MODEL: CustodyModelType = CustodyModelType.NON_CUSTODIAL

  val COMPARISON_PROFILES: List<CustodyComparisonProfile> = listOf(
    CustodyComparisonProfile(
      model = CustodyModelType.NON_CUSTODIAL,
      titleAr = "النموذج غير الوصائي (Non-Custodial) — المفعّل حالياً",
      titleEn = "Non-Custodial Model — Active Default",
      titleFr = "Modèle Non-Dépositaire — Actif par défaut",
      keyControlAr = "تبقى الأصول والمفاتيح الخاصة بالكامل تحت سيطرة محفظة المستخدم الخارجية، وتعمل SOCIAL PAY كطبقة تنسيق ومراسلة وتوجيه موثقة.",
      keyControlEn = "Assets and private keys remain 100% under user self-custody; SOCIAL PAY acts solely as a verifiable coordination & payment-routing layer.",
      keyControlFr = "Les actifs et clés privées restent sous le contrôle exclusif de l'utilisateur ; SOCIAL PAY agit comme couche de routage vérifiable.",
      regulatoryProfileAr = "يقلل مخاطر الاحتفاظ بأموال الغير ولا يتطلب حفظ ودائع العملاء داخلياً، مع التزام قواعد مكافحة غسل الأموال والاعتماد حسب السوق.",
      regulatoryProfileEn = "Minimizes safeguarding liability by never taking custody of user stablecoins while enforcing AML/KYC/KYB corridor rules.",
      regulatoryProfileFr = "Réduit les risques de garde en ne détenant jamais les fonds tout en appliquant les contrôles de conformité.",
      technicalControls = listOf(
        "Zero Private Key Storage inside App or Firestore",
        "External Wallet Address & Contract Ownership Verification",
        "Zero Synthetic Internal Balances",
        "Settlement Proof Verified Directly from External Provider / On-Chain Hash"
      ),
      isCurrentlyActive = true
    ),
    CustodyComparisonProfile(
      model = CustodyModelType.REGULATED_CUSTODY,
      titleAr = "نموذج الحفظ المنظّم (Regulated Custody) — يتطلب ترخيصاً ومزود حفظ",
      titleEn = "Regulated Custody Model — Requires Licensed Custodian",
      titleFr = "Garde Réglementée — Requiert un dépositaire agréé",
      keyControlAr = "يُفعل فقط عبر مزود حفظ مؤسسي مرخص مع فصل أموال العملاء عن أموال الشركة وتواقيع متعددة ومفاتيح معزولة في HSM.",
      keyControlEn = "Permitted only via a licensed qualified custodian with segregated client omnibus accounts, HSM/KMS signing isolation, and multi-party approval.",
      keyControlFr = "Autorisé uniquement via un dépositaire qualifié agréé avec séparation des actifs, HSM et approbation multipartite.",
      regulatoryProfileAr = "يتطلب ترخيص حفظ مؤسسي مستقل في كل سوق مستهدف وتدقيقاً أمنياً ومحاسبياً مستقلاً قبل التفعيل.",
      regulatoryProfileEn = "Requires explicit financial custody licensing per jurisdiction, daily ledger reconciliation, and independent audit before launch.",
      regulatoryProfileFr = "Exige une licence de conservation agréée par juridiction et un audit indépendant avant lancement.",
      technicalControls = listOf(
        "Client Assets Segregated from Corporate Revenue",
        "Qualified Licensed Custodian API + HSM Key Isolation",
        "Multi-Party Authorization (M-of-N) — Zero Single-Employee Control",
        "Daily Automated Reconciliation + Independent Pre-Launch Audit"
      ),
      isCurrentlyActive = false
    )
  )

  fun policyBanner(lang: AppLanguage): String = tr(
    lang,
    ar = "تعمل SOCIAL PAY وفق التصميم غير الوصائي: لا تحتفظ المنصة بأموال المستخدمين أو مفاتيحهم الخاصة ولا تنشئ أرصدة داخلية وهمية.",
    en = "SOCIAL PAY operates on a strict Non-Custodial architecture: zero client fund custody, zero private keys, and zero synthetic internal balances.",
    fr = "SOCIAL PAY fonctionne en architecture non-dépositaire stricte : aucune garde de fonds, aucune clé privée, aucun solde fictif."
  )
}
