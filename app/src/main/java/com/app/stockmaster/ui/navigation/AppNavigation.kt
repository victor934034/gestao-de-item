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
import com.app.stockmaster.ui.dashboard.DashboardScreen
import com.app.stockmaster.ui.inventory.InventoryScreen

import com.app.stockmaster.ui.adjustment.StockAdjustmentScreen
import com.app.stockmaster.ui.dashboard.DashboardScreen
import com.app.stockmaster.ui.inventory.InventoryScreen
import com.app.stockmaster.ui.newproduct.NewProductScreen

import com.app.stockmaster.ui.analytics.AnalyticsScreen
import com.app.stockmaster.ui.settings.SettingsScreen

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
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Inventory.route) {
                InventoryScreen(
                    onItemClick = { itemId ->
                        navController.navigate(Screen.StockAdjustment.createRoute(itemId))
                    },
                    onAddNewProduct = {
                        navController.navigate(Screen.NewProduct.route)
                    }
                )
            }
            composable(Screen.NewProduct.route) {
                NewProductScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.StockAdjustment.route,
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) {
                StockAdjustmentScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
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
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
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
