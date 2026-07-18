package com.example.donor.navigation

import com.example.donor.ui.request.RequesterDashboardScreen
import com.example.donor.ui.admin.AdminBloodRequestsScreen
import com.example.donor.ui.admin.AdminContactScreen
import com.example.donor.ui.request.PublicRequestsScreen
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.donor.data.model.Donor
import com.example.donor.data.session.SessionManager
import com.example.donor.ui.admin.AdminDashboardScreen
import com.example.donor.ui.admin.AdminEditDonorScreen
import com.example.donor.ui.admin.AdminLoginScreen
import com.example.donor.ui.admin.AdminViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.ui.dashboard.DashboardScreen
import com.example.donor.ui.home.HomeScreen
import com.example.donor.ui.login.LoginScreen
import com.example.donor.ui.registration.RegistrationScreen
import com.example.donor.ui.request.BloodRequestDetailScreen
import com.example.donor.ui.request.RequestBloodScreen
import com.example.donor.ui.search.FindDonorScreen
import com.example.donor.ui.splash.SplashScreen
import org.json.JSONObject

object Routes {
    const val SPLASH               = "splash"
    const val HOME                 = "home"
    const val LOGIN                = "login"
    const val REGISTRATION         = "registration"
    const val DASHBOARD            = "dashboard"
    const val FIND_DONOR           = "find_donor"
    const val ADMIN_LOGIN          = "admin_login"
    const val ADMIN_DASHBOARD      = "admin_dashboard"
    const val ADMIN_EDIT           = "admin_edit/{donorJson}"
    const val REQUEST_BLOOD        = "request_blood"
    const val BLOOD_REQUEST_DETAIL = "blood_request_detail/{requestId}"
    const val PENDING_REQUESTS     = "pending_requests"
    const val REQUESTER_DASHBOARD  = "requester_dashboard"
    const val ADMIN_REQUESTS       = "admin_requests"
    const val ADMIN_CONTACTS       = "admin_contacts"
    private const val NEW_DONOR_TOKEN = "new"

    fun bloodRequestDetailRoute(requestId: String) = "blood_request_detail/$requestId"

    fun adminEditRoute(donor: Donor): String {
        val donorJson = JSONObject().apply {
            put("uid",          donor.uid)
            put("fullName",     donor.fullName)
            put("bloodGroup",   donor.bloodGroup)
            put("mobile",       donor.mobile)
            put("district",     donor.district)
            put("city",         donor.city)
            put("dateOfBirth",  donor.dateOfBirth)
            put("weight",       donor.weight)
            put("regDate",      donor.regDate)
            put("lastDonation", donor.lastDonation)
            put("nextEligible", donor.nextEligible)
            put("available",    donor.available)
        }.toString()
        return "admin_edit/${Uri.encode(donorJson)}"
    }

    fun adminAddRoute(): String = "admin_edit/$NEW_DONOR_TOKEN"
    fun isNewDonorToken(value: String): Boolean = value == NEW_DONOR_TOKEN
}

