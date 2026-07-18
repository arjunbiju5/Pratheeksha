const functions = require("firebase-functions");
const admin     = require("firebase-admin");

admin.initializeApp();

const db = admin.database();

// ─────────────────────────────────────────────────────────────────────────────
// FUNCTION 1 — notifyDonorsOnNewRequest
//
// Triggers when a new blood request is written to /bloodRequests/{requestId}.
// Scans all donors and sends an FCM notification to every donor whose
// bloodGroup + district matches AND who is available + eligible.
// ─────────────────────────────────────────────────────────────────────────────

exports.notifyDonorsOnNewRequest = functions.database
    .ref("/bloodRequests/{requestId}")
    .onCreate(async (snapshot, context) => {
        const request = snapshot.val();
        if (!request) return null;

        // Only notify for pending requests
        if (request.status !== "pending") return null;

        const requestBloodGroup = (request.bloodGroup || "").trim().toLowerCase();
        const requestDistrict   = (request.district   || "").trim().toLowerCase();
        const isUrgent          = request.urgency === "urgent";

        // ── Load all donors ──────────────────────────────────────────────────
        const donorsSnap = await db.ref("/donors").once("value");
        if (!donorsSnap.exists()) return null;

        const tokens = [];

        donorsSnap.forEach((donorSnap) => {
            const donor = donorSnap.val();
            if (!donor || !donor.fcmToken) return;

            const donorBloodGroup = (donor.bloodGroup || "").trim().toLowerCase();
            const donorDistrict   = (donor.district   || "").trim().toLowerCase();
            const isAvailable     = donor.available === true;

            // Eligibility — check if today is past nextEligible date
            let isEligible = true;
            if (donor.nextEligible && donor.nextEligible.trim() !== "") {
                try {
                    // nextEligible is stored as dd/MM/yyyy
                    const parts     = donor.nextEligible.trim().split("/");
                    const nextDate  = new Date(
                        parseInt(parts[2]),      // year
                        parseInt(parts[1]) - 1,  // month (0-indexed)
                        parseInt(parts[0])        // day
                    );
                    isEligible = new Date() > nextDate;
                } catch (e) {
                    isEligible = true; // parse failure → assume eligible
                }
            }

            const bloodGroupMatch = donorBloodGroup === requestBloodGroup;
            const districtMatch   = donorDistrict   === requestDistrict;
            const canDonate       = isAvailable && isEligible;

            if (bloodGroupMatch && districtMatch && canDonate) {
                tokens.push(donor.fcmToken);
            }
        });

        if (tokens.length === 0) {
            console.log("No matching donors found for request:", context.params.requestId);
            return null;
        }

        // ── Build notification ───────────────────────────────────────────────
        const title = isUrgent
            ? `🚨 Urgent: ${request.bloodGroup} Blood Needed`
            : `🩸 Blood Request: ${request.bloodGroup}`;

        const body = `${request.requesterName} needs ${request.bloodGroup} at ` +
                     `${request.hospital}, ${request.city}. Tap to help.`;

        // ── Send in batches of 500 (FCM multicast limit) ─────────────────────
        const batchSize = 500;
        const sendPromises = [];

        for (let i = 0; i < tokens.length; i += batchSize) {
            const batch = tokens.slice(i, i + batchSize);
            const message = {
                tokens: batch,
                notification: { title, body },
                data: {
                    requestId: context.params.requestId,
                    screen:    "blood_request_detail",
                    urgency:   request.urgency || "normal"
                },
                android: {
                    priority: isUrgent ? "high" : "normal",
                    notification: {
                        channelId: "blood_requests",
                        priority:  isUrgent ? "max" : "high",
                        defaultVibrateTimings: true,
                        defaultSound: true
                    }
                }
            };
            sendPromises.push(admin.messaging().sendEachForMulticast(message));
        }

        const results = await Promise.all(sendPromises);
        results.forEach((result) => {
            console.log(
                `FCM batch sent — success: ${result.successCount}, ` +
                `failure: ${result.failureCount}`
            );
        });

        return null;
    });

// ─────────────────────────────────────────────────────────────────────────────
// FUNCTION 2 — notifyRequesterOnAcknowledge
//
// Triggers when a donor writes to /bloodRequests/{requestId}/responses/{donorUid}.
// Sends an FCM notification to the requester so they know a donor responded.
// ─────────────────────────────────────────────────────────────────────────────

exports.notifyRequesterOnAcknowledge = functions.database
    .ref("/bloodRequests/{requestId}/responses/{donorUid}")
    .onCreate(async (snapshot, context) => {
        const response  = snapshot.val();
        if (!response) return null;

        const requestId = context.params.requestId;

        // ── Load the parent blood request ────────────────────────────────────
        const requestSnap = await db.ref(`/bloodRequests/${requestId}`).once("value");
        if (!requestSnap.exists()) return null;

        const request = requestSnap.val();

        // requesterFcmToken is saved when the request is submitted
        const requesterToken = request.requesterFcmToken;
        if (!requesterToken) {
            console.log("No FCM token for requester on request:", requestId);
            return null;
        }

        // ── Build notification ───────────────────────────────────────────────
        const donorName   = response.donorName  || "A donor";
        const bloodGroup  = response.bloodGroup || "";
        const phone       = response.phone      || "";

        const title = "🩸 A donor has responded!";
        const body  = `${donorName} (${bloodGroup}) is willing to help. ` +
                      `Contact: ${phone}`;

        const message = {
            token: requesterToken,
            notification: { title, body },
            data: {
                requestId,
                screen:    "blood_request_detail",
                donorName,
                donorPhone: phone
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "blood_requests",
                    priority:  "high",
                    defaultVibrateTimings: true,
                    defaultSound: true
                }
            }
        };

        try {
            await admin.messaging().send(message);
            console.log("Requester notified for request:", requestId);
        } catch (e) {
            console.error("Failed to notify requester:", e.message);
        }

        return null;
    });