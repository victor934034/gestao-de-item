package com.app.stockmaster.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Inventory : Screen("inventory")
    object NewProduct : Screen("new_product")
    object StockAdjustment : Screen("stock_adjustment/{itemId}") {
        fun createRoute(itemId: Int) = "stock_adjustment/$itemId"
    }
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
}
