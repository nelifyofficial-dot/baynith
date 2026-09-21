const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const crypto = require("crypto");

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();

// PalmPesa Secret Configuration
const palmpesaApiToken = defineSecret("PALMPESA_API_TOKEN");

// Documented PalmPesa credentials
const PALMPESA_USER_ID = "USR-4CB7729FA743";
const PALMPESA_VENDOR = "61103867";
const PALMPESA_BASE_URL = "https://palmpesa.drmlelwa.co.tz";

// Server-side trusted pricing and duration mapping (TZS)
const PLAN_CONFIG = {
  daily: {
    name: "1 Day (Siku 1)",
    amount: 1000,
    durationMs: 1 * 24 * 60 * 60 * 1000 // 1 day
  },
  weekly: {
    name: "7 Days (Wiki 1)",
    amount: 3000,
    durationMs: 7 * 24 * 60 * 60 * 1000 // 7 days
  },
  monthly: {
    name: "30 Days (Mwezi 1)",
    amount: 10000,
    durationMs: 30 * 24 * 60 * 60 * 1000 // 30 days
  }
};

/**
 * Sanitizes phone number to Tanzanian standard format:
 * e.g. "0712345678" -> "255712345678" or accepted "07xxxxxxxx"
 */
function sanitizeTzPhone(rawPhone) {
  if (!rawPhone) return "";
  let digits = rawPhone.replace(/\D/g, "");
  if (digits.startsWith("0") && (digits.startsWith("06") || digits.startsWith("07"))) {
    return "255" + digits.substring(1);
  }
  if (digits.startsWith("255") && digits.length === 12) {
    return digits;
  }
  if (digits.startsWith("+255")) {
    return digits.substring(1);
  }
  return digits;
}

/**
 * Validates Tanzanian mobile phone number
 */
function isValidTzPhone(phone) {
  const sanitized = sanitizeTzPhone(phone);
  // Tanzanian mobile format with 255: 2556xxxxxxxx or 2557xxxxxxxx (12 digits)
  return /^255(6|7)\d{8}$/.test(sanitized);
}

/**
 * 1. CREATE PAYMENT FUNCTION (Callable HTTPS Cloud Function)
 *
 * Secures payment creation by identifying the authenticated Firebase UID,
 * mapping plan strictly on the server (client cannot modify the amount),
 * creating a pending order in Firestore, and calling the official PalmPesa API.
 */
