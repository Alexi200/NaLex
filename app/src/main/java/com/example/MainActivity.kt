package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ComicViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ComicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppHost(viewModel: ComicViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Verify if bottom navigation should be displayed
    val showingBottomDestinations = listOf("home", "explore", "favorites", "downloads", "settings")
    val shouldShowBottomBar = currentRoute in showingBottomDestinations

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    // Item 1: Beranda / Home
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "home") Icons.Default.Home else Icons.Outlined.Home,
                                contentDescription = "Beranda"
                            )
                        },
                        label = { Text("Beranda") },
                        modifier = Modifier.testTag("nav_item_home")
                    )

                    // Item 2: Jelajahi / Explore
                    NavigationBarItem(
                        selected = currentRoute == "explore",
                        onClick = {
                            navController.navigate("explore") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "explore") Icons.Default.Explore else Icons.Outlined.Explore,
                                contentDescription = "Jelajahi"
                            )
                        },
                        label = { Text("Jelajahi") },
                        modifier = Modifier.testTag("nav_item_explore")
                    )

                    // Item 3: Favorit / Collection
                    NavigationBarItem(
                        selected = currentRoute == "favorites",
                        onClick = {
                            navController.navigate("favorites") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "favorites") Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorit"
                            )
                        },
                        label = { Text("Favorit") },
                        modifier = Modifier.testTag("nav_item_favorites")
                    )

                    // Item 4: Unduhan / Storage
                    NavigationBarItem(
                        selected = currentRoute == "downloads",
                        onClick = {
                            navController.navigate("downloads") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "downloads") Icons.Default.CloudDownload else Icons.Outlined.CloudDownload,
                                contentDescription = "Unduhan"
                            )
                        },
                        label = { Text("Unduhan") },
                        modifier = Modifier.testTag("nav_item_downloads")
                    )

                    // Item 5: Pengaturan / Settings
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "settings") Icons.Default.Settings else Icons.Outlined.Settings,
                                contentDescription = "Pengaturan"
                            )
                        },
                        label = { Text("Pengaturan") },
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(bottom = if (shouldShowBottomBar) innerPadding.calculateBottomPadding() else MinHeightForReaderPadding)
        ) {
            // Screen 1: Home
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onComicSelect = { id -> navController.navigate("detail/$id") }
                )
            }

            // Screen 2: Explore
            composable("explore") {
                ExploreScreen(
                    viewModel = viewModel,
                    onComicSelect = { id -> navController.navigate("detail/$id") }
                )
            }

            // Screen 3: Favorites
            composable("favorites") {
                FavoritesScreen(
                    viewModel = viewModel,
                    onComicSelect = { id -> navController.navigate("detail/$id") }
                )
            }

            // Screen 4: Downloads
            composable("downloads") {
                DownloadsScreen(
                    viewModel = viewModel,
                    onChapterSelect = { chId, comId -> navController.navigate("reader/$comId/$chId") }
                )
            }

            // Screen 5: Settings
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }

            // Detail screen
            composable(
                route = "detail/{comicId}",
                arguments = listOf(navArgument("comicId") { type = NavType.StringType })
            ) { backStackEntry ->
                val comicId = backStackEntry.arguments?.getString("comicId") ?: ""
                DetailScreen(
                    comicId = comicId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onChapterSelect = { chId -> navController.navigate("reader/$comicId/$chId") }
                )
            }

            // Reader screen
            composable(
                route = "reader/{comicId}/{chapterId}",
                arguments = listOf(
                    navArgument("comicId") { type = NavType.StringType },
                    navArgument("chapterId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val comicId = backStackEntry.arguments?.getString("comicId") ?: ""
                val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                ReaderScreen(
                    comicId = comicId,
                    chapterId = chapterId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// Fixed spacing padding boundary to support proper edge-to-edge drawing
private val MinHeightForReaderPadding = 0.dp
