package com.fdev.nutrimindai.core.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fdev.nutrimindai.R

sealed class BottomBarItem(
    val route: Any,
    val icon: ImageVector,
    val labelRes: Int
) {
    object Home : BottomBarItem(
        route = NavigationDestination.Home,
        icon = Icons.Rounded.Home,
        labelRes = R.string.nav_home
    )

    object Progress : BottomBarItem(
        route = NavigationDestination.Tracking,
        icon = Icons.Rounded.BarChart,
        labelRes = R.string.nav_progress
    )

    object Coach : BottomBarItem(
        route = NavigationDestination.Coach(),
        icon = Icons.Rounded.AutoAwesome,
        labelRes = R.string.nav_coach
    )

    object Activity : BottomBarItem(
        route = NavigationDestination.Insight,
        icon = Icons.AutoMirrored.Rounded.Assignment,
        labelRes = R.string.nav_activity
    )

    object Profile : BottomBarItem(
        route = NavigationDestination.Profile,
        icon = Icons.Rounded.Person,
        labelRes = R.string.nav_profile
    )
}

@Composable
fun NutriMindBottomBar(navController: NavController) {
    val items = listOf(
        BottomBarItem.Home,
        BottomBarItem.Progress,
        BottomBarItem.Coach,
        BottomBarItem.Activity,
        BottomBarItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        NavigationBar(
            modifier = Modifier.navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                val labelText = stringResource(item.labelRes)

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo<NavigationDestination.Home> {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = labelText,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
