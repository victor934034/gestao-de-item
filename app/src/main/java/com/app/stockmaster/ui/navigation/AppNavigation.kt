package com.app.stockmaster.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.app.stockmaster.ui.adjustment.StockAdjustmentScreen
import com.app.stockmaster.ui.dashboard.DashboardScreen
import com.app.stockmaster.ui.inventory.InventoryScreen
import com.app.stockmaster.ui.newproduct.NewProductScreen

import com.app.stockmaster.ui.analytics.AnalyticsScreen
import com.app.stockmaster.ui.settings.SettingsScreen
import com.app.stockmaster.ui.scanner.BarcodeScannerScreen
import com.app.stockmaster.viewmodel.InventoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController = navController) }
            composable(
                route = Screen.Inventory.routeDefinition,
                arguments = listOf(navArgument("filter") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) {
                InventoryScreen(
                    onItemClick = { itemId ->
                        navController.navigate(Screen.StockAdjustment.createRoute(itemId))
                    },
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditProduct.createRoute(itemId))
                    },
                    onAddNewProduct = {
                        navController.navigate(Screen.NewProduct.route)
                    },
                    onScanClick = {
                        navController.navigate(Screen.Scanner.route)
                    },
                    navController = navController
                )
            }
            composable(Screen.NewProduct.route) {
                NewProductScreen(
                    onNavigateBack = { navController.popBackStack() },
                    navController = navController
                )
            }
            composable(
                route = Screen.EditProduct.route,
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) {
                NewProductScreen(
                    onNavigateBack = { navController.popBackStack() },
                    navController = navController
                )
            }
            composable(
                route = Screen.StockAdjustment.route,
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) {
                StockAdjustmentScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Scanner.route) {
                val inventoryViewModel: InventoryViewModel = hiltViewModel()
                val scope = rememberCoroutineScope()
                BarcodeScannerScreen(
                    onBarcodeScanned = { barcode ->
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        if (previousRoute == Screen.NewProduct.route || previousRoute?.startsWith("edit_product") == true) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("scan_result", barcode)
                            navController.popBackStack()
                        } else {
                            scope.launch {
                                val item = inventoryViewModel.findItemByBarcode(barcode)
                                if (item != null) {
                                    navController.navigate(Screen.StockAdjustment.createRoute(item.id)) {
                                        popUpTo(Screen.Inventory.route)
                                    }
                                } else {
                                    navController.previousBackStackEntry?.savedStateHandle?.set("scan_error", "O item com código $barcode não foi encontrado.")
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Dashboard", Screen.Dashboard.route, Icons.Default.Dashboard),
        BottomNavItem("Inventory", Screen.Inventory.route, Icons.Default.Inventory),
        BottomNavItem("Analytics", Screen.Analytics.route, Icons.Default.Analytics),
        BottomNavItem("Settings", Screen.Settings.route, Icons.Default.Settings)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.name) },
                label = { Text(item.name) },
                selected = currentDestination?.hierarchy?.any { 
                    it.route?.split("?")?.firstOrNull() == item.route 
                } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
