const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const OWNER_UID = "owner_uid_khaled";
const OWNER_EMAIL = "khaledannad29@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserProfilePayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    fullName: "Khaled Annad",
    email: "khaled@example.com",
    phoneNumber: "+966501234567",
    nationalIdOrPassport: "SA-1098421098",
    kycLevel: "BASIC_LEVEL_1",
    trustScorePercent: 99.8,
    totalTradesCount: 12,
    completionRatePercent: 99.4,
    spotBalanceUsdt: 0.0,
    lockedEscrowUsdt: 0.0,
    merchantCollateralUsdt: 0.0,
    partnerCommissionsEarnedUsd: 0.0,
    myActiveAdsCount: 0,
    onlineMerchant: true,
    createdAt,
    updatedAt,
  };
}

function validPaymentAccountPayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    bankName: "Al Rajhi Bank",
    accountNumberOrIban: "SA4480000213608010098934",
    accountHolderName: "Khaled Annad",
    accountType: "BANK_IBAN",
    currency: "SAR",
    primaryDefault: true,
    verifiedStatus: "VERIFIED",
    createdAt,
    updatedAt,
  };
}

function validFeedbackPayload(uid, createdAt, updatedAt, overrides = {}) {
  return {
    userId: uid,
    authorName: "Alice Trader",
    authorEmail: "alice@example.com",
    category: "PAYMENTS_QR",
    rating: 5,
    message: "Fast QR generation and smooth escrow release.",
    sourceScreen: "AUTH_SCREEN",
    status: "OPEN",
    ownerNote: "",
    createdAt,
    updatedAt,
    ...overrides,
  };
}

function validPartnerPayload(uid, createdAt, updatedAt, overrides = {}) {
  return {
    userId: uid,
    companyName: "Atlas Logistics Co",
    sector: "LOGISTICS_DELIVERY",
    platformType: "ENTERPRISE_API",
    websiteOrChannel: "https://atlas-logistics.example.com",
    contactName: "Alice Trader",
    contactEmail: "partners@atlas-logistics.example.com",
    contactPhone: "+966501112233",
    countryCode: "SA",
    monthlyVolumeEstimateUsd: 45000.0,
    commissionPercent: 50.0,
    status: "PENDING_REVIEW",
    notes: "Integrating cash-on-delivery escrow settlement across 12 hubs.",
    ownerReviewNote: "",
    createdAt,
    updatedAt,
    ...overrides,
  };
}

function validOwnerSecurityPayload(uid, createdAt, updatedAt, overrides = {}) {
  return {
    ownerUid: uid,
    ownerEmail: OWNER_EMAIL,
    pinHash: "a3f5e8c2d1b09876543210fedcba9876543210abcdef1234567890abcdef1234",
    pinSalt: "randomSaltBase64Value==",
    hashAlgorithm: "PBKDF2_SHA256",
    createdAt,
    updatedAt,
    ...overrides,
  };
}

function validOwnerPublicPayload(uid, createdAt, updatedAt, overrides = {}) {
  return {
    ownerUid: uid,
    fixedFeeUsd: 0.80,
    partnerCommissionPercent: 50.0,
    antiPhishingWord: "SHIELD2026",
    allowPartnerRegistrations: true,
    maintenanceMode: false,
    createdAt,
    updatedAt,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write profiles, accounts, feedback, partners, or owner_config", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validUserProfilePayload(ALICE_UID, now, now))
  );
  await assertFails(
    unauthDb.collection("feedback").doc("fb_1").set(validFeedbackPayload(ALICE_UID, now, now))
  );
  await assertFails(
    unauthDb.collection("partners").doc("pt_1").set(validPartnerPayload(ALICE_UID, now, now))
  );
  await assertFails(unauthDb.collection("owner_config").doc("security").get());
  await assertFails(unauthDb.collection("owner_config").doc("platform_public").get());
});

test("Authenticated owner can create and partially update profile while preserving immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfilePayload(ALICE_UID, created, created))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Khaled Verified Pro",
      phoneNumber: "+966559876543",
      nationalIdOrPassport: "SA-88888888",
      kycLevel: "VERIFIED_PRO_LEVEL_2",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation on users and payment_accounts", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set(validUserProfilePayload(ALICE_UID, created, created));
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("payment_accounts")
      .doc("acc_1")
      .set(validPaymentAccountPayload(ALICE_UID, created, created));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("payment_accounts").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("payment_accounts").doc("acc_1").delete()
  );
});

