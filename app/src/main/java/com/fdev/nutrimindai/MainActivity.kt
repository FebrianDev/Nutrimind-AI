package com.fdev.nutrimindai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.fdev.nutrimindai.core.designsystem.NutriMindAITheme
import com.fdev.nutrimindai.core.navigation.NavigationDestination
import com.fdev.nutrimindai.core.navigation.RootNavGraph
import com.fdev.nutrimindai.core.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.isSystemInDarkTheme
import com.fdev.nutrimindai.core.datastore.AppThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value
        }

        enableEdgeToEdge()
        viewModel.handleIntent(intent)

        setContent {
            val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
            val pendingDeepLink by viewModel.pendingDeepLink.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            NutriMindAITheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    RootNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )

                    // Handle Notification Deep Links
                    LaunchedEffect(pendingDeepLink, startDestination) {
                        val destination = pendingDeepLink
                        if (!destination.isNullOrBlank() && startDestination == NavigationDestination.Home) {
                            when (destination) {
                                NotificationHelper.NAV_DEST_BREAKFAST -> navController.navigate(NavigationDestination.MealDetail("Breakfast"))
                                NotificationHelper.NAV_DEST_LUNCH -> navController.navigate(NavigationDestination.MealDetail("Lunch"))
                                NotificationHelper.NAV_DEST_DINNER -> navController.navigate(NavigationDestination.MealDetail("Dinner"))
                                NotificationHelper.NAV_DEST_WATER -> navController.navigate(NavigationDestination.Water)
                                NotificationHelper.NAV_DEST_DAILY_REVIEW -> {
                                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    navController.navigate(NavigationDestination.DailyReview(todayStr))
                                }
                            }
                            viewModel.clearPendingDeepLink()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }
}
