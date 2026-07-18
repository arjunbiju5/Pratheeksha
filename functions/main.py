# functions/main.py

from firebase_functions import db_fn, options
from firebase_admin import initialize_app, messaging, db
import logging

initialize_app()

@db_fn.on_value_created(
    reference="/bloodRequests/{requestId}",
    region="us-central1"  # change to asia-south1 if your DB is in that region
)
def notify_donors_on_blood_request(event: db_fn.Event) -> None:
    request = event.data

    if not request or not isinstance(request, dict):
        logging.warning("Invalid or empty blood request data")
        return

    blood_group    = request.get("bloodGroup", "")
    district       = request.get("district", "")
    requester_name = request.get("requesterName", "")
    mobile         = request.get("mobile", "")
    hospital       = request.get("hospital", "")
    city           = request.get("city", "")
    urgency        = request.get("urgency", "normal")
    request_id     = event.params.get("requestId", "")

    if not blood_group or not district:
        logging.warning("Missing bloodGroup or district in request")
        return

    # Query all donors in the same district
    donors_ref = db.reference("donors")
    all_donors = donors_ref.order_by_child("district").equal_to(district).get()

    if not all_donors:
        logging.info(f"No donors found in district: {district}")
        return

    # Filter: matching blood group + available + active + has FCM token
    tokens = []
    for uid, donor in all_donors.items():
        if (
            donor.get("bloodGroup") == blood_group and
            donor.get("available") is True and
            donor.get("status") == "active" and
            donor.get("fcmToken")
        ):
            tokens.append(donor["fcmToken"])

    if not tokens:
        logging.info(f"No available {blood_group} donors in {district}")
        return

    logging.info(f"Sending FCM to {len(tokens)} donors in {district} for {blood_group}")

    is_urgent = urgency == "urgent"
    title = f"🚨 URGENT: {blood_group} Blood Needed" if is_urgent else f"🩸 Blood Request: {blood_group}"
    body  = f"{requester_name} needs {blood_group} at {hospital}, {city}. Contact: {mobile}"

    # FCM multicast supports max 500 tokens per batch
    chunk_size = 500
    for i in range(0, len(tokens), chunk_size):
        chunk = tokens[i:i + chunk_size]

        message = messaging.MulticastMessage(
            notification=messaging.Notification(
                title=title,
                body=body
            ),
            data={
                "requestId":     request_id,
                "requesterName": requester_name,
                "mobile":        mobile,
                "bloodGroup":    blood_group,
                "hospital":      hospital,
                "city":          city,
                "district":      district,
                "urgency":       urgency,
                "type":          "blood_request"
            },
            android=messaging.AndroidConfig(
                priority="high" if is_urgent else "normal",
                notification=messaging.AndroidNotification(
                    channel_id="blood_requests",
                    priority="max" if is_urgent else "default",
                    default_sound=True
                )
            ),
            tokens=chunk
        )

        try:
            response = messaging.send_each_for_multicast(message)
            logging.info(
                f"Batch {i // chunk_size + 1}: "
                f"{response.success_count} sent, {response.failure_count} failed"
            )

            # Clean up stale tokens
            for idx, resp in enumerate(response.responses):
                if not resp.success:
                    error_code = resp.exception.code if resp.exception else ""
                    if error_code in (
                        "messaging/invalid-registration-token",
                        "messaging/registration-token-not-registered"
                    ):
                        stale_token = chunk[idx]
                        # Find and remove the stale token from DB
                        for uid, donor in all_donors.items():
                            if donor.get("fcmToken") == stale_token:
                                db.reference(f"donors/{uid}/fcmToken").delete()
                                logging.info(f"Removed stale token for donor {uid}")

        except Exception as e:
            logging.error(f"FCM batch error: {e}")