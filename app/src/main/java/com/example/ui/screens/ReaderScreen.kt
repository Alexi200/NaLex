package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.viewmodel.ComicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    comicId: String,
    chapterId: String,
    viewModel: ComicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Load active chapter details
    LaunchedEffect(comicId, chapterId) {
        viewModel.selectChapter(chapterId, comicId)
    }

    val activeChapter by viewModel.activeChapter.collectAsState()
    val activePages by viewModel.activePages.collectAsState()
    val activePageIndex by viewModel.activePageIndex.collectAsState()

    // Reading options
    val readerOrientation by viewModel.readerOrientation.collectAsState() // Webtoon, Vertical, Horizontal
    val imageQuality by viewModel.imageQuality.collectAsState()

    // Overlay State Visibility
    var showOverlays by remember { mutableStateOf(true) }

    // Dropdown Settings State
    var showMenuSettings by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reader_screen_container")
    ) {
        if (activePages.isEmpty()) {
            // Loading State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Memuat Halaman...",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // Immersive Reader content based on setting
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showOverlays = !showOverlays
                    }
            ) {
                when (readerOrientation) {
                    "Horizontal" -> {
                        val pagerState = rememberPagerState(
                            initialPage = if (activePageIndex < activePages.size) activePageIndex else 0,
                            pageCount = { activePages.size }
                        )

                        // Update page index in model when swipe completes
                        LaunchedEffect(pagerState.currentPage) {
                            val title = activeChapter?.title ?: "Chapter"
                            viewModel.updateActivePage(pagerState.currentPage, comicId, chapterId, title)
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            pageSpacing = 8.dp
                        ) { pageIdx ->
                            val imageUrl = activePages[pageIdx]
                            ReaderPageImage(
                                imageUrl = imageUrl,
                                quality = imageQuality,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        // Webtoon (Continuous vertical column) / Vertical modes
                        val listState = rememberLazyListState(
                            initialFirstVisibleItemIndex = if (activePageIndex < activePages.size) activePageIndex else 0
                        )

                        // Monitor the visible page to report index to Room db history
                        val firstVisibleItem by remember { derivedStateOf { listState.firstVisibleItemIndex } }
                        LaunchedEffect(firstVisibleItem) {
                            if (activePages.isNotEmpty() && firstVisibleItem < activePages.size) {
                                val title = activeChapter?.title ?: "Chapter"
                                viewModel.updateActivePage(firstVisibleItem, comicId, chapterId, title)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(activePages) { pageIdx, imageUrl ->
                                ReaderPageImage(
                                    imageUrl = imageUrl,
                                    quality = imageQuality,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                )
                                if (readerOrientation == "Vertical" && pageIdx < activePages.lastIndex) {
                                    Spacer(
                                        modifier = Modifier
                                            .height(300.dp)
                                            .fillMaxWidth()
                                            .background(Color.Black)
                                    )
                                }
                            }

                            // Finished Footer Prompt
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Chapter Selesai Dibaca!",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onBack,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text("Kembali")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Unifying UI overlays
            AnimatedVisibility(
                visible = showOverlays,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopReaderBar(
                    title = activeChapter?.title ?: "Membaca Komik",
                    comicSubtitle = comicId.substringAfter(":").replace("_", " ").capitalizeTitle(),
                    onBack = onBack,
                    onOpenSettings = { showMenuSettings = !showMenuSettings }
                )
            }

            // Floating Index indicator when overlays are hidden
            if (!showOverlays) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Halaman ${activePageIndex + 1} / ${activePages.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom controls overlay
            AnimatedVisibility(
                visible = showOverlays,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomReaderBar(
                    currentPage = activePageIndex + 1,
                    totalPages = activePages.size,
                    readerOrientation = readerOrientation,
                    imageQuality = imageQuality,
                    onChangeOrientation = { viewModel.setReaderOrientation(it) },
                    onChangeQuality = { viewModel.setImageQuality(it) }
                )
            }
        }
    }
}

@Composable
fun ReaderPageImage(
    imageUrl: String,
    quality: String,
    modifier: Modifier = Modifier
) {
    // Quality compression mapper using local sizing tags
    val suffix = when (quality) {
        "Low" -> "&w=400&q=40"
        "Medium" -> "&w=700&q=65"
        else -> "&w=1000&q=90"
    }
    val finalUrl = imageUrl.substringBefore("&w=") + suffix

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(finalUrl)
            .crossfade(true)
            .build(),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.DarkGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.BrokenImage, null, tint = Color.Red, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Gagal memuat halaman", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        contentDescription = "Halaman Komik",
        contentScale = ContentScale.FillWidth,
        modifier = modifier
    )
}

@Composable
fun TopReaderBar(
    title: String,
    comicSubtitle: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("reader_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = comicSubtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan Membaca",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun BottomReaderBar(
    currentPage: Int,
    totalPages: Int,
    readerOrientation: String,
    imageQuality: String,
    onChangeOrientation: (String) -> Unit,
    onChangeQuality: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                )
            )
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Text(
                    text = "Halaman $currentPage dari $totalPages",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Quick shortcuts label
                Text(
                    text = "Arah: $readerOrientation | Kualitas: $imageQuality",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Settings buttons indicators row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Orientation Toggle
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Orientasi", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Webtoon", "Vertical", "Horizontal").forEach { mode ->
                                val active = readerOrientation == mode
                                Button(
                                    onClick = { onChangeOrientation(mode) },
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f),
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else Color.White
                                    ),
                                    modifier = Modifier.weight(1f).height(28.dp)
                                ) {
                                    Text(mode.substring(0, 3), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Quick Quality Toggle
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Kualitas Gambar", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Low", "Medium", "High").forEach { q ->
                                val active = imageQuality == q
                                Button(
                                    onClick = { onChangeQuality(q) },
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f),
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else Color.White
                                    ),
                                    modifier = Modifier.weight(1f).height(28.dp)
                                ) {
                                    Text(q, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers for Title Case
private fun String.capitalizeTitle(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
