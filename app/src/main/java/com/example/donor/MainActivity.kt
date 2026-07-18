package com.example.donor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.donor.data.session.SessionManager
import com.example.donor.navigation.AppNavigation
import com.example.donor.ui.theme.DonorTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {

    private var openScreen by mutableStateOf<String?>(null)
    private var requestId  by mutableStateOf<String?>(null)
    private var fromNotification by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        try { FirebaseDatabase.getInstance().setPersistenceEnabled(true) } catch (e: Exception) { }

        window.statusBarColor     = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        window.decorView.systemUiVisibility = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        // Read initial intent (app launched from notification)
        openScreen = intent.getStringExtra("openScreen")
        requestId  = intent.getStringExtra("requestId")
        fromNotification = intent.getBooleanExtra("fromNotification", false)

        setContent {
            DonorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController  = rememberNavController()
                    val context        = LocalContext.current
                    val sessionManager = SessionManager(context)

                    AppNavigation(
                        navController  = navController,
                        sessionManager = sessionManager,
                        openScreen     = openScreen,
                        requestId      = requestId,
                        fromNotification = fromNotification
                    )
                }
            }
        }
    }

    // Called when notification tapped while app is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openScreen = intent.getStringExtra("openScreen")
        requestId  = intent.getStringExtra("requestId")
        fromNotification = intent.getBooleanExtra("fromNotification", false)
    }
}