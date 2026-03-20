package com.nannymeals.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nannymeals.app.ui.children.ChildFormScreen
import com.nannymeals.app.ui.children.ChildrenListScreen
import com.nannymeals.app.ui.home.HomeScreen
import com.nannymeals.app.ui.meals.MealFormScreen
import com.nannymeals.app.ui.meals.MealsListScreen
import com.nannymeals.app.ui.reports.ReportsScreen
import com.nannymeals.app.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NannyMealsNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Meals,
        BottomNavItem.Children,
        BottomNavItem.Reports,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if bottom nav should be shown
    val showBottomNav = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, maxLines = 1, softWrap = false) },
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
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier.padding(paddingValues)
        ) {
            // Main screens
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToMeals = {
                        navController.navigate(Screen.Meals.route)
                    },
                    onNavigateToChildren = {
                        navController.navigate(Screen.Children.route)
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route)
                    },
                    onNavigateToAddMeal = { date ->
                        navController.navigate(Screen.AddMeal.createRoute(date.toString()))
                    }
                )
            }

            composable(Screen.Meals.route) {
                MealsListScreen(
                    onNavigateToAddMeal = { date ->
                        navController.navigate(Screen.AddMeal.createRoute(date.toString()))
                    },
                    onNavigateToEditMeal = { mealId ->
                        navController.navigate(Screen.EditMeal.createRoute(mealId))
                    }
                )
            }

            composable(Screen.Children.route) {
                ChildrenListScreen(
                    onNavigateToAddChild = {
                        navController.navigate(Screen.AddChild.route)
                    },
                    onNavigateToEditChild = { childId ->
                        navController.navigate(Screen.EditChild.createRoute(childId))
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // Child form screens
            composable(Screen.AddChild.route) {
                ChildFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditChild.route,
                arguments = listOf(navArgument("childId") { type = NavType.LongType })
            ) {
                ChildFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Meal form screens
            composable(
                route = Screen.AddMeal.route,
                arguments = listOf(
                    navArgument("date") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                MealFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditMeal.route,
                arguments = listOf(navArgument("mealId") { type = NavType.LongType })
            ) {
                MealFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
