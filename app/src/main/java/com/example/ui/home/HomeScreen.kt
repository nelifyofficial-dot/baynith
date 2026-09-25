package com.example.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cast.NeliPlayCastButton
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HeroBannerSlider
import com.example.ui.components.MovieCard
import com.example.ui.components.NeliPlayLogo
import com.example.ui.components.NeliPullRefreshBox
import com.example.ui.components.SectionHeader
import com.example.ui.components.WidgetPinPromptDialog
import com.example.ui.series.SeriesCard
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVioletNeon
import com.example.ui.theme.NeliVoid
import com.example.widget.NeliPlayWidgetProvider

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (String) -> Unit,
    onWatchClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onTvClick: () -> Unit,
    onSeriesClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPremiumClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Sync newest movie with the Home Screen Widget
    LaunchedEffect(uiState.latestMovies, uiState.featuredMovies) {
        val firstMovie = uiState.latestMovies.firstOrNull() ?: uiState.featuredMovies.firstOrNull()
        if (firstMovie != null) {
            NeliPlayWidgetProvider.updateLatestMovie(context, firstMovie.title, firstMovie.id)
        }
    }

    // Prompt user on installation / first run to allow our home screen widget
    val prefs = remember { context.getSharedPreferences("neliplay_widget_prefs", Context.MODE_PRIVATE) }
    var showWidgetPrompt by remember {
        mutableStateOf(!prefs.getBoolean("widget_prompt_handled", false))
    }

    if (showWidgetPrompt) {
        WidgetPinPromptDialog(
            onDismiss = {
                prefs.edit().putBoolean("widget_prompt_handled", true).apply()
                showWidgetPrompt = false
            },
            onAllow = {
                prefs.edit()
                    .putBoolean("widget_prompt_handled", true)
                    .putBoolean("widget_pinned", true)
                    .apply()
                showWidgetPrompt = false
            }
        )
    }

    // Category Tabs from the design
    val categories = listOf("Trending", "New", "Movies", "Series", "TV Show", "Swahili DJs", "Action", "Comedy")
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeliVoid)
                    .statusBarsPadding()
            ) {
                // Top Bar: EXACTLY Logo on left, and Cast, Search, Premium on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Logo
                    NeliPlayLogo(size = 36)

                    // 2. Actions: Cast, Search, Premium (neatly spaced)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cast Button with rounded glass container
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x18FFFFFF))
                                .border(1.dp, Color(0x228B5CF6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            NeliPlayCastButton(modifier = Modifier.size(24.dp))
                        }

                        // Search Button with rounded glass container
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x18FFFFFF))
                                .border(1.dp, Color(0x228B5CF6), CircleShape)
                                .clickable(onClick = onSearchClick)
                                .testTag("top_search_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // VIP button hidden for now as requested
                    }
                }

                // Horizontal Category Navigation Tabs
                val tabScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabScrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEachIndexed { index, title ->
                        val isSelected = selectedCategoryIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCategoryIndex = index
                                    if (title == "TV Show") {
                                        onTvClick()
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else NeliTextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .height(2.5.dp)
                                        .width(18.dp)
                                        .clip(CircleShape)
                                        .background(NeliVioletNeon)
                                )
                            } else {
                                Box(modifier = Modifier.height(2.5.dp))
                            }
                        }
                    }
                }
            }
        },
        containerColor = NeliVoid,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NeliPullRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTablet = maxWidth >= 600.dp
                val contentPadding = if (isTablet) 32.dp else 16.dp

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeliCyanAccent)
                    }
                } else if (uiState.featuredMovie == null && uiState.trendingMovies.isEmpty() && uiState.trendingSeries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val hasError = !uiState.errorMessage.isNullOrBlank()
                        EmptyStateView(
                            title = if (hasError) "Hitilafu Katika Kupakia" else "Hakuna Filamu Kwa Sasa",
                            message = uiState.errorMessage ?: "Filamu na tamthilia zitaonekana hapa pindi zitakapochapishwa.",
                            actionButtonText = "Tazama Live TV",
                            onActionClick = onTvClick
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        // 1. Centered Hero Card Carousel
                        val sliderMovies = if (uiState.featuredMovies.isNotEmpty()) {
                            uiState.featuredMovies
                        } else {
                            listOfNotNull(uiState.featuredMovie)
                        }

                        if (sliderMovies.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                HeroBannerSlider(
                                    movies = sliderMovies,
                                    favoritesIds = uiState.favoritesIds,
                                    onWatchClick = onWatchClick,
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onDetailsClick = onMovieClick
                                )
                            }
                        }

                        // 2. Continue Watching (if present)
                        if (uiState.continueWatching.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(title = "Endelea Kutazama")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = contentPadding),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(uiState.continueWatching, key = { it.movieId }) { item ->
                                        ContinueWatchingCard(
                                            item = item,
                                            onClick = { onWatchClick(item.movieId) },
                                            onRemove = { viewModel.removeContinueWatching(item.movieId) }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. "For You" / Zilizopendekezwa (Matching Mockup image 2 bottom section)
                        item {
                            Spacer(modifier = Modifier.height(26.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = contentPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "For You",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "See all",
                                    color = NeliVioletNeon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { onSearchClick() }
                                        .padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val forYouMovies = if (uiState.trendingMovies.isNotEmpty()) {
                                uiState.trendingMovies
                            } else {
                                uiState.latestMovies
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = contentPadding),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(forYouMovies, key = { it.id }) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = { onMovieClick(movie.id) }
                                    )
                                }
                            }
                        }

                        // Banner ad after "For You" section (separated by content)
                        item {
                            Spacer(modifier = Modifier.height(18.dp))
                            com.example.ads.NeliAdBanner(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = contentPadding)
                            )
                        }

                        // 4. Swahili Movies & Ma-DJ
                        val swahiliList = uiState.trendingMovies.filter { it.isSwahili }
                        if (swahiliList.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(title = "Filamu za Kiswahili • Ma-DJ")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = contentPadding),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(swahiliList, key = { it.id }) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = { onMovieClick(movie.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Trending TV Series
                        if (uiState.trendingSeries.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(title = "Tamthilia Zinazovuma")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = contentPadding),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(uiState.trendingSeries, key = { it.id }) { series ->
                                        SeriesCard(
                                            series = series,
                                            onClick = { onSeriesClick(series.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Latest Movies
                        if (uiState.latestMovies.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(title = "Mpya Zaidi (Latest)")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = contentPadding),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(uiState.latestMovies, key = { it.id }) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = { onMovieClick(movie.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Dynamic Genre sections
                        uiState.genreSections.forEach { (genre, movies) ->
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(title = "$genre Movies")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = contentPadding),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(movies, key = { it.id }) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = { onMovieClick(movie.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
