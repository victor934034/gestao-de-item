package com.app.stockmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.app.stockmaster.ui.navigation.AppNavigation
import com.app.stockmaster.ui.theme.StockMasterTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.runtime.LaunchedEffect
import com.app.stockmaster.worker.StockNotificationWorker

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StockMasterTheme {
                val navController = rememberNavController()
                
                // Handle intent for navigation
                LaunchedEffect(intent) {
                    if (intent?.action == StockNotificationWorker.ACTION_SHOW_LOW_STOCK) {
                        navController.navigate("inventory?filter=low_stock") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                        }
                    }
                }

                AppNavigation(navController = navController)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to $name!",
        modifier = modifier
    )
}