exports.createPalmPesaPremiumPayment = onCall(
  { secrets: [palmpesaApiToken] },
  async (request) => {
    // 1. Verify user authentication
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError(
        "unauthenticated",
        "Ingia au fungua akaunti ili kuendelea (Authentication required)."
      );
    }

    const firebaseUid = request.auth.uid;
    const requestedPlan = (request.data?.plan || "").toLowerCase().trim();

    // 2. Validate plan and determine trusted amount
    const planDetails = PLAN_CONFIG[requestedPlan];
    if (!planDetails) {
      throw new HttpsError(
        "invalid-argument",
        "Chagua kifurushi sahihi cha Premium (Valid plans: daily, weekly, monthly)."
      );
    }

    const trustedAmount = planDetails.amount;
    const currency = "TZS";

    // 3. Fetch user profile from Firestore
    const userDocRef = db.collection("users").document(firebaseUid);
    const userDoc = await userDocRef.get();
    const userData = userDoc.exists ? userDoc.data() : {};

    const buyerName =
      userData.displayName ||
      request.auth.token.name ||
      request.data?.buyerName ||
      "NeliPlay User";

    const buyerEmail =
      userData.email ||
      request.auth.token.email ||
      request.data?.buyerEmail ||
      `${firebaseUid}@neliplay.app`;

    const rawPhone =
      request.data?.phone ||
      userData.phoneNumber ||
      userData.phone ||
      "";

    const sanitizedPhone = sanitizeTzPhone(rawPhone);
    if (!isValidTzPhone(sanitizedPhone)) {
      throw new HttpsError(
        "invalid-argument",
        "Tafadhali weka nambari sahihi ya simu ya Tanzania (mfano 07xxxxxxxx au 06xxxxxxxx)."
      );
    }

    // 4. Generate cryptographically secure unique order ID
    const randomSuffix = crypto.randomBytes(4).toString("hex").toUpperCase();
    const orderId = `NELI_${Date.now()}_${randomSuffix}`;
    const paymentId = `PAY_${orderId}`;

    const now = admin.firestore.FieldValue.serverTimestamp();

    // 5. Create pending Firestore payment record
    const paymentRef = db.collection("payments").document(paymentId);
    const paymentRecord = {
      paymentId: paymentId,
      orderId: orderId,
      firebaseUid: firebaseUid,
      plan: requestedPlan,
      amount: trustedAmount,
      currency: currency,
      status: "PENDING",
      provider: "PALMPESA",
      buyerName: buyerName,
      buyerEmail: buyerEmail,
      buyerPhone: sanitizedPhone,
      createdAt: now,
      updatedAt: now,
      providerReference: null,
      transactionId: null,
      completedAt: null
    };

    await paymentRef.set(paymentRecord);

    console.log(
      `[PalmPesa] Order created for UID=${firebaseUid}, OrderId=${orderId}, Plan=${requestedPlan}, Amount=${trustedAmount} TZS`
    );

    // 6. Build PalmPesa Request Payload according to official documentation
    const secretToken = palmpesaApiToken.value();
    if (!secretToken) {
      console.error("[PalmPesa] Missing PALMPESA_API_TOKEN in secret storage.");
      throw new HttpsError(
        "internal",
        "Mfumo wa malipo hauko tayari kwa sasa. Tafadhali jaribu baadaye."
      );
    }

    const projectId = process.env.GCLOUD_PROJECT || "neliplay";
    const region = process.env.FUNCTION_REGION || "us-central1";
    const webhookUrl = `https://${region}-${projectId}.cloudfunctions.net/palmPesaWebhook`;

    const palmPesaPayload = {
      user_id: PALMPESA_USER_ID,
      vendor: PALMPESA_VENDOR,
      order_id: orderId,
      buyer_email: buyerEmail,
      buyer_name: buyerName,
      buyer_phone: sanitizedPhone,
      amount: trustedAmount,
      currency: currency,
      redirect_url: `https://neliplay.app/payment/return?order_id=${orderId}`,
      cancel_url: `https://neliplay.app/payment/cancel?order_id=${orderId}`,
      webhook: webhookUrl,
      buyer_remarks: `NeliPlay Premium subscription (${planDetails.name})`,
      merchant_remarks: "NeliPlay Premium",
      no_of_items: 1
    };

    try {
      const response = await fetch(`${PALMPESA_BASE_URL}/api/process-payment`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          Authorization: `Bearer ${secretToken}`
        },
        body: JSON.stringify(palmPesaPayload)
      });

      const responseText = await response.text();
      let responseData = {};
      try {
        responseData = JSON.parse(responseText);
      } catch (parseErr) {
        console.warn("[PalmPesa] Non-JSON response received:", responseText);
      }

      console.log(
        `[PalmPesa] Response status=${response.status} for OrderId=${orderId}`
      );

      // Extract transaction identifiers
      const transactionId =
        responseData?.transaction_id ||
        responseData?.data?.[0]?.transaction_id ||
        responseData?.data?.transaction_id ||
        null;

      const providerReference =
        responseData?.reference ||
        responseData?.data?.[0]?.reference ||
        responseData?.reference_id ||
        null;

      const checkoutUrl =
        responseData?.payment_url ||
        responseData?.checkout_url ||
        responseData?.data?.[0]?.payment_url ||
        null;

      // Update payment with provider references
      await paymentRef.update({
        transactionId: transactionId,
        providerReference: providerReference,
        checkoutUrl: checkoutUrl,
        providerInitialResponse: responseData,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      return {
        success: true,
        paymentId: paymentId,
        orderId: orderId,
        plan: requestedPlan,
        amount: trustedAmount,
        currency: currency,
        buyerPhone: sanitizedPhone,
        checkoutUrl: checkoutUrl,
        transactionId: transactionId,
        message: "Ombi la malipo limepokelewa. Angalia simu yako kuthibitisha kwa PIN."
      };
    } catch (err) {
      console.error(`[PalmPesa] Error calling process-payment for ${orderId}:`, err.message);
      await paymentRef.update({
        status: "FAILED",
        errorMessage: err.message,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      throw new HttpsError(
        "unavailable",
        "Imeshindwa kuwasiliana na mfumo wa malipo wa PalmPesa. Tafadhali jaribu tena."
      );
    }
  }
);

/**
 * Helper to activate Premium on a user document idempotently
 */
async function activatePremiumIdempotent(paymentDoc, providerTxId, providerRef) {
  const paymentData = paymentDoc.data();
  if (paymentData.status === "COMPLETED") {
    console.log(`[PalmPesa] Payment ${paymentDoc.id} is already COMPLETED. Skipping.`);
    return { alreadyCompleted: true };
  }

  const firebaseUid = paymentData.firebaseUid;
  const plan = paymentData.plan;
  const planDetails = PLAN_CONFIG[plan] || PLAN_CONFIG.monthly;
  const durationMs = planDetails.durationMs;

  const userRef = db.collection("users").document(firebaseUid);

  await db.runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    const existingUserData = userSnapshot.exists ? userSnapshot.data() : {};

    const nowMs = Date.now();
    let currentExpiryMs = 0;

    if (existingUserData.premiumUntil) {
      if (typeof existingUserData.premiumUntil.toMillis === "function") {
        currentExpiryMs = existingUserData.premiumUntil.toMillis();
      } else if (existingUserData.premiumUntil instanceof Date) {
        currentExpiryMs = existingUserData.premiumUntil.getTime();
      }
    }

    // If existing premium has not expired, add duration to current expiry; otherwise add to now
    let newPremiumUntilMs;
    if (currentExpiryMs > nowMs) {
      newPremiumUntilMs = currentExpiryMs + durationMs;
    } else {
      newPremiumUntilMs = nowMs + durationMs;
    }

    const newPremiumUntilTimestamp = admin.firestore.Timestamp.fromMillis(newPremiumUntilMs);
    const serverNow = admin.firestore.FieldValue.serverTimestamp();

    // 1. Update user document
    transaction.set(
      userRef,
      {
        premiumActive: true,
        premiumUntil: newPremiumUntilTimestamp,
        premiumPlan: plan,
        premiumUpdatedAt: serverNow,
        updatedAt: serverNow
      },
      { merge: true }
    );

    // 2. Update payment document
    transaction.update(paymentDoc.ref, {
      status: "COMPLETED",
      transactionId: providerTxId || paymentData.transactionId || null,
      providerReference: providerRef || paymentData.providerReference || null,
      completedAt: serverNow,
      updatedAt: serverNow
    });
  });

  console.log(
    `[PalmPesa] Activated Premium for UID=${firebaseUid}, Plan=${plan}, DurationMs=${durationMs}`
  );
  return { alreadyCompleted: false, success: true };
}