test("Payment account lifecycle: empty list, create, update primaryDefault, delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payment_accounts").get()
  );
  const accRef = aliceDb.collection("users").doc(ALICE_UID).collection("payment_accounts").doc("acc_1");
  await assertSucceeds(accRef.set(validPaymentAccountPayload(ALICE_UID, created, created)));
  await assertSucceeds(
    accRef.update({
      primaryDefault: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("payment_accounts")
      .doc("acc_extra_id")
      .set({
        id: "acc_extra_id",
        ...validPaymentAccountPayload(ALICE_UID, created, created),
      })
  );
  await assertSucceeds(accRef.delete());
});

test("User feedback enforces per-user query/write isolation and allows platform owner moderation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const created = new Date(Date.now() - 10000);

  // Alice can submit OPEN feedback for herself
  await assertSucceeds(
    aliceDb.collection("feedback").doc("fb_1").set(validFeedbackPayload(ALICE_UID, created, created))
  );
  // Alice cannot forge feedback for Bob or mark own status RESOLVED on create
  await assertFails(
    aliceDb.collection("feedback").doc("fb_forged").set(validFeedbackPayload(BOB_UID, created, created))
  );
  await assertFails(
    aliceDb.collection("feedback").doc("fb_bad_status").set(validFeedbackPayload(ALICE_UID, created, created, { status: "RESOLVED" }))
  );

  // Alice can query her own feedback; Bob cannot read Alice's feedback or list all
  await assertSucceeds(aliceDb.collection("feedback").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("feedback").doc("fb_1").get());
  await assertFails(bobDb.collection("feedback").get());

  // Regular user cannot mark own feedback RESOLVED
  await assertFails(
    aliceDb.collection("feedback").doc("fb_1").update({
      status: "RESOLVED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Verified platform owner can list all and mark feedback REVIEWED / RESOLVED
  await assertSucceeds(ownerDb.collection("feedback").get());
  await assertSucceeds(
    ownerDb.collection("feedback").doc("fb_1").update({
      status: "REVIEWED",
      ownerNote: "Added to next sprint",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Partner applications support all sectors, isolate users, and allow owner approval", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("partners").doc("pt_1").set(validPartnerPayload(ALICE_UID, created, created))
  );

  // Self-approval or fake commission on create/update must fail
  await assertFails(
    aliceDb.collection("partners").doc("pt_cheat").set(validPartnerPayload(ALICE_UID, created, created, { status: "APPROVED" }))
  );
  await assertFails(
    aliceDb.collection("partners").doc("pt_1").update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can update her company name while status remains PENDING_REVIEW
  await assertSucceeds(
    aliceDb.collection("partners").doc("pt_1").update({
      companyName: "Atlas Global Logistics Ltd",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot inspect Alice's partner application
  await assertFails(bobDb.collection("partners").doc("pt_1").get());
  await assertFails(bobDb.collection("partners").get());

  // Platform owner can list all partners and approve with custom commission
  await assertSucceeds(ownerDb.collection("partners").get());
  await assertSucceeds(
    ownerDb.collection("partners").doc("pt_1").update({
      status: "APPROVED",
      commissionPercent: 55.0,
      ownerReviewNote: "Verified corporate entity",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner config enforces strict owner verification for PIN security doc and read-only public governance for standard users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const unverifiedOwnerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: false }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const created = new Date(Date.now() - 10000);

  // Non-owner and unverified owner cannot write owner_config
  await assertFails(
    aliceDb.collection("owner_config").doc("security").set(validOwnerSecurityPayload(ALICE_UID, created, created))
  );
  await assertFails(
    unverifiedOwnerDb.collection("owner_config").doc("security").set(validOwnerSecurityPayload(OWNER_UID, created, created))
  );
  await assertFails(
    aliceDb.collection("owner_config").doc("platform_public").set(validOwnerPublicPayload(ALICE_UID, created, created))
  );

  // Verified owner creates security + platform_public docs
  await assertSucceeds(
    ownerDb.collection("owner_config").doc("security").set(validOwnerSecurityPayload(OWNER_UID, created, created))
  );
  await assertSucceeds(
    ownerDb.collection("owner_config").doc("platform_public").set(validOwnerPublicPayload(OWNER_UID, created, created))
  );

  // Standard user CANNOT read security doc (PIN hash/salt), CAN read platform_public fee/maintenance
  await assertFails(aliceDb.collection("owner_config").doc("security").get());
  await assertSucceeds(aliceDb.collection("owner_config").doc("platform_public").get());
  await assertFails(
    aliceDb.collection("owner_config").doc("platform_public").update({
      fixedFeeUsd: 0.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Verified owner can read security doc & update platform settings
  await assertSucceeds(ownerDb.collection("owner_config").doc("security").get());
  await assertSucceeds(
    ownerDb.collection("owner_config").doc("platform_public").update({
      fixedFeeUsd: 0.75,
      maintenanceMode: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Privacy requests isolate user reads/writes, allow owner updates, and forbid deletion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const now = new Date(Date.now() - 1000);

  const reqRef = aliceDb.collection("users").doc(ALICE_UID).collection("privacy_requests").doc("pr_1");
  await assertSucceeds(
    reqRef.set({
      userId: ALICE_UID,
      requestType: "DATA_EXPORT",
      status: "SUBMITTED",
      details: "JSON export requested by user",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read or write Alice's privacy requests
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("privacy_requests").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("privacy_requests").doc("pr_1").get());

  // Owner can mark privacy request COMPLETED; nobody can delete it
  await assertSucceeds(
    ownerDb.collection("users").doc(ALICE_UID).collection("privacy_requests").doc("pr_1").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(reqRef.delete());
  await assertFails(ownerDb.collection("users").doc(ALICE_UID).collection("privacy_requests").doc("pr_1").delete());
});

test("KYC/KYB submissions prevent self-approval, allow owner verification, and block deletion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const created = new Date(Date.now() - 5000);

  const subPayload = {
    userId: ALICE_UID,
    applicantType: "INDIVIDUAL_KYC",
    entityOrFullName: "Alice Benali",
    countryCode: "DZ",
    registryOrIdNumber: "ID-99887766",
    documentReferenceHash: "sha256:abc123456",
    status: "SUBMITTED",
    reviewerNote: "",
    createdAt: created,
    updatedAt: created,
  };

  await assertSucceeds(aliceDb.collection("kyc_kyb_submissions").doc("kyc_1").set(subPayload));
  // Self-approval on create or update must fail
  await assertFails(aliceDb.collection("kyc_kyb_submissions").doc("kyc_cheat").set({ ...subPayload, status: "VERIFIED" }));
  await assertFails(
    aliceDb.collection("kyc_kyb_submissions").doc("kyc_1").update({
      status: "VERIFIED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Cross-user isolation
  await assertFails(bobDb.collection("kyc_kyb_submissions").doc("kyc_1").get());

  // Owner can approve and nobody can delete
  await assertSucceeds(ownerDb.collection("kyc_kyb_submissions").get());
  await assertSucceeds(
    ownerDb.collection("kyc_kyb_submissions").doc("kyc_1").update({
      status: "VERIFIED",
      reviewerNote: "Passport verified cleanly",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(ownerDb.collection("kyc_kyb_submissions").doc("kyc_1").delete());
});

test("Merchant profiles and staff RBAC enforce owner isolation and prevent self-KYB verification", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("merchant_profiles").doc(ALICE_UID).set({
      ownerUserId: ALICE_UID,
      businessName: "Atlas Commerce SARL",
      countryCode: "DZ",
      kybStatus: "NOT_STARTED",
      webhookUrl: "https://atlas.example.com/webhooks/socialpay",
      apiKeyReference: "sp_live_ref_001",
      defaultStablecoin: "USDC",
      defaultNetwork: "POLYGON",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice cannot self-verify her kybStatus, Bob cannot read Alice's merchant profile
  await assertFails(
    aliceDb.collection("merchant_profiles").doc(ALICE_UID).update({
      kybStatus: "VERIFIED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("merchant_profiles").doc(ALICE_UID).get());

  // Alice can manage her own staff subcollection
  await assertSucceeds(
    aliceDb.collection("merchant_profiles").doc(ALICE_UID).collection("staff").doc("st_1").set({
      merchantOwnerUid: ALICE_UID,
      staffEmail: "cashier@atlas.example.com",
      staffName: "Yacine Cashier",
      role: "CASHIER",
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(bobDb.collection("merchant_profiles").doc(ALICE_UID).collection("staff").get());

  // Platform owner can verify merchant KYB
  await assertSucceeds(
    ownerDb.collection("merchant_profiles").doc(ALICE_UID).update({
      kybStatus: "VERIFIED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Ledger transactions enforce stablecoins-only, immutable financial amounts/recipients, and disallow deletion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const created = new Date(Date.now() - 5000);

  const txPayload = {
    senderUserId: ALICE_UID,
    transactionId: "TX-SP-10001",
    intentId: "INT-10001",
    stablecoinTicker: "USDC",
    networkCode: "POLYGON",
    amount: 250.0,
    declaredFeeUsd: 0.8,
    recipientName: "Atlas Store",
    recipientAddressOrRail: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F",
    providerId: "CIRCLE_SANDBOX",
    providerReference: "",
    state: "CREATED",
    reconciliationStatus: "PENDING_PROVIDER_PROOF",
    disputeReason: "",
    idempotencyKey: "idem-key-10001",
    createdAt: created,
    updatedAt: created,
  };

  await assertSucceeds(aliceDb.collection("ledger_transactions").doc("TX-SP-10001").set(txPayload));

  // Reject volatile coin BTC or creating directly as SETTLED
  await assertFails(
    aliceDb.collection("ledger_transactions").doc("TX-BTC-BAD").set({ ...txPayload, stablecoinTicker: "BTC" })
  );
  await assertFails(
    aliceDb.collection("ledger_transactions").doc("TX-SETTLE-BAD").set({ ...txPayload, state: "SETTLED" })
  );

  // Tampering with amount or recipient after creation must fail even by owner or sender
  await assertFails(
    aliceDb.collection("ledger_transactions").doc("TX-SP-10001").update({
      amount: 9999.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    ownerDb.collection("ledger_transactions").doc("TX-SP-10001").update({
      recipientAddressOrRail: "0xATTACKER",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Allowed state progression + reconciliation update succeeds; deletion fails
  await assertSucceeds(
    aliceDb.collection("ledger_transactions").doc("TX-SP-10001").update({
      state: "SETTLED",
      providerReference: "ONCHAIN-0xHASH123",
      reconciliationStatus: "MATCHED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("ledger_transactions").doc("TX-SP-10001").get());
  await assertFails(ownerDb.collection("ledger_transactions").doc("TX-SP-10001").delete());
});

test("Connectivity connectors, append-only system_audit_logs, and emergency_incidents enforce zero-trust rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const created = new Date(Date.now() - 5000);

  // Connectors: only owner writes, signed-in users can inspect live availability
  await assertFails(
    aliceDb.collection("connectivity_connectors").doc("conn_circle").set({
      connectorId: "conn_circle",
      name: "Circle USDC API",
      category: "STABLECOIN_PROVIDER",
      status: "CONNECTED",
      environment: "PRODUCTION",
      latencyMs: 120,
      statusNote: "Fake connected",
      updatedByUid: ALICE_UID,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    ownerDb.collection("connectivity_connectors").doc("conn_circle").set({
      connectorId: "conn_circle",
      name: "Circle USDC API",
      category: "STABLECOIN_PROVIDER",
      status: "INTEGRATION_PENDING",
      environment: "SANDBOX",
      latencyMs: 0,
      statusNote: "Requires Circle Mint API secret & entity KYB",
      updatedByUid: OWNER_UID,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection("connectivity_connectors").get());

  // Append-only audit logs: create allowed, update & delete strictly forbidden even for platform owner
  await assertSucceeds(
    aliceDb.collection("system_audit_logs").doc("log_1").set({
      actorUid: ALICE_UID,
      targetId: "TX-SP-10001",
      action: "PAYMENT_AUTHORIZED",
      details: "User confirmed 7-point review screen",
      entryHash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      createdAt: created,
    })
  );
  await assertFails(ownerDb.collection("system_audit_logs").doc("log_1").update({ details: "tampered" }));
  await assertFails(ownerDb.collection("system_audit_logs").doc("log_1").delete());

  // Emergency incidents: owner-only, non-deletable
  await assertFails(aliceDb.collection("emergency_incidents").get());
  await assertSucceeds(
    ownerDb.collection("emergency_incidents").doc("inc_1").set({
      ownerUid: OWNER_UID,
      title: "Stablecoin Depeg Alert Monitor",
      affectedService: "SETTLEMENT_ADAPTER",
      severity: "HIGH",
      phase: "DETECTION_AND_VERIFICATION",
      serviceIsolated: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(ownerDb.collection("emergency_incidents").doc("inc_1").delete());
});

