package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.account.AccountScreen
import com.example.ui.details.MovieDetailsScreen
import com.example.ui.details.MovieDetailsViewModel
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.downloads.DownloadsViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.player.NeliPlayPlayerScreen
import com.example.ui.player.PlayerViewModel
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.example.ui.tv.TvScreen
import com.example.ui.tv.TvViewModel

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Filled.Home, Icons.Outlined.Home)
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Search : Screen("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
    object Tv : Screen("tv", "Live TV", Icons.Filled.LiveTv, Icons.Outlined.LiveTv)
    object Downloads : Screen("downloads", "Downloads", Icons.Filled.Download, Icons.Outlined.Download)
    object Library : Screen("library", "Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
    object Account : Screen("account", "Account", Icons.Filled.Person, Icons.Outlined.Person)

    // Sub-screens
    object MovieDetails : Screen("movie_details/{movieId}", "Details", Icons.Filled.Home, Icons.Outlined.Home) {
        fun createRoute(movieId: String) = "movie_details/$movieId"
    }

    object Player : Screen("player/{contentId}/{isLive}", "Player", Icons.Filled.Home, Icons.Outlined.Home) {
        fun createRoute(contentId: String, isLive: Boolean) = "player/$contentId/$isLive"
    }

    object Settings : Screen("settings", "Settings", Icons.Filled.Home, Icons.Outlined.Home)

    object Series : Screen("series", "Series", Icons.Filled.LiveTv, Icons.Outlined.LiveTv)
    object SeriesDetails : Screen("series_details/{seriesId}", "Series Details", Icons.Filled.LiveTv, Icons.Outlined.LiveTv) {
        fun createRoute(seriesId: String) = "series_details/$seriesId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Downloads,
    Screen.Library,
    Screen.Account
)

@Composable
fun NeliPlayApp(
    initialPlayMovieId: String? = null,
    initialNavigateMovieId: String? = null,
    showUpdateDialogOnStart: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val updateViewModel: com.example.update.ui.UpdateViewModel = viewModel()
    val updateState by updateViewModel.state.collectAsState()
    val isUpdateDialogVisible by updateViewModel.isDialogVisible.collectAsState()

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates(isManual = false)
    }

    LaunchedEffect(showUpdateDialogOnStart) {
        if (showUpdateDialogOnStart) {
            updateViewModel.showUpdateDialog()
        }
    }

    // Handle deep link / notification click intent
    LaunchedEffect(initialPlayMovieId) {
        if (!initialPlayMovieId.isNullOrBlank()) {
            navController.navigate(Screen.Player.createRoute(initialPlayMovieId, isLive = false))
        }
    }

    LaunchedEffect(initialNavigateMovieId) {
        if (!initialNavigateMovieId.isNullOrBlank()) {
            navController.navigate(Screen.MovieDetails.createRoute(initialNavigateMovieId))
        }
    }

    // Hide bottom bar on Player and Opening Splash Screen
    val isPlayerRoute = currentRoute?.startsWith("player/") == true
    val isSplashRoute = currentRoute == Screen.Splash.route
    val hideBottomBar = isPlayerRoute || isSplashRoute

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = !hideBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = NeliSurface.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 16.dp)
                        .testTag("bottom_navigation_bar"),
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeliCyanAccent,
                                selectedTextColor = NeliCyanAccent,
                                indicatorColor = NeliBluePrimary.copy(alpha = 0.2f),
                                unselectedIconColor = NeliTextSecondary,
                                unselectedTextColor = NeliTextSecondary
                            )
                        )
                    }
                }
            }
        },
        containerColor = NeliVoid,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (!hideBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                val homeVm: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeVm,
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetails.createRoute(movieId))
                    },
                    onWatchClick = { movieId ->
                        navController.navigate(Screen.Player.createRoute(movieId, isLive = false))
                    },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onTvClick = { navController.navigate(Screen.Tv.route) },
                    onSeriesClick = { seriesId ->
                        navController.navigate(Screen.SeriesDetails.createRoute(seriesId))
                    },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Search.route) {
                val searchVm: SearchViewModel = viewModel()
                SearchScreen(
                    viewModel = searchVm,
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetails.createRoute(movieId))
                    },
                    onSeriesClick = { seriesId ->
                        navController.navigate(Screen.SeriesDetails.createRoute(seriesId))
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Player.createRoute(channelId, isLive = true))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Tv.route) {
                val tvVm: TvViewModel = viewModel()
                TvScreen(
                    viewModel = tvVm,
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Player.createRoute(channelId, isLive = true))
                    }
                )
            }

            composable(Screen.Downloads.route) {
                val downloadsVm: DownloadsViewModel = viewModel()
                DownloadsScreen(
                    viewModel = downloadsVm,
                    onPlayMovie = { movieId ->
                        navController.navigate(Screen.Player.createRoute(movieId, isLive = false))
                    },
                    onBrowseMovies = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Library.route) {
                val libraryVm: LibraryViewModel = viewModel()
                LibraryScreen(
                    viewModel = libraryVm,
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetails.createRoute(movieId))
                    },
                    onWatchClick = { movieId ->
                        navController.navigate(Screen.Player.createRoute(movieId, isLive = false))
                    },
                    onBrowseMovies = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(
                route = Screen.MovieDetails.route,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                val detailsVm: MovieDetailsViewModel = viewModel()
                MovieDetailsScreen(
                    movieId = movieId,
                    viewModel = detailsVm,
                    onWatchClick = { id ->
                        navController.navigate(Screen.Player.createRoute(id, isLive = false))
                    },
                    onMovieClick = { id ->
                        navController.navigate(Screen.MovieDetails.createRoute(id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("contentId") { type = NavType.StringType },
                    navArgument("isLive") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val contentId = backStackEntry.arguments?.getString("contentId") ?: ""
                val isLive = backStackEntry.arguments?.getBoolean("isLive") ?: false
                val playerVm: PlayerViewModel = viewModel()
                NeliPlayPlayerScreen(
                    contentId = contentId,
                    isLive = isLive,
                    viewModel = playerVm,
                    onBack = { navController.popBackStack() },
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.Player.createRoute(movieId, isLive = false))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                val settingsVm: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsVm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Series.route) {
                val seriesVm: com.example.ui.series.SeriesViewModel = viewModel()
                com.example.ui.series.SeriesScreen(
                    viewModel = seriesVm,
                    onSeriesClick = { seriesId ->
                        navController.navigate(Screen.SeriesDetails.createRoute(seriesId))
                    },
                    onSearchClick = { navController.navigate(Screen.Search.route) }
                )
            }

            composable(
                route = Screen.SeriesDetails.route,
                arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
            ) { backStackEntry ->
                val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
                val seriesDetailsVm: com.example.ui.series.SeriesDetailsViewModel = viewModel()
                com.example.ui.series.SeriesDetailsScreen(
                    seriesId = seriesId,
                    viewModel = seriesDetailsVm,
                    onPlayEpisode = { episodeId ->
                        navController.navigate(Screen.Player.createRoute(episodeId, isLive = false))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Account.route) {
                AccountScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Library.route) },
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) }
                )
            }
        }
    }

    if (isUpdateDialogVisible) {
        com.example.update.ui.UpdateDialog(
            state = updateState,
            onUpdateClick = { info -> updateViewModel.startDownload(info) },
            onCancelDownload = { updateViewModel.cancelDownload() },
            onRetryInstall = {
                val cur = updateState
                if (cur is com.example.update.model.UpdateState.Downloaded) {
                    updateViewModel.triggerInstallation(cur.info, cur.apkFile)
                } else if (cur is com.example.update.model.UpdateState.PermissionRequired) {
                    updateViewModel.triggerInstallation(cur.info, cur.apkFile)
                }
            },
            onOpenPermissionSettings = { updateViewModel.openUnknownAppSourcesSettings() },
            onDismiss = { updateViewModel.dismissDialog() }
        )
    }
}