/**
 * 2. PALMPESA WEBHOOK (Public HTTPS Cloud Function)
 *
 * Receives notifications from PalmPesa, matches order_id, verifies amount/currency,
 * activates Premium idempotently, and returns HTTP 200 within 10 seconds.
 */
exports.palmPesaWebhook = onRequest(
  { secrets: [palmpesaApiToken] },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const payload = req.body || {};
    console.log("[PalmPesa Webhook] Received notification:", JSON.stringify(payload));

    const orderId =
      payload.order_id ||
      payload.orderId ||
      payload.data?.[0]?.order_id ||
      payload.data?.order_id;

    if (!orderId) {
      console.warn("[PalmPesa Webhook] Missing order_id in webhook payload.");
      res.status(400).json({ error: "Missing order_id" });
      return;
    }

    try {
      // Find matching payment in Firestore
      const paymentsSnapshot = await db
        .collection("payments")
        .where("orderId", "==", orderId)
        .limit(1)
        .get();

      if (paymentsSnapshot.empty) {
        console.warn(`[PalmPesa Webhook] No payment record found for orderId=${orderId}`);
        res.status(404).json({ error: "Order not found" });
        return;
      }

      const paymentDoc = paymentsSnapshot.docs[0];
      const paymentData = paymentDoc.data();

      // Check for duplicate processing (idempotency)
      if (paymentData.status === "COMPLETED") {
        console.log(`[PalmPesa Webhook] Order ${orderId} already completed. Responding 200.`);
        res.status(200).json({ status: "already_completed", order_id: orderId });
        return;
      }

      // Verify currency
      const receivedCurrency = payload.currency || payload.data?.[0]?.currency || "TZS";
      if (receivedCurrency.toUpperCase() !== "TZS") {
        console.error(`[PalmPesa Webhook] Currency mismatch for ${orderId}: ${receivedCurrency}`);
        res.status(400).json({ error: "Invalid currency" });
        return;
      }

      // Check payment status indicators from PalmPesa
      const paymentStatus = (
        payload.status ||
        payload.payment_status ||
        payload.data?.[0]?.payment_status ||
        payload.data?.[0]?.status ||
        ""
      ).toUpperCase();

      const isSuccessful =
        paymentStatus === "COMPLETED" ||
        paymentStatus === "SUCCESS" ||
        paymentStatus === "SUCCESSFUL" ||
        payload.success === true;

      const providerTxId =
        payload.transaction_id ||
        payload.data?.[0]?.transaction_id ||
        payload.data?.transaction_id ||
        null;

      const providerRef =
        payload.reference ||
        payload.data?.[0]?.reference ||
        null;

      if (isSuccessful) {
        // Verify amount
        const receivedAmount = Number(payload.amount || payload.data?.[0]?.amount || paymentData.amount);
        if (receivedAmount < paymentData.amount) {
          console.error(
            `[PalmPesa Webhook] Underpaid amount: expected ${paymentData.amount}, received ${receivedAmount}`
          );
          await paymentDoc.ref.update({
            status: "UNDERPAID",
            receivedAmount: receivedAmount,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          res.status(400).json({ error: "Underpaid amount" });
          return;
        }

        await activatePremiumIdempotent(paymentDoc, providerTxId, providerRef);
        res.status(200).json({ status: "success", order_id: orderId });
      } else if (
        paymentStatus === "FAILED" ||
        paymentStatus === "CANCELLED" ||
        paymentStatus === "REJECTED"
      ) {
        await paymentDoc.ref.update({
          status: paymentStatus,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        res.status(200).json({ status: "recorded_failure", order_id: orderId });
      } else {
        // Pending or in-progress status update
        console.log(`[PalmPesa Webhook] Order ${orderId} status update: ${paymentStatus}`);
        await paymentDoc.ref.update({
          providerStatus: paymentStatus,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        res.status(200).json({ status: "pending", order_id: orderId });
      }
    } catch (err) {
      console.error(`[PalmPesa Webhook] Error processing webhook for order ${orderId}:`, err);
      res.status(500).json({ error: "Internal server error" });
    }
  }
);

/**
 * 3. ORDER STATUS FALLBACK / VERIFY PAYMENT FUNCTION (Callable HTTPS Cloud Function)
 *
 * Secure server-side verification: checks Firestore payment record and if still pending,
 * queries the official PalmPesa Order Status endpoint with the secure secret token.
 */
exports.verifyPalmPesaPayment = onCall(
  { secrets: [palmpesaApiToken] },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError("unauthenticated", "User must be authenticated.");
    }

    const firebaseUid = request.auth.uid;
    const paymentId = request.data?.paymentId;
    const orderId = request.data?.orderId;

    if (!paymentId && !orderId) {
      throw new HttpsError("invalid-argument", "paymentId or orderId is required.");
    }

    let paymentDoc;
    if (paymentId) {
      const doc = await db.collection("payments").document(paymentId).get();
      if (doc.exists) paymentDoc = doc;
    }

    if (!paymentDoc && orderId) {
      const q = await db
        .collection("payments")
        .where("orderId", "==", orderId)
        .limit(1)
        .get();
      if (!q.empty) paymentDoc = q.docs[0];
    }

    if (!paymentDoc || !paymentDoc.exists) {
      throw new HttpsError("not-found", "Payment record not found.");
    }

    const paymentData = paymentDoc.data();

    // Security check: Order must belong to the requesting user
    if (paymentData.firebaseUid !== firebaseUid) {
      throw new HttpsError("permission-denied", "Unauthorized access to payment record.");
    }

    // If already completed in Firestore, return success immediately
    if (paymentData.status === "COMPLETED") {
      const userDoc = await db.collection("users").document(firebaseUid).get();
      const userData = userDoc.exists ? userDoc.data() : {};
      return {
        status: "COMPLETED",
        orderId: paymentData.orderId,
        plan: paymentData.plan,
        amount: paymentData.amount,
        premiumActive: userData.premiumActive === true,
        premiumUntil: userData.premiumUntil || null,
        message: "Premium imewashwa!"
      };
    }

    // If still pending, query PalmPesa status endpoint
    const secretToken = palmpesaApiToken.value();
    if (secretToken && paymentData.orderId) {
      try {
        const statusUrl = `${PALMPESA_BASE_URL}/api/order-status?order_id=${encodeURIComponent(
          paymentData.orderId
        )}`;
        const statusRes = await fetch(statusUrl, {
          method: "GET",
          headers: {
            Authorization: `Bearer ${secretToken}`,
            Accept: "application/json"
          }
        });

        if (statusRes.ok) {
          const statusData = await statusRes.json();
          const remoteStatus = (
            statusData?.data?.[0]?.payment_status ||
            statusData?.status ||
            statusData?.payment_status ||
            ""
          ).toUpperCase();

          const txId =
            statusData?.data?.[0]?.transaction_id ||
            statusData?.transaction_id ||
            null;

          if (
            remoteStatus === "COMPLETED" ||
            remoteStatus === "SUCCESS" ||
            remoteStatus === "SUCCESSFUL"
          ) {
            await activatePremiumIdempotent(paymentDoc, txId, null);
            const userDoc = await db.collection("users").document(firebaseUid).get();
            const userData = userDoc.exists ? userDoc.data() : {};
            return {
              status: "COMPLETED",
              orderId: paymentData.orderId,
              plan: paymentData.plan,
              amount: paymentData.amount,
              premiumActive: true,
              premiumUntil: userData.premiumUntil || null,
              message: "Premium imewashwa!"
            };
          } else if (
            remoteStatus === "FAILED" ||
            remoteStatus === "CANCELLED"
          ) {
            await paymentDoc.ref.update({
              status: remoteStatus,
              updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            return {
              status: remoteStatus,
              orderId: paymentData.orderId,
              message: "Malipo yameshindikana au yameghairiwa."
            };
          }
        }
      } catch (checkErr) {
        console.warn(`[PalmPesa] Order status check notice for ${paymentData.orderId}:`, checkErr.message);
      }
    }

    // Default return current Firestore state
    return {
      status: paymentData.status || "PENDING",
      orderId: paymentData.orderId,
      plan: paymentData.plan,
      amount: paymentData.amount,
      message:
        paymentData.status === "PENDING"
          ? "Malipo bado yanathibitishwa."
          : `Hali ya malipo: ${paymentData.status}`
    };
  }
);
