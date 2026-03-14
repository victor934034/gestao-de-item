package com.app.stockmaster.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsCategory("General")
            SettingsItem("Company Profile", Icons.Default.Business, "Logo, address, and name")
            SettingsItem("Notifications", Icons.Default.Notifications, "Stock alerts thresholds")
            
            Spacer(modifier = Modifier.height(16.dp))
            SettingsCategory("Data Management")
            SettingsItem("Export Data", Icons.Default.FileDownload, "Export as CSV or PDF")
            SettingsItem("Import Data", Icons.Default.FileUpload, "Import items from CSV")
            SettingsItem("Backup & Restore", Icons.Default.CloudSync, "Sync with Google Drive")

            Spacer(modifier = Modifier.height(16.dp))
            SettingsCategory("Appearance")
            SettingsItem("Theme", Icons.Default.Palette, "Dark mode / Light mode")
            SettingsItem("Language", Icons.Default.Language, "Portuguese / English")

            Spacer(modifier = Modifier.height(16.dp))
            SettingsCategory("About")
            SettingsItem("App Version", Icons.Default.Info, "v1.0.0 (Build 001)")
            SettingsItem("Licenses", Icons.Default.CardMembership, "Open source libraries")
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, subtitle: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}