@Composable
fun AppNavigation(
    openScreen: String? = null,
    requestId:  String? = null,
    fromNotification: Boolean = false,
    navController:  NavHostController,
    sessionManager: SessionManager
) {
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)
    val isAdmin    by sessionManager.isAdmin.collectAsState(initial = false)

    NavHost(
        navController    = navController,
        startDestination = Routes.SPLASH
    ) {

        // ── Splash ───────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                deepLinkRequestId = if (openScreen == "blood_request_detail") requestId else null,
                deepLinkPending   = openScreen == "pending_requests",
                fromNotification  = fromNotification, // ← can't access intent here
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onDeepLink = { reqId, fromNotif ->
                    if (fromNotif) {
                        // Push HOME → PENDING_REQUESTS → DETAIL so back works correctly
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                        navController.navigate(Routes.PENDING_REQUESTS)
                        navController.navigate(Routes.bloodRequestDetailRoute(reqId))
                    } else {
                        navController.navigate(Routes.bloodRequestDetailRoute(reqId)) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                },
                onDeepLinkPending = {
                    navController.navigate(Routes.PENDING_REQUESTS) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }



        // ── Admin Contacts ────────────────────────────────────────────────────
        composable(Routes.ADMIN_CONTACTS) {
            AdminContactScreen(onBack = { navController.popBackStack() })
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onRequestBlood        = { navController.navigate(Routes.REQUEST_BLOOD) },
                onFindDonor           = { navController.navigate(Routes.FIND_DONOR) },
                onDonorLogin          = { navController.navigate(Routes.LOGIN) },
                onAdminLogin          = { navController.navigate(Routes.ADMIN_LOGIN) },
                onViewPendingRequests = { navController.navigate(Routes.PENDING_REQUESTS) }
            )
        }

        // ── Requester Dashboard ───────────────────────────────────────────────
        composable(Routes.REQUESTER_DASHBOARD) {
            RequesterDashboardScreen(
                onBack        = { navController.popBackStack() },
                onNewRequest  = { navController.navigate(Routes.REQUEST_BLOOD) },
                onOpenRequest = { reqId ->
                    navController.navigate(Routes.bloodRequestDetailRoute(reqId))
                }
            )
        }

        // ── Donor Login ───────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTRATION)
                }
            )
        }

        // ── Request Blood ─────────────────────────────────────────────────────
        composable(Routes.REQUEST_BLOOD) {
            RequestBloodScreen(
                onBack         = { navController.popBackStack() },
                onSubmitted    = { navController.popBackStack() },
                onTrackRequest = { _ ->
                    navController.navigate(Routes.REQUESTER_DASHBOARD) {
                        popUpTo(Routes.REQUEST_BLOOD) { inclusive = true }
                    }
                }
            )
        }

        // ── Pending Requests (public, no login) ───────────────────────────────
        composable(Routes.PENDING_REQUESTS) {
            PublicRequestsScreen(
                onBack        = { navController.popBackStack() },
                onOpenRequest = { reqId ->
                    navController.navigate(Routes.bloodRequestDetailRoute(reqId))
                }
            )
        }

        // ── Registration ──────────────────────────────────────────────────────
        composable(Routes.REGISTRATION) {
            RegistrationScreen(
                onRegistered = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        // ── Donor Dashboard ───────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onLogout = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Blood Request Detail ──────────────────────────────────────────────
        composable(Routes.BLOOD_REQUEST_DETAIL) { backStackEntry ->
            val reqId = backStackEntry.arguments?.getString("requestId") ?: ""
            BloodRequestDetailScreen(
                requestId = reqId,
                isAdmin   = isAdmin,
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Find Donor ────────────────────────────────────────────────────────
        composable(Routes.FIND_DONOR) {
            FindDonorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Admin Login ───────────────────────────────────────────────────────
        composable(Routes.ADMIN_LOGIN) {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.ADMIN_DASHBOARD) {
                        popUpTo(Routes.ADMIN_LOGIN) { inclusive = true }
                    }
                },
                onBackToHome = { navController.popBackStack() }
            )
        }

        // ── Admin Dashboard ───────────────────────────────────────────────────
        composable(Routes.ADMIN_DASHBOARD) {
            val adminViewModel: AdminViewModel = viewModel()
            AdminDashboardScreen(
                viewModel        = adminViewModel,
                onLogout         = {
                    adminViewModel.adminLogout()
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onEditDonor      = { donor -> navController.navigate(Routes.adminEditRoute(donor)) },
                onAddDonor       = { navController.navigate(Routes.adminAddRoute()) },
                onViewRequests   = { navController.navigate(Routes.ADMIN_REQUESTS) },
                onManageContacts = { navController.navigate(Routes.ADMIN_CONTACTS) }
            )
        }

        // ── Admin Blood Requests ──────────────────────────────────────────────
        composable(Routes.ADMIN_REQUESTS) {
            AdminBloodRequestsScreen(
                onBack        = { navController.popBackStack() },
                onOpenRequest = { reqId ->
                    navController.navigate(Routes.bloodRequestDetailRoute(reqId))
                }
            )
        }

        // ── Admin Edit / Add Donor ────────────────────────────────────────────
        composable(Routes.ADMIN_EDIT) { backStackEntry ->
            val donorJson = backStackEntry.arguments?.getString("donorJson") ?: "{}"
            val donor = if (Routes.isNewDonorToken(donorJson)) null
            else parseDonorFromJson(donorJson)
            AdminEditDonorScreen(
                donor  = donor,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

fun parseDonorFromJson(json: String): Donor {
    return try {
        val obj = JSONObject(json)
        Donor(
            uid          = obj.optString("uid",          ""),
            fullName     = obj.optString("fullName",     ""),
            bloodGroup   = obj.optString("bloodGroup",   ""),
            mobile       = obj.optString("mobile",       ""),
            district     = obj.optString("district",     ""),
            city         = obj.optString("city",         ""),
            dateOfBirth  = obj.optString("dateOfBirth",  ""),
            weight       = obj.optString("weight",       ""),
            regDate      = obj.optString("regDate",      ""),
            lastDonation = obj.optString("lastDonation", ""),
            nextEligible = obj.optString("nextEligible", ""),
            available    = obj.optBoolean("available",   true)
        )
    } catch (e: Exception) {
        Donor()
    }
}