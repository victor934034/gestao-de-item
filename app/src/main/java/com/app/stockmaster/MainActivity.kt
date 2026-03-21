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
import com.app.stockmaster.BuildConfig

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import com.app.stockmaster.data.remote.BridgeApi
import com.app.stockmaster.worker.StockNotificationWorker
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bridgeApi: BridgeApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StockMasterTheme {
                val navController = rememberNavController()
                
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateUrl by remember { mutableStateOf("") }
                var isMandatoryUpdate by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    try {
                        val response = bridgeApi.getLatestVersion()
                        if (response.isSuccessful && response.body()?.success == true) {
                            val versionInfo = response.body()?.version
                            if (versionInfo != null) {
                                val currentVersion = BuildConfig.VERSION_NAME
                                
                                // Simple string comparison (assumes format like "1.0.0")
                                // In production, a better semantic version comparison should be used
                                if (isVersionOutdated(currentVersion, versionInfo.latestVersion)) {
                                    showUpdateDialog = true
                                    updateUrl = versionInfo.apkUrl
                                    isMandatoryUpdate = isVersionOutdated(currentVersion, versionInfo.minVersion)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (showUpdateDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            if (!isMandatoryUpdate) showUpdateDialog = false
                        },
                        title = { Text(text = "Atualização Disponível", fontWeight = FontWeight.Bold) },
                        text = { 
                            Text(
                                if (isMandatoryUpdate) 
                                    "Uma nova versão obrigatória do aplicativo está disponível. Por favor, atualize para continuar usando o app." 
                                else 
                                    "Uma nova versão do aplicativo está disponível. Deseja atualizar agora?"
                            ) 
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                                    startActivity(intent)
                                }
                            ) {
                                Text("Atualizar Agora")
                            }
                        },
                        dismissButton = {
                            if (!isMandatoryUpdate) {
                                TextButton(onClick = { showUpdateDialog = false }) {
                                    Text("Depois")
                                }
                            }
                        }
                    )
                }
                
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

    private fun isVersionOutdated(current: String, target: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toInt() }
            val targetParts = target.split(".").map { it.toInt() }
            
            for (i in 0 until maxOf(currentParts.size, targetParts.size)) {
                val c = currentParts.getOrElse(i) { 0 }
                val t = targetParts.getOrElse(i) { 0 }
                if (c < t) return true
                if (c > t) return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to $name!",
        modifier = modifier
    )
}
