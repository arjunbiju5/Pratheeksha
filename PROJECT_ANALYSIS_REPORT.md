# Project Analysis Report: Donor (BloodLink)

## 1. Overview
**Donor** (also referred to as **BloodLink** in the code) is a specialized Android application designed to bridge the gap between blood donors and those in urgent need of blood. The app focuses on location-based matching (by District and City) and blood group compatibility.

## 2. Core Functional Modules

### A. Donor Module
*   **Registration:** Donors sign up with details including Full Name, Phone Number, Date of Birth, Blood Group, Weight, District, and City.
*   **Authentication:** Uses a custom "Phone + Date of Birth" login mechanism.
*   **Dashboard:** Donors can see their eligibility status (calculated based on the 90-day recovery period), toggle their availability, and update their donation history.
*   **Eligibility Engine:** Automatically calculates `nextEligible` date after a donation is recorded and hides the donor from search results until they are recovered.

### B. Requester Module
*   **Public Access:** Requesters can view pending blood requests and find donors without necessarily having an account.
*   **Search:** Filterable search for donors based on Blood Group and Location.
*   **Request Submission:** Form to submit blood requirements including hospital details, urgency (Urgent/Normal), and contact info.

### C. Notification System
*   **Matching Notifications:** When a new request is created, the system identifies matching donors (same district + same blood group + currently available) and sends them push notifications.
*   **Multi-Channel Delivery:** 
    *   **Cloud Functions:** A Python-based Firebase Function (`main.py`) triggers on database writes.
    *   **In-App Dispatcher:** The app itself can send FCM messages using the `FcmNotificationSender`.
    *   **Best-Effort Listener:** `BloodRequestService` (in-app) listens for live updates while the app is foregrounded.

### D. Admin Module
*   **Management:** Admins can add, edit, or delete donor profiles.
*   **Request Oversight:** Admins receive notifications for every new request and can monitor the status of all pending requirements.

---

## 3. Technical Architecture

*   **UI Framework:** Jetpack Compose (Modern, declarative UI).
*   **Navigation:** Compose Navigation with a central `AppNavigation` graph.
*   **Concurrency:** Kotlin Coroutines & Flow for asynchronous operations.
*   **Backend / Database:** 
    *   **Firebase Realtime Database:** Primary storage with offline persistence enabled.
    *   **Firebase Cloud Functions (Python):** Server-side triggers for notifications.
    *   **Firebase Cloud Messaging (FCM):** Push notification delivery.
*   **Data Layer:**
    *   **ViewModels:** Encapsulate business logic and state management.
    *   **DataStore Preferences:** Used for persistent session management (SessionManager).
    *   **Models:** Clean data classes for `Donor` and `BloodRequest`.

---

## 4. Key Logic Flows

### Blood Request Lifecycle
1.  **Submission:** Requester fills form -> Written to `/bloodRequests`.
2.  **Trigger:** Firebase Function detects new entry.
3.  **Matching:** Query `/donors` where `district == request.district`.
4.  **Filtering:** Filter results for `bloodGroup` and `isCurrentlyAvailable()`.
5.  **Notification:** Send FCM to all matching tokens.

### Donor Eligibility Logic
*   **Rule:** 90 days recovery period.
*   **Check:** `nextEligible` date is compared against `currentDate`.
*   **Availability:** A donor is "Available" only if (`manualToggle == true`) AND (`today >= nextEligible`).

---

## 5. Identified Issues & Risks

### 🚨 Critical Security Risk: Service Account Exposure
The `FcmNotificationSender.kt` reads a `service_account.json` file directly from the app's `assets` folder. 
*   **Problem:** This file contains the private key for your entire Firebase project. Anyone who downloads the APK can decompile it, extract this key, and gain full administrative access to your Firebase Database, Auth, and Messaging.
*   **Fix:** **Remove this file immediately.** Move all notification-sending logic to the Firebase Cloud Function (Backend).

### ⚠️ Authentication Security
*   **Problem:** Login relies on "Phone Number + DOB". This is highly insecure as birthdays are often public or easily guessed. 
*   **Fix:** Implement Firebase Phone Authentication (OTP-based) for true security.

### 🔄 Logic Redundancy
*   **Problem:** Notifications are being sent from **two places**: the Android app (`RequestBloodViewModel.kt`) and the Firebase Function (`main.py`). This leads to duplicate notifications and wasted API calls.
*   **Fix:** Centralize all matching and notification logic in the Cloud Function. The app should only write the data to the database.

### 📡 Reliability of "Services"
*   **Problem:** `BloodRequestService.kt` is a plain class, not an Android `Service`. It will stop working if the app is killed by the OS.
*   **Fix:** Rely primarily on FCM (Push Notifications) for background alerts.

### 🔒 Data Privacy
*   **Problem:** Mobile numbers of donors are stored in plain text and retrieved in bulk during searches.
*   **Fix:** Use a proxy or mask numbers, and only reveal the contact info when a donor accepts a request (if the flow allows).

---

## 6. Recommendations for Improvement

1.  **Secure the Backend:** Delete the `service_account.json` from the Android project and move the `FcmNotificationSender` logic into a secure backend environment.
2.  **OTP Login:** Switch to Firebase Phone Auth to ensure only the owner of the phone number can access the donor profile.
3.  **Input Validation:** Add stricter server-side rules (Firebase Security Rules) to prevent unauthorized users from editing other people's donor profiles.
4.  **Admin Security:** Currently, admin login is likely handled via a hardcoded or simple check. Ensure admin nodes in the database are protected by specific Auth UID checks in Security Rules.
