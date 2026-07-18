# Technical Audit Report: BloodLink (Donor App)

This audit evaluates the codebase for correctness, data integrity, and architectural robustness.

## 1. Data Model ↔ Data Flow Consistency
- **File + Function:** `RegistrationScreen.kt` line 280 / `RegistrationViewModel.kt` line 58.
- **What's wrong:** The `mobile` field is hardcoded to `""` in the UI call, and the ViewModel falls back to `auth.currentUser?.phoneNumber`.
- **Failure scenario:** If a user logs in via a method other than Phone Auth, or if the Firebase Auth object doesn't contain the phone number (e.g., test accounts), the donor profile is created with an empty mobile number, making it impossible for others to contact them.
- **Minimal fix:** Add a mobile number `OutlinedTextField` to the `RegistrationScreen` to ensure a verified contact number is explicitly provided and validated.

## 2. Read Consistency (Live vs. One-time Fetches)
- **no issues found** (All search and profile screens now use `addValueEventListener` with proper `onCleared` detachment).

## 3. State Management
- **File + Function:** `AdminViewModel.kt` line 140.
- **What's wrong:** `loadAllDonors()` is called inside `addDonor` success listener even though a live listener is already active.
- **Failure scenario:** Not a crash, but creates redundant network overhead. Since `loadAllDonors()` re-attaches a listener, and the listener is already active, it triggers double-processing of the data list.
- **Minimal fix:** Remove manual calls to `loadAllDonors()` from `addDonor`, `updateDonor`, and `deleteDonor` as the `allDonorsListener` will automatically update the state on any change.

## 4. Validation & Form Logic
- **File + Function:** `RegistrationScreen.kt` line 53 / `AdminEditDonorScreen.kt` line 110.
- **What's wrong:** Dependent city logic is duplicated across two files with different handling for empty states.
- **Failure scenario:** If the `districtCityMap` (see section 5) is updated in one file but not the other, users in `Registration` will see different options than an `Admin` editing the same profile, leading to data corruption (e.g., a city belonging to the wrong district).
- **Minimal fix:** Centralize the `availableCities` logic into a shared `LocationRepository` or helper class.

## 5. Duplication & Single-Source-of-Truth Violations
- **File + Function:** `AdminEditDonorScreen.kt` (L23, L34) and `RegistrationScreen.kt` (L19, L24).
- **What's wrong:** `bloodGroups`, `keralaDistricts`, and `districtCityMap` are defined as private constants in multiple UI files.
- **Failure scenario:** Adding a new blood group (e.g., "Oh-") or a new city to the map requires hunting through every UI file. Missing one results in a "drift" where registration allows options that the admin panel cannot display or edit.
- **Minimal fix:** Move all lookup lists and maps into a `Constants.kt` or a `DataConfig` singleton in the `data` package.

## 6. Navigation & Lifecycle
- **File + Function:** `AppNavigation.kt` line 166.
- **What's wrong:** `AdminEditDonorScreen` receives a serialized JSON string that is parsed back into a `Donor` object.
- **Failure scenario:** If the donor object contains characters that break JSON serialization/URL encoding (like certain symbols in names), the route will fail or the object will arrive malformed, defaulting to a blank `Donor()` profile.
- **Minimal fix:** Pass only the `donorUid` in the route and have the `AdminViewModel` fetch/provide the specific donor from its existing `allDonors` list or a separate fetch.

## 7. Auth & Permissions
- **File + Function:** `AdminViewModel.kt` line 52.
- **What's wrong:** Admin authorization check is performed client-side using `snapshot.exists()` on the `/admins` node.
- **Failure scenario:** A malicious user can bypass this UI gate using a modified APK or by interacting directly with the Firebase REST API. If database rules are not set to restrict `.write` to `auth.token.admin == true`, anyone can delete the entire `donors` node.
- **Minimal fix:** This requires a server-side change (Firebase Security Rules). The code should also ideally check a custom claim rather than a database node.

## 8. Error Handling Completeness
- **File + Function:** `DashboardViewModel.kt` lines 76 and 91.
- **What's wrong:** `saveDonationDate` and `clearDonationDate` calls to `updateChildren` have no `addOnFailureListener`.
- **Failure scenario:** If the write fails (e.g., network timeout, permission denied), the UI doesn't know. The user sees the checkbox change but the data isn't actually saved, leading to a "silent fail."
- **Minimal fix:** Add `.addOnFailureListener { _uiState.update { ... } }` to all database write operations.

## 9. Edge Cases
- **File + Function:** `AdminDashboardScreen.kt` line 34 / `FindDonorViewModel.kt` line 108.
- **What's wrong:** Duplicate availability calculation logic (`isAvailableForDashboard` vs `isDonorAvailable`).
- **Failure scenario:** The `AdminDashboard` might show a donor as "Available" while the `FindDonor` search hides them because they used slightly different date-comparison logic.
- **Minimal fix:** Move `isDonorAvailable(donor)` into the `Donor` data class itself or a shared `DonorExtensions.kt`.

## 10. Build/Output Sanity
- **File + Function:** `FindDonorSearch.kt`
- **What's wrong:** File contains a massive static map (`districtCityMap`) and a UI skeleton, but the logic for searching is in a different file.
- **Failure scenario:** Confusion during maintenance; developers may edit the map in this file without realizing `AdminEditDonorScreen` has its own copy.
- **Minimal fix:** Rename this file to `FindDonorScreen.kt` and move the static map to a shared Constants file.
