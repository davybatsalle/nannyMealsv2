package com.nannymeals.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Main screens
    object Home : Screen("home")
    object Meals : Screen("meals")
    object Children : Screen("children")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    
    // Detail/Form screens
    object AddChild : Screen("children/add")
    object EditChild : Screen("children/edit/{childId}") {
        fun createRoute(childId: Long) = "children/edit/$childId"
    }
    object AddMeal : Screen("meals/add?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "meals/add?date=$date" else "meals/add"
    }
    object EditMeal : Screen("meals/edit/{mealId}") {
        fun createRoute(mealId: Long) = "meals/edit/$mealId"
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        Screen.Home.route,
        "Accueil",
        Icons.Filled.Home
    )
    object Meals : BottomNavItem(
        Screen.Meals.route,
        "Repas",
        Icons.Filled.Restaurant
    )
    object Children : BottomNavItem(
        Screen.Children.route,
        "Enfants",
        Icons.Filled.ChildCare
    )
    object Reports : BottomNavItem(
        Screen.Reports.route,
        "Rapports",
        Icons.Filled.Assessment
    )
    object Settings : BottomNavItem(
        Screen.Settings.route,
        "Paramètres",
        Icons.Filled.Settings
    )
}
